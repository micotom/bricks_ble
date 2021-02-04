package com.funglejunk.bricksble.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.Context
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class LegoConnectProcedure(private val bleProxy: BleProxy) {

    fun start(
        context: Context,
        onSuccess: (BluetoothGatt) -> Unit,
        onError: (Throwable) -> Unit
    ): Disposable =
        bleProxy.scanForLegoHub()
            .take(1)
            .doOnNext {
                bleProxy.stopScan()
            }
            .observeOn(Schedulers.io())
            .flatMap {
                when (it) {
                    is BleDiscoverer.ScanEvent.DeviceFound -> {
                        Observable.just(it.device)
                    }
                    is BleDiscoverer.ScanEvent.ScanStartFailed -> {
                        Observable.error<BluetoothDevice>(IllegalStateException("scan start failed"))
                    }
                    is BleDiscoverer.ScanEvent.BatchResult -> {
                        Observable.empty<BluetoothDevice>()
                    }
                }
            }
            .flatMapSingle {
                bleProxy.connect(it, context)
            }
            .flatMap {
                when (it) {
                    is BleCallback.BleEvent.Connect.Connected -> {
                        val gatt = it.gatt
                        Observable.just(gatt)
                    }
                    is BleCallback.BleEvent.Connect.Disconnected -> {
                        Observable.error<BluetoothGatt>(IllegalStateException("disconnected on connect"))
                    }
                    is BleCallback.BleEvent.Connect.Error -> {
                        Observable.error<BluetoothGatt>(IllegalStateException("error on connect: ${it.status}"))
                    }
                }
            }
            .flatMapSingle {
                bleProxy.discoverServices(it)
            }
            .flatMap {
                when (it) {
                    is BleCallback.BleEvent.Discovery.ServicesDiscovered -> {
                        Observable.just(it.gatt to it.services)
                    }
                    is BleCallback.BleEvent.Discovery.Error -> {
                        Observable.error<Pair<BluetoothGatt, List<BluetoothGattService>>>(
                            IllegalStateException("error on service discovery: ${it.status}")
                        )
                    }
                }
            }
            .doOnNext { (gatt, _) ->
                val service = gatt.getService(legoCustomServiceUuid)
                val characteristic = service.getCharacteristic(legoCharacteristicUuid)
                bleProxy.readCharacteristic(
                    gatt,
                    characteristic,
                    { _ ->
                        Timber.d("init characteristic read")
                        bleProxy.enableNotifications(
                            gatt,
                            characteristic,
                            {
                                onSuccess(gatt)
                            },
                            { onError(it) }
                        )
                    },
                    {
                        onError(it)
                    }
                )
            }.subscribe()

}