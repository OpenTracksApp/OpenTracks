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

import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

/**
 * A DialogFragment to show information about My Tracks.
 * 
 * @author Jimmy Shih
 */
public class AboutDialogFragment extends DialogFragment {

  public static final String ABOUT_DIALOG_TAG = "aboutDialog";

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View view = getActivity().getLayoutInflater().inflate(R.layout.about, null);
    TextView aboutVersion = (TextView) view.findViewById(R.id.about_version);
    aboutVersion.setText(SystemUtils.getMyTracksVersion(getActivity()));
    return new AlertDialog.Builder(getActivity())
        .setNegativeButton(R.string.about_license, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            EulaDialogFragment.newInstance(true).show(
                getActivity().getSupportFragmentManager(), EulaDialogFragment.EULA_DIALOG_TAG);
          }
        })
        .setPositiveButton(R.string.generic_ok, null)
        .setTitle(R.string.help_about)
        .setView(view)
        .create();
  }
}