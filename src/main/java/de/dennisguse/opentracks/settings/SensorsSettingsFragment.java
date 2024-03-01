package de.dennisguse.opentracks.settings;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeSensorPreference;
import de.dennisguse.opentracks.util.PermissionRequester;

public class SensorsSettingsFragment extends PreferenceFragmentCompat {

    private final static String TAG = SensorsSettingsFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_sensors);
        setWheelCircumferenceInputFilter();

        UnitSystem unitSystem = PreferencesUtils.getUnitSystem();

        final DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalCount(0)
                .setUnit(unitSystem)
                .build(getContext());

        findPreference(getString(R.string.recording_gps_accuracy_key))
                .setSummaryProvider(
                        preference -> {
                            Distance distance = PreferencesUtils.getThresholdHorizontalAccuracy();
                            return getString(R.string.settings_recording_min_required_accuracy_summary, formatter.formatDistance(distance));
                        }
                );

        findPreference(getString(R.string.min_sampling_interval_key))
                .setSummaryProvider(
                        preference -> {
                            Duration interval = PreferencesUtils.getMinSamplingInterval();
                            return getString(R.string.settings_recording_location_frequency_summary, getString(R.string.value_integer_second, interval.getSeconds()));
                        }
                );
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_sensors_title);
    }


    @Override
    public void onResume() {
        super.onResume();

        ListPreference minSamplingInterval = findPreference(getString(R.string.min_sampling_interval_key));
        minSamplingInterval.setEntries(PreferencesUtils.getMinSamplingIntervalEntries());

        ListPreference recordingGpsAccuracy = findPreference(getString(R.string.recording_gps_accuracy_key));
        recordingGpsAccuracy.setEntries(PreferencesUtils.getThresholdHorizontalAccuracyEntries());
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
