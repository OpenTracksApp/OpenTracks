package de.dennisguse.opentracks.settings.bluetooth;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.List;

import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingDistanceSpeed;

public class BluetoothLeCyclingSpeedPreference extends BluetoothLeSensorPreference {

    public BluetoothLeCyclingSpeedPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BluetoothLeCyclingSpeedPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BluetoothLeCyclingSpeedPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothLeCyclingSpeedPreference(Context context) {
        super(context);
    }

    @Override
    public PreferenceDialogFragmentCompat createInstance() {
        return BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(getKey(), List.of(BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE));
    }
}
