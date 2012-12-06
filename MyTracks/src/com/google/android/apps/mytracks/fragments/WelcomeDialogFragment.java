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
import android.support.v4.app.FragmentActivity;

/**
 * A DialogFrament to show the welcome info.
 * 
 * @author Jimmy Shih
 */
public class WelcomeDialogFragment extends DialogFragment {

  public static final String WELCOME_DIALOG_TAG = "welcomeDialog";

  private FragmentActivity activity;
  
  @Override
  public void onCancel(DialogInterface arg0) {
    onDone();
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    activity = getActivity();
    return new AlertDialog.Builder(activity)
        .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            onDone();
          }
        })
        .setTitle(R.string.welcome_title)
        .setMessage(R.string.welcome_message)
        .create();
  }

  private void onDone() {
    EulaUtils.setShowWelcome(activity);
    TrackListActivity trackListActivity = (TrackListActivity) activity;
    trackListActivity.showStartupDialogs();
  }
}
