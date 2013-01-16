/*
 * Copyright 2013 Google Inc.
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
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity for accessing the Google settings.
 * 
 * @author Jimmy Shih
 */
public class GoogleSettingsActivity extends AbstractSettingsActivity {

  private ListPreference googleAccount;
  private CheckBoxPreference driveSync;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.google_settings);

    googleAccount = (ListPreference) findPreference(
        getString(R.string.google_account_key));
    List<String> entries = new ArrayList<String>();
    List<String> entryValues = new ArrayList<String>();
    Account[] accounts = AccountManager.get(this).getAccountsByType(Constants.ACCOUNT_TYPE);
    for (Account account : accounts) {
      entries.add(account.name);
      entryValues.add(account.name);
    }
    entries.add(getString(R.string.value_none));
    entryValues.add(PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    
    googleAccount.setEntries(entries.toArray(new CharSequence[entries.size()]));
    googleAccount.setEntryValues(entryValues.toArray(new CharSequence[entries.size()]));
    googleAccount.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        updateUi((String) newValue, true);
        return true;
      }
    });

    driveSync = (CheckBoxPreference) findPreference(
        getString(R.string.drive_sync_key));

    CheckBoxPreference settingsGoogleMapsPublic = (CheckBoxPreference) findPreference(
        getString(R.string.default_map_public_key));
    settingsGoogleMapsPublic.setSummaryOn(getString(R.string.settings_google_maps_public_summary_on,
        getString(R.string.maps_public_unlisted_url)));
    settingsGoogleMapsPublic.setSummaryOff(getString(
        R.string.settings_google_maps_public_summary_off,
        getString(R.string.maps_public_unlisted_url)));

    String account = PreferencesUtils.getString(this, R.string.google_account_key,
        PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    updateUi(account, false);
  }

  /**
   * Updates the UI.
   * 
   * @param account the account
   * @param uncheckDriveSync true to uncheck drive sync
   */
  private void updateUi(String account, boolean uncheckDriveSync) {
    googleAccount.setSummary(PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT.equals(
        account) ? getString(R.string.value_unknown)
        : account);
    boolean hasAccount = !PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT.equals(account);
    driveSync.setEnabled(hasAccount);
    driveSync.setSummaryOn(
        getString(R.string.settings_google_drive_sync_summary_on, account));
    if (uncheckDriveSync) {
      driveSync.setChecked(false);
    }
  }
}
