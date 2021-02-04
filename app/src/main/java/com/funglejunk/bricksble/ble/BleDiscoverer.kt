package com.funglejunk.bricksble.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber

class BleDiscoverer(private val scanner: BluetoothLeScanner) {

    private companion object {
        const val LEGO_HUB_NAME = "LEGO Move Hub"
    }

    sealed class ScanEvent {
        data class DeviceFound(val device: BluetoothDevice) : ScanEvent()
        data class BatchResult(val result: MutableList<ScanResult>) : ScanEvent()
        data class ScanStartFailed(val error: Int) : ScanEvent()
    }

    class BleScanCallback(val subject: Subject<ScanEvent>) : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.let {
                subject.onNext(
                    ScanEvent.BatchResult(
                        it
                    )
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            subject.onNext(
                ScanEvent.ScanStartFailed(
                    errorCode
                )
            )
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { scanResult ->
                when (callbackType) {
                    ScanSettings.CALLBACK_TYPE_ALL_MATCHES, ScanSettings.CALLBACK_TYPE_FIRST_MATCH -> {
                        scanResult.scanRecord?.let { record ->
                            val deviceName = record.deviceName
                            val deviceAddress = scanResult.device.address
                            if (deviceName == LEGO_HUB_NAME && deviceAddress != "00:00:00:00:00:00") {
                                subject.onNext(
                                    ScanEvent.DeviceFound(
                                        scanResult.device
                                    )
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private val discoverSubject: Subject<ScanEvent> = PublishSubject.create()
    private var scanCallback: BleScanCallback? = null

    fun startScan(): Observable<ScanEvent> =
        BleScanCallback(discoverSubject)
            .also {
                scanCallback = it
            }.also {
                Timber.d("scan started.")
                scanner.startScan(it)
            }.run {
                subject.hide()
            }

    fun stopScan(): Unit = scanner.stopScan(scanCallback).also {
        scanCallback = null
    }.also {
        Timber.d("scan stopped.")
    }

}