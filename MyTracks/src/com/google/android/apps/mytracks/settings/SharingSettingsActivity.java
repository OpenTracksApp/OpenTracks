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

package com.google.android.apps.mytracks.settings;

import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

/**
 * An activity for accessing the sharing settings.
 * 
 * @author Jimmy Shih
 */
public class SharingSettingsActivity extends AbstractSettingsActivity {

  private static final int DIALOG_CONFIRM_ALLOW_ACCESS_ID = 0;

  private CheckBoxPreference allowAccessCheckBoxPreference;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.sharing_settings);

    allowAccessCheckBoxPreference = (CheckBoxPreference) findPreference(
        getString(R.string.allow_access_key));
    allowAccessCheckBoxPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ((Boolean) newValue) {
          showDialog(DIALOG_CONFIRM_ALLOW_ACCESS_ID);
          return false;
        } else {
          return true;
        }
      }
    });
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id != DIALOG_CONFIRM_ALLOW_ACCESS_ID) {
      return null;
    }
    return DialogUtils.createConfirmationDialog(this,
        R.string.settings_sharing_allow_access_confirm_message,
        new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int button) {
            allowAccessCheckBoxPreference.setChecked(true);
          }
        });
  }
}
