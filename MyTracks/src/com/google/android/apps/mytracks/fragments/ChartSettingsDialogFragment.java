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

import static com.google.android.apps.mytracks.Constants.CHART_TAB_TAG;

import com.google.android.apps.mytracks.ChartView;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

/**
 * A DialogFragment to show chart settings.
 *
 * @author Jimmy Shih
 */
public class ChartSettingsDialogFragment extends DialogFragment {

  public static final String CHART_SETTINGS_DIALOG_TAG = "chartSettingsDialog";

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final ChartFragment chartFragment = (ChartFragment) getActivity()
        .getSupportFragmentManager().findFragmentByTag(CHART_TAB_TAG);
    View view = getActivity().getLayoutInflater().inflate(R.layout.chart_settings, null);
    final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.chart_settings_x);
    radioGroup.check(chartFragment.getMode() == ChartView.Mode.BY_DISTANCE
        ? R.id.chart_settings_by_distance : R.id.chart_settings_by_time);

    final CheckBox[] checkBoxes = new CheckBox[ChartView.NUM_SERIES];
    checkBoxes[ChartView.ELEVATION_SERIES] = (CheckBox) view.findViewById(
        R.id.chart_settings_elevation);
    checkBoxes[ChartView.SPEED_SERIES] = (CheckBox) view.findViewById(R.id.chart_settings_speed);
    checkBoxes[ChartView.POWER_SERIES] = (CheckBox) view.findViewById(R.id.chart_settings_power);
    checkBoxes[ChartView.CADENCE_SERIES] = (CheckBox) view.findViewById(
        R.id.chart_settings_cadence);
    checkBoxes[ChartView.HEART_RATE_SERIES] = (CheckBox) view.findViewById(
        R.id.chart_settings_heart_rate);

    // set checkboxes values
    for (int i = 0; i < ChartView.NUM_SERIES; i++) {
      checkBoxes[i].setChecked(chartFragment.isChartValueSeriesEnabled(i));
    }
    checkBoxes[ChartView.SPEED_SERIES].setText(chartFragment.isReportSpeed()
        ? R.string.stats_speed : R.string.stats_pace);

    return new AlertDialog.Builder(getActivity())
        .setCancelable(true)
        .setNegativeButton(R.string.generic_cancel, null)
        .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            chartFragment.setMode(
                radioGroup.getCheckedRadioButtonId() == R.id.chart_settings_by_distance 
                ? ChartView.Mode.BY_DISTANCE : ChartView.Mode.BY_TIME);
            for (int i = 0; i < ChartView.NUM_SERIES; i++) {
              chartFragment.setChartValueSeriesEnabled(i, checkBoxes[i].isChecked());
            }
            chartFragment.update();
          }
        })
        .setTitle(R.string.menu_chart_settings)
        .setView(view)
        .create();
  }
}