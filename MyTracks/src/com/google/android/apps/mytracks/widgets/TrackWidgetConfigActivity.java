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

package com.google.android.apps.mytracks.widgets;

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity to configure the track widget.
 * 
 * @author Jimmy Shih
 */
public class TrackWidgetConfigActivity extends Activity {

  private int appWidgetId;
  private Spinner item1;
  private Spinner item2;
  private Spinner item3;
  private Spinner item4;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    /*
     * Set the result to CANCELED. This will cause the widget host to cancel out
     * of the widget placement if they press the back button.
     */
    setResult(RESULT_CANCELED);

    setContentView(R.layout.track_widget_config);
    item1 = (Spinner) findViewById(R.id.track_widget_config_item1);
    item2 = (Spinner) findViewById(R.id.track_widget_config_item2);
    item3 = (Spinner) findViewById(R.id.track_widget_config_item3);
    item4 = (Spinner) findViewById(R.id.track_widget_config_item4);

    boolean reportSpeed = PreferencesUtils.getBoolean(
        this, R.string.report_speed_key, PreferencesUtils.REPORT_SPEED_DEFAULT);

    List<CharSequence> list = new ArrayList<CharSequence>();
    addItem(list, R.string.stats_distance);
    addItem(list, R.string.stats_total_time);
    addItem(list, reportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);
    addItem(list, R.string.stats_moving_time);
    addItem(list,
        reportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

    configSpinner(item1, list, PreferencesUtils.getInt(
        this, R.string.track_widget_item1, PreferencesUtils.TRACK_WIDGET_ITEM1_DEFAULT));
    configSpinner(item2, list, PreferencesUtils.getInt(
        this, R.string.track_widget_item2, PreferencesUtils.TRACK_WIDGET_ITEM2_DEFAULT));
    configSpinner(item3, list, PreferencesUtils.getInt(
        this, R.string.track_widget_item3, PreferencesUtils.TRACK_WIDGET_ITEM3_DEFAULT));
    configSpinner(item4, list, PreferencesUtils.getInt(
        this, R.string.track_widget_item4, PreferencesUtils.TRACK_WIDGET_ITEM4_DEFAULT));

    findViewById(R.id.track_widget_config_add).setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {

        // Push widget update to surface with newly set prefix
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(
            TrackWidgetConfigActivity.this);
        TrackWidgetProvider.updateAppWidget(
            TrackWidgetConfigActivity.this, appWidgetManager, appWidgetId, -1L);
        PreferencesUtils.setInt(TrackWidgetConfigActivity.this, R.string.track_widget_item1,
            item1.getSelectedItemPosition());
        PreferencesUtils.setInt(TrackWidgetConfigActivity.this, R.string.track_widget_item2,
            item2.getSelectedItemPosition());
        PreferencesUtils.setInt(TrackWidgetConfigActivity.this, R.string.track_widget_item3,
            item3.getSelectedItemPosition());
        PreferencesUtils.setInt(TrackWidgetConfigActivity.this, R.string.track_widget_item4,
            item4.getSelectedItemPosition());

        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
      }
    });
    findViewById(R.id.track_widget_config_cancel).setOnClickListener(new View.OnClickListener() {
        @Override
      public void onClick(View v) {
        finish();
      }
    });

    // Find the app widget id from the intent.
    Intent intent = getIntent();
    Bundle extras = intent.getExtras();
    appWidgetId = extras != null ? extras.getInt(
        AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        : AppWidgetManager.INVALID_APPWIDGET_ID;

    // If they gave us an intent without the widget id, just bail.
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish();
    }
  }

  private void addItem(List<CharSequence> list, int id) {
    list.add(getString(id).toUpperCase(Locale.getDefault()));
  }

  private void configSpinner(Spinner spinner, List<CharSequence> list, int position) {
    ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
        this, android.R.layout.simple_spinner_item, list);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setSelection(position);
  }
}
