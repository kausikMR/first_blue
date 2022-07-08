package com.example.first_blue;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;


public class FirstBluePlugin implements FlutterPlugin, ActivityAware {

  private MethodChannel methodChannel;
  private EventChannel eventChannel;
  private BinaryMessenger binaryMessenger;
  private BluetoothManager manager;
  private BluetoothAdapter adapter;
  private Context context;
  private Activity activity;
  private BroadcastReceiver bluetoothStateReceiver;
  private EventChannel.EventSink bluetoothStateEventSink;


  public FirstBluePlugin(){
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
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    Log.v("FirstBluePlugin", "onAttachedToEngine");
    binaryMessenger = flutterPluginBinding.getBinaryMessenger();
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    methodChannel.setMethodCallHandler(null);
    eventChannel.setStreamHandler(null);
    context.unregisterReceiver(bluetoothStateReceiver);
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
    methodChannel.setMethodCallHandler(new FirstBlueMethodCallHandler(context,binding.getActivity(), adapter, manager));
    // event channel
    eventChannel = new EventChannel(binaryMessenger, "first_blue_event_channel");
    eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
      @Override
      public void onListen(Object arguments, EventChannel.EventSink events) {
        bluetoothStateEventSink = events;
        context.registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        bluetoothStateEventSink.success(adapter.isEnabled());
        Log.v("FirstBluePlugin", "Registered bluetooth state receiver");
      }

      @Override
      public void onCancel(Object arguments) {
        bluetoothStateEventSink = null;
        context.unregisterReceiver(bluetoothStateReceiver);
        Log.v("FirstBluePlugin", "UnRegistered bluetooth state receiver");
      }
    });

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
