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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

/**
 * Screen in which the user enters details about a waypoint.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracksWaypointDetails extends Activity
    implements OnClickListener {

  /**
   * The id of the way point being edited (taken from bundle, "waypointid")
   */
  private Long waypointId;

  private EditText name;
  private EditText description;
  private AutoCompleteTextView category;
  private View detailsView;
  private View statsView;

  private StatsUtilities utils;
  private Waypoint waypoint;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.mytracks_waypoint_details);

    utils = new StatsUtilities(this);
    SharedPreferences preferences =
        getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (preferences != null) {
      boolean useMetric =
          preferences.getBoolean(MyTracksSettings.METRIC_UNITS, true);
      utils.setMetricUnits(useMetric);

      boolean displaySpeed =
          preferences.getBoolean(MyTracksSettings.REPORT_SPEED, true);
      utils.setReportSpeed(displaySpeed);

      utils.updateWaypointUnits();
      utils.setSpeedLabels();
    }

    // Required extra when launching this intent:
    waypointId = getIntent().getLongExtra("waypointid", -1);
    if (waypointId < 0) {
      Log.d(MyTracksConstants.TAG,
          "MyTracksWaypointsDetails intent was launched w/o waypoint id.");
      finish();
      return;
    }

    // Optional extra that can be used to suppress the cancel button:
    boolean hasCancelButton =
        getIntent().getBooleanExtra("hasCancelButton", true);

    name = (EditText) findViewById(R.id.waypointdetails_name);
    description = (EditText) findViewById(R.id.waypointdetails_description);
    category =
        (AutoCompleteTextView) findViewById(R.id.waypointdetails_category);
    statsView = findViewById(R.id.waypoint_stats);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        this,
        R.array.waypoint_types,
        android.R.layout.simple_dropdown_item_1line);
    category.setAdapter(adapter);
    detailsView = findViewById(R.id.waypointdetails_description_layout);

    Button cancel = (Button) findViewById(R.id.waypointdetails_cancel);
    if (hasCancelButton) {
      cancel.setOnClickListener(this);
      cancel.setVisibility(View.VISIBLE);
    } else {
      cancel.setVisibility(View.GONE);
    }
    Button save = (Button) findViewById(R.id.waypointdetails_save);
    save.setOnClickListener(this);

    fillDialog();
  }

  private void fillDialog() {
    waypoint = MyTracksProviderUtils.Factory.get(this).getWaypoint(waypointId);
    if (waypoint != null) {
      name.setText(waypoint.getName());
      ImageView icon = (ImageView) findViewById(R.id.waypointdetails_icon);
      int iconId = -1;
      switch(waypoint.getType()) {
        case Waypoint.TYPE_WAYPOINT:
          description.setText(waypoint.getDescription());
          detailsView.setVisibility(View.VISIBLE);
          category.setText(waypoint.getCategory());
          statsView.setVisibility(View.GONE);
          iconId = R.drawable.blue_pushpin;
          break;
        case Waypoint.TYPE_STATISTICS:
          detailsView.setVisibility(View.GONE);
          statsView.setVisibility(View.VISIBLE);
          iconId = R.drawable.ylw_pushpin;
          TripStatistics waypointStats = waypoint.getStatistics();
          utils.setAllStats(waypointStats);
          utils.setTime(R.id.total_time_register, waypointStats.getTotalTime());
          utils.setAltitude(
              R.id.elevation_register, waypoint.getLocation().getAltitude());
          break;
      }
      icon.setImageDrawable(getResources().getDrawable(iconId));
    }
  }

  private void saveDialog() {
    ContentValues values = new ContentValues();
    values.put(WaypointsColumns.NAME, name.getText().toString());
    if (waypoint != null && waypoint.getType() == Waypoint.TYPE_WAYPOINT) {
      values.put(WaypointsColumns.DESCRIPTION,
          description.getText().toString());
      values.put(WaypointsColumns.CATEGORY, category.getText().toString());
    }
    getContentResolver().update(
        WaypointsColumns.CONTENT_URI,
        values,
        "_id = " + waypointId,
        null /*selectionArgs*/);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.waypointdetails_cancel:
        finish();
        break;
      case R.id.waypointdetails_save:
        saveDialog();
        finish();
        break;
    }
  }
}
