import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:first_blue/first_blue_platform_channel.dart';

void main() {
  PlatformChannelFirstBlue platform = PlatformChannelFirstBlue();
  const MethodChannel channel = MethodChannel('first_blue');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
