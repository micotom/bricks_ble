package com.funglejunk.bricksble

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.leoboost.R
import com.example.leoboost.databinding.ActivityMainBinding
import com.funglejunk.bricksble.util.PermissionHelper
import com.funglejunk.bricksble.msgtypes.PortOutputCmd

class MainActivity : AppCompatActivity() {

    private lateinit var views: ActivityMainBinding
    private lateinit var vm: ActivityViewModel

    private val connectedActiveViews: List<View> by lazy {
        with(views) {
            listOf(
                disconnectButton,
                redButton,
                greenButton,
                blueButton,
                rssiInfoTxt,
                batteryInfoTxt,
                arrowUp,
                arrowDown,
                arrowRight,
                arrowLeft
            )
        }
    }

    private val disconnectedActiveViews: List<View> by lazy {
        with(views) {
            listOf(connectButton)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)

        vm = ViewModelProvider(this).get(ActivityViewModel::class.java)
        vm.getLiveData().observe(this, Observer<ActivityViewModel.Event> { data ->
            when (data) {
                ActivityViewModel.Event.Disconnected -> onDisconnected()
                is ActivityViewModel.Event.Connected -> onConnected()
                is ActivityViewModel.Event.RssiLevelReport -> views.use {
                    rssiInfoTxt.text = getString(R.string.rssi_info, data.level)
                }
                is ActivityViewModel.Event.BatteryLevelReport -> views.use {
                    batteryInfoTxt.text = getString(R.string.battery_info, data.level)
                }
            }
        })

        PermissionHelper.check(this, ::onDisconnected)
    }

    override fun onPause() {
        vm.command(ActivityViewModel.Command.StopScan)
        super.onPause()
    }

    private fun onDisconnected() = kotlin.run {
        disconnectedActiveViews.forEach { it.isEnabled = true }
        connectedActiveViews.forEach { it.isEnabled = false }
    }.also {
        views.use {
            connectButton.setOnClickListener {
                it.isEnabled = false
                vm.command(
                    ActivityViewModel.Command.Connect(
                        this@MainActivity,
                        (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                    )
                )
            }
        }
    }

    private fun onConnected() = kotlin.run {
        disconnectedActiveViews.forEach { it.isEnabled = false }
        connectedActiveViews.forEach { it.isEnabled = true }
    }.also {
        views.use {
            disconnectButton.setOnClickListener {
                it.isEnabled = false
                vm.command(ActivityViewModel.Command.Disconnect)
            }

            redButton.setOnClickListener {
                vm.viewAction(
                    ActivityViewModel.ViewAction.SetLed(
                        PortOutputCmd.ColorByte.RED
                    )
                )
            }
            greenButton.setOnClickListener {
                vm.viewAction(
                    ActivityViewModel.ViewAction.SetLed(
                        PortOutputCmd.ColorByte.GREEN
                    )
                )
            }
            blueButton.setOnClickListener {
                vm.viewAction(
                    ActivityViewModel.ViewAction.SetLed(
                        PortOutputCmd.ColorByte.BLUE
                    )
                )
            }

            with(arrowUp) {
                onPressHold { vm.viewAction(ActivityViewModel.ViewAction.DriveForward) }
                onRelease { vm.viewAction(ActivityViewModel.ViewAction.StopDriving) }
            }

            with(arrowDown) {
                onPressHold { vm.viewAction(ActivityViewModel.ViewAction.DriveBackward) }
                onRelease { vm.viewAction(ActivityViewModel.ViewAction.StopDriving) }
            }

            with(arrowRight) {
                onPressHold { vm.viewAction(ActivityViewModel.ViewAction.DriveRight) }
                onRelease { vm.viewAction(ActivityViewModel.ViewAction.StopDriving) }
            }

            with(arrowLeft) {
                onPressHold { vm.viewAction(ActivityViewModel.ViewAction.DriveLeft) }
                onRelease { vm.viewAction(ActivityViewModel.ViewAction.StopDriving) }
            }
        }
    }

    private fun ActivityMainBinding.use(f: ActivityMainBinding.() -> Unit) = runOnUiThread {
        f(this)
    }

}