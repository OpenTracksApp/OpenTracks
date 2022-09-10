package de.dennisguse.opentracks.settings.bluetooth;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.PreferenceDialogFragmentCompat;

import de.dennisguse.opentracks.sensors.BluetoothUtils;

public class BluetoothLeRunningSpeedAndCadencePreference extends BluetoothLeSensorPreference {

    public BluetoothLeRunningSpeedAndCadencePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BluetoothLeRunningSpeedAndCadencePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BluetoothLeRunningSpeedAndCadencePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothLeRunningSpeedAndCadencePreference(Context context) {
        super(context);
    }

    @Override
    public PreferenceDialogFragmentCompat createInstance() {
        return BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(getKey(), BluetoothUtils.RUNNING_SPEED_CADENCE);
    }
}
