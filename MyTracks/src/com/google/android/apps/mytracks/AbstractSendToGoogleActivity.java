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

package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.fragments.AddEmailsDialogFragment;
import com.google.android.apps.mytracks.fragments.AddEmailsDialogFragment.AddEmailsCaller;
import com.google.android.apps.mytracks.fragments.ChooseAccountDialogFragment;
import com.google.android.apps.mytracks.fragments.ChooseAccountDialogFragment.ChooseAccountCaller;
import com.google.android.apps.mytracks.fragments.ChooseActivityDialogFragment;
import com.google.android.apps.mytracks.fragments.ChooseActivityDialogFragment.ChooseActivityCaller;
import com.google.android.apps.mytracks.io.drive.SendDriveActivity;
import com.google.android.apps.mytracks.io.fusiontables.SendFusionTablesActivity;
import com.google.android.apps.mytracks.io.gdata.maps.MapsConstants;
import com.google.android.apps.mytracks.io.maps.ChooseMapActivity;
import com.google.android.apps.mytracks.io.maps.SendMapsActivity;
import com.google.android.apps.mytracks.io.sendtogoogle.PermissionCallback;
import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.io.sendtogoogle.UploadResultActivity;
import com.google.android.apps.mytracks.io.spreadsheets.SendSpreadsheetsActivity;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * An abstract class for sending a track to Google services.
 * 
 * @author Jimmy Shih
 */
public abstract class AbstractSendToGoogleActivity extends AbstractMyTracksActivity
    implements ChooseAccountCaller, AddEmailsCaller, ChooseActivityCaller {

  private static final String TAG = AbstractMyTracksActivity.class.getSimpleName();

  private SendRequest sendRequest;

  private PermissionCallback driveCallback = new PermissionCallback() {
      @Override
    public void onSuccess() {
      getPermission(MapsConstants.SERVICE_NAME, sendRequest.isSendMaps(), mapsCallback);
    }

      @Override
    public void onFailure() {
      handleNoAccountPermission();
    }
  };

  private PermissionCallback mapsCallback = new PermissionCallback() {
      @Override
    public void onSuccess() {
      checkFusionTablesPermission();
    }

      @Override
    public void onFailure() {
      handleNoAccountPermission();
    }
  };

  private PermissionCallback fusionTablesCallback = new PermissionCallback() {
      @Override
    public void onSuccess() {
      checkSpreadsheetPermission();
    }

      @Override
    public void onFailure() {
      handleNoAccountPermission();
    }
  };

  private PermissionCallback spreadsheetsCallback = new PermissionCallback() {
      @Override
    public void onSuccess() {
      startNextActivity();
    }

      @Override
    public void onFailure() {
      handleNoAccountPermission();
    }
  };

  public void sendToGoogle(SendRequest request) {
    sendRequest = request;
    new ChooseAccountDialogFragment().show(
        getSupportFragmentManager(), ChooseAccountDialogFragment.CHOOSE_ACCOUNT_DIALOG_TAG);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case SendToGoogleUtils.DRIVE_PERMISSION_REQUEST_CODE:
        SendToGoogleUtils.cancelNotification(this, SendToGoogleUtils.DRIVE_NOTIFICATION_ID);
        if (resultCode == Activity.RESULT_OK) {
          driveCallback.onSuccess();
        } else {
          driveCallback.onFailure();
        }
        break;
      case SendToGoogleUtils.FUSION_TABLES_PERMISSION_REQUEST_CODE:
        SendToGoogleUtils.cancelNotification(this, SendToGoogleUtils.FUSION_TABLES_NOTIFICATION_ID);
        if (resultCode == Activity.RESULT_OK) {
          fusionTablesCallback.onSuccess();
        } else {
          fusionTablesCallback.onFailure();
        }
        break;
      case SendToGoogleUtils.SPREADSHEET_PERMISSION_REQUEST_CODE:
        SendToGoogleUtils.cancelNotification(this, SendToGoogleUtils.SPREADSHEET_NOTIFICATION_ID);
        if (resultCode == Activity.RESULT_OK) {
          spreadsheetsCallback.onSuccess();
        } else {
          spreadsheetsCallback.onFailure();
        }
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  public void onChooseAccountDone() {
    String googleAccount = PreferencesUtils.getString(
        this, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    if (googleAccount == null || googleAccount.equals(PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT)) {
      return;
    }
    sendRequest.setAccount(new Account(googleAccount, Constants.ACCOUNT_TYPE));
    checkDrivePermission();
  }

  @Override
  public void onAddEmailsDone(String emails) {
    if (emails != null && !emails.equals("")) {
      sendRequest.setDriveShareEmails(emails);
      Intent intent = IntentUtils.newIntent(this, SendDriveActivity.class)
          .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
      startActivity(intent);
    }
  }

  @Override
  public void onChooseActivityDone(String packageName, String className) {
    if (packageName != null && className != null) {
      sendRequest.setMapsSharePackageName(packageName);
      sendRequest.setMapsShareClassName(className);
      Intent intent = IntentUtils.newIntent(
          this, sendRequest.isMapsExistingMap() ? ChooseMapActivity.class : SendMapsActivity.class)
          .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
      startActivity(intent);
    }
  }

  /**
   * Checks the Drive permission.
   */
  private void checkDrivePermission() {
    boolean needDrivePermission = sendRequest.isSendDrive();
    if (!needDrivePermission) {
      boolean defaultTablePublic = PreferencesUtils.getBoolean(
          this, R.string.default_table_public_key, PreferencesUtils.DEFAULT_TABLE_PUBLIC_DEFAULT);
      needDrivePermission = defaultTablePublic && sendRequest.isSendFusionTables();
    }
    if (!needDrivePermission) {
      needDrivePermission = sendRequest.isSendSpreadsheets();
    }

    if (needDrivePermission) {
      SendToGoogleUtils.checkPermissionByActivity(this, sendRequest.getAccount().name,
          SendToGoogleUtils.DRIVE_SCOPE, SendToGoogleUtils.DRIVE_PERMISSION_REQUEST_CODE,
          driveCallback);
    } else {
      driveCallback.onSuccess();
    }
  }

  /**
   * Checks the Fusion Tables permission.
   */
  private void checkFusionTablesPermission() {
    if (sendRequest.isSendFusionTables()) {
      SendToGoogleUtils.checkPermissionByActivity(this, sendRequest.getAccount().name,
          SendToGoogleUtils.FUSION_TABLES_SCOPE,
          SendToGoogleUtils.FUSION_TABLES_PERMISSION_REQUEST_CODE, fusionTablesCallback);
    } else {
      fusionTablesCallback.onSuccess();
    }
  }

  /**
   * Checks the Spreadsheet permission.
   */
  private void checkSpreadsheetPermission() {
    if (sendRequest.isSendSpreadsheets()) {
      SendToGoogleUtils.checkPermissionByActivity(this, sendRequest.getAccount().name,
          SendToGoogleUtils.SPREADSHEET_SCOPE,
          SendToGoogleUtils.SPREADSHEET_PERMISSION_REQUEST_CODE, spreadsheetsCallback);
    } else {
      spreadsheetsCallback.onSuccess();
    }
  }

  /**
   * Gets the user permission to access a service.
   * 
   * @param authTokenType the auth token type of the service
   * @param needPermission true if need the permission
   * @param callback callback after getting the permission
   */
  private void getPermission(
      String authTokenType, boolean needPermission, final PermissionCallback callback) {
    if (needPermission) {
      AccountManager.get(this).getAuthToken(sendRequest.getAccount(), authTokenType, null, this,
          new AccountManagerCallback<Bundle>() {
              @Override
            public void run(AccountManagerFuture<Bundle> future) {
              try {
                if (future.getResult().getString(AccountManager.KEY_AUTHTOKEN) != null) {
                  callback.onSuccess();
                } else {
                  Log.d(TAG, "auth token is null");
                  callback.onFailure();
                }
              } catch (OperationCanceledException e) {
                Log.d(TAG, "Unable to get auth token", e);
                callback.onFailure();
              } catch (AuthenticatorException e) {
                Log.d(TAG, "Unable to get auth token", e);
                callback.onFailure();
              } catch (IOException e) {
                Log.d(TAG, "Unable to get auth token", e);
                callback.onFailure();
              }
            }
          }, null);
    } else {
      callback.onSuccess();
    }
  }

  /**
   * Starts the next activity. If
   * <p>
   * sendDrive -> {@link SendDriveActivity}
   * <p>
   * sendMaps and newMap -> {@link SendMapsActivity}
   * <p>
   * sendMaps and !newMap -> {@link ChooseMapActivity}
   * <p>
   * !sendMaps && sendFusionTables -> {@link SendFusionTablesActivity}
   * <p>
   * !sendMaps && !sendFusionTables && sendSpreadsheets ->
   * {@link SendSpreadsheetsActivity}
   * <p>
   * !sendMaps && !sendFusionTables && !sendSpreadsheets ->
   * {@link UploadResultActivity}
   */
  private void startNextActivity() {
    Class<?> next;
    if (sendRequest.isSendDrive()) {
      if (sendRequest.isDriveShare()) {
        AddEmailsDialogFragment.newInstance(sendRequest.getTrackId())
            .show(getSupportFragmentManager(), AddEmailsDialogFragment.ADD_EMAILS_DIALOG_TAG);
        return;
      } else {
        next = SendDriveActivity.class;
      }
    } else if (sendRequest.isSendMaps()) {
      if (sendRequest.isMapsShare()) {
        new ChooseActivityDialogFragment().show(
            getSupportFragmentManager(), ChooseActivityDialogFragment.CHOOSE_ACTIVITY_DIALOG_TAG);
        return;
      }
      next = sendRequest.isMapsExistingMap() ? ChooseMapActivity.class : SendMapsActivity.class;
    } else if (sendRequest.isSendFusionTables()) {
      next = SendFusionTablesActivity.class;
    } else if (sendRequest.isSendSpreadsheets()) {
      next = SendSpreadsheetsActivity.class;
    } else {
      next = UploadResultActivity.class;
    }
    Intent intent = IntentUtils.newIntent(this, next)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
  }

  /**
   * Handles when not able to get account permission.
   */
  private void handleNoAccountPermission() {
    Toast.makeText(this, R.string.send_google_no_account_permission, Toast.LENGTH_LONG).show();
  }
}
