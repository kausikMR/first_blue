import 'first_blue_platform_interface.dart';

class FirstBlue {
  Future<String?> getPlatformVersion() {
    return FirstBluePlatform.instance.getPlatformVersion();
  }

  Future<void> turnOnBluetooth(){
    return FirstBluePlatform.instance.turnOnBluetooth();
  }

  Future<void> turnOffBluetooth(){
    return FirstBluePlatform.instance.turnOffBluetooth();
  }

  Stream<bool> get bluetoothStateStream => FirstBluePlatform.instance.bluetoothStateStream;

}
