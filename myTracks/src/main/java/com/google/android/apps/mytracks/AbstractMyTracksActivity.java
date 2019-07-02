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

import com.google.android.apps.mytracks.util.TrackIconUtils;

import android.app.ActionBar;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import androidx.fragment.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * An abstract class for all My Tracks activities.
 * 
 * @author Jimmy Shih
 */
public abstract class AbstractMyTracksActivity extends FragmentActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Set volume control stream for text to speech
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);

    setContentView(getLayoutResId());

    // Configure action bar must be after setContentView
    if (configureActionBarHomeAsUp()) {
      ActionBar actionBar = this.getActionBar();
      if (actionBar != null) {
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
      }
    }
  }

  /**
   * Gets the layout resource id.
   */
  protected abstract int getLayoutResId();

  /**
   * Returns true to configure the action bar home button as the up button.
   */
  protected boolean configureActionBarHomeAsUp() {
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    // Set menu icon color
    TrackIconUtils.setMenuIconColor(menu);

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    // Handle home menu item, up navigation
    if (item.getItemId() == android.R.id.home) {
      onHomeSelected();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Callback when the home menu item is selected. E.g., setup the back stack
   * when home is selected.
   */
  protected void onHomeSelected() {
    finish();
  }
}
