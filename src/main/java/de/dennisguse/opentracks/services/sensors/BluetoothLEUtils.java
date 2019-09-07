package de.dennisguse.opentracks.services.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

final class BluetoothLEUtils {

    private BluetoothLEUtils() {
    }

    static int parseHeartRate(BluetoothGattCharacteristic characteristic) {
        //DOCUMENTATION https://www.bluetooth.com/specifications/gatt/characteristics/
        byte[] raw = characteristic.getValue();
        int index = ((raw[0] & 0x1) == 1) ? 2 : 1;
        int format = (index == 1) ? BluetoothGattCharacteristic.FORMAT_UINT8 : BluetoothGattCharacteristic.FORMAT_UINT16;
        return characteristic.getIntValue(format, index);
    }
}
