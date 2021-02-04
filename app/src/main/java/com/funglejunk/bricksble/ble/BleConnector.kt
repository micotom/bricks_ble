package com.funglejunk.bricksble.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class BleConnector(private val callback: BleCallback) {

    var gatt: BluetoothGatt? = null
        private set

    fun connect(device: BluetoothDevice, context: Context?): Single<BleCallback.BleEvent.Connect> =
        gatt?.close()
            .also {
                gatt = null
            }
            .run {
                Single.fromCallable {
                    device.connectGatt(context, false, callback)
                }.flatMapObservable {
                    callback.connectionObservable
                }.take(1).singleOrError().doAfterSuccess {
                    if (it is BleCallback.BleEvent.Connect.Connected) {
                        gatt = it.gatt
                    }
                }
            }


    fun disconnect(): Single<BleCallback.BleEvent.Connect> =
        Observable.fromCallable {
            gatt?.disconnect()
        }.observeOn(Schedulers.io())
            .flatMap {
                if (gatt == null) {
                    Observable.error<BleCallback.BleEvent.Connect>(
                        IllegalStateException("Null gatt on disconnect")
                    )
                } else {
                    callback.connectionObservable
                }
            }.take(1).singleOrError()

    fun close(gatt: BluetoothGatt) = gatt.close().also {
        this.gatt = null
    }

}