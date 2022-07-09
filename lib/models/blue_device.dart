

enum DeviceType {
  classic('Classic'),
  ble('BLE'),
  dual('Dual'),
  unknown('Unknown');

  final String name;

  const DeviceType(this.name);
}

class BlueDevice {
  final String name;
  final String address;
  final DeviceType type;

  BlueDevice({
    required this.name,
    required this.address,
    required this.type,
  });

  factory BlueDevice.fromMap(Map<String, dynamic> map) {
    return BlueDevice(
      name: map['name'].toString(),
      address: map['address'].toString(),
      type: _getDeviceType(int.tryParse(map['type'].toString()) ?? 0),
    );
  }

  static DeviceType _getDeviceType(int val) {
    switch (val) {
      case 1:
        return DeviceType.classic;
      case 2:
        return DeviceType.ble;
      case 3:
        return DeviceType.dual;
      default:
        return DeviceType.unknown;
    }
  }
}
