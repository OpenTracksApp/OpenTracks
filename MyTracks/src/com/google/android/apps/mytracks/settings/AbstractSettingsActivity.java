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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.io.backup.BackupPreferencesListener;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.MenuItem;

/**
 * An abstract activity for all the settings activities.
 * 
 * @author Jimmy Shih
 */
public class AbstractSettingsActivity extends PreferenceActivity {

  private BackupPreferencesListener backupPreferencesListener;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);
    ApiAdapterFactory.getApiAdapter().configureActionBarHomeAsUp(this);

    PreferenceManager preferenceManager = getPreferenceManager();
    preferenceManager.setSharedPreferencesName(Constants.SETTINGS_NAME);
    preferenceManager.setSharedPreferencesMode(Context.MODE_PRIVATE);

    // Set up automatic preferences backup
    backupPreferencesListener = new BackupPreferencesListener(this);

    preferenceManager.getSharedPreferences()
        .registerOnSharedPreferenceChangeListener(backupPreferencesListener);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() != android.R.id.home) {
      return super.onOptionsItemSelected(item);
    }
    finish();
    return true;
  }

  @SuppressWarnings("deprecation")
  @Override
  protected void onDestroy() {
    super.onDestroy();
    PreferenceManager preferenceManager = getPreferenceManager();
    preferenceManager.getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(backupPreferencesListener);
  }

  /**
   * Configures a list preference.
   * 
   * @param listPreference the list preference
   * @param summary the summary array
   * @param options the options array
   * @param values the values array
   * @param value the value
   * @param listener optional listener
   */
  protected void configureListPreference(ListPreference listPreference, final String[] summary,
      final String[] options, final String[] values, String value,
      final OnPreferenceChangeListener listener) {
    listPreference.setEntryValues(values);
    listPreference.setEntries(options);
    listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference pref, Object newValue) {
        updatePreferenceSummary(pref, summary, values, (String) newValue);
        if (listener != null) {
          listener.onPreferenceChange(pref, newValue);
        }
        return true;
      }
    });
    updatePreferenceSummary(listPreference, summary, values, value);
    if (listener != null) {
      listener.onPreferenceChange(listPreference, value);
    }
  }

  /**
   * Update the preference summary.
   * 
   * @param preference the preference
   * @param summary the summary array
   * @param values the values array
   * @param value the value
   */
  private void updatePreferenceSummary(
      Preference preference, String[] summary, String[] values, String value) {
    int index = getIndex(values, value);
    if (index == -1) {
      preference.setSummary(R.string.value_unknown);
    } else {
      preference.setSummary(summary[index]);
    }
  }

  /**
   * Get the array index for a value.
   * 
   * @param values the array
   * @param value the value
   */
  private int getIndex(String[] values, String value) {
    for (int i = 0; i < values.length; i++) {
      if (value.equals(values[i])) {
        return i;
      }
    }
    return -1;
  }
}
