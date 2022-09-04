package de.dennisguse.opentracks.settings.bluetooth;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.PreferenceDialogFragmentCompat;

import de.dennisguse.opentracks.sensors.BluetoothUtils;

public class BluetoothLeCyclingCadenceAndSpeedPreference extends BluetoothLeSensorPreference {

    public BluetoothLeCyclingCadenceAndSpeedPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BluetoothLeCyclingCadenceAndSpeedPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BluetoothLeCyclingCadenceAndSpeedPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothLeCyclingCadenceAndSpeedPreference(Context context) {
        super(context);
    }

    @Override
    public PreferenceDialogFragmentCompat createInstance() {
        return BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(getKey(), BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID);
    }
}
