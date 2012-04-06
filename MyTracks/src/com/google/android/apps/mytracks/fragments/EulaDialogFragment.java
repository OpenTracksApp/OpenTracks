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
import com.google.android.apps.mytracks.WelcomeActivity;
import com.google.android.apps.mytracks.util.EulaUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Eula DialogFragment.
 */
public class EulaDialogFragment extends DialogFragment {

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity())
        .setCancelable(true)
        .setMessage(EulaUtils.getEulaMessage(getActivity()))
        .setNegativeButton(R.string.eula_decline, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            getActivity().finish();
          }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            getActivity().finish();
          }
        })
        .setPositiveButton(R.string.eula_accept, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            EulaUtils.setEulaValue(getActivity());
            getActivity().startActivityForResult(
                new Intent(getActivity(), WelcomeActivity.class), TrackListActivity.WELCOME_ACTIVITY_REQUEST_CODE);
          }
        })
        .setTitle(R.string.eula_title)
        .create();
  }
}