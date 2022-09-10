package de.dennisguse.opentracks.settings;

import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeSensorPreference;

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
        if (preference instanceof BluetoothLeSensorPreference) {
            DialogFragment dialogFragment = ((BluetoothLeSensorPreference) preference).createInstance();
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
