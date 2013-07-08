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

import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.GoogleEarthUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.io.File;

/**
 * An activity for saving tracks to the external storage. If saving a specific
 * track, option to save it to a temp directory and play the track afterward.
 * 
 * @author Rodrigo Damazio
 */
public class SaveActivity extends Activity {

  public static final String EXTRA_TRACK_FILE_FORMAT = "track_file_format";
  public static final String EXTRA_TRACK_IDS = "track_ids";
  public static final String EXTRA_PLAY_TRACK = "play_track";

  private static final int DIALOG_PROGRESS_ID = 0;
  private static final int DIALOG_RESULT_ID = 1;

  private TrackFileFormat trackFileFormat;
  private long[] trackIds;
  private boolean playTrack;
  private String directoryDisplayName;

  private SaveAsyncTask saveAsyncTask;
  private ProgressDialog progressDialog;

  // the number of tracks successfully saved
  private int successCount;

  // the number of tracks to save
  private int totalCount;

  // the last successfully saved path
  private String savedPath;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    trackFileFormat = intent.getParcelableExtra(EXTRA_TRACK_FILE_FORMAT);
    trackIds = intent.getLongArrayExtra(EXTRA_TRACK_IDS);
    playTrack = intent.getBooleanExtra(EXTRA_PLAY_TRACK, false);

    if (!FileUtils.isExternalStorageWriteable()) {
      Toast.makeText(this, R.string.external_storage_not_writable, Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    directoryDisplayName = playTrack ? FileUtils.getDirectoryDisplayName(
        trackFileFormat.getExtension(), FileUtils.TEMP_DIR)
        : FileUtils.getDirectoryDisplayName(trackFileFormat.getExtension());

    String directoryPath = playTrack ? FileUtils.getDirectoryPath(
        trackFileFormat.getExtension(), FileUtils.TEMP_DIR)
        : FileUtils.getDirectoryPath(trackFileFormat.getExtension());
    File directory = new File(directoryPath);
    if (!FileUtils.ensureDirectoryExists(directory)) {
      Toast.makeText(this, R.string.external_storage_not_writable, Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof SaveAsyncTask) {
      saveAsyncTask = (SaveAsyncTask) retained;
      saveAsyncTask.setActivity(this);
    } else {
      saveAsyncTask = new SaveAsyncTask(this, trackFileFormat, trackIds, directory);
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
        progressDialog = DialogUtils.createHorizontalProgressDialog(this,
            R.string.export_external_storage_progress_message,
            new DialogInterface.OnCancelListener() {
                @Override
              public void onCancel(DialogInterface dialog) {
                saveAsyncTask.cancel(true);
                dialog.dismiss();
                finish();
              }
            }, directoryDisplayName);
        return progressDialog;
      case DIALOG_RESULT_ID:
        int iconId;
        int titleId;
        String message;
        String totalTracks = getResources()
            .getQuantityString(R.plurals.tracks, totalCount, totalCount);
        if (successCount == totalCount) {
          if (totalCount == 0) {
            iconId = android.R.drawable.ic_dialog_info;
            titleId = R.string.export_external_storage_no_track_title;
            message = getString(R.string.export_external_storage_no_track);
          } else {
            iconId = R.drawable.ic_dialog_success;
            titleId = R.string.generic_success_title;
            message = getString(
                R.string.export_external_storage_success, totalTracks, directoryDisplayName);
          }
        } else {
          iconId = android.R.drawable.ic_dialog_alert;
          titleId = R.string.generic_error_title;
          message = getString(R.string.export_external_storage_error, successCount, totalTracks,
              directoryDisplayName);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setCancelable(true)
            .setIcon(iconId).setMessage(message)
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
              public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                finish();
              }
            }).setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                @Override
              public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
                finish();
              }
            }).setTitle(titleId);
        if (!playTrack && trackIds.length == 1 && trackIds[0] != -1L && successCount == totalCount
            && savedPath != null) {
          builder.setNegativeButton(
              R.string.share_track_share_file, new DialogInterface.OnClickListener() {
                  @Override
                public void onClick(DialogInterface dialog, int which) {
                  Intent intent = IntentUtils.newShareFileIntent(
                      SaveActivity.this, trackIds[0], savedPath, trackFileFormat);
                  startActivity(
                      Intent.createChooser(intent, getString(R.string.share_track_picker_title)));
                  finish();
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
   * @param aSuccessCount the number of tracks successfully saved
   * @param aTotalCount the number of tracks to save
   * @param aSavedPath the last successfully saved path
   */
  public void onAsyncTaskCompleted(int aSuccessCount, int aTotalCount, String aSavedPath) {
    successCount = aSuccessCount;
    totalCount = aTotalCount;
    savedPath = aSavedPath;
    removeDialog(DIALOG_PROGRESS_ID);
    if (playTrack && successCount == 1 && totalCount == 1 && savedPath != null) {
      startActivity(GoogleEarthUtils.getPlayInEarthIntent(savedPath));
      finish();
    } else {
      showDialog(DIALOG_RESULT_ID);
    }
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
}
