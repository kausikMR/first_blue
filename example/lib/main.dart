import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:first_blue/first_blue.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  final _firstBluePlugin = FirstBlue();

  @override
  void initState() {
    super.initState();
    initPlatformState();
    _firstBluePlugin.bluetoothStateStream.listen((event) {
      debugPrint('bluetoothStateStream: $event');
    });
  }

  Future<void> initPlatformState() async {
    String platformVersion;
    try {
      platformVersion = await _firstBluePlugin.getPlatformVersion() ?? 'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('FirstPlugin example app'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('Running on: $_platformVersion\n'),
              StreamBuilder<bool>(
                stream: _firstBluePlugin.bluetoothStateStream,
                builder: (context, snap) {
                  final isOn = snap.data ?? false;
                  return ElevatedButton(
                    child: Text('TURN ${isOn ? 'OFF' : 'ON'} BLUE'),
                    onPressed: () {
                      if (isOn) {
                        _firstBluePlugin.turnOffBluetooth();
                      } else {
                        _firstBluePlugin.turnOnBluetooth();
                      }
                    },
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
