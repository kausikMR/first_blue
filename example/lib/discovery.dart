import 'package:first_blue/first_blue.dart';
import 'package:first_blue/models/blue_device.dart';
import 'package:flutter/material.dart';

class DiscoveryPage extends StatefulWidget {
  const DiscoveryPage({Key? key}) : super(key: key);

  @override
  State<DiscoveryPage> createState() => _DiscoveryPageState();
}

class _DiscoveryPageState extends State<DiscoveryPage> {
  final firstBlue = FirstBlue.instance;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('First Blue'),
      ),
      body: StreamBuilder<bool>(
        stream: firstBlue.blueState(),
        builder: (context, snap) {
          final isOn = snap.data ?? false;
          if (!isOn) {
            return bluetoothTurnedOff();
          }
          return discoveryListView();
        },
      ),
    );
  }

  Widget bluetoothTurnedOff() {
    return Center(
      child: ElevatedButton(
        child: const Text(
          'Turn On Bluetooth',
        ),
        onPressed: () {
          firstBlue.turnOnBlue();
        },
      ),
    );
  }

  Widget discoveryListView() {
    return StreamBuilder<List<BlueDevice>>(
      stream: firstBlue.discoveredDevices(),
      builder: (context, snap) {
        if (snap.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snap.hasError) {
          return Center(
            child: Text('Error : ${snap.error}'),
          );
        }
        final devices = snap.data ?? [];
        if (devices.isEmpty) {
          return const Center(child: Text('No Devices Found'));
        }
        return ListView.builder(
          itemCount: devices.length,
          itemBuilder: (context, index) {
            return bluetoothDeviceItem(devices[index]);
          },
        );
      },
    );
  }

  Widget bluetoothDeviceItem(BlueDevice device) {
    return Card(
        child: ListTile(
      title: Text('Device ${device.name}'),
      subtitle: Text('${device.address} | ${device.type.name}'),
    ));
  }
}
