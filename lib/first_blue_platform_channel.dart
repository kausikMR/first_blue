import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'first_blue_platform_interface.dart';

class PlatformChannelFirstBlue implements FirstBluePlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('first_blue_method_channel');
  final eventChannel = const EventChannel('first_blue_event_channel');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<void> turnOffBluetooth() {
    return methodChannel.invokeMethod<void>('turnOffBluetooth');
  }

  @override
  Future<void> turnOnBluetooth() {
    return methodChannel.invokeMethod<void>('turnOnBluetooth');
  }

  @override
  Stream<bool> get bluetoothStateStream => eventChannel.receiveBroadcastStream().map((value) => value as bool);
}
