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

import com.google.android.apps.mytracks.MarkerListActivity;
import com.google.android.apps.mytracks.content.DescriptionGeneratorImpl;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * A DialogFragment to delete one marker.
 *
 * @author Jimmy Shih
 */
public class DeleteOneMarkerDialogFragment extends DialogFragment {

  public static final String DELETE_ONE_MARKER_DIALOG_TAG = "deleteOneMarkerDialog";
  private static final String KEY_MARKER_ID = "markerId";

  public static DeleteOneMarkerDialogFragment newInstance(long markerId) {
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_MARKER_ID, markerId);

    DeleteOneMarkerDialogFragment deleteOneMarkerDialogFragment = new DeleteOneMarkerDialogFragment();
    deleteOneMarkerDialogFragment.setArguments(bundle);
    return deleteOneMarkerDialogFragment;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return DialogUtils.createConfirmationDialog(getActivity(),
        R.string.marker_delete_one_marker_confirm_message, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            MyTracksProviderUtils.Factory.get(getActivity()).deleteWaypoint(
                getArguments().getLong(KEY_MARKER_ID), new DescriptionGeneratorImpl(getActivity()));
            startActivity(new Intent(getActivity(), MarkerListActivity.class).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
          }
        });
  }
}