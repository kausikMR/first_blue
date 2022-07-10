package com.example.first_blue;

import static io.flutter.plugin.common.EventChannel.*;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;


public class FirstBluePlugin implements FlutterPlugin, ActivityAware {

    private MethodChannel methodChannel;
    private EventChannel blueStateChannel;
    private EventChannel discoveryChannel;
    private EventChannel discoveryStateChannel;
    private EventSink blueStateSink;
    private EventSink discoverySink;
    private EventSink discoveryStateSink;
    private BinaryMessenger binaryMessenger;
    private BluetoothManager manager;
    private BluetoothAdapter adapter;
    private static Context context;
    private final BroadcastReceiver bluetoothStateReceiver;
    private final BroadcastReceiver discoveryReceiver;
    private static String discoveryFilter = "All";
    private List<Map<String, Object>> prevDevices;


    static void setDiscoveryFilter(String filter) {
        discoveryFilter = filter;
    }

    public FirstBluePlugin() {
        prevDevices = new ArrayList<>();
        bluetoothStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    Log.v("FirstBluePlugin", "onReceive: state=" + state);
                    switch (state) {
                        case BluetoothAdapter.STATE_ON:
                            blueStateSink.success(true);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_OFF:
                            blueStateSink.success(false);
                            break;
                        default:
                            Log.v("FirstBluePlugin", "Default Case handled BluetoothState: " + state);
                            break;
                    }
                }
            }

            @NonNull
            @Override
            public String toString() {
                return "BluetoothStateReceiver";
            }
        };
        discoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND:
                        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        final int deviceType = device.getType();
                        Log.v("FirstBluePlugin", "onReceive: device=" + device.getName());
                        final Map<String, Object> result = new HashMap<>();
                        result.put("name", device.getName());
                        result.put("address", device.getAddress());
                        result.put("type", device.getType());
//                        switch (discoveryFilter) {
//                            case "All":
//                                discoverySink.success(prevDevices.add(result));
//                                break;
//                            case "BLE":
//                                if (deviceType == BluetoothDevice.DEVICE_TYPE_LE) {
//                                    discoverySink.success(prevDevices.add(result));
//                                }
//                                break;
//                            case "Classic":
//                                if (deviceType == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
//                                    discoverySink.success(prevDevices.add(result));
//                                }
//                                break;
//                            case "Dual":
//                                if (deviceType == BluetoothDevice.DEVICE_TYPE_DUAL) {
//                                    discoverySink.success(prevDevices.add(result));
//                                }
//                                break;
//                        }
                        if (isFiltered(deviceType)) {
                            handleDiscoverySink(result);
                        } else {
                            discoverySink.success(prevDevices);
                        }
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        // clear all the prev devices
                        prevDevices.clear();
                        Log.v("FirstBluePlugin", "onReceive: ACTION_DISCOVERY_STARTED");
                        discoveryStateSink.success(true);
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        Log.v("FirstBluePlugin", "onReceive: ACTION_DISCOVERY_FINISHED");
                        discoveryStateSink.success(false);
                        unregisterReceiver(discoveryReceiver);
                        break;
                }
            }

            @NonNull
            @Override
            public String toString() {
                return "DiscoveryReceiver";
            }
        };
    }

    private boolean isFiltered(int deviceType) {
        if (Objects.equals(discoveryFilter, "Classic") && deviceType == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            return true;
        } else if (Objects.equals(discoveryFilter, "Dual") && deviceType == BluetoothDevice.DEVICE_TYPE_DUAL) {
            return true;
        } else if (Objects.equals(discoveryFilter, "BLE") && deviceType == BluetoothDevice.DEVICE_TYPE_LE) {
            return true;
        } else return Objects.equals(discoveryFilter, "All");
    }

    private void handleDiscoverySink(Map<String, Object> device) {
        if (discoverySink != null && !alreadyContains(device)) {
            prevDevices.add(device);
            discoverySink.success(prevDevices);
        }
    }

    boolean alreadyContains(Map<String, Object> device) {
        for (final Map<String, Object> d : prevDevices) {
            if (Objects.equals(d.get("address"), device.get("address"))) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        Log.v("FirstBluePlugin", "onAttachedToEngine");
        binaryMessenger = flutterPluginBinding.getBinaryMessenger();
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel.setMethodCallHandler(null);
        blueStateChannel.setStreamHandler(null);
        discoveryChannel.setStreamHandler(null);
        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(discoveryReceiver);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        Log.v("FirstBluePlugin", "onAttachedToActivity");
        Activity activity = binding.getActivity();
        context = activity.getApplicationContext();
        manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();
        if (adapter == null) {
            Log.v("FirstBluePlugin", "Bluetooth not supported");
            return;
        }
        // method channel
        methodChannel = new MethodChannel(binaryMessenger, "first_blue_method_channel");
        methodChannel.setMethodCallHandler(new FirstBlueMethodCallHandler(context, binding.getActivity(), adapter, manager, bluetoothStateReceiver, discoveryReceiver));
        // event channel
        blueStateChannel = new EventChannel(binaryMessenger, "first_blue_state_event_channel");
        blueStateChannel.setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                blueStateSink = events;
                registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
                blueStateSink.success(adapter.isEnabled());
                Log.v("FirstBluePlugin", "Registered bluetooth state receiver");
            }

            @Override
            public void onCancel(Object arguments) {
                unregisterReceiver(bluetoothStateReceiver);
                Log.v("FirstBluePlugin", "UnRegistered bluetooth state receiver");
            }
        });

        discoveryChannel = new EventChannel(binaryMessenger, "discovery_channel");
        discoveryChannel.setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                discoverySink = events;
                registerDiscoveryReceiver(discoveryReceiver);
                if (!adapter.isDiscovering()) {
                    adapter.startDiscovery();
                }
            }

            @Override
            public void onCancel(Object arguments) {
                unregisterReceiver(discoveryReceiver);
                adapter.cancelDiscovery();
            }
        });
        discoveryStateChannel = new EventChannel(binaryMessenger, "discovery_state_channel");
        discoveryStateChannel.setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                discoveryStateSink = events;
                discoveryStateSink.success(adapter.isDiscovering());
                registerDiscoveryReceiver(discoveryReceiver);
            }

            @Override
            public void onCancel(Object arguments) {
            }
        });
    }

    static void registerDiscoveryReceiver(BroadcastReceiver discoveryReceiver) {
        IntentFilter discoveryFilter = new IntentFilter();
        discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);
        discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, discoveryFilter);
    }

    static void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        try {
            context.registerReceiver(receiver, filter);
            Log.v("FirstBluePlugin", receiver.toString() + " registered");
        } catch (IllegalArgumentException e) {
            Log.v("FirstBluePlugin", receiver.toString() + " already registered");
        }
    }

    static void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            context.unregisterReceiver(receiver);
            Log.v("FirstBluePlugin", receiver.toString() + " unRegistered");
        } catch (IllegalArgumentException e) {
            Log.v("FirstBluePlugin", receiver.toString() + "cannot be unRegistered");
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
