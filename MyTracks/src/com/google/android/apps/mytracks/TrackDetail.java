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
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

/**
 * An activity that let's the user see and edit the user editable track meta
 * data such as track name, activity type, and track description.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackDetail extends Activity implements OnClickListener {

  public static final String TRACK_ID = "trackId";
  public static final String SHOW_CANCEL = "showCancel";

  private static final String TAG = TrackDetail.class.getSimpleName();

  private Long trackId;
  private MyTracksProviderUtils myTracksProviderUtils;
  private Track track;

  private EditText trackName;
  private AutoCompleteTextView activityType;
  private EditText trackDescription;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.track_detail);

    trackId = getIntent().getLongExtra(TRACK_ID, -1);
    if (trackId < 0) {
      Log.e(TAG, "invalid trackId.");
      finish();
      return;
    }

    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);

    track = myTracksProviderUtils.getTrack(trackId);
    if (track == null) {
      Log.e(TAG, "no track.");
      finish();
      return;
    }

    trackName = (EditText) findViewById(R.id.track_detail_track_name);
    trackName.setText(track.getName());

    activityType = (AutoCompleteTextView) findViewById(R.id.track_detail_activity_type);
    activityType.setText(track.getCategory());

    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        this, R.array.activity_types, android.R.layout.simple_dropdown_item_1line);
    activityType.setAdapter(adapter);
    trackDescription = (EditText) findViewById(R.id.track_detail_track_description);
    trackDescription.setText(track.getDescription());

    Button save = (Button) findViewById(R.id.track_detail_save);
    save.setOnClickListener(this);

    Button cancel = (Button) findViewById(R.id.track_detail_cancel);
    if (getIntent().getBooleanExtra(SHOW_CANCEL, true)) {
      cancel.setOnClickListener(this);
      cancel.setVisibility(View.VISIBLE);
    } else {
      cancel.setVisibility(View.INVISIBLE);
    }
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.track_detail_save:
        save();
        finish();
        break;
      case R.id.track_detail_cancel:
        finish();
        break;
      default:   
        finish();
    }
  }

  private void save() {
    track.setName(trackName.getText().toString());
    track.setCategory(activityType.getText().toString());
    track.setDescription(trackDescription.getText().toString());
    myTracksProviderUtils.updateTrack(track);
  }
}
