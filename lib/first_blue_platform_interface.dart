import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'first_blue_method_channel.dart';

abstract class FirstBluePlatform extends PlatformInterface {
  /// Constructs a FirstBluePlatform.
  FirstBluePlatform() : super(token: _token);

  static final Object _token = Object();

  static FirstBluePlatform _instance = MethodChannelFirstBlue();

  /// The default instance of [FirstBluePlatform] to use.
  ///
  /// Defaults to [MethodChannelFirstBlue].
  static FirstBluePlatform get instance => _instance;
  
  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FirstBluePlatform] when
  /// they register themselves.
  static set instance(FirstBluePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
