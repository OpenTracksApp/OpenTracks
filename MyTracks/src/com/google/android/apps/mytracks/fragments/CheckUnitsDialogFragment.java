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
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import java.util.Locale;

/**
 * A DialogFragment to check preferred units.
 *
 * @author Jimmy Shih
 */
public class CheckUnitsDialogFragment extends DialogFragment {

  public static final String CHECK_UNITS_DIALOG_TAG = "checkUnitsDialog";
  
  @Override
  public void onCancel(DialogInterface arg0) {
    onDone();
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {    
    Locale defaultLocale = Locale.getDefault();        
    boolean defaultMetric = !defaultLocale.equals(Locale.US) && !defaultLocale.equals(Locale.UK);
    PreferencesUtils.setBoolean(getActivity(), R.string.metric_units_key, defaultMetric);
    final String metric = getString(R.string.settings_stats_units_metric);
    final String imperial = getString(R.string.settings_stats_units_imperial);
    final CharSequence[] items = defaultMetric ? new CharSequence[] { metric, imperial }
        : new CharSequence[] { imperial, metric };
    return new AlertDialog.Builder(getActivity())
        .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            PreferencesUtils.setBoolean(
                getActivity(), R.string.metric_units_key, items[position].equals(metric));
            onDone();
          }
        })
        .setSingleChoiceItems(items, 0, null)
        .setTitle(R.string.settings_stats_units_title).create();
  }
  
  /**
   * Tasks to perform when done.
   */
  private void onDone() {
    EulaUtils.setShowCheckUnits(getActivity());
    TrackListActivity trackListActivity = (TrackListActivity) getActivity();
    trackListActivity.showStartupDialogs();
  }
}