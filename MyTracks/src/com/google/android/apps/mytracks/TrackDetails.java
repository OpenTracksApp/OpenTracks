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

import com.google.android.apps.mytracks.content.MyTracksProviderUtilsFactory;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.ContentValues;
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
 * data such as name, activity type and description.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackDetails extends Activity implements OnClickListener {

  /**
   * The id of the track being edited (taken from bundle, "trackid")
   */
  private Long trackId;

  private EditText name;
  private EditText description;
  private AutoCompleteTextView category;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.mytracks_detail);

    // Required extra when launching this intent:
    trackId = getIntent().getLongExtra("trackid", -1);
    if (trackId < 0) {
      Log.d(Constants.TAG,
          "MyTracksDetails intent was launched w/o track id.");
      finish();
      return;
    }

    // Optional extra that can be used to suppress the cancel button:
    boolean hasCancelButton =
        getIntent().getBooleanExtra("hasCancelButton", true);

    name = (EditText) findViewById(R.id.trackdetails_name);
    description = (EditText) findViewById(R.id.trackdetails_description);
    category = (AutoCompleteTextView) findViewById(R.id.trackdetails_category);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        this,
        R.array.activity_types,
        android.R.layout.simple_dropdown_item_1line);
    category.setAdapter(adapter);

    Button cancel = (Button) findViewById(R.id.trackdetails_cancel);
    if (hasCancelButton) {
      cancel.setOnClickListener(this);
      cancel.setVisibility(View.VISIBLE);
    } else {
      cancel.setVisibility(View.GONE);
    }
    Button save = (Button) findViewById(R.id.trackdetails_save);
    save.setOnClickListener(this);

    fillDialog();
  }

  private void fillDialog() {
    Track track = MyTracksProviderUtilsFactory.get(this).getTrack(trackId);
    if (track != null) {
      name.setText(track.getName());
      description.setText(track.getDescription());
      category.setText(track.getCategory());
    }
  }

  private void saveDialog() {
    ContentValues values = new ContentValues();
    values.put(TracksColumns.NAME, name.getText().toString());
    values.put(TracksColumns.DESCRIPTION, description.getText().toString());
    values.put(TracksColumns.CATEGORY, category.getText().toString());
    getContentResolver().update(
        TracksColumns.DATABASE_CONTENT_URI,
        values,
        "_id = " + trackId,
        null/*selectionArgs*/);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.trackdetails_cancel:
        finish();
        break;
      case R.id.trackdetails_save:
        saveDialog();
        finish();
        break;
    }
  }
}
