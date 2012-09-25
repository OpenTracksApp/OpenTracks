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
    backupPreferencesListener = ApiAdapterFactory.getApiAdapter()
        .getBackupPreferencesListener(this);
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
   * Configures a preference.
   * 
   * @param preference the preference
   * @param options the list of displayed options
   * @param values the list of stored values
   * @param summaryId the summary id
   * @param value the stored value
   */
  protected void configurePreference(final Preference preference, final String[] options,
      final String[] values, final int summaryId, String value) {
    configurePreference(preference, options, values, summaryId, value, null);
  }

  /**
   * Configures a preference.
   * 
   * @param preference the preference
   * @param options the list of displayed options
   * @param values the list of stored values
   * @param summaryId the summary id
   * @param value the stored value
   * @param listener listener to invoke
   */
  protected void configurePreference(final Preference preference, final String[] options,
      final String[] values, final int summaryId, String value,
      final OnPreferenceChangeListener listener) {
    if (options != null) {
      ((ListPreference) preference).setEntries(options);
    }
    if (values != null) {
      ((ListPreference) preference).setEntryValues(values);
    }
    preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference pref, Object newValue) {
        updatePreferenceSummary(pref, options, values, summaryId, (String) newValue);
        if (listener != null) {
          listener.onPreferenceChange(pref, newValue);
        }
        return true;
      }
    });
    updatePreferenceSummary(preference, options, values, summaryId, value);
    if (listener != null) {
      listener.onPreferenceChange(preference, value);
    }
  }

  /**
   * Updates a preference when a stored value changes.
   * 
   * @param preference the preference
   * @param options the list of displayed options
   * @param values the list of stored values
   * @param summaryId the summary id
   * @param value the stored value
   */
  private void updatePreferenceSummary(
      Preference preference, String[] options, String[] values, int summaryId, String value) {
    String summary = getString(summaryId);
    String option;
    if (options != null && values != null) {
      option = getOption(options, values, value);
      if (option == null) {
        option = getString(R.string.value_unknown);
      }
    } else {
      option = value != null && value.length() != 0 ? value : getString(R.string.value_unknown);
    }
    summary += "\n" + option;
    preference.setSummary(summary);
  }

  /**
   * Gets the display option for a stored value.
   * 
   * @param options the list of the display options
   * @param values the list of the stored values
   * @param value the store value
   */
  private String getOption(String[] options, String[] values, String value) {
    for (int i = 0; i < values.length; i++) {
      if (value.equals(values[i])) {
        return options[i];
      }
    }
    return null;
  }
}
