package de.dennisguse.opentracks.settings;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeSensorPreference;
import de.dennisguse.opentracks.util.PermissionRequester;

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
            if(!PermissionRequester.BLUETOOTH.hasPermission(preference.getContext())) {
                // TODO: maybe use requestPermissionsIfNeeded here or disable preferences at all
                FragmentActivity activity = getActivity();
                Toast.makeText(activity, getString(R.string.permission_bluetooth_failed), Toast.LENGTH_LONG).show();
                return;
            }

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
            if (newValue instanceof String newValueString) {
                try {
                    int newValueInt = Integer.parseInt(newValueString);
                    return newValueInt >= 100 && newValueInt < 4000;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Entered string is no number.");
                }
            }
            return false;
        });
    }
}
