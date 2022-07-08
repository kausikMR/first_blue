import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class FirstBlue {

  FirstBlue._();

  static final FirstBlue _instance = FirstBlue._();

  static FirstBlue get instance => _instance;

  final methodChannel = const MethodChannel('first_blue_method_channel');
  final eventChannel = const EventChannel('first_blue_event_channel');

  Future<void> turnOnBlue() async{
    try {
      await methodChannel.invokeMethod('turnOnBluetooth');
    }catch(e){
      debugPrint('Failed to turnOn: $e');
    }
  }

  Future<void> turnOffBlue() async{
    try {
      await methodChannel.invokeMethod('turnOffBluetooth');
    }catch(e){
      debugPrint('Failed to turnOff: $e');
    }
  }

  Stream<bool> blueStateStream() {
    return eventChannel.receiveBroadcastStream().map((value) => value as bool);
  }



}
