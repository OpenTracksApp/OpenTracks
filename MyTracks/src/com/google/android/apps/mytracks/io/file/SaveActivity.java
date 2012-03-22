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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.PlayTrackUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.File;

/**
 * Activity for saving a track to the SD card, and optionally share or play the
 * track.
 *
 * @author Rodrigo Damazio
 */
public class SaveActivity extends Activity {

  public static final String EXTRA_TRACK_ID = "track_id";
  public static final String EXTRA_TRACK_FILE_FORMAT = "track_file_format";
  public static final String EXTRA_SHARE_TRACK = "share_track";
  public static final String EXTRA_PLAY_TRACK = "play_track";

  private static final String TAG = SaveActivity.class.getSimpleName();

  private static final int DIALOG_PROGRESS_ID = 0;
  private static final int DIALOG_RESULT_ID = 1;

  private long trackId;
  private TrackFileFormat trackFileFormat;
  private boolean shareTrack;
  private boolean playTrack;

  private SaveAsyncTask saveAsyncTask;
  private ProgressDialog progressDialog;

  // result from the AsyncTask
  private boolean success;
  
  // message id from the AsyncTask
  private int messageId;
  
  // path of the saved file
  private String filePath;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1L);
    if (trackId < 0) {
      Log.d(TAG, "Invalid track id");
      finish();
      return;
    }

    trackFileFormat = intent.getParcelableExtra(EXTRA_TRACK_FILE_FORMAT);
    shareTrack = intent.getBooleanExtra(EXTRA_SHARE_TRACK, false);
    playTrack = intent.getBooleanExtra(EXTRA_PLAY_TRACK, false);

    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof SaveAsyncTask) {
      saveAsyncTask = (SaveAsyncTask) retained;
      saveAsyncTask.setActivity(this);
    } else {
      TrackWriter trackWriter = TrackWriterFactory.newWriter(
          this, MyTracksProviderUtils.Factory.get(this), trackId, trackFileFormat);
      if (trackWriter == null) {
        Log.e(TAG, "Track writer is null");
        finish();
        return;
      }
      if (shareTrack || playTrack) {
        // Save to the temp directory
        String dirName = new FileUtils().buildExternalDirectoryPath(
            trackFileFormat.getExtension(), "tmp");
        trackWriter.setDirectory(new File(dirName));
      }
      saveAsyncTask = new SaveAsyncTask(this, trackWriter);
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
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.sd_card_progress_write_file));
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            saveAsyncTask.cancel(true);
            finish();
          }
        });
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle(R.string.generic_progress_title);
        return progressDialog;
      case DIALOG_RESULT_ID:
        return new AlertDialog.Builder(this)
            .setCancelable(true)
            .setIcon(success 
                ? android.R.drawable.ic_dialog_info : android.R.drawable.ic_dialog_alert)
            .setMessage(messageId)
            .setOnCancelListener(new OnCancelListener() {
              @Override
              public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                onPostResultDialog();
              }
            })
            .setPositiveButton(R.string.generic_ok, new OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
                onPostResultDialog();
              }
            })
            .setTitle(success ? R.string.generic_success_title : R.string.generic_error_title)
            .create();
      default:
        return null;
    }
  }

  /**
   * Invokes when the associated AsyncTask completes.
   *
   * @param isSuccess true if the AsyncTask is successful
   * @param aMessageId the id of the AsyncTask message
   * @param aPath the path of the saved file
   */
  public void onAsyncTaskCompleted(boolean isSuccess, int aMessageId, String aPath) {
    this.success = isSuccess;
    this.messageId = aMessageId;
    this.filePath = aPath;
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
    if (success) {
      if (shareTrack) {
        Intent intent = new Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)))
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_track_subject))
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_track_file_body_format))
            .putExtra(getString(R.string.track_id_broadcast_extra), trackId)
            .setType(trackFileFormat.getMimeType());
        startActivity(Intent.createChooser(intent, getString(R.string.share_track_picker_title)));
      } else if (playTrack) {
        Intent intent = new Intent()
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(PlayTrackUtils.TOUR_FEATURE_ID, KmlTrackWriter.TOUR_FEATURE_ID)
            .setClassName(PlayTrackUtils.GOOGLE_EARTH_PACKAGE, PlayTrackUtils.GOOGLE_EARTH_CLASS)
            .setDataAndType(Uri.fromFile(new File(filePath)), PlayTrackUtils.KML_MIME_TYPE);
        startActivity(intent);
      }
    }
    finish();
  }
}
