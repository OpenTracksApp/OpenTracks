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

package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.content.WaypointCreationRequest.WaypointType;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.maps.mytracks.R;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

/**
 * An activity to add/edit a marker.
 *
 * @author Jimmy Shih
 */
public class MarkerEditActivity extends AbstractMyTracksActivity {

  private static final String TAG = MarkerEditActivity.class.getSimpleName();

  public static final String EXTRA_TRACK_ID = "track_id";
  public static final String EXTRA_MARKER_ID = "marker_id";
  public static final String EXTRA_STATISTICS_MARKER = "statistics_marker";

  private long trackId;
  private long markerId;
  private boolean statisticsMarker;
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private Waypoint waypoint;

  // UI elements
  private View typeSection;
  private RadioGroup type;
  private View statisticsSection;
  private EditText statisticsName;
  private View waypointSection;
  private EditText waypointName;
  private AutoCompleteTextView waypointMarkerType;
  private EditText waypointDescription;
  private Button done;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.marker_edit);

    trackId = getIntent().getLongExtra(EXTRA_TRACK_ID, -1L);
    markerId = getIntent().getLongExtra(EXTRA_MARKER_ID, -1L);
    statisticsMarker = getIntent().getBooleanExtra(EXTRA_STATISTICS_MARKER, true);
    trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, null);   
        
    // Setup UI elements
    typeSection = findViewById(R.id.marker_edit_type_section);
    type = (RadioGroup) findViewById(R.id.marker_edit_type);
    type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        boolean statistics = checkedId == R.id.marker_edit_statistics;
        statisticsSection.setVisibility(statistics ? View.VISIBLE : View.GONE);
        waypointSection.setVisibility(statistics ? View.GONE : View.VISIBLE);
        int nextMarkerNumber = trackId == -1L ? -1
            : MyTracksProviderUtils.Factory.get(MarkerEditActivity.this)
                .getNextMarkerNumber(trackId, statistics);
        if (statistics) {
          String name = nextMarkerNumber == -1 ? getString(R.string.marker_type_statistics)
              : getString(R.string.marker_statistics_name_format, nextMarkerNumber);
          statisticsName.setText(name);
          statisticsName.selectAll();
        } else {
          String name = nextMarkerNumber == -1 ? getString(R.string.marker_type_waypoint)
              : getString(R.string.marker_waypoint_name_format, nextMarkerNumber);
          waypointName.setText(name);
          waypointName.selectAll();
          waypointMarkerType.setText("");
          waypointDescription.setText("");
        }
      }
    });
    statisticsSection = findViewById(R.id.marker_edit_statistics_section);
    statisticsName = (EditText) findViewById(R.id.marker_edit_statistics_name);
    
    waypointSection = findViewById(R.id.marker_edit_waypoint_section);
    waypointName = (EditText) findViewById(R.id.marker_edit_waypoint_name);   
    waypointMarkerType = (AutoCompleteTextView) findViewById(R.id.marker_edit_waypoint_marker_type);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        this, R.array.waypoint_types, android.R.layout.simple_dropdown_item_1line);
    waypointMarkerType.setAdapter(adapter);
    waypointDescription = (EditText) findViewById(R.id.marker_edit_waypoint_description);

    Button cancel = (Button) findViewById(R.id.marker_edit_cancel);
    cancel.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
    done = (Button) findViewById(R.id.marker_edit_done);
    updateUiByMarkerId();
  }

  /**
   * Updates the UI based on the marker id.
   */
  private void updateUiByMarkerId() {
    final boolean newMarker = markerId == -1L;

    setTitle(newMarker ? R.string.menu_insert_marker : R.string.menu_edit);
    typeSection.setVisibility(newMarker ? View.VISIBLE : View.GONE);
    done.setText(newMarker ? R.string.generic_add : R.string.generic_save);
    done.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (newMarker) {
          addMarker();
        } else {
          saveMarker();
        }
        finish();
      }
    });

    if (newMarker) {
      type.check(statisticsMarker ? R.id.marker_edit_statistics : R.id.marker_edit_waypoint);    
    } else {
      waypoint = MyTracksProviderUtils.Factory.get(this).getWaypoint(markerId);
      if (waypoint == null) {
        Log.d(TAG, "waypoint is null");
        finish();
        return;
      }
      boolean statistics = waypoint.getType() == Waypoint.TYPE_STATISTICS;
      statisticsSection.setVisibility(statistics ? View.VISIBLE : View.GONE);
      waypointSection.setVisibility(statistics ? View.GONE : View.VISIBLE);
      if (statistics) {
        statisticsName.setText(waypoint.getName());
      } else {
        waypointName.setText(waypoint.getName());
        waypointMarkerType.setText(waypoint.getCategory());
        waypointDescription.setText(waypoint.getDescription());
      }  
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    TrackRecordingServiceConnectionUtils.resume(this, trackRecordingServiceConnection);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    trackRecordingServiceConnection.unbind();
  }

  /**
   * Adds a marker.
   */
  private void addMarker() {
    boolean statistics = type.getCheckedRadioButtonId() == R.id.marker_edit_statistics;
    PreferencesUtils.setBoolean(this, R.string.pick_statistics_marker_key, statistics);

    WaypointType waypointType = statistics ? WaypointType.STATISTICS : WaypointType.WAYPOINT;
    String markerName = statistics ? statisticsName.getText().toString()
        : waypointName.getText().toString();
    String markerCategory = statistics ? null : waypointMarkerType.getText().toString();
    String markerDescription = statistics ? null : waypointDescription.getText().toString();
    String markerIconUrl = getString(statistics ? R.string.marker_statistics_icon_url
        : R.string.marker_waypoint_icon_url);
    WaypointCreationRequest waypointCreationRequest = new WaypointCreationRequest(
        waypointType, markerName, markerCategory, markerDescription, markerIconUrl);
    TrackRecordingServiceConnectionUtils.addMarker(
        this, trackRecordingServiceConnection, waypointCreationRequest);
  }

  /**
   * Saves a marker.
   */
  private void saveMarker() {
    boolean statistics = waypoint.getType() == Waypoint.TYPE_STATISTICS;
    if (statistics) {
      waypoint.setName(statisticsName.getText().toString());
    } else {
      waypoint.setName(waypointName.getText().toString());
      waypoint.setCategory(waypointMarkerType.getText().toString());
      waypoint.setDescription(waypointDescription.getText().toString());
    }
    MyTracksProviderUtils.Factory.get(this).updateWaypoint(waypoint);
  }
}
