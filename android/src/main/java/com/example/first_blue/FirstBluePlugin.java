package com.example.first_blue;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** FirstBluePlugin */
public class FirstBluePlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel methodChannel;
  private EventChannel eventChannel;
  private BluetoothManager bluetoothManager;
  private BluetoothAdapter bluetoothAdapter;
  private Context context;
  private BroadcastReceiver bluetoothStateReceiver;
  private EventChannel.EventSink bluetoothStateEventSink;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    Log.v("FirstBluePlugin", "onAttachedToEngine");
    context = flutterPluginBinding.getApplicationContext();

    // method channel
    methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "first_blue_method_channel");
    methodChannel.setMethodCallHandler(this);

    // event channel
    eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "first_blue_event_channel");

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

    eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
      @Override
      public void onListen(Object arguments, EventChannel.EventSink events) {
        bluetoothStateEventSink = events;
        context.registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        bluetoothStateEventSink.success(bluetoothAdapter.isEnabled());
        Log.v("FirstBluePlugin", "Registered bluetooth state receiver");
      }

      @Override
      public void onCancel(Object arguments) {
        bluetoothStateEventSink = null;
//        context.unregisterReceiver(bluetoothStateReceiver);
        Log.v("FirstBluePlugin", "UnRegistered bluetooth state receiver");
      }
    });

    bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    assert bluetoothManager != null; // assert bluetoothManager to be non null
    bluetoothAdapter = bluetoothManager.getAdapter();
  }

  private void requestPermissions(String[] permissions) {

  }

  @TargetApi(Build.VERSION_CODES.M)
  private boolean isPermissionGranted(String permission) {
    return context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED;
  }

    // methods
    void requestToEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBtIntent);
        }
    }

    private void turnOnBluetooth() {
        requestToEnableBluetooth();
    }

    private void turnOffBluetooth(){
        bluetoothAdapter.disable();
    }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
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

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    methodChannel.setMethodCallHandler(null);
    eventChannel.setStreamHandler(null);
    context.unregisterReceiver(bluetoothStateReceiver);
  }
}
