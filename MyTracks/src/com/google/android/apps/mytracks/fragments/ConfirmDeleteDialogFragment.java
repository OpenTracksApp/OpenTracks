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

import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * A DialogFragment to confirm delete.
 * 
 * @author Jimmy Shih
 */
public class ConfirmDeleteDialogFragment extends AbstractMyTracksDialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface ConfirmDeleteCaller {

    /**
     * Called when confirm delete is done.
     * 
     * @param trackIds list of track ids to delete. To delete all, set to size 1
     *          with trackIds[0] == -1L
     */
    public void onConfirmDeleteDone(long[] trackIds);
  }

  public static final String CONFIRM_DELETE_DIALOG_TAG = "confirmDeleteDialog";
  private static final String KEY_TRACK_IDS = "trackIds";

  /**
   * Create a new instance.
   * 
   * @param trackIds list of track ids to delete. To delete all, set to size 1
   *          with trackIds[0] == -1L
   */
  public static ConfirmDeleteDialogFragment newInstance(long[] trackIds) {
    Bundle bundle = new Bundle();
    bundle.putLongArray(KEY_TRACK_IDS, trackIds);

    ConfirmDeleteDialogFragment deleteTrackDialogFragment = new ConfirmDeleteDialogFragment();
    deleteTrackDialogFragment.setArguments(bundle);
    return deleteTrackDialogFragment;
  }

  private ConfirmDeleteCaller caller;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (ConfirmDeleteCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + ConfirmDeleteCaller.class.getSimpleName());
    }
  }

  @Override
  protected Dialog createDialog() {
    final long[] trackIds = getArguments().getLongArray(KEY_TRACK_IDS);
    int titleId;
    int messageId;
    if (trackIds.length == 1 && trackIds[0] == -1L) {
      boolean driveSync = PreferencesUtils.getBoolean(
          getActivity(), R.string.drive_sync_key, PreferencesUtils.DRIVE_SYNC_DEFAULT);
      titleId = R.string.generic_delete_all_confirm_title;
      messageId = driveSync ? R.string.track_delete_all_confirm_message_sync_on
          : R.string.track_delete_all_confirm_message_sync_off;
    } else {
      titleId = trackIds.length > 1 ? R.string.generic_delete_selected_confirm_title
          : R.string.track_delete_one_confirm_title;
      messageId = trackIds.length > 1 ? R.string.track_delete_multiple_confirm_message
          : R.string.track_delete_one_confirm_message;
    }
    return DialogUtils.createConfirmationDialog(
        getActivity(), titleId, getString(messageId), new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            caller.onConfirmDeleteDone(trackIds);
          }
        });
  }
}