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
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

/**
 * A DialogFragment to delete all tracks.
 * 
 * @author Jimmy Shih
 */
public class DeleteAllTrackDialogFragment extends DialogFragment {

  public static final String DELETE_ALL_TRACK_DIALOG_TAG = "deleteAllTrackDialog";

  private FragmentActivity activity;
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    activity = getActivity();
    return DialogUtils.createConfirmationDialog(activity,
        R.string.track_list_delete_all_confirm_message, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            new Thread(new Runnable() {
              @Override
              public void run() {
                PreferencesUtils.setBoolean(
                    activity, R.string.drive_sync_key, PreferencesUtils.DRIVE_SYNC_DEFAULT);
                SyncUtils.disableSync(activity);
                MyTracksProviderUtils.Factory.get(activity).deleteAllTracks();
                PreferencesUtils.setLong(activity, R.string.drive_largest_change_id_key,
                    PreferencesUtils.DRIVE_LARGEST_CHANGE_ID_DEFAULT);

                // Clear the drive_deleted_list_key last
                PreferencesUtils.setString(activity, R.string.drive_deleted_list_key,
                    PreferencesUtils.DRIVE_DELETED_LIST_DEFAULT);
              }
            }).start();
          }
        });
  }
}