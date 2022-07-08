// import 'package:flutter_test/flutter_test.dart';
// import 'package:first_blue/first_blue.dart';
// import 'package:first_blue/first_blue_platform_interface.dart';
// import 'package:first_blue/first_blue_platform_channel.dart';
// import 'package:plugin_platform_interface/plugin_platform_interface.dart';
//
// class MockFirstBluePlatform
//     with MockPlatformInterfaceMixin
//     implements FirstBluePlatform {
//
//   @override
//   Future<String?> getPlatformVersion() => Future.value('42');
//
//   @override
//   // TODO: implement bluetoothStateStream
//   Stream<bool> get bluetoothStateStream => throw UnimplementedError();
//
//   @override
//   Future<void> turnOffBluetooth() {
//     // TODO: implement turnOffBluetooth
//     throw UnimplementedError();
//   }
//
//   @override
//   Future<void> turnOnBluetooth() {
//     // TODO: implement turnOnBluetooth
//     throw UnimplementedError();
//   }
// }
//
// void main() {
//   final FirstBluePlatform initialPlatform = FirstBluePlatform.instance;
//
//   test('$PlatformChannelFirstBlue is the default instance', () {
//     expect(initialPlatform, isInstanceOf<PlatformChannelFirstBlue>());
//   });
//
//   test('getPlatformVersion', () async {
//     FirstBlue firstBluePlugin = FirstBlue();
//     MockFirstBluePlatform fakePlatform = MockFirstBluePlatform();
//     FirstBluePlatform.instance = fakePlatform;
//
//     expect(await firstBluePlugin.getPlatformVersion(), '42');
//   });
// }
