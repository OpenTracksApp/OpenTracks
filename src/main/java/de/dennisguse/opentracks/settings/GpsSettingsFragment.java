package de.dennisguse.opentracks.settings;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.DistanceFormatter;

public class GpsSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_gps);

        UnitSystem unitSystem = PreferencesUtils.getUnitSystem();

        final DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalCount(0)
                .setUnit(unitSystem)
                .build(getContext());

        findPreference(getString(R.string.recording_distance_interval_key))
                .setSummaryProvider(
                        preference -> {
                            Distance distance = PreferencesUtils.getRecordingDistanceInterval();
                            return getString(R.string.settings_recording_location_frequency_summary, formatter.formatDistance(distance));
                        }
                );

        findPreference(getString(R.string.max_recording_distance_key))
                .setSummaryProvider(
                        preference -> {
                            Distance distance = PreferencesUtils.getMaxRecordingDistance();
                            return getString(R.string.settings_recording_max_recording_distance_summary, formatter.formatDistance(distance));
                        }
                );

        findPreference(getString(R.string.recording_gps_accuracy_key))
                .setSummaryProvider(
                        preference -> {
                            Distance distance = PreferencesUtils.getThresholdHorizontalAccuracy();
                            return getString(R.string.settings_recording_min_required_accuracy_summary, formatter.formatDistance(distance));
                        }
                );

        findPreference(getString(R.string.min_recording_interval_key))
                .setSummaryProvider(
                        preference -> {
                            Duration interval = PreferencesUtils.getMinRecordingInterval();
                            return getString(R.string.settings_recording_location_frequency_summary, getString(R.string.value_integer_second, interval.getSeconds()));
                        }
                );
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_gps_title);
    }

    @Override
    public void onResume() {
        super.onResume();

        ListPreference minRecordingInterval = findPreference(getString(R.string.min_recording_interval_key));
        minRecordingInterval.setEntries(PreferencesUtils.getMinRecordingIntervalEntries());

        ListPreference recordingDistanceInterval = findPreference(getString(R.string.recording_distance_interval_key));
        recordingDistanceInterval.setEntries(PreferencesUtils.getRecordingDistanceIntervalEntries());

        ListPreference maxRecordingDistance = findPreference(getString(R.string.max_recording_distance_key));
        maxRecordingDistance.setEntries(PreferencesUtils.getMaxRecordingDistanceEntries());

        ListPreference recordingGpsAccuracy = findPreference(getString(R.string.recording_gps_accuracy_key));
        recordingGpsAccuracy.setEntries(PreferencesUtils.getThresholdHorizontalAccuracyEntries());

        ListPreference idleDuration = findPreference(getString(R.string.idle_duration_key));
        idleDuration.setEntries(PreferencesUtils.getIdleDurationEntries());
    }
}
