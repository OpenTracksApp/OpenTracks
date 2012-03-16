/*
 * Copyright 2009 Google Inc.
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

import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * An activity to export all the tracks to the SD card.
 *
 * @author Sandor Dornbush
 */
public class ExportAllActivity extends Activity {

  public static final String EXTRA_TRACK_FILE_FORMAT = "track_file_format";

  private static final int DIALOG_PROGRESS_ID = 0;

  private ExportAllAsyncTask exportAllAsyncTask;
  private ProgressDialog progressDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof ExportAllAsyncTask) {
      exportAllAsyncTask = (ExportAllAsyncTask) retained;
      exportAllAsyncTask.setActivity(this);
    } else {
      Intent intent = getIntent();
      TrackFileFormat trackFileFormat = intent.getParcelableExtra(EXTRA_TRACK_FILE_FORMAT);
      
      exportAllAsyncTask = new ExportAllAsyncTask(this, trackFileFormat);
      exportAllAsyncTask.execute();
    }
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    exportAllAsyncTask.setActivity(null);
    return exportAllAsyncTask;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_PROGRESS_ID:
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
        progressDialog.setIndeterminate(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            exportAllAsyncTask.cancel(true);
            finish();
          }
        });
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle(R.string.track_list_export_all);
        return progressDialog;
      default:
        return null;
    }
  }

  /**
   * Invokes when the associated AsyncTask completes.
   *
   * @param success true if the AsyncTask is successful
   * @param messageId message id to display to user
   */
  public void onAsyncTaskCompleted(boolean success, int messageId) {
    removeDialog(DIALOG_PROGRESS_ID);
    Toast.makeText(this, messageId, success ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
    finish();
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
   * @param number the number of tracks exported
   * @param max the maximum number of tracks
   */
  public void setProgressDialogValue(int number, int max) {
    if (progressDialog != null) {
      progressDialog.setIndeterminate(false);
      progressDialog.setMax(max);
      progressDialog.setProgress(Math.min(number, max));
    }
  }
}
