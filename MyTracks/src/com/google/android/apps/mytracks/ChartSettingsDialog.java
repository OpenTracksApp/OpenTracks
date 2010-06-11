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
  private CheckBox elevation;
  private CheckBox speed;

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
        dismiss();
      }
    });
    Button ok = (Button) findViewById(R.id.chart_settings_ok);
    ok.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        handleOk();
      }
    });

    distance = (RadioButton) findViewById(R.id.chart_settings_by_distance);
    elevation = (CheckBox) findViewById(R.id.chart_settings_elevation);
    speed = (CheckBox) findViewById(R.id.chart_settings_speed);
  }

  public void setup(ChartActivity chart) {
    RadioGroup rd = (RadioGroup) findViewById(R.id.chart_settings_x);
    rd.check(chart.getMode() == Mode.BY_DISTANCE
             ? R.id.chart_settings_by_distance
             : R.id.chart_settings_by_time);
    elevation.setChecked(chart.isSeriesEnabled(ChartView.ELEVATION_SERIES));
    speed.setChecked(chart.isSeriesEnabled(ChartView.SPEED_SERIES));
  }

  private void handleOk() {
    ChartActivity chart = MyTracks.getInstance().getChartActivity();
    chart.setMode(distance.isChecked() ? Mode.BY_DISTANCE : Mode.BY_TIME);

    // TODO: check that something is visible.
    chart.setSeriesEnabled(ChartView.ELEVATION_SERIES, elevation.isChecked());
    chart.setSeriesEnabled(ChartView.SPEED_SERIES, speed.isChecked());

    dismiss();
  }
}
