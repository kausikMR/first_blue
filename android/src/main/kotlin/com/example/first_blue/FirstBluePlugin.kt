package com.example.first_blue

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.*

class FirstBluePlugin : FlutterPlugin, ActivityAware, MethodCallHandler {

    // consts
    private val reqEnableBluetoothCode = 123
    private val reqPermissionsCode = 101

    private lateinit var binaryMessenger: BinaryMessenger
    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var methodChannel: MethodChannel
    private lateinit var blueStateChannel: EventChannel
    private lateinit var discoveryChannel: EventChannel
    private lateinit var discoveryStateChannel: EventChannel
    private var blueStateSink: EventSink? = null
    private var discoverySink: EventSink? = null
    private var discoveryStateSink: EventSink? = null
    private var bluetoothStateReceiver: BroadcastReceiver
    private lateinit var discoveryReceiver: BroadcastReceiver
    private val prevDevices: MutableList<Map<String?, Any?>>
    private var locationManager: LocationManager? = null
    private var manager: BluetoothManager? = null
    private lateinit var adapter: BluetoothAdapter
    private var discoveryFilter = "All"

//    private fun ensureLocationEnabled() {
//        if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            return
//        }
//        ensureLocationPermissions()
//    }

    // Permission handlers
    private fun ensureLocationPermissions() {
        if (isNotGranted(Manifest.permission.ACCESS_COARSE_LOCATION) || isNotGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun isNotGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(activity, permissions, reqPermissionsCode)
    }

    private fun isFiltered(deviceType: Int?): Boolean {
        return if (discoveryFilter == "Classic" && deviceType == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            true
        } else if (discoveryFilter == "Dual" && deviceType == BluetoothDevice.DEVICE_TYPE_DUAL) {
            true
        } else if (discoveryFilter == "BLE" && deviceType == BluetoothDevice.DEVICE_TYPE_LE) {
            true
        } else discoveryFilter == "All"
    }

    private fun handleDiscoverySink(device: Map<String?, Any?>) {
        if (discoverySink != null && !alreadyContains(device)) {
            prevDevices.add(device)
            discoverySink!!.success(prevDevices)
        }
    }

    private fun alreadyContains(device: Map<String?, Any?>): Boolean {
        for (d in prevDevices) {
            if (d["address"] == device["address"]) {
                return true
            }
        }
        return false
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
        Log.v("FirstBluePlugin", "onAttachedToEngine")
        binaryMessenger = flutterPluginBinding.binaryMessenger
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        blueStateChannel.setStreamHandler(null)
        discoveryChannel.setStreamHandler(null)
        unregisterReceiver(bluetoothStateReceiver)
        unregisterReceiver(discoveryReceiver)
    }


    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.v("FirstBluePlugin", "onAttachedToActivity")
        activity = binding.activity
        context = activity.applicationContext
        manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager!!.adapter
        // location
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.addRequestPermissionsResultListener { requestCode: Int, _: Array<String?>?, grantResults: IntArray ->
                if (requestCode == reqPermissionsCode) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.v("FirstBluePlugin", "Location Permission Granted")
                        return@addRequestPermissionsResultListener true
                    } else {
                        Log.v("FirstBluePlugin", "Location Permission Denied, Retry")
                        ensureLocationPermissions()
                    }
                }
                false
            }
        }

        // method channel
        methodChannel = MethodChannel(binaryMessenger, "first_blue_method_channel")
        methodChannel.setMethodCallHandler(this)
        // event channel
        blueStateChannel = EventChannel(binaryMessenger, "first_blue_state_event_channel")
        blueStateChannel.setStreamHandler(object : StreamHandler {
            override fun onListen(args: Any?, events: EventSink?) {
                blueStateSink = events
                registerReceiver(
                    bluetoothStateReceiver,
                    IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                )
                blueStateSink?.success(adapter.isEnabled)
                Log.v("FirstBluePlugin", "Registered bluetooth state receiver")
            }

            override fun onCancel(args: Any?) {
                unregisterReceiver(bluetoothStateReceiver)
                if (blueStateSink == null) return
                blueStateSink = null
            }
        })
        discoveryChannel = EventChannel(binaryMessenger, "discovery_channel")
        discoveryChannel.setStreamHandler(object : StreamHandler {
            override fun onListen(args: Any?, events: EventSink?) {
                discoverySink = events
                registerDiscoveryReceiver(discoveryReceiver)
                if (!adapter.isDiscovering) {
                    adapter.startDiscovery()
                }
            }

            override fun onCancel(args: Any?) {
                unregisterReceiver(discoveryReceiver)
                adapter.cancelDiscovery()
            }
        })
        discoveryStateChannel = EventChannel(binaryMessenger, "discovery_state_channel")
        discoveryStateChannel.setStreamHandler(object : StreamHandler {
            override fun onListen(args: Any?, events: EventSink?) {
                discoveryStateSink = events
                discoveryStateSink?.success(adapter.isDiscovering)
                registerDiscoveryReceiver(discoveryReceiver)
            }

            override fun onCancel(args: Any?) {
                if (discoveryStateSink == null) return
                discoveryStateSink = null
            }
        })
    }

    override fun onDetachedFromActivityForConfigChanges() {}
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}
    override fun onDetachedFromActivity() {}


//    fun setDiscoveryFilter(filter: String) {
//        discoveryFilter = filter
//    }

    fun registerDiscoveryReceiver(discoveryReceiver: BroadcastReceiver) {
        val discoveryFilter = IntentFilter()
        discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND)
        discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(discoveryReceiver, discoveryFilter)
    }

    fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
        try {
            context.registerReceiver(receiver, filter)
            Log.v("FirstBluePlugin", "$receiver registered")
        } catch (e: IllegalArgumentException) {
            Log.v("FirstBluePlugin", "$receiver already registered")
        }
    }

    fun unregisterReceiver(receiver: BroadcastReceiver) {
        try {
            context.unregisterReceiver(receiver)
            Log.v("FirstBluePlugin", "$receiver unRegistered")
        } catch (e: IllegalArgumentException) {
            Log.v("FirstBluePlugin", receiver.toString() + "cannot be unRegistered")
        }
    }

    init {
        prevDevices = ArrayList()
        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    Log.v("FirstBluePlugin", "onReceive: state=$state")
                    when (state) {
                        BluetoothAdapter.STATE_ON -> blueStateSink!!.success(true)
                        BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> blueStateSink!!.success(
                            false
                        )
                        else -> Log.v(
                            "FirstBluePlugin",
                            "Default Case handled BluetoothState: $state"
                        )
                    }
                }
            }

            override fun toString(): String {
                return "BluetoothStateReceiver"
            }
        }
        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val deviceType = device?.type
                        val deviceName = device?.name ?: "Unknown"
                        Log.v("FirstBluePlugin", "onReceive: device=$deviceName")
                        val result: MutableMap<String?, Any?> = hashMapOf(
                            "name" to (device?.name ?: "Unknown"),
                            "address" to device?.address,
                            "type" to deviceType
                        )
                        if (isFiltered(deviceType)) {
                            handleDiscoverySink(result)
                        } else {
                            discoverySink?.success(prevDevices)
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        // clear all the prev devices
                        prevDevices.clear()
                        Log.v("FirstBluePlugin", "onReceive: ACTION_DISCOVERY_STARTED")
                        discoveryStateSink?.success(true)
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Log.v("FirstBluePlugin", "onReceive: ACTION_DISCOVERY_FINISHED")
                        discoveryStateSink?.success(false)
                        unregisterReceiver(discoveryReceiver)
                    }
                }
            }

            override fun toString(): String {
                return "DiscoveryReceiver"
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "turnOnBluetooth" -> {
                turnOnBluetooth()
            }
            "turnOffBluetooth" -> {
                turnOffBluetooth()
            }
            "startDiscovery" -> {
                startDiscovery()
            }
            "stopDiscovery" -> {
                cancelDiscovery()
            }
        }
    }

    private fun isBluetoothOn(): Boolean {
        return adapter.isEnabled
    }

    private fun requestToEnableBluetooth() {
        if (isBluetoothOn()) return
        val enableBtIntent =
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityCompat.startActivityForResult(
            activity,
            enableBtIntent,
            reqEnableBluetoothCode,
            null
        )
    }

    private fun turnOnBluetooth() {
        requestToEnableBluetooth()
    }

    private fun turnOffBluetooth() {
        if (!isBluetoothOn()) return
        adapter.disable()
    }

    private fun startDiscovery() {
        if (adapter.isDiscovering) return
        registerDiscoveryReceiver(discoveryReceiver)
        adapter.startDiscovery()
    }

    private fun cancelDiscovery() {
        if (!adapter.isDiscovering) return
        unregisterReceiver(discoveryReceiver)
        adapter.cancelDiscovery()
    }

}