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
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An activity for accessing the Google settings.
 * 
 * @author Jimmy Shih
 */
public class GoogleSettingsActivity extends AbstractSettingsActivity {

  private static final String TAG = GoogleSettingsActivity.class.getSimpleName();
  private static final int REQUEST_AUTHORIZATION = 1;

  private static final String ACCOUNT_NAME_KEY = "accountName";
  private static final int DIALOG_CONFIRM_SWITCH_ACCOUNT = 0;
  private static final int DIALOG_CONFIRM_DRIVE_SYNC_ON = 1;
  private static final int DIALOG_CONFIRM_DRIVE_SYNC_OFF = 2;

  private ListPreference googleAccountPreference;
  private CheckBoxPreference driveSyncPreference;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.google_settings);

    googleAccountPreference = (ListPreference) findPreference(
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

    googleAccountPreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
    googleAccountPreference.setEntryValues(entryValues.toArray(new CharSequence[entries.size()]));
    googleAccountPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        String newGoogleAccount = (String) newValue;
        String googleAccount = PreferencesUtils.getString(
            GoogleSettingsActivity.this, R.string.google_account_key,
            PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
        boolean driveSync = PreferencesUtils.getBoolean(
            GoogleSettingsActivity.this, R.string.drive_sync_key,
            PreferencesUtils.DRIVE_SYNC_DEFAULT);
        if (driveSync && !newGoogleAccount.equals(googleAccount)) {
          Bundle newBundle = new Bundle();
          newBundle.putString(ACCOUNT_NAME_KEY, newGoogleAccount);
          showDialog(DIALOG_CONFIRM_SWITCH_ACCOUNT, newBundle);
          return false;
        }
        updateUiByAccountName(newGoogleAccount);
        return true;
      }
    });

    driveSyncPreference = (CheckBoxPreference) findPreference(getString(R.string.drive_sync_key));
    driveSyncPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        showDialog(
            (Boolean) newValue ? DIALOG_CONFIRM_DRIVE_SYNC_ON : DIALOG_CONFIRM_DRIVE_SYNC_OFF);
        return false;
      }
    });

    CheckBoxPreference defaultMapPublicPreference = (CheckBoxPreference) findPreference(
        getString(R.string.default_map_public_key));
    defaultMapPublicPreference.setSummaryOn(getString(
        R.string.settings_google_maps_public_summary_on,
        getString(R.string.maps_public_unlisted_url)));
    defaultMapPublicPreference.setSummaryOff(getString(
        R.string.settings_google_maps_public_summary_off,
        getString(R.string.maps_public_unlisted_url)));

    updateUiByAccountName(PreferencesUtils.getString(
        this, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT));
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_AUTHORIZATION:
        if (resultCode == Activity.RESULT_OK) {
          handleSync(true);
        } else {
          showDriveNoPermission(this);
        }
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected Dialog onCreateDialog(int id, Bundle bundle) {
    Dialog dialog;
    switch (id) {
      case DIALOG_CONFIRM_SWITCH_ACCOUNT:
        dialog = DialogUtils.createConfirmationDialog(
            this, R.string.settings_google_drive_sync_confirm_message_off, null);
        break;
      case DIALOG_CONFIRM_DRIVE_SYNC_ON:
        dialog = DialogUtils.createConfirmationDialog(this,
            R.string.settings_google_drive_sync_confirm_message_on,
            new DialogInterface.OnClickListener() {
                @Override
              public void onClick(DialogInterface d, int button) {
                String googleAccount = PreferencesUtils.getString(
                    GoogleSettingsActivity.this, R.string.google_account_key,
                    PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
                checkDrivePermission(GoogleSettingsActivity.this, googleAccount);
              }
            });
        break;
      case DIALOG_CONFIRM_DRIVE_SYNC_OFF:
        dialog = DialogUtils.createConfirmationDialog(this,
            R.string.settings_google_drive_sync_confirm_message_off,
            new DialogInterface.OnClickListener() {
                @Override
              public void onClick(DialogInterface d, int button) {
                handleSync(false);
              }
            });
        break;
      default:
        dialog = null;
    }
    return dialog;
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
    AlertDialog alertDialog = (AlertDialog) dialog;
    String googleAccount = PreferencesUtils.getString(
        this, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    switch (id) {
      case DIALOG_CONFIRM_SWITCH_ACCOUNT:
        final String newValue = bundle.getString(ACCOUNT_NAME_KEY);
        alertDialog.setMessage(
            getString(R.string.settings_google_drive_sync_confirm_message_off, googleAccount));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
            new DialogInterface.OnClickListener() {
                @Override
              public void onClick(DialogInterface d, int button) {
                googleAccountPreference.setValue(newValue);
                handleSync(false);
                updateUiByAccountName(newValue);
              }
            });
        break;
      case DIALOG_CONFIRM_DRIVE_SYNC_ON:
        alertDialog.setMessage(getString(
            R.string.settings_google_drive_sync_confirm_message_on, googleAccount,
            getString(R.string.my_tracks_app_name)));
        break;
      case DIALOG_CONFIRM_DRIVE_SYNC_OFF:
        alertDialog.setMessage(
            getString(R.string.settings_google_drive_sync_confirm_message_off, googleAccount));
        break;
      default:
    }
    super.onPrepareDialog(id, dialog, bundle);
  }

  /**
   * Handles sync.
   * 
   * @param value true to sync
   */
  private void handleSync(boolean value) {
    driveSyncPreference.setChecked(value);

    // Turn off everything
    Account[] accounts = AccountManager.get(GoogleSettingsActivity.this)
        .getAccountsByType(Constants.ACCOUNT_TYPE);
    for (Account account : accounts) {
      SyncUtils.disableSync(account);
    }

    PreferencesUtils.setLong(this, R.string.drive_largest_change_id_key,
        PreferencesUtils.DRIVE_LARGEST_CHANGE_ID_DEFAULT);
    PreferencesUtils.setString(
        this, R.string.drive_deleted_list_key, PreferencesUtils.DRIVE_DELETED_LIST_DEFAULT);
    MyTracksProviderUtils myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);

    Cursor cursor = myTracksProviderUtils.getTrackCursor(SyncUtils.DRIVE_IDS_QUERY, null, null);
    if (cursor != null && cursor.moveToFirst()) {
      do {
        Track track = myTracksProviderUtils.createTrack(cursor);
        track.setDriveId("");
        track.setModifiedTime(-1L);
        track.setSharedWithMe(false);
        myTracksProviderUtils.updateTrack(track);
      } while (cursor.moveToNext());
    }

    if (value) {

      // Turn on sync
      ContentResolver.setMasterSyncAutomatically(true);

      // Enable sync for account
      String googleAccount = PreferencesUtils.getString(GoogleSettingsActivity.this,
          R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
      for (Account account : accounts) {
        if (account.name.equals(googleAccount)) {
          SyncUtils.enableSync(account);
          break;
        }
      }
    }
  }

  /**
   * Updates UI by account.
   * 
   * @param accountName the account name
   */
  private void updateUiByAccountName(String accountName) {
    boolean hasAccount = !PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT.equals(accountName);
    googleAccountPreference.setSummary(
        hasAccount ? accountName : getString(R.string.value_unknown));
    driveSyncPreference.setEnabled(hasAccount);
    driveSyncPreference.setSummaryOn(
        getString(R.string.settings_google_drive_sync_summary_on, accountName));
  }

  /**
   * Checks Drive permission.
   * 
   * @param context the context
   * @param accountName the account name
   */
  private void checkDrivePermission(final Context context, final String accountName) {
    Thread thread = new Thread(new Runnable() {
        @Override
      public void run() {
        try {
          SyncUtils.checkDrivePermission(context, accountName);
          runOnUiThread(new Runnable() {
              @Override
            public void run() {
              handleSync(true);
            }
          });
        } catch (UserRecoverableAuthException e) {
          startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
        } catch (IOException e) {
          Log.e(TAG, "IOException", e);
          showDriveNoPermission(context);
        } catch (GoogleAuthException e) {
          Log.e(TAG, "GoogleAuthException", e);
          showDriveNoPermission(context);
        }
      }
    });
    thread.start();
  }

  /**
   * Shows Drive no permission toast.
   * 
   * @param context the context
   */
  private void showDriveNoPermission(Context context) {
    Toast.makeText(context, R.string.settings_google_drive_sync_no_permission, Toast.LENGTH_LONG)
        .show();
  }
}
