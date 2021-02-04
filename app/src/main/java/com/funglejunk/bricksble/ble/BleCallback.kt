package com.funglejunk.bricksble.ble

import android.bluetooth.*
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.*

class BleCallback : BluetoothGattCallback() {

    sealed class BleEvent {

        abstract val gatt: BluetoothGatt

        sealed class Connect : BleEvent() {
            data class Connected(override val gatt: BluetoothGatt) : Connect()
            data class Disconnected(override val gatt: BluetoothGatt) : Connect()
            data class Error(override val gatt: BluetoothGatt, val status: Int) : Connect()
        }

        sealed class Discovery : BleEvent() {
            data class ServicesDiscovered(
                override val gatt: BluetoothGatt,
                val services: List<BluetoothGattService>
            ) : Discovery()

            data class Error(override val gatt: BluetoothGatt, val status: Int) : Discovery()
        }

        sealed class CharacteristicEvent : BleEvent() {
            data class CharacteristicRead(
                override val gatt: BluetoothGatt,
                val uuid: UUID,
                val value: ByteArray
            ) : CharacteristicEvent()

            data class CharacteristicWritten(
                override val gatt: BluetoothGatt,
                val characteristic: BluetoothGattCharacteristic
            ) : CharacteristicEvent()

            data class Error(override val gatt: BluetoothGatt, val status: Int) : CharacteristicEvent()
        }

        sealed class NotificationEvent : BleEvent() {
            data class Data(override val gatt: BluetoothGatt, val value: ByteArray) : NotificationEvent() {
                override fun toString(): String {
                    return "Notification: ${value.joinToString { "0x${com.funglejunk.bricksble.byteToHex(
                        it
                    )}"}}"
                }
            }
            data class Error(override val gatt: BluetoothGatt) : NotificationEvent()
        }

        sealed class DescriptorEvent : BleEvent() {
            data class DescriptorRead(
                override val gatt: BluetoothGatt,
                val uuid: UUID,
                val value: ByteArray
            ) : DescriptorEvent()

            data class DescriptorWritten(
                override val gatt: BluetoothGatt,
                val descriptor: BluetoothGattDescriptor
            ) : DescriptorEvent()

            data class Error(override val gatt: BluetoothGatt, val status: Int) : DescriptorEvent()
        }

    }


    private val connectionSubject: Subject<BleEvent.Connect> = PublishSubject.create()
    val connectionObservable: Observable<BleEvent.Connect> = connectionSubject.hide()

    private val discoverySubject: Subject<BleEvent.Discovery> = PublishSubject.create()
    val discoveryObservable: Observable<BleEvent.Discovery> = discoverySubject.hide()

    private val characteristicSubject: Subject<BleEvent.CharacteristicEvent> = PublishSubject.create()
    val characteristicObservable: Observable<BleEvent.CharacteristicEvent> = characteristicSubject.hide()

    private val descriptorSubject: Subject<BleEvent.DescriptorEvent> = PublishSubject.create()
    val descriptorObservable: Observable<BleEvent.DescriptorEvent> = descriptorSubject.hide()

    private val notificationSubject: Subject<BleEvent.NotificationEvent> = PublishSubject.create()
    val notificationsObservable: Observable<BleEvent.NotificationEvent> = notificationSubject.hide()

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        log("onConnectionStateChange(): ${newState.gattConnectionStatusString()} (newState) -> ${status.gattStatusString()} (state)")
        gatt.catchNull { safeGatt ->
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED ->
                        connectionSubject.onNext(
                            BleEvent.Connect.Connected(
                                safeGatt
                            )
                        )
                    BluetoothProfile.STATE_DISCONNECTED ->
                        connectionSubject.onNext(
                            BleEvent.Connect.Disconnected(
                                safeGatt
                            )
                        )
                }
            } else {
                connectionSubject.onNext(
                    BleEvent.Connect.Error(
                        safeGatt,
                        status
                    )
                )
                safeGatt.close()
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        log("onServicesDiscovered(): -> ${status.gattStatusString()}")
        gatt.catchNull { safeGatt ->
            if (status == BluetoothGatt.GATT_SUCCESS) {
                discoverySubject.onNext(
                    BleEvent.Discovery.ServicesDiscovered(
                        safeGatt,
                        safeGatt.services
                    )
                )
            } else {
                discoverySubject.onNext(
                    BleEvent.Discovery.Error(
                        safeGatt,
                        status
                    )
                )
            }
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        log("onCharacteristicRead(): ${status.gattStatusString()}")
        gatt.catchNull { safeGatt ->
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic?.let { c ->
                    val copy = c.copy()
                    characteristicSubject.onNext(
                        BleEvent.CharacteristicEvent.CharacteristicRead(
                            safeGatt,
                            copy.uuid, copy.value
                        )
                    )
                } ?: {
                    characteristicSubject.onNext(
                        BleEvent.CharacteristicEvent.Error(
                            safeGatt,
                            status
                        )
                    )
                }()
            } else {
                characteristicSubject.onNext(
                    BleEvent.CharacteristicEvent.Error(
                        safeGatt,
                        status
                    )
                )
            }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        log("onCharacteristicWrite(): ${status.gattStatusString()}")
        gatt.catchNull { safeGatt ->
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic?.let { _ ->
                    characteristicSubject.onNext(
                        BleEvent.CharacteristicEvent.CharacteristicWritten(
                            safeGatt, characteristic.copy()
                        )
                    )
                } ?: {
                    characteristicSubject.onNext(
                        BleEvent.CharacteristicEvent.Error(
                            safeGatt,
                            status
                        )
                    )
                }()
            } else {
                characteristicSubject.onNext(
                    BleEvent.CharacteristicEvent.Error(
                        safeGatt,
                        status
                    )
                )
            }
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        log("onDescriptorWrite(): ${status.gattStatusString()}")
        gatt.catchNull { safeGatt ->
            if (status == BluetoothGatt.GATT_SUCCESS) {
                descriptor?.let { _ ->
                    descriptorSubject.onNext(
                        BleEvent.DescriptorEvent.DescriptorWritten(
                            safeGatt, descriptor.copy()
                        )
                    )
                } ?: {
                    descriptorSubject.onNext(
                        BleEvent.DescriptorEvent.Error(
                            safeGatt,
                            status
                        )
                    )
                }()
            } else {
                descriptorSubject.onNext(
                    BleEvent.DescriptorEvent.Error(
                        safeGatt,
                        status
                    )
                )
            }
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        log("onCharacteristicChanged()")
        gatt.catchNull { safeGatt ->
            characteristic?.let { c ->
                notificationSubject.onNext(
                    BleEvent.NotificationEvent.Data(
                        safeGatt,
                        c.value
                    )
                )
            } ?: {
                notificationSubject.onNext(
                    BleEvent.NotificationEvent.Error(
                        safeGatt
                    )
                )
            }()
        }
    }

    private fun BluetoothGatt?.catchNull(
        ifNotNull: (BluetoothGatt) -> Unit
    ) = optionalFold(
        { Timber.e("Gatt is null!") },
        ifNotNull
    )

    private fun BluetoothGatt?.optionalFold(
        ifNull: () -> Unit,
        ifNotNull: (BluetoothGatt) -> Unit
    ): Unit = this?.let { ifNotNull(this) } ?: ifNull()

    private companion object {
        fun log(msg: String) = Timber.tag("GATT_CALLBACK").d(msg)
    }

    private fun BluetoothGattCharacteristic.copy(): BluetoothGattCharacteristic =
        BluetoothGattCharacteristic(
            uuid, properties, permissions
        ).apply {
            value = if (value != null) {
                Arrays.copyOf(this@copy.value, this@copy.value.size)
            } else {
                byteArrayOf()
            }
        }.apply {
            this@copy.descriptors.forEach { d ->
                descriptors.add(d.copy())
            }
            this@copy.service.copy(this)
        }

    private fun BluetoothGattDescriptor.copy(): BluetoothGattDescriptor =
        BluetoothGattDescriptor(
            uuid, permissions
        ).apply {
            if (value != null) {
                value = Arrays.copyOf(this@copy.value, this@copy.value.size)
            } else {
                value = byteArrayOf()
            }
        }

    private fun BluetoothGattService.copy(characteristic: BluetoothGattCharacteristic): BluetoothGattService =
        BluetoothGattService(
            uuid, type
        ).apply {
            addCharacteristic(characteristic)
        }

}