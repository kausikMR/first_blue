import 'package:first_blue/models/blue_device.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'enums.dart';

class FirstBlue {
  FirstBlue._();

  static final FirstBlue _instance = FirstBlue._();

  static FirstBlue get instance => _instance;

  final methodChannel = const MethodChannel('first_blue_method_channel');
  final blueStateChannel = const EventChannel('first_blue_state_event_channel');
  final appStateChannel = const EventChannel('app_state_channel');
  final discoveryChannel = const EventChannel('discovery_channel');
  final discoveryStateChannel = const EventChannel('discovery_state_channel');
  final discoverableStateChannel =
      const EventChannel('discoverable_state_channel');

  /// getters
  ///
  Future<bool> get isBlueOn async {
    try {
      final isOn = await methodChannel.invokeMethod('isBluetoothOn') as bool;
      return isOn;
    } catch (e) {
      debugPrint('Failed to get isBlueOn : $e');
      return false;
    }
  }

  Future<bool> get isDiscovering async {
    try {
      final isDiscovering =
          await methodChannel.invokeMethod('isDiscovering') as bool;
      return isDiscovering;
    } catch (e) {
      debugPrint('Failed to get isDiscovering: $e');
      return false;
    }
  }

  Future<bool> get isDiscoverable async {
    try {
      final isDiscoverable =
          await methodChannel.invokeMethod('isDiscoverable') as bool;
      return isDiscoverable;
    } catch (e) {
      debugPrint('Failed to get isDiscoverable: $e');
      return false;
    }
  }

  Stream<AppState> get appState {
    try {
      return appStateChannel
          .receiveBroadcastStream()
          .map((event) => _getAppState(event as String));
    } catch (e) {
      debugPrint('Failed to get AppState stream: $e');
      return Stream.value(AppState.error);
    }
  }

  Stream<bool> get blueState {
    try {
      return blueStateChannel
          .receiveBroadcastStream()
          .map((value) => value as bool);
    } catch (e) {
      debugPrint('Failed to get BluetoothState stream: $e');
      return Stream.value(false);
    }
  }

  Stream<bool> get discoveryState {
    try {
      return discoveryStateChannel
          .receiveBroadcastStream()
          .map((event) => event as bool);
    } catch (e) {
      debugPrint('Failed to get DiscoveryState stream: $e');
      return Stream.value(false);
    }
  }

  Stream<List<BlueDevice>> get discoveredDevices {
    try {
      return discoveryChannel.receiveBroadcastStream().map((event) {
        final devices = List<Map<dynamic, dynamic>>.from(event);
        final blueDevices =
            devices.map((device) => BlueDevice.fromMap(device)).toList();
        return blueDevices;
      });
    } catch (e) {
      debugPrint('Failed to get discoveredDevices stream: $e');
      return Stream.value(<BlueDevice>[]);
    }
  }

  Stream<bool> get discoverableState {
    try {
      return discoverableStateChannel
          .receiveBroadcastStream()
          .map((event) => event as bool);
    } catch (e) {
      debugPrint('Failed to get discoverableState stream: $e');
      return Stream.value(false);
    }
  }

  /// methods
  ///
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

  Future<void> makeDiscoverable() async {
    try {
      await methodChannel.invokeMethod('makeDiscoverable');
    } catch (e) {
      debugPrint('Failed to make Discoverable: $e');
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
}
