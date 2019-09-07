/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.settings;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * An activity for accessing stats settings.
 *
 * @author Jimmy Shih
 */
public class StatsSettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.settings_statistics);

        configUnitsListPreference();
        boolean metricUnits = PreferencesUtils.STATS_UNITS_DEFAULT.equals(PreferencesUtils.getString(this, R.string.stats_units_key, ""));
        configRateListPreference(metricUnits);
    }

    /**
     * Configures the preferred units list preference.
     */
    private void configUnitsListPreference() {
        ListPreference listPreference = (ListPreference) findPreference(getString(R.string.stats_units_key));
        final OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                boolean metricUnits = PreferencesUtils.STATS_UNITS_DEFAULT.equals(newValue);
                configRateListPreference(metricUnits);
                return true;
            }
        };
        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                return listener.onPreferenceChange(pref, newValue);
            }
        });
    }

    /**
     * Configures the preferred rate list preference.
     *
     * @param metricUnits true if metric units
     */
    private void configRateListPreference(boolean metricUnits) {
        ListPreference listPreference = (ListPreference) findPreference(getString(R.string.stats_rate_key));
        String[] options = getResources().getStringArray(metricUnits ? R.array.stats_rate_metric_options : R.array.stats_rate_imperial_options);

        listPreference.setEntries(options);
    }
}
