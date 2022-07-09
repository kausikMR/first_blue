package com.example.first_blue;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;


public class FirstBluePlugin implements FlutterPlugin, ActivityAware {

    private MethodChannel methodChannel;
    private EventChannel bluetoothStateEventChannel;
    private EventChannel discoveryChannel;
    private EventChannel.EventSink bluetoothStateEventSink;
    private EventChannel.EventSink discoveryEventSink;
    private BinaryMessenger binaryMessenger;
    private BluetoothManager manager;
    private BluetoothAdapter adapter;
    private static Context context;
    private Activity activity;
    private final BroadcastReceiver bluetoothStateReceiver;
    private final BroadcastReceiver discoveryReceiver;


    public FirstBluePlugin() {
        bluetoothStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    Log.v("FirstBluePlugin", "onReceive: state=" + state);
                    switch (state) {
                        case BluetoothAdapter.STATE_ON:
                            bluetoothStateEventSink.success(true);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_OFF:
                            bluetoothStateEventSink.success(false);
                            break;
                        default:
                            Log.v("FirstBluePlugin", "Default Case handled BluetoothState: " + state);
                            break;
                    }
                }
            }
        };
        discoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.v("FirstBluePlugin", "onReceive: device=" + device.getName());
                    final Map<String, Object> result = new HashMap<>();
                    result.put("name", device.getName());
                    result.put("address", device.getAddress());
                    result.put("type", device.getType());
                    discoveryEventSink.success(result);
                }
            }
        };
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        Log.v("FirstBluePlugin", "onAttachedToEngine");
        binaryMessenger = flutterPluginBinding.getBinaryMessenger();
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel.setMethodCallHandler(null);
        bluetoothStateEventChannel.setStreamHandler(null);
        context.unregisterReceiver(bluetoothStateReceiver);
        context.unregisterReceiver(discoveryReceiver);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        Log.v("FirstBluePlugin", "onAttachedToActivity");
        this.activity = binding.getActivity();
        this.context = activity.getApplicationContext();
        this.manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = manager.getAdapter();
        if (adapter == null) {
            Log.v("FirstBluePlugin", "Bluetooth not supported");
            return;
        }
        // method channel
        methodChannel = new MethodChannel(binaryMessenger, "first_blue_method_channel");
        methodChannel.setMethodCallHandler(new FirstBlueMethodCallHandler(context, binding.getActivity(), adapter, manager, bluetoothStateReceiver, discoveryReceiver));
        // event channel
        bluetoothStateEventChannel = new EventChannel(binaryMessenger, "first_blue_state_event_channel");
        bluetoothStateEventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                bluetoothStateEventSink = events;
                registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
                bluetoothStateEventSink.success(adapter.isEnabled());
                Log.v("FirstBluePlugin", "Registered bluetooth state receiver");
            }

            @Override
            public void onCancel(Object arguments) {
                unregisterReceiver(bluetoothStateReceiver);
                bluetoothStateEventSink = null;
                Log.v("FirstBluePlugin", "UnRegistered bluetooth state receiver");
            }
        });

        discoveryChannel = new EventChannel(binaryMessenger, "discovered_devices_event_channel");
        discoveryChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                registerReceiver(discoveryReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                if (!adapter.isDiscovering()) {
                    adapter.startDiscovery();
                }
                discoveryEventSink = events;
            }

            @Override
            public void onCancel(Object arguments) {
//                unregisterReceiver(discoveryReceiver);
//                discoveryEventSink = null;
//                Log.v("FirstBluePlugin", "UnRegistered devices discovered receiver");
            }
        });
    }

    static void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        try {
            context.registerReceiver(receiver, filter);
        } catch (IllegalArgumentException e) {
            Log.v("FirstBluePlugin", "Receiver already registered");
        }
    }

    static void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
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

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {

    }
}
