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

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

/**
 * A DialogFragment to show stats settings.
 *
 * @author Jimmy Shih
 */
public class StatsSettingsDialogFragment extends DialogFragment {

  public static final String STATS_SETTINGS_DIALOG_TAG = "statsSettingsDialog";

  public interface OnStatsSettingsChangedListener {
    public void onStatsSettingsChanged();
  }

  private OnStatsSettingsChangedListener listener;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      listener = (OnStatsSettingsChangedListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString()
          + " must implement OnArticleSelectedListener");
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View view = getActivity().getLayoutInflater().inflate(R.layout.stats_settings, null);
    final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.stats_settings_time);
    boolean totalTime = PreferencesUtils.getBoolean(
        getActivity(), R.string.stats_show_total_time_key, true);
    radioGroup.check(totalTime ? R.id.stats_settings_total_time : R.id.stats_settings_moving_time);

    final CheckBox elevation = (CheckBox) view.findViewById(R.id.stats_settings_elevation);
    final CheckBox grade = (CheckBox) view.findViewById(R.id.stats_settings_grade);
    final CheckBox coordinate = (CheckBox) view.findViewById(R.id.stats_settings_coordinate);

    elevation.setChecked(
        PreferencesUtils.getBoolean(getActivity(), R.string.stats_show_elevation_key, false));
    grade.setChecked(
        PreferencesUtils.getBoolean(getActivity(), R.string.stats_show_grade_key, false));
    coordinate.setChecked(
        PreferencesUtils.getBoolean(getActivity(), R.string.stats_show_coordinate_key, false));

    return new AlertDialog.Builder(getActivity()).setNegativeButton(R.string.generic_cancel, null)
        .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            PreferencesUtils.setBoolean(getActivity(), R.string.stats_show_total_time_key,
                radioGroup.getCheckedRadioButtonId() == R.id.stats_settings_total_time);
            PreferencesUtils.setBoolean(
                getActivity(), R.string.stats_show_elevation_key, elevation.isChecked());
            PreferencesUtils.setBoolean(
                getActivity(), R.string.stats_show_grade_key, grade.isChecked());
            PreferencesUtils.setBoolean(
                getActivity(), R.string.stats_show_coordinate_key, coordinate.isChecked());
            listener.onStatsSettingsChanged();
          }
        })
        .setTitle(R.string.menu_stats_settings)
        .setView(view)
        .create();
  }
}