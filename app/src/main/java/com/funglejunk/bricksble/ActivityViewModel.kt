package com.funglejunk.bricksble

import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.funglejunk.bricksble.ble.BleCallback
import com.funglejunk.bricksble.ble.BleProxy
import com.funglejunk.bricksble.ble.LegoConnectProcedure
import com.funglejunk.bricksble.util.addTo
import com.funglejunk.bricksble.msgtypes.HubProperty
import com.funglejunk.bricksble.msgtypes.PortOutputCmd
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposables
import io.reactivex.internal.disposables.SequentialDisposable
import timber.log.Timber

class ActivityViewModel : ViewModel() {

    sealed class Event {
        object Connected : Event()
        object Disconnected : Event()
        data class BatteryLevelReport(val level: Int) : Event()
        data class RssiLevelReport(val level: Byte) : Event()
    }

    sealed class Command {
        data class Connect(val context: Context, val adapter: BluetoothAdapter) : Command()
        object Disconnect : Command()
        object CleanUpDisposables : Command()
        object StopScan : Command()
    }

    sealed class ViewAction {
        object DriveForward : ViewAction()
        object DriveBackward : ViewAction()
        object DriveLeft : ViewAction()
        object DriveRight : ViewAction()
        object StopDriving : ViewAction()
        data class SetLed(val color: PortOutputCmd.ColorByte) : ViewAction()
    }

    private val liveData: MutableLiveData<Event> by lazy {
        MutableLiveData<Event>()
    }

    private var bleProxy: BleProxy? = null
    private val connectionDisposable = SequentialDisposable()
    private val notificationsDisposable = SequentialDisposable()
    private val commandDisposables = CompositeDisposable()

    fun getLiveData(): LiveData<Event> = liveData

    override fun onCleared() {
        bleProxy?.let { proxy ->
            disconnect(proxy)
        }
        cleanUpDisposables()
        super.onCleared()
    }

    fun command(command: Command) = when (command) {
        is Command.Connect -> connect(command.adapter, command.context)
        Command.Disconnect -> bleProxy?.let { disconnect(it) }
        Command.CleanUpDisposables -> cleanUpDisposables()
        Command.StopScan -> bleProxy?.stopScan()
    }

    fun viewAction(viewAction: ViewAction) = bleProxy?.let { proxy ->
        when (viewAction) {
            ViewAction.DriveBackward -> {
                proxy.writeLegoCharacteristic(
                    Encoder.Motor.startSpeed(
                        Ports.INT_MOTOR_W_TACHO_VIRT, -100, 100
                    )
                )?.addTo(commandDisposables)
            }
            ViewAction.DriveForward -> {
                proxy.writeLegoCharacteristic(
                    Encoder.Motor.startSpeed(
                        Ports.INT_MOTOR_W_TACHO_VIRT, 100, 100
                    )
                )?.addTo(commandDisposables)
            }
            ViewAction.DriveLeft -> {
                proxy.writeLegoCharacteristic(
                    Encoder.Motor.startSpeedLr(Ports.INT_MOTOR_W_TACHO_VIRT, 0, 100, 100)
                )?.addTo(commandDisposables)
            }
            ViewAction.DriveRight -> {
                proxy.writeLegoCharacteristic(
                    Encoder.Motor.startSpeedLr(Ports.INT_MOTOR_W_TACHO_VIRT, 100, 0, 100)
                )?.addTo(commandDisposables)
            }
            ViewAction.StopDriving -> {
                proxy.writeLegoCharacteristic(
                    Encoder.Motor.startSpeed(Ports.INT_MOTOR_W_TACHO_VIRT, 0, 0)
                )?.addTo(commandDisposables)
            }
            is ViewAction.SetLed -> {
                proxy.writeLegoCharacteristic(
                    Encoder.Led.setColor(viewAction.color)
                )?.addTo(commandDisposables)
            }
        }
    }

    private fun connect(adapter: BluetoothAdapter, context: Context) {
        bleProxy = BleProxy(adapter)
        bleProxy?.let { proxy ->
            Timber.d("init connecting")
            LegoConnectProcedure(proxy).start(
                context,
                {
                    Timber.d("connect procedure success!")
                    connected(proxy)
                },
                {
                    Timber.e("connect procedure failed: $it")
                    disconnect(proxy)
                }
            )
        }
    }

    private fun connected(bleProxy: BleProxy) {
        bleProxy.writeLegoCharacteristic(
            Encoder.Updates.getFor<Int>(HubProperty.Property.BATTERY_VOLTAGE)
        )?.addTo(commandDisposables)

        bleProxy.writeLegoCharacteristic(
            Encoder.Updates.getFor<Int>(HubProperty.Property.RSSI)
        )?.addTo(commandDisposables)

        listenForConnectionState()
        listenForNotifications()

        liveData.postValue(Event.Connected)
    }

    private fun listenForNotifications() {
        notificationsDisposable.update(
            bleProxy?.receiveNotifications()?.subscribe(
                {
                    Timber.d("received notification event: $it")
                    when (it) {
                        is BleCallback.BleEvent.NotificationEvent.Data -> {
                            onNewNotification(it)
                        }
                        is BleCallback.BleEvent.NotificationEvent.Error -> {
                            Timber.w("received notification error")
                        }
                    }
                },
                { Timber.e("$it") }
            )
        )
    }

    private fun onNewNotification(data: BleCallback.BleEvent.NotificationEvent.Data) {
        if (data.value.isNotEmpty()) {
            try {
                val message = com.funglejunk.bricksble.LegoMessage.UpstreamMessage.decode(data.value)
                Timber.w("new msg: ${message.content} / ${data.value.joinToString {
                    "0x${com.funglejunk.bricksble.byteToHex(it)}"
                }}")
                message.onHubPropertyMessage<HubProperty.Uint8Payload>(HubProperty.Property.BATTERY_VOLTAGE) {
                    val payload = this
                    liveData.postValue(
                        Event.BatteryLevelReport(
                            payload.content as Int
                        )
                    )
                }

                message.onHubPropertyMessage<HubProperty.Int8Payload>(HubProperty.Property.RSSI) {
                    val payload = this
                    liveData.postValue(
                        Event.RssiLevelReport(
                            payload.content as Byte
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e("exception parsing notification: ${data.value.joinToString {
                    "0x${com.funglejunk.bricksble.byteToHex(it)}"
                }}: $e")
            }
        } else {
            Timber.w("empty data")
        }
    }

    private fun listenForConnectionState() {
        bleProxy?.let { proxy ->
            connectionDisposable.update(
                proxy.connectionStatusObservable.subscribe {
                    Timber.d("connection callback in status observable: $it")
                    when (it) {
                        is BleCallback.BleEvent.Connect.Connected -> {
                            liveData.postValue(Event.Connected)
                        }
                        is BleCallback.BleEvent.Connect.Disconnected -> {
                            liveData.postValue(Event.Disconnected)
                        }
                        is BleCallback.BleEvent.Connect.Error -> {
                            liveData.postValue(Event.Connected)
                        }
                    }
                }
            )
        }
    }

    private fun disconnect(proxy: BleProxy) {
        connectionDisposable.update(
            proxy.disconnect().subscribe(
                {
                    cleanUpDisposables()
                    liveData.postValue(Event.Disconnected)
                },
                { Timber.e("error intentional disconnect: $it") }
            )
        )
    }

    private fun cleanUpDisposables() {
        connectionDisposable.update(Disposables.empty())
        notificationsDisposable.update(Disposables.empty())
        commandDisposables.clear()
    }

}