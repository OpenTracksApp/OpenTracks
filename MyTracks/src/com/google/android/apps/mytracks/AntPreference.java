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
package com.google.android.apps.mytracks;

import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * A preference for an ANT device pairing.
 * Currently this shows the ID and lets the user clear that ID for future pairing.
 * TODO: Support pairing from this preference.
 *
 * @author Sandor Dornbush
 */
public class AntPreference extends Preference {

  public AntPreference(Context context) {
    super(context);
    init();
  }

  public AntPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    int sensorId = getPersistedInt(0);
    if (sensorId == 0) {
      setSummary(R.string.settings_ant_not_paired);
    } else {
      setSummary(String.format(getContext().getString(R.string.settings_ant_paired), sensorId));
    }

    // Add actions to allow repairing.
    setOnPreferenceClickListener(
        new OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            AntPreference.this.persistInt(0);
            setSummary(R.string.settings_ant_not_paired);
            return true;
          }
        });
  }
}
