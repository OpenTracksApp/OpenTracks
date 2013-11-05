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
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

/**
 * An activity for advanced settings.
 * 
 * @author Jimmy Shih
 */
public class AdvancedSettingsActivity extends AbstractSettingsActivity {

  private static final int DIALOG_CONFIRM_ALLOW_ACCESS_ID = 0;

  private CheckBoxPreference allowAccessCheckBoxPreference;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.advanced_settings);

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

    ListPreference preference = (ListPreference) findPreference(getString(R.string.photo_size_key));
    int value = PreferencesUtils.getInt(
        this, R.string.photo_size_key, PreferencesUtils.PHOTO_SIZE_DEFAULT);
    String[] values = getResources().getStringArray(R.array.photo_size_values);
    String[] options = new String[values.length];
    String[] summary = new String[values.length];
    setPhotoSizeSummaryAndOptions(summary, options, values);
    configureListPreference(preference, summary, options, values, String.valueOf(value), null);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id != DIALOG_CONFIRM_ALLOW_ACCESS_ID) {
      return null;
    }
    return DialogUtils.createConfirmationDialog(this,
        R.string.settings_sharing_allow_access_confirm_title,
        getString(R.string.settings_sharing_allow_access_confirm_message),
        new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int button) {
            allowAccessCheckBoxPreference.setChecked(true);
          }
        });
  }

  private void setPhotoSizeSummaryAndOptions(String[] summary, String[] options, String[] values) {
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      if (value == -1) {
        options[i] = getString(R.string.settings_advanced_photo_size_original);
        summary[i] = getString(R.string.settings_advanced_photo_size_original);
      } else if (value < 1024) {
        options[i] = getString(R.string.value_integer_kilobyte, value);
        summary[i] = getString(R.string.settings_advanced_photo_size_summary, options[i]);
      } else {
        int megabyte = value / 1024;
        options[i] = getString(R.string.value_integer_megabyte, megabyte);
        summary[i] = getString(R.string.settings_advanced_photo_size_summary, options[i]);
      }
    }
  }
}
