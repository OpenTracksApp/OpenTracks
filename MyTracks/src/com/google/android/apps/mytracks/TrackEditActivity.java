/*
 * Copyright 2008 Google Inc.
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
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.util.TrackIconUtils;
import com.google.android.apps.mytracks.util.TrackNameUtils;
import com.google.android.maps.mytracks.R;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * An activity that let's the user see and edit the user editable track meta
 * data such as track name, activity type, and track description.
 * 
 * @author Leif Hendrik Wilden
 */
public class TrackEditActivity extends AbstractMyTracksActivity {

  public static final String EXTRA_TRACK_ID = "track_id";
  public static final String EXTRA_NEW_TRACK = "new_track";

  private static final String TAG = TrackEditActivity.class.getSimpleName();

  private Long trackId;
  private MyTracksProviderUtils myTracksProviderUtils;
  private Track track;

  private EditText name;
  private TextView activityTypeLabel;
  private AutoCompleteTextView activityType;
  private EditText description;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    trackId = getIntent().getLongExtra(EXTRA_TRACK_ID, -1L);
    if (trackId == -1L) {
      Log.e(TAG, "invalid trackId");
      finish();
      return;
    }

    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    track = myTracksProviderUtils.getTrack(trackId);
    if (track == null) {
      Log.e(TAG, "No track for " + trackId);
      finish();
      return;
    }

    name = (EditText) findViewById(R.id.track_edit_name);
    name.setText(track.getName());

    activityType = (AutoCompleteTextView) findViewById(R.id.track_edit_activity_type);
    activityType.setText(track.getCategory());

    activityTypeLabel = (TextView) findViewById(R.id.track_edit_activity_type_label);
    setActivityTypeIcon(track.getIcon());
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        this, R.array.activity_types, android.R.layout.simple_dropdown_item_1line);
    activityType.setAdapter(adapter);
    activityType.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String iconValue = TrackIconUtils.getIconValue(
            TrackEditActivity.this, (String) activityType.getAdapter().getItem(position));
        setActivityTypeIcon(iconValue);
      }
    });
    activityType.setOnFocusChangeListener(new View.OnFocusChangeListener() {

        @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
          String iconValue = TrackIconUtils.getIconValue(
              TrackEditActivity.this, activityType.getText().toString());
          setActivityTypeIcon(iconValue);
        }

      }
    });
    description = (EditText) findViewById(R.id.track_edit_description);
    description.setText(track.getDescription());

    Button save = (Button) findViewById(R.id.track_edit_save);
    save.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        track.setName(name.getText().toString());
        String category = activityType.getText().toString();
        track.setCategory(category);
        track.setIcon(TrackIconUtils.getIconValue(TrackEditActivity.this, category));
        track.setDescription(description.getText().toString());
        myTracksProviderUtils.updateTrack(track);
        finish();
      }
    });

    Button cancel = (Button) findViewById(R.id.track_edit_cancel);
    if (getIntent().getBooleanExtra(EXTRA_NEW_TRACK, false)) {
      String trackName = TrackNameUtils.getTrackName(
          this, -1L, -1L, myTracksProviderUtils.getLastValidTrackPoint(trackId));
      if (trackName != null) {
        name.setText(trackName);
      }
      setTitle(R.string.track_edit_new_track_title);
      cancel.setVisibility(View.GONE);
    } else {
      setTitle(R.string.menu_edit);
      cancel.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          finish();
        }
      });
      cancel.setVisibility(View.VISIBLE);
    }
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.track_edit;
  }

  /**
   * Sets the activity type icon.
   * 
   * @param iconValue the icon value
   */
  private void setActivityTypeIcon(String iconValue) {
    activityTypeLabel.setCompoundDrawablesWithIntrinsicBounds(
        TrackIconUtils.getIconDrawable(iconValue), 0, 0, 0);
  }
}
