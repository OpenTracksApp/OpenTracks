/*
 * Copyright 2009 Google Inc.
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
package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.ChartView.Mode;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

/**
 * An activity that allows the user to set the chart settings.
 *
 * @author Sandor Dornbush
 */
public class ChartSettingsDialog extends Dialog {
  private RadioButton distance;
  private CheckBox[] series;
  private OnClickListener clickListener;

  public ChartSettingsDialog(Context context) {
    super(context);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.chart_settings);
    Button cancel = (Button) findViewById(R.id.chart_settings_cancel);
    cancel.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        if (clickListener != null) {
          clickListener.onClick(ChartSettingsDialog.this, BUTTON_NEGATIVE);
        }
        dismiss();
      }
    });

    Button okButton = (Button) findViewById(R.id.chart_settings_ok);
    okButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        if (clickListener != null) {
          clickListener.onClick(ChartSettingsDialog.this, BUTTON_POSITIVE);
        }
        dismiss();
      }
    });

    distance = (RadioButton) findViewById(R.id.chart_settings_by_distance);

    series = new CheckBox[ChartView.NUM_SERIES];
    series[ChartView.ELEVATION_SERIES] =
        (CheckBox) findViewById(R.id.chart_settings_elevation);
    series[ChartView.SPEED_SERIES] =
        (CheckBox) findViewById(R.id.chart_settings_speed);
    series[ChartView.POWER_SERIES] =
        (CheckBox) findViewById(R.id.chart_settings_power);
    series[ChartView.CADENCE_SERIES] =
        (CheckBox) findViewById(R.id.chart_settings_cadence);
    series[ChartView.HEART_RATE_SERIES] =
        (CheckBox) findViewById(R.id.chart_settings_heart_rate);
  }

  public void setMode(Mode mode) {
    RadioGroup rd = (RadioGroup) findViewById(R.id.chart_settings_x);
    rd.check(mode == Mode.BY_DISTANCE
             ? R.id.chart_settings_by_distance
             : R.id.chart_settings_by_time);
  }

  public void setSeriesEnabled(int seriesIdx, boolean enabled) {
    series[seriesIdx].setChecked(enabled);
  }

  public Mode getMode() {
    if (distance == null) return Mode.BY_DISTANCE;

    return distance.isChecked() ? Mode.BY_DISTANCE : Mode.BY_TIME;
  }

  public boolean isSeriesEnabled(int seriesIdx) {
    if (series == null) return true;

    return series[seriesIdx].isChecked();
  }

  public void setOnClickListener(OnClickListener clickListener) {
    this.clickListener = clickListener;
  }
}
