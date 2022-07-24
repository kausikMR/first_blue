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
import android.provider.Settings
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
    private val reqLocationCode = 908
    private val defaultDiscoverableSeconds = 120

    private lateinit var binaryMessenger: BinaryMessenger
    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var methodChannel: MethodChannel
    private lateinit var appStateChannel: EventChannel
    private lateinit var blueStateChannel: EventChannel
    private lateinit var discoveryChannel: EventChannel
    private lateinit var discoveryStateChannel: EventChannel
    private lateinit var discoverableStateChannel: EventChannel
    private var appStateSink: EventSink? = null
    private var blueStateSink: EventSink? = null
    private var discoverySink: EventSink? = null
    private var discoveryStateSink: EventSink? = null
    private var discoverableStateSink: EventSink? = null
    private var bluetoothStateReceiver: BroadcastReceiver
    private var discoveryReceiver: BroadcastReceiver
    private var locationStateReceiver: BroadcastReceiver
    private val prevDevices: MutableList<Map<String?, Any?>>
    private var locationManager: LocationManager? = null
    private var manager: BluetoothManager? = null
    private lateinit var adapter: BluetoothAdapter
    private var discoveryFilter = "All"

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
        appStateChannel.setStreamHandler(null)
        blueStateChannel.setStreamHandler(null)
        discoveryChannel.setStreamHandler(null)
        appStateSink = null
        blueStateSink = null
        discoverySink = null
        unregisterReceiver(bluetoothStateReceiver)
        unregisterReceiver(discoveryReceiver)
        unregisterReceiver(locationStateReceiver)
    }


    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.v("FirstBluePlugin", "onAttachedToActivity")
        activity = binding.activity
        context = activity.applicationContext
        manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager!!.adapter
        // location
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Request permission result listener
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
        // app state channel
        appStateChannel = EventChannel(binaryMessenger, "app_state_channel")
        appStateChannel.setStreamHandler(object : StreamHandler {
            override fun onListen(arguments: Any?, events: EventSink?) {
                appStateSink = events

                if (isLocationEnabled() && isBluetoothOn()) {
                    appStateSink?.success("SATISFIED")
                } else if (!isBluetoothOn()) {
                    appStateSink?.success("BLUETOOTH_DISABLED")
                } else if (!isBluetoothOn()) {
                    appStateSink?.success("LOCATION_DISABLED")
                }

                registerReceiver(
                    bluetoothStateReceiver,
                    IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                )
                registerReceiver(
                    locationStateReceiver,
                    IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
                )
            }

            override fun onCancel(arguments: Any?) {
                appStateSink = null
            }
        })
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
                blueStateSink = null
            }
        })
        discoveryChannel = EventChannel(binaryMessenger, "discovery_channel")
        discoveryChannel.setStreamHandler(object : StreamHandler {
            override fun onListen(args: Any?, events: EventSink?) {
                discoverySink = events
                registerDiscoveryReceiver()
                if (!adapter.isDiscovering) {
                    adapter.startDiscovery()
                }
            }

            override fun onCancel(args: Any?) {
                adapter.cancelDiscovery()
            }
        })
        discoveryStateChannel = EventChannel(binaryMessenger, "discovery_state_channel")
        discoveryStateChannel.setStreamHandler(object : StreamHandler {
            override fun onListen(args: Any?, events: EventSink?) {
                discoveryStateSink = events
                discoveryStateSink?.success(adapter.isDiscovering)
                registerDiscoveryReceiver()
            }

            override fun onCancel(args: Any?) {
                discoveryStateSink = null
            }
        })
        discoverableStateChannel = EventChannel(binaryMessenger, "discoverable_state_channel")
        discoverableStateChannel.setStreamHandler(object : StreamHandler {
            override fun onListen(arguments: Any?, events: EventSink?) {
                discoverableStateSink = events
                discoverableStateSink?.success(isDiscoverable())
                registerDiscoveryReceiver()
            }

            override fun onCancel(arguments: Any?) {
                discoverableStateSink = null
            }
        })
    }

    override fun onDetachedFromActivityForConfigChanges() {}
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}
    override fun onDetachedFromActivity() {}


    fun registerDiscoveryReceiver() {
        val discoveryFilter = IntentFilter()
        discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND)
        discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        discoveryFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
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
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    Log.v("FirstBluePlugin", "onReceive: state=$state")
                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            blueStateSink?.success(true)
                            if (isLocationEnabled()) {
                                appStateSink?.success("SATISFIED")
                            } else {
                                appStateSink?.success("LOCATION_DISABLED")
                            }
                        }
                        BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                            blueStateSink?.success(false)
                            appStateSink?.success("BLUETOOTH_DISABLED")
                        }
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
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
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
                    }
                    BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                        val scanMode = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_SCAN_MODE,
                            BluetoothAdapter.SCAN_MODE_NONE
                        )
                        val isDiscoverable: Boolean =
                            scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                        discoverableStateSink?.success(isDiscoverable)
                    }
                }
            }

            override fun toString(): String {
                return "DiscoveryReceiver"
            }
        }
        locationStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    LocationManager.PROVIDERS_CHANGED_ACTION -> {
                        if (isLocationEnabled() && isBluetoothOn()) {
                            appStateSink?.success("SATISFIED")
                        } else {
                            appStateSink?.success("LOCATION_DISABLED")
                        }
                    }
                }
            }

        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val method = call.method;
        val args = call.arguments;

        when (method) {
            "turnOnBluetooth" -> {
                turnOnBluetooth()
            }
            "turnOffBluetooth" -> {
                turnOffBluetooth()
            }
            "isBluetoothOn" -> {
                result.success(isBluetoothOn())
            }
            "startDiscovery" -> {
                startDiscovery()
            }
            "stopDiscovery" -> {
                cancelDiscovery()
            }
            "isDiscovering" -> {
                result.success(isDiscovering())
            }
            "isDiscoverable" -> {
                result.success(isDiscoverable())
            }
            "ensurePermissions" -> {
                ensureLocationPermissions()
            }
            "isLocationEnabled" -> {
                result.success(isLocationEnabled())
            }
            "makeDiscoverable" -> {
                makeDiscoverable(tryParseInt(args) ?: defaultDiscoverableSeconds)
            }
            else -> result.notImplemented()
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
        if (isDiscovering() || !isLocationEnabled()) return
        registerDiscoveryReceiver()
        adapter.startDiscovery()
    }

    private fun cancelDiscovery() {
        if (isDiscovering()) return
//        unregisterReceiver(discoveryReceiver)
        adapter.cancelDiscovery()
    }

    private fun isDiscovering(): Boolean {
        return adapter.isDiscovering
    }

    private fun isDiscoverable(): Boolean {
        return adapter.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
    }

    private fun isLocationEnabled(): Boolean {
        val gpsEnabled: Boolean =
            locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled: Boolean =
            locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return (gpsEnabled || networkEnabled)
    }

    // Permission handlers
    private fun ensureLocationPermissions() {
        if (isNotGranted(Manifest.permission.ACCESS_COARSE_LOCATION) || isNotGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            ActivityCompat.startActivityForResult(
                activity,
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                reqLocationCode,
                null
            )
        }
    }

    private fun makeDiscoverable(seconds: Int) {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds)// duration in seconds
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(discoverableIntent)
    }

}

/// helper-functions
private fun tryParseInt(value: Any?): Int? {
    val stringValue = value.toString()
    try {
        return Integer.parseInt(stringValue)
    } catch (e: Exception) {
        Log.v("FirstBluePlugin", "NON_FATAL: Failed to parse $stringValue as Int, $e");
    }
    return null
}