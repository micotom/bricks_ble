package com.funglejunk.bricksble.ble

import android.bluetooth.BluetoothGatt
import io.reactivex.Observable
import io.reactivex.Single

class BleServiceDiscoverer(private val callback: BleCallback) {

    fun discover(gatt: BluetoothGatt): Single<BleCallback.BleEvent.Discovery> =
        Observable.fromCallable {
            gatt.discoverServices()
        }.flatMap {
            callback.discoveryObservable
        }.take(1).singleOrError()

}