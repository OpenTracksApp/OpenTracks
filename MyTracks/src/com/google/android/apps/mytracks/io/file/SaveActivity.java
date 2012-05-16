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

import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;

/**
 * An activity for saving tracks to the SD card. If saving a specific track,
 * option to save it to a temp directory and play the track afterward.
 * 
 * @author Rodrigo Damazio
 */
public class SaveActivity extends Activity {

  public static final String EXTRA_TRACK_FILE_FORMAT = "track_file_format";
  public static final String EXTRA_TRACK_ID = "track_id";
  public static final String EXTRA_PLAY_TRACK = "play_track";

  public static final String GOOGLE_EARTH_KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";
  public static final String GOOGLE_EARTH_PACKAGE = "com.google.earth";
  public static final String GOOGLE_EARTH_MARKET_URL = "market://details?id="
      + GOOGLE_EARTH_PACKAGE;
  private static final String
      GOOGLE_EARTH_TOUR_FEATURE_ID = "com.google.earth.EXTRA.tour_feature_id";
  private static final String GOOGLE_EARTH_CLASS = "com.google.earth.EarthActivity";

  private static final int DIALOG_PROGRESS_ID = 0;
  private static final int DIALOG_RESULT_ID = 1;

  private TrackFileFormat trackFileFormat;
  private long trackId;
  private boolean playTrack;

  private SaveAsyncTask saveAsyncTask;
  private ProgressDialog progressDialog;

  // result from the AsyncTask
  private boolean success;
  
  // message id from the AsyncTask
  private int messageId;
  
  // saved file path from the AsyncTask
  private String savedPath;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    trackFileFormat = intent.getParcelableExtra(EXTRA_TRACK_FILE_FORMAT);
    trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1L);
    playTrack = intent.getBooleanExtra(EXTRA_PLAY_TRACK, false);

    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof SaveAsyncTask) {
      saveAsyncTask = (SaveAsyncTask) retained;
      saveAsyncTask.setActivity(this);
    } else {
      saveAsyncTask = new SaveAsyncTask(this, trackFileFormat, trackId, playTrack);
      saveAsyncTask.execute();
    }
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    saveAsyncTask.setActivity(null);
    return saveAsyncTask;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_PROGRESS_ID:
        progressDialog = DialogUtils.createHorizontalProgressDialog(
            this, R.string.sd_card_save_progress_message, new DialogInterface.OnCancelListener() {
              @Override
              public void onCancel(DialogInterface dialog) {
                saveAsyncTask.cancel(true);
                finish();
              }
            });
        return progressDialog;
      case DIALOG_RESULT_ID:
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setCancelable(true)
            .setIcon(success 
                ? android.R.drawable.ic_dialog_info : android.R.drawable.ic_dialog_alert)
            .setMessage(messageId)
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
              @Override
              public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                onPostResultDialog();
              }
            })
            .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
                onPostResultDialog();
              }
            })
            .setTitle(success ? R.string.generic_success_title : R.string.generic_error_title);

        if (success && trackId != -1L && !playTrack) {
          builder.setNegativeButton(
              R.string.share_track_share_file, new DialogInterface.OnClickListener() {
                  @Override
                public void onClick(DialogInterface dialog, int which) {
                  Intent intent = new Intent(Intent.ACTION_SEND)
                      .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(savedPath)))
                      .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_track_subject))
                      .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_track_file_body_format))
                      .putExtra(getString(R.string.track_id_broadcast_extra), trackId)
                      .setType(trackFileFormat.getMimeType());
                  startActivity(
                      Intent.createChooser(intent, getString(R.string.share_track_picker_title)));
                }
              });
        }
        return builder.create();
      default:
        return null;
    }
  }

  /**
   * Invokes when the associated AsyncTask completes.
   *
   * @param isSuccess true if the AsyncTask is successful
   * @param aMessageId the id of the AsyncTask message
   * @param aSavedPath the path of the saved file
   */
  public void onAsyncTaskCompleted(boolean isSuccess, int aMessageId, String aSavedPath) {
    this.success = isSuccess;
    this.messageId = aMessageId;
    this.savedPath = aSavedPath;
    removeDialog(DIALOG_PROGRESS_ID);
    showDialog(DIALOG_RESULT_ID);
  }

  /**
   * Shows the progress dialog.
   */
  public void showProgressDialog() {
    showDialog(DIALOG_PROGRESS_ID);
  }

  /**
   * Sets the progress dialog value.
   *
   * @param number the number of points saved
   * @param max the maximum number of points
   */
  public void setProgressDialogValue(int number, int max) {
    if (progressDialog != null) {
      progressDialog.setIndeterminate(false);
      progressDialog.setMax(max);
      progressDialog.setProgress(Math.min(number, max));
    }
  }

  /**
   * To be invoked after showing the result dialog.
   */
  private void onPostResultDialog() {
    if (success && playTrack) {
      Intent intent = new Intent()
          .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK)
          .putExtra(GOOGLE_EARTH_TOUR_FEATURE_ID, KmlTrackWriter.TOUR_FEATURE_ID)
          .setClassName(GOOGLE_EARTH_PACKAGE, GOOGLE_EARTH_CLASS)
          .setDataAndType(Uri.fromFile(new File(savedPath)), GOOGLE_EARTH_KML_MIME_TYPE);
      startActivity(intent);
    }
    finish();
  }
}
