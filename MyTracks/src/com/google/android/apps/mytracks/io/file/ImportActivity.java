/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks.io.file;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.ImportAllTracks;
import com.google.android.apps.mytracks.util.UriUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * An activity that imports a track from a file and displays the track in My Tracks.
 *
 * @author Rodrigo Damazio
 */
public class ImportActivity extends Activity {

  @Override
  public void onStart() {
    super.onStart();

    Intent intent = getIntent();
    String action = intent.getAction();
    Uri data = intent.getData();
    if (!(Intent.ACTION_VIEW.equals(action) || Intent.ACTION_ATTACH_DATA.equals(action))) {
      Log.e(TAG, "Received an intent with unsupported action: " + intent);
      finish();
      return;
    }
    
    if (!UriUtils.isFileUri(data)) {
      Log.e(TAG, "Received an intent with unsupported data: " + intent);
      finish();
      return;
    }
    
    String path = data.getPath();
    Log.i(TAG, "Importing GPX file at " + path);
    new ImportAllTracks(this, path);
  }
}
