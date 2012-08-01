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

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * A DialogFragment to delete one track.
 * 
 * @author Jimmy Shih
 */
public class DeleteOneTrackDialogFragment extends DialogFragment {

  public static final String DELETE_ONE_TRACK_DIALOG_TAG = "deleteOneTrackDialog";
  private static final String KEY_TRACK_ID = "trackId";

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface DeleteOneTrackCaller {
    public TrackRecordingServiceConnection getTrackRecordingServiceConnection();
  }

  private DeleteOneTrackCaller caller;

  public static DeleteOneTrackDialogFragment newInstance(long trackId) {
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_TRACK_ID, trackId);

    DeleteOneTrackDialogFragment deleteOneTrackDialogFragment = new DeleteOneTrackDialogFragment();
    deleteOneTrackDialogFragment.setArguments(bundle);
    return deleteOneTrackDialogFragment;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (DeleteOneTrackCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement DeleteOneTrackCaller");
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return DialogUtils.createConfirmationDialog(getActivity(),
        R.string.track_detail_delete_confirm_message, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            final long trackId = getArguments().getLong(KEY_TRACK_ID);
            final Context context = getActivity();
            if (trackId == PreferencesUtils.getLong(context, R.string.recording_track_id_key)) {
              TrackRecordingServiceConnectionUtils.stop(
                  context, caller.getTrackRecordingServiceConnection(), false);
            }
            new Thread(new Runnable() {
              @Override
              public void run() {
                MyTracksProviderUtils.Factory.get(context).deleteTrack(trackId);
              }
            }).start();
            Intent intent = IntentUtils.newIntent(context, TrackListActivity.class);
            startActivity(intent);
            // Close the activity since its content can change after delete
            getActivity().finish();
          }
        });
  }
}