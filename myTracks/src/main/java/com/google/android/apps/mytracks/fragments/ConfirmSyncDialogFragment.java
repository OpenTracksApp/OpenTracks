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

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.FragmentActivity;

/**
 * A DialogFrament to confirm sync to Google Drive.
 * 
 * @author Jimmy Shih
 */
public class ConfirmSyncDialogFragment extends AbstractMyTracksDialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface ConfirmSyncCaller {

    /**
     * Called when confirm sync is done.
     */
    public void onConfirmSyncDone(boolean enable);
  }

  public static final String CONFIRM_SYNC_DIALOG_TAG = "confirmSyncDialog";

  private ConfirmSyncCaller caller;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (ConfirmSyncCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + ConfirmSyncCaller.class.getSimpleName());
    }
  }

  @Override
  protected Dialog createDialog() {
    FragmentActivity fragmentActivity = getActivity();
    String googleAccount = PreferencesUtils.getString(
        fragmentActivity, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    String message = getString(
        R.string.sync_drive_confirm_message, googleAccount, getString(R.string.my_tracks_app_name));
    return new AlertDialog.Builder(fragmentActivity).setMessage(message)
        .setNegativeButton(R.string.generic_no, new OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            caller.onConfirmSyncDone(false);
          }
        }).setPositiveButton(R.string.generic_yes, new OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            caller.onConfirmSyncDone(true);
          }
        }).setTitle(R.string.sync_drive_confirm_title).create();
  }

  @Override
  public void onCancel(DialogInterface arg0) {
    caller.onConfirmSyncDone(false);
  }
}
