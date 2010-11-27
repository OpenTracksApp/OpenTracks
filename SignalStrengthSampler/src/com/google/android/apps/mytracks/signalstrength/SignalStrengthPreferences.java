/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks.signalstrength;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

/**
 * Main signal strength sampler activity, which displays preferences.
 *
 * @author Rodrigo Damazio
 */
public class SignalStrengthPreferences extends PreferenceActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences);

    // Attach service control funciontality
    findPreference(getString(R.string.settings_control_start_key))
        .setOnPreferenceClickListener(new OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            SignalStrengthService.startService(SignalStrengthPreferences.this);
            return true;
          }
        });
    findPreference(getString(R.string.settings_control_stop_key))
        .setOnPreferenceClickListener(new OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            SignalStrengthService.stopService(SignalStrengthPreferences.this);
            return true;
          }
        });

    // TODO: Check that my tracks is installed - if not, give a warning and
    //       offer to go to the android market.
  }
}