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
import com.google.android.apps.mytracks.util.EulaUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * A DialogFragment to show EULA.
 * 
 * @author Jimmy Shih
 */
public class EulaDialogFragment extends DialogFragment {

  public static final String EULA_DIALOG_TAG = "eulaDialog";
  private static final String KEY_HAS_ACCEPTED = "hasAccepted";

  /**
   * Creates a new instance of {@link EulaDialogFragment}.
   * 
   * @param hasAccepted true if the user has accepted the eula.
   */
  public static EulaDialogFragment newInstance(boolean hasAccepted) {
    Bundle bundle = new Bundle();
    bundle.putBoolean(KEY_HAS_ACCEPTED, hasAccepted);

    EulaDialogFragment eulaDialogFragment = new EulaDialogFragment();
    eulaDialogFragment.setArguments(bundle);
    return eulaDialogFragment;
  }

  @Override
  public void onCancel(DialogInterface arg0) {
    if (!getArguments().getBoolean(KEY_HAS_ACCEPTED)) {
      exitApp();
    }
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    boolean hasAccepted = getArguments().getBoolean(KEY_HAS_ACCEPTED);
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
        .setMessage(EulaUtils.getEulaMessage(getActivity()))
        .setTitle(R.string.eula_title);

    if (hasAccepted) {
      builder.setPositiveButton(R.string.generic_ok, null);
    } else {
      builder.setNegativeButton(R.string.eula_decline, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          exitApp();
        }
      })
      .setPositiveButton(R.string.eula_accept, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          EulaUtils.setEulaValue(getActivity());
          TrackListActivity trackListActivity = (TrackListActivity) getActivity();
          trackListActivity.showStartupDialogs();
        }
      });
    }
    return builder.create();
  }
  
  /**
   * Exits the application.
   */
  private void exitApp() {
    getActivity().finish();
  }
}