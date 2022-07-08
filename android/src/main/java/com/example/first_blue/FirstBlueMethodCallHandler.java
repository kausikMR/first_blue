package com.example.first_blue;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;

public class FirstBlueMethodCallHandler implements MethodCallHandler {

    private static final int REQUEST_ENABLE_BT = 192;
    final private Context context;
    final private Activity activity;
    final private BluetoothAdapter adapter;
    final private BluetoothManager manager;

     public FirstBlueMethodCallHandler(Context context, Activity activity, BluetoothAdapter adapter, BluetoothManager manager) {
        this.context = context;
        this.activity = activity;
        this.adapter = adapter;
        this.manager = manager;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("turnOnBluetooth")) {
            turnOnBluetooth();
            result.success("Bluetooth turned on");
        } else if (call.method.equals("turnOffBluetooth")){
            turnOffBluetooth();
            result.success("Bluetooth turned off");
        } else {
            result.notImplemented();
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

    private void turnOffBluetooth(){
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

}
