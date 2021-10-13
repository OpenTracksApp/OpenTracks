package de.dennisguse.opentracks.settings;

import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeCyclingCadenceAndSpeedPreference;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeCyclingPowerPreference;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeHeartRatePreference;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeRunningSpeedAndCadencePreference;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeSensorPreference;
import de.dennisguse.opentracks.util.BluetoothUtils;

public class SensorsSettingsFragment extends PreferenceFragmentCompat {

    private final static String TAG = SensorsSettingsFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_sensors);
        setWheelCircumferenceInputFilter();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_sensors_title);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;
        if (preference instanceof BluetoothLeHeartRatePreference) {
            dialogFragment = BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(preference.getKey(), BluetoothUtils.HEART_RATE_SUPPORTING_DEVICES);
        } else if (preference instanceof BluetoothLeCyclingCadenceAndSpeedPreference) {
            dialogFragment = BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(preference.getKey(), BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID);
        } else if (preference instanceof BluetoothLeCyclingPowerPreference) {
            dialogFragment = BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(preference.getKey(), BluetoothUtils.CYCLING_POWER_UUID);
        } else if (preference instanceof BluetoothLeRunningSpeedAndCadencePreference) {
            dialogFragment = BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(preference.getKey(), BluetoothUtils.RUNNING_RUNNING_SPEED_CADENCE_UUID);
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), getClass().getSimpleName());
            return;
        }

        super.onDisplayPreferenceDialog(preference);
    }

    private void setWheelCircumferenceInputFilter() {
        EditTextPreference wheelPreference = findPreference(getString(R.string.settings_sensor_bluetooth_cycling_speed_wheel_circumference_key));
        wheelPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue instanceof String) {
                try {
                    int newValueInt = Integer.parseInt((String) newValue);
                    return newValueInt > 500 && newValueInt < 4000;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Entered string is no number.");
                }
            }
            return false;
        });
    }
}
