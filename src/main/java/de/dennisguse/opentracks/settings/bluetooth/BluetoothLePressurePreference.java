package de.dennisguse.opentracks.settings.bluetooth;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.PreferenceDialogFragmentCompat;

import de.dennisguse.opentracks.sensors.BluetoothUtils;

public class BluetoothLePressurePreference extends BluetoothLeSensorPreference {

    public BluetoothLePressurePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BluetoothLePressurePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BluetoothLePressurePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothLePressurePreference(Context context) {
        super(context);
    }

    @Override
    public PreferenceDialogFragmentCompat createInstance() {
        return BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(getKey(), BluetoothUtils.PRESSURE);

    }
}