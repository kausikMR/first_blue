import 'package:first_blue/enums.dart';
import 'package:first_blue/first_blue.dart';
import 'package:flutter/material.dart';

import 'helper_widgets.dart';

class DiscoveryPage extends StatefulWidget {
  const DiscoveryPage({Key? key}) : super(key: key);

  @override
  State<DiscoveryPage> createState() => _DiscoveryPageState();
}

class _DiscoveryPageState extends State<DiscoveryPage> {
  late final FirstBlue _firstBlue;

  @override
  void initState() {
    super.initState();
    _firstBlue = FirstBlue.instance;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('First Blue Example'),
        actions: [
          StreamBuilder<bool>(
            stream: _firstBlue.discoverableState(),
            builder: (context, snap) {
              final isDiscoverable = snap.data ?? false;
              return IconButton(
                splashRadius: 20,
                onPressed: () {
                  if (!isDiscoverable) {
                    _firstBlue.makeDiscoverable();
                  }
                },
                icon: Icon(
                  isDiscoverable
                      ? Icons.wifi_tethering
                      : Icons.wifi_tethering_off,
                ),
              );
            },
          )
        ],
      ),
      body: StreamBuilder<AppState>(
        stream: _firstBlue.appState(),
        builder: (context, snap) {
          if (snap.data != null) {
            final state = snap.data;
            if (state == AppState.satisfied) {
              return discoveryListView(_firstBlue);
            } else if (state == AppState.locationDisabled) {
              return const Center(child: Text('Turn on Location'));
            } else if (state == AppState.bluetoothDisabled) {
              return bluetoothTurnedOff(_firstBlue);
            }
            return const Center(child: Text('Something went wrong'));
          }
          return const Center(child: CircularProgressIndicator());
        },
      ),
      floatingActionButton: buildDiscoverButton(_firstBlue),
    );
  }
}
