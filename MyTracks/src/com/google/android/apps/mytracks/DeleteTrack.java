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
package com.google.android.apps.mytracks;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.UriUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * Activity used to delete a track.
 *
 * @author Rodrigo Damazio
 */
public class DeleteTrack extends Activity
    implements DialogInterface.OnClickListener, OnCancelListener {
  private static final int CONFIRM_DIALOG = 1;

  private MyTracksProviderUtils providerUtils;

  private long deleteTrackId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    providerUtils = MyTracksProviderUtils.Factory.get(this);

    Intent intent = getIntent();
    String action = intent.getAction();
    Uri data = intent.getData();
    if (!Intent.ACTION_DELETE.equals(action) ||
        !UriUtils.matchesContentUri(data, TracksColumns.CONTENT_URI)) {
      Log.e(TAG, "Got bad delete intent: " + intent);
      finish();
    }

    deleteTrackId = ContentUris.parseId(data);

    showDialog(CONFIRM_DIALOG);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id != CONFIRM_DIALOG) {
      Log.e(TAG, "Unknown dialog " + id);
      return null;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(getString(R.string.track_will_be_permanently_deleted));
    builder.setTitle(getString(R.string.confirmation_title_are_you_sure));
    builder.setIcon(android.R.drawable.ic_dialog_alert);
    builder.setPositiveButton(getString(R.string.yes), this);
    builder.setNegativeButton(getString(R.string.no), this);
    builder.setOnCancelListener(this);
    return builder.create();
  }

  @Override
  public void onClick(DialogInterface dialogInterface, int which) {
    dialogInterface.dismiss();
    if (which == DialogInterface.BUTTON_POSITIVE) {
      deleteTrack();
    }
    finish();
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
  }

  private void deleteTrack() {
    providerUtils.deleteTrack(deleteTrackId);

    // If the track we just deleted was selected, unselect it.
    String selectedKey = getString(R.string.selected_track_key);
    SharedPreferences preferences = getSharedPreferences(Constants.SETTINGS_NAME, 0);
    if (preferences.getLong(selectedKey, -1) == deleteTrackId) {
      Editor editor = preferences.edit().putLong(selectedKey, -1);
      ApiFeatures.getInstance().getApiAdapter().applyPreferenceChanges(editor);
    }
  }
}
