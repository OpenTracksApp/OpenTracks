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

package com.google.android.apps.mytracks.fragments;

import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

/**
 * A DialogFrament to prompt user to sync to Google Drive after saving first track.
 * 
 * @author Jimmy Shih
 */
public class EnableSyncDialogFragment extends AbstractMyTracksDialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface EnableSyncCaller {

    /**
     * Called when enable sync is done.
     */
    public void onEnableSyncDone(boolean enable);
  }

  public static final String ENABLE_SYNC_DIALOG_TAG = "enableSyncDialog";

  private EnableSyncCaller caller;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (EnableSyncCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + EnableSyncCaller.class.getSimpleName());
    }
  }

  @Override
  protected Dialog createDialog() {
    return new AlertDialog.Builder(getActivity()).setMessage(R.string.sync_drive_enable_message)
        .setNegativeButton(R.string.generic_no, new OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            caller.onEnableSyncDone(false);
          }
        }).setPositiveButton(R.string.generic_yes, new OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            caller.onEnableSyncDone(true);
          }
        }).setTitle(R.string.sync_drive_enable_title).create();
  }

  @Override
  public void onCancel(DialogInterface arg0) {
    caller.onEnableSyncDone(false);
  }
}
