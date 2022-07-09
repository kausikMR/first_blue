package com.example.first_blue;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;

public class FirstBlueMethodCallHandler implements MethodCallHandler {

    private static final int REQUEST_ENABLE_BT = 192;
    private static final int REQUEST_PERMISSIONS = 101;
    final private Context context;
    final private Activity activity;
    final private BluetoothAdapter adapter;
    final private BluetoothManager manager;
    final private BroadcastReceiver discoveryReceiver;
    final private BroadcastReceiver blueStateReceiver;

    public FirstBlueMethodCallHandler(Context context,
                                      Activity activity,
                                      BluetoothAdapter adapter,
                                      BluetoothManager manager,
                                      BroadcastReceiver blueStateReceiver,
                                      BroadcastReceiver devicesDiscoveredReceiver) {
        this.context = context;
        this.activity = activity;
        this.adapter = adapter;
        this.manager = manager;
        this.blueStateReceiver = blueStateReceiver;
        this.discoveryReceiver = devicesDiscoveredReceiver;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case "turnOnBluetooth":
                turnOnBluetooth();
                result.success("Bluetooth turned on");
                break;
            case "turnOffBluetooth":
                turnOffBluetooth();
                result.success("Bluetooth turned off");
                break;
            case "startDiscovery":
                ensurePermissions();
                FirstBluePlugin.registerReceiver(discoveryReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                Log.v("FirstBluePlugin", "Registered discovery receiver");
                startDiscovery();
                break;
            case "stopDiscovery":
                stopDiscovery();
                FirstBluePlugin.unregisterReceiver(discoveryReceiver);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    void requestToEnableBluetooth() {
        if (!adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBtIntent);
        }
    }

    private void turnOnBluetooth() {
        requestToEnableBluetooth();
    }

    private void turnOffBluetooth() {
        adapter.disable();
    }

    private void startDiscovery() {
        if (adapter.isDiscovering()) {
            adapter.startDiscovery();
        }
    }

    private void stopDiscovery() {
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
    }

    // Permission handlers
    private void ensurePermissions() {
        if (!isGranted(Manifest.permission.ACCESS_COARSE_LOCATION) || !isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    private boolean isGranted(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions(String[] permissions) {
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_PERMISSIONS);
    }

}
