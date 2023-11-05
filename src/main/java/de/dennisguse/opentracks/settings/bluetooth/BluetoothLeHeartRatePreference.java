package de.dennisguse.opentracks.settings.bluetooth;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.PreferenceDialogFragmentCompat;

import de.dennisguse.opentracks.sensors.BluetoothHandlerManagerHeartRate;

public class BluetoothLeHeartRatePreference extends BluetoothLeSensorPreference {

    public BluetoothLeHeartRatePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BluetoothLeHeartRatePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BluetoothLeHeartRatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothLeHeartRatePreference(Context context) {
        super(context);
    }

    @Override
    public PreferenceDialogFragmentCompat createInstance() {
        return BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(getKey(), BluetoothHandlerManagerHeartRate.HEART_RATE_SUPPORTING_DEVICES);
    }
}