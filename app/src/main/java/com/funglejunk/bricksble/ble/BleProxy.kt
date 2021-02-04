package com.funglejunk.bricksble.ble

import android.bluetooth.*
import android.content.Context
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import timber.log.Timber


class BleProxy(adapter: BluetoothAdapter) {

    private val callback = BleCallback()

    private var discoverer =
        BleDiscoverer(adapter.bluetoothLeScanner)
    private val connector: BleConnector =
        BleConnector(callback)
    private val serviceDiscoverer: BleServiceDiscoverer =
        BleServiceDiscoverer(callback)
    private var characteristicChannel: BleCharacteristicChannel? = null

    val connectionStatusObservable: Observable<BleCallback.BleEvent.Connect> =
        callback.connectionObservable

    private var gatt: BluetoothGatt? = null

    fun scanForLegoHub(): Observable<BleDiscoverer.ScanEvent> =
        discoverer.also {
            it.stopScan()
        }.run {
            startScan()
        }

    fun stopScan(): Unit = discoverer.stopScan()

    fun connect(device: BluetoothDevice, context: Context): Single<BleCallback.BleEvent.Connect> =
        connector.connect(device, context).doOnEvent { t1, _ ->
            when (t1) {
                is BleCallback.BleEvent.Connect.Connected -> {
                    Timber.d("connected!")
                    this.gatt = t1.gatt
                    characteristicChannel?.close()
                    characteristicChannel =
                        BleCharacteristicChannel(
                            callback
                        )
                }
                is BleCallback.BleEvent.Connect.Disconnected -> {
                    connector.close(t1.gatt)
                    this.gatt = null
                }
                is BleCallback.BleEvent.Connect.Error -> {
                    connector.close(t1.gatt)
                }
            }
        }

    fun disconnect(): Completable =
        connector.disconnect().doOnEvent { t1, t2 ->
            when (t1) {
                is BleCallback.BleEvent.Connect.Connected -> Timber.w("connected after disconnect?")
                is BleCallback.BleEvent.Connect.Disconnected -> {
                    connector.close(t1.gatt)
                }
                is BleCallback.BleEvent.Connect.Error -> {
                    connector.close(t1.gatt)
                }
            }
        }.flatMapCompletable {
            Completable.complete()
        }

    fun discoverServices(gatt: BluetoothGatt): Single<BleCallback.BleEvent.Discovery> =
        serviceDiscoverer.discover(gatt).doOnEvent { t1, t2 ->
            when (t1) {
                is BleCallback.BleEvent.Discovery.Error -> {
                    connector.close(t1.gatt)
                }
            }
        }

    fun readCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        onEvent: (BleCallback.BleEvent) -> Unit,
        onError: (Throwable) -> Unit
    ) =
        characteristicChannel?.command(
            BleCharacteristicChannel.Command.ReadCharacteristic(
                gatt,
                characteristic
            ),
            onEvent, onError
        ) ?: {
            onError(IllegalStateException("No characteristic channel available"))
            Disposables.empty()
        }()

    fun writeLegoCharacteristic(
        value: ByteArray,
        onEvent: (BleCallback.BleEvent) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) = gatt?.let { safeGatt ->
        writeCharacteristic(
            safeGatt,
            safeGatt.getService(legoCustomServiceUuid).getCharacteristic(
                legoCharacteristicUuid
            ),
            value,
            onEvent,
            onError
        )
    }

    private fun writeCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        onEvent: (BleCallback.BleEvent) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) =
        characteristicChannel?.command(
            BleCharacteristicChannel.Command.WriteCharacteristic(
                gatt,
                characteristic,
                value
            ),
            onEvent, onError
        ).also {
            Timber.d("write bytes: ${value.joinToString {
                "0x${com.funglejunk.bricksble.byteToHex(it)}"
            }}")
        } ?: {
            onError(IllegalStateException("No characteristic channel available"))
            Disposables.empty()
        }()

    fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        onEvent: (BleCallback.BleEvent) -> Unit,
        onError: (Throwable) -> Unit
    ) =
        characteristicChannel?.command(
            BleCharacteristicChannel.Command.Notify(
                gatt,
                characteristic
            ),
            onEvent, onError
        ) ?: {
            onError(IllegalStateException("No characteristic channel available"))
            Disposables.empty()
        }()

    fun receiveNotifications(): Observable<BleCallback.BleEvent.NotificationEvent> =
        callback.notificationsObservable
}