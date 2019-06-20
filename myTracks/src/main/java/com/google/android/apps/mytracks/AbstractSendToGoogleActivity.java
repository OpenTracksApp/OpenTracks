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
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.maps.mytracks.R;

import android.content.Intent;
import android.widget.Toast;

/**
 * An abstract class for the following common tasks across
 * {@link TrackListActivity}, {@link TrackDetailActivity}, and
 * {@link SearchListActivity}:
 * <p>
 * - share track <br>
 * - delete tracks <br>
 *
 * @author Jimmy Shih
 */
public abstract class AbstractSendToGoogleActivity extends AbstractMyTracksActivity implements ConfirmDeleteCaller {

  private static final int DELETE_REQUEST_CODE = 3;
  protected static final int GOOGLE_PLAY_SERVICES_REQUEST_CODE = 4;
  protected static final int CAMERA_REQUEST_CODE = 5;

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
}
