import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'first_blue_platform_interface.dart';

/// An implementation of [FirstBluePlatform] that uses method channels.
class MethodChannelFirstBlue extends FirstBluePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('first_blue');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
