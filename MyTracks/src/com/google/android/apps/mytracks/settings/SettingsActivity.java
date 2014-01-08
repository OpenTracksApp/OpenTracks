/*
 * Copyright 2008 Google Inc.
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
import com.google.android.apps.mytracks.services.tasks.CheckPermissionAsyncTask;
import com.google.android.apps.mytracks.services.tasks.CheckPermissionAsyncTask.CheckPermissionCaller;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

/**
 * An activity for accessing settings.
 * 
 * @author Jimmy Shih
 */
public class SettingsActivity extends AbstractSettingsActivity implements CheckPermissionCaller {

  private static final int DIALOG_NO_ACCOUNT = 0;
  private static final int DIALOG_CHOOSE_ACCOUNT = 1;
  private static final int DIALOG_CONFIRM_DRIVE_SYNC_ON = 2;

  private static final int DRIVE_REQUEST_CODE = 0;

  private CheckBoxPreference syncDrivePreference;
  private CheckPermissionAsyncTask syncDriveAsyncTask;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof CheckPermissionAsyncTask) {
      syncDriveAsyncTask = (CheckPermissionAsyncTask) retained;
      syncDriveAsyncTask.setActivity(this);
    }

    addPreferencesFromResource(R.xml.settings);

    syncDrivePreference = (CheckBoxPreference) findPreference(getString(R.string.drive_sync_key));
    syncDrivePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ((Boolean) newValue) {
          Account[] accounts = AccountManager.get(SettingsActivity.this)
              .getAccountsByType(Constants.ACCOUNT_TYPE);
          if (accounts.length == 0) {
            PreferencesUtils.setString(SettingsActivity.this, R.string.google_account_key,
                PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
            handleSync(false);
            showDialog(DIALOG_NO_ACCOUNT);
          } else if (accounts.length == 1) {
            PreferencesUtils.setString(
                SettingsActivity.this, R.string.google_account_key, accounts[0].name);
            showDialog(DIALOG_CONFIRM_DRIVE_SYNC_ON);
          } else {
            showDialog(DIALOG_CHOOSE_ACCOUNT);
          }
        } else {
          PreferencesUtils.setString(SettingsActivity.this, R.string.google_account_key,
              PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
          handleSync(false);
        }
        return false;
      }
    });
    updateSyncDriveSummary();

    configPreference(R.string.settings_map_key, MapSettingsActivity.class);
    configPreference(R.string.settings_chart_key, ChartSettingsActivity.class);
    configPreference(R.string.settings_stats_key, StatsSettingsActivity.class);
    configPreference(R.string.settings_recording_key, RecordingSettingsActivity.class);
    configPreference(R.string.settings_sensor_key, SensorSettingsActivity.class);
    configPreference(R.string.settings_advanced_key, AdvancedSettingsActivity.class);
  }

  @Deprecated
  public Object onRetainNonConfigurationInstance() {
    if (syncDriveAsyncTask != null) {
      syncDriveAsyncTask.setActivity(null);
    }
    return syncDriveAsyncTask;
  }

  @Override
  protected Dialog onCreateDialog(int id, Bundle bundle) {
    final Dialog dialog;
    switch (id) {
      case DIALOG_NO_ACCOUNT:
        dialog = new AlertDialog.Builder(this).setMessage(R.string.send_google_no_account_message)
            .setTitle(R.string.send_google_no_account_title)
            .setPositiveButton(R.string.generic_ok, null).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
          public void onShow(DialogInterface dialogInterface) {
            DialogUtils.setDialogTitleDivider(SettingsActivity.this, dialog);
          }
        });
        break;
      case DIALOG_CHOOSE_ACCOUNT:
        Account[] accounts = AccountManager.get(SettingsActivity.this)
            .getAccountsByType(Constants.ACCOUNT_TYPE);
        final String[] choices = new String[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
          choices[i] = accounts[i].name;
        }
        dialog = new AlertDialog.Builder(this).setNegativeButton(R.string.generic_cancel, null)
            .setPositiveButton(R.string.generic_ok, new OnClickListener() {
                @SuppressWarnings("deprecation")
                @Override
              public void onClick(DialogInterface dialogInterface, int which) {
                int position = ((AlertDialog) dialogInterface).getListView()
                    .getCheckedItemPosition();
                PreferencesUtils.setString(
                    SettingsActivity.this, R.string.google_account_key, choices[position]);
                dismissDialog(DIALOG_CHOOSE_ACCOUNT);
                showDialog(DIALOG_CONFIRM_DRIVE_SYNC_ON);
              }
            }).setSingleChoiceItems(choices, 0, null)
            .setTitle(R.string.send_google_choose_account_title).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
          public void onShow(DialogInterface dialogInterface) {
            DialogUtils.setDialogTitleDivider(SettingsActivity.this, dialog);
          }
        });
        break;
      case DIALOG_CONFIRM_DRIVE_SYNC_ON:
        dialog = DialogUtils.createConfirmationDialog(this, R.string.sync_drive_confirm_title,
            getString(R.string.sync_drive_confirm_message), new DialogInterface.OnClickListener() {
                @Override
              public void onClick(DialogInterface d, int button) {
                if (syncDriveAsyncTask == null) {
                  String googleAccount = PreferencesUtils.getString(SettingsActivity.this,
                      R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
                  syncDriveAsyncTask = new CheckPermissionAsyncTask(
                      SettingsActivity.this, googleAccount, SendToGoogleUtils.DRIVE_SCOPE);
                  syncDriveAsyncTask.execute();
                }
              }
            });
        break;
      default:
        dialog = null;
    }
    return dialog;
  }

  @SuppressWarnings("deprecation")
  @Override
  protected void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
    if (id == DIALOG_CONFIRM_DRIVE_SYNC_ON) {
      AlertDialog alertDialog = (AlertDialog) dialog;
      String googleAccount = PreferencesUtils.getString(
          this, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
      alertDialog.setMessage(getString(R.string.sync_drive_confirm_message, googleAccount,
          getString(R.string.my_tracks_app_name)));
    }
    super.onPrepareDialog(id, dialog, bundle);
  }

  @Override
  public void onCheckPermissionDone(String scope, boolean success, Intent userRecoverableIntent) {
    syncDriveAsyncTask = null;
    if (success) {
      onDrivePermissionSuccess();
    } else {
      if (userRecoverableIntent != null) {
        startActivityForResult(userRecoverableIntent, DRIVE_REQUEST_CODE);
      } else {
        onDrivePermissionFailure();
      }
    }
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

  private void onDrivePermissionSuccess() {
    handleSync(true);
  }

  private void onDrivePermissionFailure() {
    Toast.makeText(
        SettingsActivity.this, R.string.send_google_no_account_permission, Toast.LENGTH_LONG)
        .show();
  }

  /**
   * Handles sync.
   * 
   * @param value true to sync
   */
  private void handleSync(boolean value) {
    syncDrivePreference.setChecked(value);
    updateSyncDriveSummary();

    if (value) {
      SyncUtils.enableSync(this);     
    } else {     
      SyncUtils.disableSync(this);
    }
  }

  /**
   * Updates UI by account.
   */
  private void updateSyncDriveSummary() {
    String googleAccount = PreferencesUtils.getString(
        this, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    boolean hasAccount = !PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT.equals(googleAccount);
    syncDrivePreference.setSummaryOn(
        hasAccount ? getString(R.string.settings_google_drive_sync_summary_on, googleAccount) : "");
  }

  /**
   * Configures a preference by starting a new activity when it is clicked.
   * 
   * @param key the preference key
   * @param cl the class to start the new activity
   */
  @SuppressWarnings("deprecation")
  private void configPreference(int key, final Class<?> cl) {
    Preference preference = findPreference(getString(key));
    preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference pref) {
        Intent intent = IntentUtils.newIntent(SettingsActivity.this, cl);
        startActivity(intent);
        return true;
      }
    });
  }
}
