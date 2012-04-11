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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.CheckUnitsUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * A DialogFragment to check preferred units.
 * 
 * @author Jimmy Shih
 */
public class CheckUnitsDialogFragment extends DialogFragment {

  public static final String CHECK_UNITS_DIALOG_TAG = "checkUnitsDialog";
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    return new AlertDialog.Builder(getActivity())
        .setCancelable(true)
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            CheckUnitsUtils.setCheckUnitsValue(getActivity());
          }
        })
        .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            CheckUnitsUtils.setCheckUnitsValue(getActivity());

            int position = ((AlertDialog) dialog).getListView().getSelectedItemPosition();
            SharedPreferences sharedPreferences = getActivity()
                .getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
            ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(sharedPreferences.edit()
                .putBoolean(getString(R.string.metric_units_key), position == 0));
          }
        })
        .setSingleChoiceItems(new CharSequence[] { getString(R.string.preferred_units_metric),
            getString(R.string.preferred_units_imperial) }, 0, null)
        .setTitle(R.string.preferred_units_title)
        .create();
  }
}