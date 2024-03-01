package de.dennisguse.opentracks.settings;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.DistanceFormatter;

public class RecordingSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_recording);

        UnitSystem unitSystem = PreferencesUtils.getUnitSystem();

        final DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalCount(0)
                .setUnit(unitSystem)
                .build(getContext());

        findPreference(getString(R.string.recording_distance_interval_key))
                .setSummaryProvider(
                        preference -> {
                            Distance distance = PreferencesUtils.getRecordingDistanceInterval();
                            return getString(R.string.settings_recording_location_distance_summary, formatter.formatDistance(distance));
                        }
                );

        findPreference(getString(R.string.max_recording_distance_key))
                .setSummaryProvider(
                        preference -> {
                            Distance distance = PreferencesUtils.getMaxRecordingDistance();
                            return getString(R.string.settings_recording_max_recording_distance_summary, formatter.formatDistance(distance));
                        }
                );
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_recording_title);
    }

    @Override
    public void onResume() {
        super.onResume();

        ListPreference recordingDistanceInterval = findPreference(getString(R.string.recording_distance_interval_key));
        recordingDistanceInterval.setEntries(PreferencesUtils.getRecordingDistanceIntervalEntries());

        ListPreference maxRecordingDistance = findPreference(getString(R.string.max_recording_distance_key));
        maxRecordingDistance.setEntries(PreferencesUtils.getMaxRecordingDistanceEntries());

        ListPreference idleDuration = findPreference(getString(R.string.idle_duration_key));
        idleDuration.setEntries(PreferencesUtils.getIdleDurationEntries());
    }
}
