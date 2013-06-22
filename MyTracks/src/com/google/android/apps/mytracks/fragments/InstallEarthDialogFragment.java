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

import com.google.android.apps.mytracks.util.GoogleEarthUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

/**
 * A DialogFragment to install Google Earth.
 * 
 * @author Jimmy Shih
 */
public class InstallEarthDialogFragment extends DialogFragment {

  public static final String INSTALL_EARTH_DIALOG_TAG = "installEarthDialog";

  private FragmentActivity fragmentActivity;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    fragmentActivity = getActivity();
    return new AlertDialog.Builder(fragmentActivity).setMessage(
        R.string.track_detail_install_earth_message).setNegativeButton(R.string.generic_no, null)
        .setPositiveButton(R.string.generic_yes, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            Intent intent = new Intent().setData(
                Uri.parse(GoogleEarthUtils.GOOGLE_EARTH_MARKET_URL));
            try {
              startActivity(intent);
            } catch (ActivityNotFoundException e) {
              Toast.makeText(
                  fragmentActivity, R.string.track_detail_install_earth_error, Toast.LENGTH_LONG)
                  .show();
            }
          }
        }).setTitle(R.string.track_detail_install_earth_title).create();
  }
}