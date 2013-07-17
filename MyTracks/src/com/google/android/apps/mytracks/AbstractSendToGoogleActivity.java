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

import com.google.android.apps.mytracks.fragments.CheckPermissionFragment;
import com.google.android.apps.mytracks.fragments.CheckPermissionFragment.CheckPermissionCaller;
import com.google.android.apps.mytracks.fragments.ChooseAccountDialogFragment;
import com.google.android.apps.mytracks.fragments.ChooseAccountDialogFragment.ChooseAccountCaller;
import com.google.android.apps.mytracks.fragments.ConfirmDeleteDialogFragment;
import com.google.android.apps.mytracks.fragments.ConfirmDeleteDialogFragment.ConfirmDeleteCaller;
import com.google.android.apps.mytracks.fragments.ConfirmPlayDialogFragment;
import com.google.android.apps.mytracks.fragments.ConfirmPlayDialogFragment.ConfirmPlayCaller;
import com.google.android.apps.mytracks.fragments.ConfirmSyncDialogFragment;
import com.google.android.apps.mytracks.fragments.ConfirmSyncDialogFragment.ConfirmSyncCaller;
import com.google.android.apps.mytracks.fragments.InstallEarthDialogFragment;
import com.google.android.apps.mytracks.fragments.ShareTrackDialogFragment;
import com.google.android.apps.mytracks.fragments.ShareTrackDialogFragment.ShareTrackCaller;
import com.google.android.apps.mytracks.io.drive.SendDriveActivity;
import com.google.android.apps.mytracks.io.file.SaveActivity;
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.io.fusiontables.SendFusionTablesActivity;
import com.google.android.apps.mytracks.io.gdata.maps.MapsConstants;
import com.google.android.apps.mytracks.io.maps.SendMapsActivity;
import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.io.sendtogoogle.UploadResultActivity;
import com.google.android.apps.mytracks.io.spreadsheets.SendSpreadsheetsActivity;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.GoogleEarthUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * An abstract class for sending a track to Google services.
 * 
 * @author Jimmy Shih
 */
public abstract class AbstractSendToGoogleActivity extends AbstractMyTracksActivity implements
    ChooseAccountCaller, ConfirmSyncCaller, CheckPermissionCaller, ShareTrackCaller, ConfirmPlayCaller,
    ConfirmDeleteCaller {

  private static final String TAG = AbstractMyTracksActivity.class.getSimpleName();
  private static final String SEND_REQUEST_KEY = "send_request_key";
  private static final int DRIVE_REQUEST_CODE = 0;
  private static final int FUSION_TABLES_REQUEST_CODE = 1;
  private static final int SPREADSHEETS_REQUEST_CODE = 2;
  private static final int DELETE_REQUEST_CODE = 3;
  protected static final int GOOGLE_PLAY_SERVICES_REQUEST_CODE = 4; 
  
  private SendRequest sendRequest;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      sendRequest = savedInstanceState.getParcelable(SEND_REQUEST_KEY);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(SEND_REQUEST_KEY, sendRequest);
  }

  /**
   * Share a track.
   * 
   * @param trackId the track id
   */
  protected void shareTrack(long trackId) {
    AnalyticsUtils.sendPageViews(this, AnalyticsUtils.ACTION_SHARE_DRIVE);
    SendRequest newRequest;
    newRequest = new SendRequest(trackId);
    newRequest.setSendDrive(true);
    newRequest.setDriveShare(true);
    sendToGoogle(newRequest);
  }

  /**
   * Sends a request to Google.
   * 
   * @param request the request
   */
  protected void sendToGoogle(SendRequest request) {
    sendRequest = request;
    new ChooseAccountDialogFragment().show(
        getSupportFragmentManager(), ChooseAccountDialogFragment.CHOOSE_ACCOUNT_DIALOG_TAG);
  }

  @Override
  public void onChooseAccountDone() {
    String googleAccount = PreferencesUtils.getString(
        this, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    if (googleAccount == null || googleAccount.equals(PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT)) {
      return;
    }
    sendRequest.setAccount(new Account(googleAccount, Constants.ACCOUNT_TYPE));
  
    if (sendRequest.isSendDrive() && sendRequest.isDriveSync() && sendRequest.isDriveSyncConfirm()) {
      new ConfirmSyncDialogFragment().show(
          getSupportFragmentManager(), ConfirmSyncDialogFragment.CONFIRM_SYNC_DIALOG_TAG);
    } else {
      onConfirmSyncDone(true);
    }
  }

  @Override
  public void onConfirmSyncDone(boolean enable) {
    if (enable) {
      // Check Drive permission
      boolean needDrivePermission = sendRequest.isSendDrive();
      if (!needDrivePermission && sendRequest.isSendFusionTables()) {
        needDrivePermission = PreferencesUtils.getBoolean(this,
            R.string.export_google_fusion_tables_public_key,
            PreferencesUtils.EXPORT_GOOGLE_FUSION_TABLES_PUBLIC_DEFAULT);
      }
      if (!needDrivePermission) {
        needDrivePermission = sendRequest.isSendSpreadsheets();
      }
  
      if (needDrivePermission) {
        Fragment fragment = CheckPermissionFragment.newInstance(
            sendRequest.getAccount().name, SendToGoogleUtils.DRIVE_SCOPE);
        getSupportFragmentManager().beginTransaction()
            .add(fragment, CheckPermissionFragment.CHECK_PERMISSION_TAG).commit();
      } else {
        onDrivePermissionSuccess();
      }
    }
  }

  @Override
  public void onCheckPermissionDone(String scope, boolean success, Intent intent) {
    if (success) {
      if (scope.equals(SendToGoogleUtils.DRIVE_SCOPE)) {
        onDrivePermissionSuccess();
      } else if (scope.equals(SendToGoogleUtils.FUSION_TABLES_SCOPE)) {
        onFusionTablesSuccess();
      } else {
        onSpreadsheetsPermissionSuccess();
      }
    } else {
      if (intent != null) {
        int requestCode;
        if (scope.equals(SendToGoogleUtils.DRIVE_SCOPE)) {
          requestCode = DRIVE_REQUEST_CODE;
        } else if (scope.equals(SendToGoogleUtils.FUSION_TABLES_SCOPE)) {
          requestCode = FUSION_TABLES_REQUEST_CODE;
        } else {
          requestCode = SPREADSHEETS_REQUEST_CODE;
        }
        startActivityForResult(intent, requestCode);
      } else {
        onPermissionFailure();
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
          onPermissionFailure();
        }
        break;
      case FUSION_TABLES_REQUEST_CODE:
        SendToGoogleUtils.cancelNotification(this, SendToGoogleUtils.FUSION_TABLES_NOTIFICATION_ID);
        if (resultCode == Activity.RESULT_OK) {
          onFusionTablesSuccess();
        } else {
          onPermissionFailure();
        }
        break;
      case SPREADSHEETS_REQUEST_CODE:
        SendToGoogleUtils.cancelNotification(this, SendToGoogleUtils.SPREADSHEETS_NOTIFICATION_ID);
        if (resultCode == Activity.RESULT_OK) {
          onSpreadsheetsPermissionSuccess();
        } else {
          onPermissionFailure();
        }
        break;
      case DELETE_REQUEST_CODE:
        onDeleted();
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void onDrivePermissionSuccess() {
    // Check Maps permission
    if (sendRequest.isSendMaps()) {
      AccountManager.get(this).getAuthToken(
          sendRequest.getAccount(), MapsConstants.SERVICE_NAME, null, this,
          new AccountManagerCallback<Bundle>() {
              @Override
            public void run(AccountManagerFuture<Bundle> future) {
              try {
                if (future.getResult().getString(AccountManager.KEY_AUTHTOKEN) != null) {
                  runOnUiThread(new Runnable() {
                      @Override
                    public void run() {
                      onMapsPermissionSuccess();
                    }
                  });
                  return;
                } else {
                  Log.d(TAG, "auth token is null");
                }
              } catch (OperationCanceledException e) {
                Log.d(TAG, "Unable to get auth token", e);
              } catch (AuthenticatorException e) {
                Log.d(TAG, "Unable to get auth token", e);
              } catch (IOException e) {
                Log.d(TAG, "Unable to get auth token", e);
              }
              runOnUiThread(new Runnable() {
                  @Override
                public void run() {
                  onPermissionFailure();
                }
              });
            }
          }, null);
    } else {
      onMapsPermissionSuccess();
    }
  }

  private void onMapsPermissionSuccess() {
    // Check Fusion Tables permission
    if (sendRequest.isSendFusionTables()) {
      Fragment fragment = CheckPermissionFragment.newInstance(
          sendRequest.getAccount().name, SendToGoogleUtils.FUSION_TABLES_SCOPE);
      getSupportFragmentManager()
          .beginTransaction().add(fragment, CheckPermissionFragment.CHECK_PERMISSION_TAG).commit();
    } else {
      onFusionTablesSuccess();
    }
  }

  private void onFusionTablesSuccess() {
    // Check Spreadsheets permission
    if (sendRequest.isSendSpreadsheets()) {
      Fragment fragment = CheckPermissionFragment.newInstance(
          sendRequest.getAccount().name, SendToGoogleUtils.SPREADSHEETS_SCOPE);
      getSupportFragmentManager()
          .beginTransaction().add(fragment, CheckPermissionFragment.CHECK_PERMISSION_TAG).commit();
    } else {
      onSpreadsheetsPermissionSuccess();
    }
  }

  /**
   * On spreadsheets permission success. If
   * <p>
   * isSendDrive and isDriveEnableSync -> enable sync
   * <p>
   * isSendDrive and isDriveShare -> show {@link ShareTrackDialogFragment}
   * <p>
   * isSendDrive -> start {@link SendDriveActivity}
   * <p>
   * isSendMaps -> start {@link SendMapsActivity}
   * <p>
   * isSendFusionTables -> start {@link SendFusionTablesActivity}
   * <p>
   * isSendSpreadsheets -> start {@link SendSpreadsheetsActivity}
   * <p>
   * else -> start {@link UploadResultActivity}
   */
  private void onSpreadsheetsPermissionSuccess() {
    Class<?> next;
    if (sendRequest.isSendDrive()) {
      if (sendRequest.isDriveSync()) {
        PreferencesUtils.setBoolean(this, R.string.drive_sync_key, true);

        // Turn off everything
        SyncUtils.disableSync(this);

        // Turn on sync
        ContentResolver.setMasterSyncAutomatically(true);

        // Enable sync for account
        SyncUtils.enableSync(sendRequest.getAccount());
        return;
      } else if (sendRequest.isDriveShare()) {
        ShareTrackDialogFragment.newInstance(sendRequest.getTrackId())
            .show(getSupportFragmentManager(), ShareTrackDialogFragment.SHARE_TRACK_DIALOG_TAG);
        return;
      } else {
        next = SendDriveActivity.class;
      }
    } else if (sendRequest.isSendMaps()) {
      next = SendMapsActivity.class;
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
   * Call when not able to get permission for a google service.
   */
  private void onPermissionFailure() {
    Toast.makeText(this, R.string.send_google_no_account_permission, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onShareTrackDone(String emails, boolean makePublic) {
    sendRequest.setDriveShareEmails(emails);
    sendRequest.setDriveSharePublic(makePublic);
    Intent intent = IntentUtils.newIntent(this, SendDriveActivity.class)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
  }

  /**
   * Confirm playing tracks in Google Earth.
   * 
   * @param trackIds the track ids
   */
  protected void confirmPlay(long[] trackIds) {
    AnalyticsUtils.sendPageViews(this, AnalyticsUtils.ACTION_PLAY);
    if (GoogleEarthUtils.isEarthInstalled(this)) {
      if (PreferencesUtils.getBoolean(
          this, R.string.confirm_play_earth_key, PreferencesUtils.CONFIRM_PLAY_EARTH_DEFAULT)) {
        ConfirmPlayDialogFragment.newInstance(trackIds)
            .show(getSupportFragmentManager(), ConfirmPlayDialogFragment.CONFIRM_PLAY_DIALOG_TAG);
      } else {
        onConfirmPlayDone(trackIds);
      }
    } else {
      new InstallEarthDialogFragment().show(
          getSupportFragmentManager(), InstallEarthDialogFragment.INSTALL_EARTH_DIALOG_TAG);
    }
  }

  @Override
  public void onConfirmPlayDone(long[] trackIds) {
    Intent intent = IntentUtils.newIntent(this, SaveActivity.class)
        .putExtra(SaveActivity.EXTRA_TRACK_IDS, trackIds)
        .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) TrackFileFormat.KML)
        .putExtra(SaveActivity.EXTRA_PLAY_TRACK, true);
    startActivity(intent);
  }
  
  protected void deleteTrack(long[] trackIds) {
    ConfirmDeleteDialogFragment.newInstance(trackIds)
        .show(getSupportFragmentManager(), ConfirmDeleteDialogFragment.CONFIRM_DELETE_DIALOG_TAG);
  }

  abstract protected TrackRecordingServiceConnection getTrackRecordingServiceConnection();

  abstract protected void onDeleted();

  @Override
  public void onConfirmDeleteDone(long[] trackIds) {
    boolean stopRecording = false;
    if (trackIds.length == 1 && trackIds[0] == -1L) {
      stopRecording = true;
    } else {
      long recordingTrackId = PreferencesUtils.getLong(this, R.string.recording_track_id_key);
      for (long trackId : trackIds) {
        if (trackId == recordingTrackId) {
          stopRecording = true;
          break;
        }
      }
    }
    if (stopRecording) {
      TrackRecordingServiceConnectionUtils.stopRecording(
          this, getTrackRecordingServiceConnection(), false);
    }
    Intent intent = IntentUtils.newIntent(this, DeleteActivity.class);
    intent.putExtra(DeleteActivity.EXTRA_TRACK_IDS, trackIds);
    startActivityForResult(intent, DELETE_REQUEST_CODE);
  }
}
