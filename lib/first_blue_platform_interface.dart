import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'first_blue_platform_channel.dart';

abstract class FirstBluePlatform extends PlatformInterface {

  FirstBluePlatform() : super(token: _token);

  static final Object _token = Object();

  static FirstBluePlatform _instance = PlatformChannelFirstBlue();

  static FirstBluePlatform get instance => _instance;

  static set instance(FirstBluePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Stream<bool> get bluetoothStateStream{
    throw UnimplementedError('bluetoothStateStream has not been implemented.');
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<void> turnOnBluetooth() {
    throw UnimplementedError('turnOnBluetooth() has not been implemented.');
  }

  Future<void> turnOffBluetooth() {
    throw UnimplementedError('turnOffBluetooth() has not been implemented.');
  }


}
