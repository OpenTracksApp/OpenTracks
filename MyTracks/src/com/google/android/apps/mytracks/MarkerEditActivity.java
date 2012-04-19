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
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * An activity to add/edit a marker.
 *
 * @author Jimmy Shih
 */
public class MarkerEditActivity extends Activity {

  private static final String TAG = MarkerEditActivity.class.getSimpleName();

  public static final String EXTRA_MARKER_ID = "marker_id";

  private long markerId;
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private Waypoint waypoint;

  // UI elements
  private View typeSection;
  private RadioGroup type;
  private EditText name;
  private View waypointSection;
  private AutoCompleteTextView markerType;
  private EditText description;
  private Button done;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);
    ApiAdapterFactory.getApiAdapter().configureActionBarHomeAsUp(this);
    setContentView(R.layout.marker_edit);

    markerId = getIntent().getLongExtra(EXTRA_MARKER_ID, -1L);
    trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, null);

    // Setup UI elements
    typeSection = findViewById(R.id.marker_edit_type_section);
    type = (RadioGroup) findViewById(R.id.marker_edit_type);
    type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        boolean statistics = checkedId == R.id.marker_edit_type_statistics;
        name.setText(
            statistics ? R.string.marker_edit_type_statistics : R.string.marker_edit_type_waypoint);
        updateUiByMarkerType(statistics);
      }
    });
    name = (EditText) findViewById(R.id.marker_edit_name);
    waypointSection = findViewById(R.id.marker_edit_waypoint_section);
    markerType = (AutoCompleteTextView) findViewById(R.id.marker_edit_marker_type);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        this, R.array.waypoint_types, android.R.layout.simple_dropdown_item_1line);
    markerType.setAdapter(adapter);
    description = (EditText) findViewById(R.id.marker_edit_description);

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

    setTitle(newMarker ? R.string.marker_edit_add_title : R.string.menu_edit);
    typeSection.setVisibility(newMarker ? View.VISIBLE : View.GONE);
    done.setText(newMarker ? R.string.marker_edit_add : R.string.generic_save);
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
      type.check(R.id.marker_edit_type_statistics);
    } else {
      waypoint = MyTracksProviderUtils.Factory.get(this).getWaypoint(markerId);
      if (waypoint == null) {
        Log.d(TAG, "waypoint is null");
        finish();
        return;
      }
      
      name.setText(waypoint.getName());
      boolean statistics = waypoint.getType() == Waypoint.TYPE_STATISTICS;
      updateUiByMarkerType(statistics);
      if (!statistics) {
        markerType.setText(waypoint.getCategory());
        description.setText(waypoint.getDescription());
      }     
    }
  }

  /**
   * Updates the UI by marker type.
   *
   * @param statistics true for a statistics marker
   */
  private void updateUiByMarkerType(boolean statistics) {
    name.setCompoundDrawablesWithIntrinsicBounds(
        statistics ? R.drawable.ylw_pushpin : R.drawable.blue_pushpin, 0, 0, 0);
    name.setImeOptions(statistics ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);
    waypointSection.setVisibility(statistics ? View.GONE : View.VISIBLE);
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

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() != android.R.id.home) {
      return false;
    }
    finish();
    return true;
  }

  /**
   * Adds a marker.
   */
  private void addMarker() {
    boolean statistics = type.getCheckedRadioButtonId() == R.id.marker_edit_type_statistics;
    WaypointType waypointType = statistics ? WaypointType.STATISTICS : WaypointType.MARKER;
    String markerCategory = statistics ? null : markerType.getText().toString();
    String markerDescription = statistics ? null : description.getText().toString();
    String markerIconUrl = getString(
        statistics ? R.string.marker_statistics_icon_url : R.string.marker_waypoint_icon_url);
    WaypointCreationRequest waypointCreationRequest = new WaypointCreationRequest(
        waypointType, name.getText().toString(), markerCategory, markerDescription, markerIconUrl);
    ITrackRecordingService trackRecordingService =
        trackRecordingServiceConnection.getServiceIfBound();
    if (trackRecordingService == null) {
      Log.d(TAG, "Unable to add marker, no track recording service");
    } else {
      try {
        if (trackRecordingService.insertWaypoint(waypointCreationRequest) != -1L) {
          Toast.makeText(this, R.string.marker_edit_add_success, Toast.LENGTH_SHORT).show();
          return;
        }
      } catch (RemoteException e) {
        Log.e(TAG, "Unable to add marker", e);
      } catch (IllegalStateException e) {
        Log.e(TAG, "Unable to add marker.", e);
      }
    }
    Toast.makeText(this, R.string.marker_edit_add_error, Toast.LENGTH_LONG).show();
  }

  /**
   * Saves a marker.
   */
  private void saveMarker() {
    waypoint.setName(name.getText().toString());
    if (waypoint.getType() == Waypoint.TYPE_WAYPOINT) {
      waypoint.setCategory(markerType.getText().toString());
      waypoint.setDescription(description.getText().toString());
    }
    MyTracksProviderUtils.Factory.get(this).updateWaypoint(waypoint);
  }
}
