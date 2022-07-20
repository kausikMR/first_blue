class BlueDevice {
  final String name;
  final String address;
  final DeviceType type;

  const BlueDevice({
    required this.name,
    required this.address,
    required this.type,
  });

  factory BlueDevice.fromMap(Map<dynamic, dynamic> map) {
    return BlueDevice(
      name: map['name'] ?? 'Unknown',
      address: map['address'] ?? 'Unknown',
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

/// Device type enum
enum DeviceType {
  unknown('UNKNOWN', 0),
  classic('CLASSIC', 1),
  ble('LE', 2),
  dual('DUAL', 3);

  final String name;
  final int value;

  const DeviceType(this.name, this.value);
}
