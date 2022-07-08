import 'package:first_blue/first_blue.dart';
import 'package:flutter/material.dart';

class DiscoveryPage extends StatefulWidget {
  const DiscoveryPage({Key? key}) : super(key: key);

  @override
  State<DiscoveryPage> createState() => _DiscoveryPageState();
}

class _DiscoveryPageState extends State<DiscoveryPage> {
  final firstBlue = FirstBlue.instance;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('First Blue'),
      ),
      body: StreamBuilder<bool>(
        stream: firstBlue.blueStateStream(),
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
    return ListView.builder(
      itemCount: 10,
      itemBuilder: (context, index) {
        return ListTile(
          title: Text('Device $index'),
        );
      },
    );
  }
}
