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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;

import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.R;

/**
 * An activity for accessing chart settings.
 *
 * @author Jimmy Shih
 */
public class ChartSettingsActivity extends AbstractSettingsActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.settings_chart);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    private void updateUi() {
        CheckBoxPreference speedCheckBoxPreference = (CheckBoxPreference) findPreference(getString(R.string.chart_show_speed_key));
        speedCheckBoxPreference.setTitle(PreferencesUtils.isReportSpeed(this) ? R.string.stats_speed : R.string.stats_pace);
    }

}
