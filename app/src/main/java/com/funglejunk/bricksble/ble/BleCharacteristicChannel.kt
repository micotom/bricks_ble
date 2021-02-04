package com.funglejunk.bricksble.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class BleCharacteristicChannel(private val callback: BleCallback) {

    private val lock = Semaphore(1)

    private val scheduler = Schedulers.single()

    sealed class Command {
        abstract val gatt: BluetoothGatt

        data class ReadCharacteristic(
            override val gatt: BluetoothGatt,
            val characteristic: BluetoothGattCharacteristic
        ) : Command()

        data class WriteCharacteristic(
            override val gatt: BluetoothGatt,
            val characteristic: BluetoothGattCharacteristic,
            val content: ByteArray
        ) : Command() {
            override fun toString(): String = "Write: ${content.joinToString { "0x${com.funglejunk.bricksble.byteToHex(
                it
            )}" }}"
        }

        data class Notify(
            override val gatt: BluetoothGatt,
            val characteristic: BluetoothGattCharacteristic
        ) : Command()
    }

    fun close() {
        lock.release()
    }

    fun command(
        command: Command,
        onEvent: (BleCallback.BleEvent) -> Unit,
        onError: (Throwable) -> Unit
    ): Disposable =
        Single.just(true)
            .observeOn(scheduler)
            .subscribeOn(scheduler)
            .doOnEvent { _, _ ->
                Timber.d("handle command: $command")
                lock.acquire()
            }
            .flatMap {
                when (command) {
                    is Command.ReadCharacteristic -> {
                        Single.fromCallable {
                            command.gatt.readCharacteristic(command.characteristic)
                        }.flatMap {
                            callback.characteristicObservable.take(1).singleOrError()
                        }
                    }
                    is Command.WriteCharacteristic -> {
                        Single.fromCallable {
                            val c = command.characteristic.apply {
                                value = command.content
                                writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            }
                            val writeResult = command.gatt.writeCharacteristic(c)
                            Timber.d("characteristic write: $writeResult")
                            writeResult
                        }.flatMap { success ->
                            if (success) {
                                callback.characteristicObservable.take(1).singleOrError()
                            } else {
                                Single.just(
                                    BleCallback.BleEvent.CharacteristicEvent.Error(
                                        command.gatt,
                                        0xff
                                    )
                                )
                            }
                        }
                    }
                    is Command.Notify -> {
                        Single.fromCallable {
                            val c = command.characteristic
                            command.gatt.setCharacteristicNotification(c, true)
                            command.gatt to c
                        }
                            .delay(1L, TimeUnit.SECONDS)
                            .map { (updatedGatt, updatedCharacteristic) ->
                                val descriptor =
                                    updatedCharacteristic.getDescriptor(notifyDescriptorUuid)
                                        .apply {
                                            value =
                                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                        }
                                updatedGatt.writeDescriptor(descriptor)
                            }.flatMap { success ->
                                if (success) {
                                    Timber.d("descriptor write pre success")
                                    callback.descriptorObservable.take(1).singleOrError()
                                } else {
                                    Single.just(
                                        BleCallback.BleEvent.DescriptorEvent.Error(
                                            command.gatt,
                                            0x0
                                        )
                                    )
                                }
                            }
                    }
                }
            }.subscribe(
                {
                    Timber.d("write success: $command")
                    lock.release()
                    onEvent(it)
                },
                {
                    lock.release()
                    onError(it)
                }
            )


    /*
    kotlin.run {
        Timber.d("got command: $command")
        lock.acquire()
    }.run {
        when (command) {
            is Command.ReadCharacteristic -> {
                Single.fromCallable {
                    command.gatt.readCharacteristic(command.characteristic)
                }.flatMap {
                    callback.characteristicObservable.take(1).singleOrError()
                }
            }
            is Command.WriteCharacteristic -> {
                Single.fromCallable {
                    val c = command.characteristic.apply {
                        value = command.content
                        writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    }
                    val writeResult = command.gatt.writeCharacteristic(c)
                    Timber.d("characteristic write: $writeResult")
                    writeResult
                }.flatMap { success ->
                    if (success) {
                        callback.characteristicObservable.take(1).singleOrError()
                    } else {
                        Single.just(
                            BleCallback.BleEvent.CharacteristicEvent.Error(
                                command.gatt,
                                0xff
                            )
                        )
                    }
                }
            }
            is Command.Notify -> {
                Single.fromCallable {
                    val c = command.characteristic
                    command.gatt.setCharacteristicNotification(c, true)
                    command.gatt to c
                }
                    .delay(1L, TimeUnit.SECONDS)
                    .map { (updatedGatt, updatedCharacteristic) ->
                        val descriptor =
                            updatedCharacteristic.getDescriptor(notifyDescriptorUuid).apply {
                                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            }
                        updatedGatt.writeDescriptor(descriptor)
                    }.flatMap { success ->
                        if (success) {
                            Timber.d("descriptor write pre success")
                            callback.descriptorObservable.take(1).singleOrError()
                        } else {
                            Single.just(
                                BleCallback.BleEvent.DescriptorEvent.Error(
                                    command.gatt,
                                    0x0
                                )
                            )
                        }
                    }
            }
        }
    }.subscribe(
        {
            lock.release()
            onEvent(it)
        },
        {
            lock.release()
            onError(it)
        }
    )

     */

    private val notifyDescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

}