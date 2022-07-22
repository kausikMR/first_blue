enum BlueFilter {
  all('All'),
  onlyBLE('BLE'),
  onlyClassic('Classic'),
  onlyDual('Dual');

  final String name;

  const BlueFilter(this.name);
}

enum AppState {
  bluetoothDisabled('BLUETOOTH_DISABLED'),
  locationDisabled('LOCATION_DISABLED'),
  satisfied('SATISFIED'),
  error('ERROR');

  final String name;

  const AppState(this.name);
}
