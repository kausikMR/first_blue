import 'package:first_blue/models/blue_device.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class FirstBlue {
  FirstBlue._();

  static final FirstBlue _instance = FirstBlue._();

  static FirstBlue get instance => _instance;

  final methodChannel = const MethodChannel('first_blue_method_channel');
  final blueStateChannel = const EventChannel('first_blue_state_event_channel');
  final appStateChannel = const EventChannel('app_state_channel');
  final discoveryChannel = const EventChannel('discovery_channel');
  final discoveryStateChannel = const EventChannel('discovery_state_channel');

  Future<bool> isBlueOn() async {
    try {
      final isOn = await methodChannel.invokeMethod('isBluetoothOn') as bool;
      return isOn;
    } catch (e) {
      debugPrint('Failed to get isBlueOn : $e');
      return false;
    }
  }

  Future<void> turnOnBlue() async {
    try {
      await methodChannel.invokeMethod('turnOnBluetooth');
    } catch (e) {
      debugPrint('Failed to turnOn: $e');
    }
  }

  Future<void> turnOffBlue() async {
    try {
      await methodChannel.invokeMethod('turnOffBluetooth');
    } catch (e) {
      debugPrint('Failed to turnOff: $e');
    }
  }

  Future<void> ensurePermissions() async {
    try {
      await methodChannel.invokeMethod('ensurePermissions');
    } catch (e) {
      debugPrint('Failed to ensurePermissions: $e');
    }
  }

  /// This method is used to start discovery of bluetooth devices.
  /// Note: This method will auto trigger when you listen to the [discoveredDevices] stream.
  Future<void> startDiscovery() async {
    try {
      await methodChannel.invokeMethod('startDiscovery');
    } catch (e) {
      debugPrint('Failed to start Discovery: $e');
    }
  }

  Future<void> stopDiscovery() async {
    try {
      await methodChannel.invokeMethod('stopDiscovery');
    } catch (e) {
      debugPrint('Failed to stop Discovery: $e');
    }
  }

  Future<void> setDiscoveryFilter(BlueFilter filter) async {
    try {
      return methodChannel.invokeMethod('setDiscoveryFilter', filter.name);
    } on PlatformException catch (e) {
      debugPrint('Failed to change Discovery Filter: $e');
    }
  }

  AppState _getAppState(String state) {
    switch (state) {
      case 'SATISFIED':
        return AppState.satisfied;
      case 'BLUETOOTH_DISABLED':
        return AppState.bluetoothDisabled;
      case 'LOCATION_DISABLED':
        return AppState.locationDisabled;
      default:
        return AppState.error;
    }
  }

  // Streams
  Stream<bool> blueState() {
    return blueStateChannel
        .receiveBroadcastStream()
        .map((value) => value as bool);
  }

  Stream<AppState> appState() {
    return appStateChannel.receiveBroadcastStream().map((event) =>
        _getAppState(event as String));
  }

  Stream<List<BlueDevice>> discoveredDevices() {
    return discoveryChannel.receiveBroadcastStream().map((event) {
      final devices = List<Map<dynamic, dynamic>>.from(event);
      final blueDevices =
      devices.map((device) => BlueDevice.fromMap(device)).toList();
      return blueDevices;
    });
  }

  Stream<bool> discoveryState() {
    return discoveryStateChannel.receiveBroadcastStream().map((event) {
      return event as bool;
    });
  }
}

enum BlueFilter {
  all('All'),
  onlyBLE('BLE'),
  onlyClassic('Classic'),
  onlyDual('Dual');

  final String name;

  const BlueFilter(this.name);
}

enum AppState {
  bluetoothDisabled('BLUETOOTH_DISABLED'),
  locationDisabled('LOCATION_DISABLED'),
  satisfied('SATISFIED'),
  error('ERROR');

  final String name;

  const AppState(this.name);
}
