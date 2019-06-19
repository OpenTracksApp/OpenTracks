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

import com.google.android.apps.mytracks.fragments.ConfirmDeleteDialogFragment;
import com.google.android.apps.mytracks.fragments.ConfirmDeleteDialogFragment.ConfirmDeleteCaller;
import com.google.android.apps.mytracks.fragments.ExportDialogFragment.ExportType;
import com.google.android.apps.mytracks.fragments.InstallEarthDialogFragment;
import com.google.android.apps.mytracks.fragments.ShareTrackDialogFragment;
import com.google.android.apps.mytracks.fragments.ShareTrackDialogFragment.ShareTrackCaller;
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.io.file.exporter.SaveActivity;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.services.tasks.CheckPermissionAsyncTask;
import com.google.android.apps.mytracks.services.tasks.CheckPermissionAsyncTask.CheckPermissionCaller;
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
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * An abstract class for the following common tasks across
 * {@link TrackListActivity}, {@link TrackDetailActivity}, and
 * {@link SearchListActivity}:
 * <p>
 * - share track <br>
 * - export track to Google services<br>
 * - enable sync to Google Drive <br>
 * - delete tracks <br>
 * - play tracks
 * 
 * @author Jimmy Shih
 */
public abstract class AbstractSendToGoogleActivity extends AbstractMyTracksActivity
    implements ShareTrackCaller, ConfirmDeleteCaller {

  private static final String TAG = AbstractMyTracksActivity.class.getSimpleName();
  private static final String SEND_REQUEST_KEY = "send_request_key";
  private static final int DRIVE_REQUEST_CODE = 0;
  private static final int FUSION_TABLES_REQUEST_CODE = 1;
  private static final int SPREADSHEETS_REQUEST_CODE = 2;
  private static final int DELETE_REQUEST_CODE = 3;
  protected static final int GOOGLE_PLAY_SERVICES_REQUEST_CODE = 4;
  protected static final int CAMERA_REQUEST_CODE = 5;

  private CheckPermissionAsyncTask asyncTask;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      //sendRequest = savedInstanceState.getParcelable(SEND_REQUEST_KEY);
    }
    Object retained = getLastCustomNonConfigurationInstance();
    if (retained instanceof CheckPermissionAsyncTask) {
      asyncTask = (CheckPermissionAsyncTask) retained;
      asyncTask.setActivity(this);
    }
  }

  @Override
  public Object onRetainCustomNonConfigurationInstance() {
    if (asyncTask != null) {
      asyncTask.setActivity(null);
    }
    return asyncTask;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case DELETE_REQUEST_CODE:
        onDeleted();
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  /**
   * Shares a track.
   * 
   * @param trackId the track id
   */
  protected void shareTrack(long trackId) {
    AnalyticsUtils.sendPageViews(this, AnalyticsUtils.ACTION_SHARE_DRIVE);
    ShareTrackDialogFragment.newInstance(trackId)
        .show(getSupportFragmentManager(), ShareTrackDialogFragment.SHARE_TRACK_DIALOG_TAG);
  }

  /**
   * Call when not able to get permission for a google service.
   */
  private void onPermissionFailure() {
    Toast.makeText(this, R.string.send_google_no_account_permission, Toast.LENGTH_LONG).show();
  }
  
  /**
   * Delete tracks.
   * 
   * @param trackIds the track ids
   */
  protected void deleteTracks(long[] trackIds) {
    ConfirmDeleteDialogFragment.newInstance(trackIds)
        .show(getSupportFragmentManager(), ConfirmDeleteDialogFragment.CONFIRM_DELETE_DIALOG_TAG);
  }
  
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

  /**
   * Gets the track recording service connection. For stopping the current
   * recording if need to delete the current recording track.
   */
  abstract protected TrackRecordingServiceConnection getTrackRecordingServiceConnection();

  /**
   * Called after {@link DeleteActivity} returns its result.
   */
  abstract protected void onDeleted();

  /**
   * Play tracks in Google Earth.
   * 
   * @param trackIds the track ids
   */
  protected void playTracks(long[] trackIds) {
    AnalyticsUtils.sendPageViews(this, AnalyticsUtils.ACTION_PLAY);
    if (GoogleEarthUtils.isEarthInstalled(this)) {
      Intent intent = IntentUtils.newIntent(this, SaveActivity.class)
          .putExtra(SaveActivity.EXTRA_TRACK_IDS, trackIds)
          .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) TrackFileFormat.KML)
          .putExtra(SaveActivity.EXTRA_PLAY_TRACK, true);
      startActivity(intent);
    } else {
      new InstallEarthDialogFragment().show(
          getSupportFragmentManager(), InstallEarthDialogFragment.INSTALL_EARTH_DIALOG_TAG);
    }
  }
}
