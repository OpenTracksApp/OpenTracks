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
import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.maps.mytracks.R;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
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
  private static final String ACCOUNT_NAME_KEY = "accountName";
  private static final int DIALOG_CONFIRM_SWITCH_ACCOUNT = 0;
  private static final int DIALOG_CONFIRM_DRIVE_SYNC_ON = 1;
  private static final int DRIVE_REQUEST_CODE = 0;
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
        if (googleAccount == null
            || googleAccount.equals(PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT)) {
          updateUiByAccountName(newGoogleAccount);
          return true;
        }
        if (!googleAccount.equals(newGoogleAccount)) {
          Bundle newBundle = new Bundle();
          newBundle.putString(ACCOUNT_NAME_KEY, newGoogleAccount);
          showDialog(DIALOG_CONFIRM_SWITCH_ACCOUNT, newBundle);
        }
        return false;
      }
    });

    driveSyncPreference = (CheckBoxPreference) findPreference(getString(R.string.drive_sync_key));
    driveSyncPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ((Boolean) newValue) {
          showDialog(DIALOG_CONFIRM_DRIVE_SYNC_ON);
        } else {
          handleSync(false);
        }
        return false;
      }
    });  

    updateUiByAccountName(PreferencesUtils.getString(
        this, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT));
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case DRIVE_REQUEST_CODE:
        SendToGoogleUtils.cancelNotification(this, SendToGoogleUtils.DRIVE_NOTIFICATION_ID);
        if (resultCode == Activity.RESULT_OK) {
          onDrivePermissionSuccess();
        } else {
          onDrivePermissionFailure();
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
            this, R.string.settings_google_account_confirm_message, null);
        break;
      case DIALOG_CONFIRM_DRIVE_SYNC_ON:
        dialog = DialogUtils.createConfirmationDialog(this, R.string.sync_drive_title,
            R.string.sync_drive_confirm_message, null, new DialogInterface.OnClickListener() {
                @Override
              public void onClick(DialogInterface d, int button) {
                final String googleAccount = PreferencesUtils.getString(GoogleSettingsActivity.this,
                    R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
                /*
                 * This class, a PreferenceActivity, needs to support api level
                 * 8+, thus cannot use CheckPermissionFragment because a
                 * Fragment is only available for api level 11+ and there is no
                 * support library for PreferenceActivity.
                 */
                Thread thread = new Thread(new Runnable() {
                    @Override
                  public void run() {
                    try {
                      SendToGoogleUtils.getGoogleAccountCredential(
                          GoogleSettingsActivity.this, googleAccount,
                          SendToGoogleUtils.DRIVE_SCOPE);
                      handleDriveAccess(true, null);
                    } catch (UserRecoverableAuthException e) {
                      handleDriveAccess(false, e.getIntent());
                    } catch (GoogleAuthException e) {
                      Log.e(TAG, "GoogleAuthException", e);
                      handleDriveAccess(false, null);
                    } catch (UserRecoverableAuthIOException e) {
                      handleDriveAccess(false, e.getIntent());
                    } catch (IOException e) {
                      Log.e(TAG, "IOException", e);
                      handleDriveAccess(false, null);
                    }
                  }
                });
                thread.start();
              }
            }, null);
        break;
      default:
        dialog = null;
    }
    return dialog;
  }

  private void handleDriveAccess(final boolean success, final Intent intent) {
    runOnUiThread(new Runnable() {
        @Override
      public void run() {
        if (success) {
          onDrivePermissionSuccess();
        } else {
          if (intent != null) {
            startActivityForResult(intent, DRIVE_REQUEST_CODE);
          } else {
            onDrivePermissionFailure();
          }
        }
      }
    });
  }

  private void onDrivePermissionSuccess() {
    handleSync(true);
  }

  private void onDrivePermissionFailure() {
    Toast.makeText(
        GoogleSettingsActivity.this, R.string.send_google_no_account_message, Toast.LENGTH_LONG)
        .show();
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
            getString(R.string.settings_google_account_confirm_message, googleAccount));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.generic_yes),
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
        alertDialog.setMessage(getString(R.string.sync_drive_confirm_message, googleAccount,
            getString(R.string.my_tracks_app_name)));
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
    SyncUtils.disableSync(this);

    if (value) {

      // Turn on sync
      ContentResolver.setMasterSyncAutomatically(true);

      // Enable sync for account
      String googleAccount = PreferencesUtils.getString(
          this, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
      Account[] accounts = AccountManager.get(this).getAccountsByType(Constants.ACCOUNT_TYPE);
      for (Account account : accounts) {
        if (account.name.equals(googleAccount)) {
          SyncUtils.enableSync(account);
          break;
        }
      }
    } else {
      SyncUtils.clearSyncState(this);
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
}
