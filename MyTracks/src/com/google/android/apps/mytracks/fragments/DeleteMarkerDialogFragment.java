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

import com.google.android.apps.mytracks.content.DescriptionGeneratorImpl;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

/**
 * A DialogFragment to delete marker.
 * 
 * @author Jimmy Shih
 */
public class DeleteMarkerDialogFragment extends DialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface DeleteMarkerCaller {

    /**
     * Called when delete marker is done.
     */
    public void onDeleteMarkerDone();
  }

  public static final String DELETE_MARKER_DIALOG_TAG = "deleteMarkerDialog";
  private static final String KEY_MARKER_IDS = "markerIds";

  public static DeleteMarkerDialogFragment newInstance(long[] markerIds) {
    Bundle bundle = new Bundle();
    bundle.putLongArray(KEY_MARKER_IDS, markerIds);

    DeleteMarkerDialogFragment deleteMarkerDialogFragment = new DeleteMarkerDialogFragment();
    deleteMarkerDialogFragment.setArguments(bundle);
    return deleteMarkerDialogFragment;
  }

  private DeleteMarkerCaller caller;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (DeleteMarkerCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + DeleteMarkerCaller.class.getSimpleName());
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final FragmentActivity fragmentActivity = getActivity();
    final long[] markerIds = getArguments().getLongArray(KEY_MARKER_IDS);
    int messageId = markerIds.length > 1 ? R.string.marker_delete_multiple_confirm_message
        : R.string.marker_delete_one_confirm_message;
    return DialogUtils.createConfirmationDialog(
        fragmentActivity, messageId, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            new Thread(new Runnable() {
                @Override
              public void run() {
                for (long markerId : markerIds) {
                  MyTracksProviderUtils.Factory.get(fragmentActivity)
                      .deleteWaypoint(markerId, new DescriptionGeneratorImpl(fragmentActivity));
                }
                caller.onDeleteMarkerDone();
              }
            }).start();
          }
        });
  }
}