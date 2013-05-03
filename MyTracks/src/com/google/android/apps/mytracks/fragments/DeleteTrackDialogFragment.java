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

package com.google.android.apps.mytracks.fragments;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

/**
 * A DialogFragment to track.
 * 
 * @author Jimmy Shih
 */
public class DeleteTrackDialogFragment extends DialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface DeleteTrackCaller {

    /**
     * Gets the track recording service connection.
     */
    public TrackRecordingServiceConnection getTrackRecordingServiceConnection();

    /**
     * Called when delete track is done.
     */
    public void onDeleteTrackDone();
  }

  public static final String DELETE_TRACK_DIALOG_TAG = "deleteTrackDialog";
  private static final String KEY_DELETE_ALL = "deleteAll";
  private static final String KEY_TRACK_IDS = "trackIds";

  /**
   * Create a new instance.
   * 
   * @param deleteAll true to delete all tracks
   * @param trackIds list of track ids to delete when deleteAll if false
   */
  public static DeleteTrackDialogFragment newInstance(boolean deleteAll, long[] trackIds) {
    Bundle bundle = new Bundle();
    bundle.putBoolean(KEY_DELETE_ALL, deleteAll);
    bundle.putLongArray(KEY_TRACK_IDS, trackIds);

    DeleteTrackDialogFragment deleteTrackDialogFragment = new DeleteTrackDialogFragment();
    deleteTrackDialogFragment.setArguments(bundle);
    return deleteTrackDialogFragment;
  }

  private DeleteTrackCaller caller;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (DeleteTrackCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + DeleteTrackCaller.class.getSimpleName());
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    boolean deleteAll = getArguments().getBoolean(KEY_DELETE_ALL);
    final FragmentActivity fragmentActivity = getActivity();
    if (deleteAll) {
      return DialogUtils.createConfirmationDialog(fragmentActivity,
          R.string.track_delete_all_confirm_message, new DialogInterface.OnClickListener() {
              @Override
            public void onClick(DialogInterface dialog, int which) {
              new Thread(new Runnable() {
                  @Override
                public void run() {
                  PreferencesUtils.setBoolean(fragmentActivity, R.string.drive_sync_key,
                      PreferencesUtils.DRIVE_SYNC_DEFAULT);
                  SyncUtils.disableSync(fragmentActivity);
                  SyncUtils.clearSyncState(fragmentActivity);
                  MyTracksProviderUtils.Factory.get(fragmentActivity).deleteAllTracks();
                  caller.onDeleteTrackDone();
                }
              }).start();
            }
          });
    } else {
      final long[] trackIds = getArguments().getLongArray(KEY_TRACK_IDS);
      int messageId = trackIds.length > 1 ? R.string.track_delete_multiple_confirm_message
          : R.string.track_delete_one_confirm_message;
      return DialogUtils.createConfirmationDialog(
          fragmentActivity, messageId, new DialogInterface.OnClickListener() {
              @Override
            public void onClick(DialogInterface dialog, int which) {
              for (long trackId : trackIds) {
                if (trackId == PreferencesUtils.getLong(
                    fragmentActivity, R.string.recording_track_id_key)) {
                  TrackRecordingServiceConnectionUtils.stopRecording(
                      fragmentActivity, caller.getTrackRecordingServiceConnection(), false);
                }
              }
              new Thread(new Runnable() {
                  @Override
                public void run() {
                  for (long id : trackIds) {
                    MyTracksProviderUtils.Factory.get(fragmentActivity).deleteTrack(id);
                  }
                  caller.onDeleteTrackDone();
                }
              }).start();
            }
          });
    }
  }
}