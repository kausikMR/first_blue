import 'package:first_blue/first_blue.dart';
import 'package:first_blue/models/blue_device.dart';
import 'package:flutter/material.dart';

Widget bluetoothDeviceItem(BlueDevice device) {
  return Card(
    child: ListTile(
      title: Text(
        device.name,
        style: const TextStyle(fontWeight: FontWeight.bold),
      ),
      subtitle: Text(
        '${device.address} | ${device.type.name}',
      ),
    ),
  );
}

Widget buildDiscoverButton(FirstBlue firstBlue) {
  return StreamBuilder<bool>(
    stream: firstBlue.discoveryState,
    builder: (context, snap) {
      final isDiscovering = snap.data ?? false;
      return FloatingActionButton(
        onPressed: () {
          firstBlue.startDiscovery();
        },
        child: Icon(isDiscovering ? Icons.stop : Icons.search),
      );
    },
  );
}

Widget bluetoothTurnedOff(FirstBlue firstBlue) {
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

Widget discoveryListView(FirstBlue firstBlue) {
  return StreamBuilder<List<BlueDevice>>(
    stream: firstBlue.discoveredDevices,
    builder: (context, snap) {
      if (snap.connectionState == ConnectionState.waiting) {
        return const Center(child: CircularProgressIndicator());
      }
      if (snap.hasError) {
        return Center(
          child: Text(
            'Error : ${snap.error}',
            style: const TextStyle(
              fontSize: 16,
            ),
          ),
        );
      }
      final devices = snap.data ?? [];
      if (devices.isEmpty) {
        return const Center(
          child: Text(
            'No Devices Found',
            style: TextStyle(fontSize: 16),
          ),
        );
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
