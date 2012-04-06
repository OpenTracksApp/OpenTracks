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

import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.UriUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * An activity to import GPX files from the SD card. Optionally to import one
 * GPX file and display it in My Tracks.
 *
 * @author Rodrigo Damazio
 */
public class ImportActivity extends Activity {

  public static final String EXTRA_IMPORT_ALL = "import_all";

  private static final String TAG = ImportActivity.class.getSimpleName();

  private static final int DIALOG_PROGRESS_ID = 0;
  private static final int DIALOG_RESULT_ID = 1;

  private ImportAsyncTask importAsyncTask;
  private ProgressDialog progressDialog;
  
  private boolean importAll;
  
  // path on the SD card to import
  private String path;

  // number of succesfully imported files
  private int successCount;
  
  // number of files to import
  private int totalCount;
  
  // last successfully imported track id
  private long trackId;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    importAll = intent.getBooleanExtra(EXTRA_IMPORT_ALL, false);
    if (importAll) {
      path = FileUtils.buildExternalDirectoryPath("gpx");
    } else {
      String action = intent.getAction();
      if (!(Intent.ACTION_ATTACH_DATA.equals(action) || Intent.ACTION_VIEW.equals(action))) {
        Log.d(TAG, "Invalid action: " + intent);
        finish();
        return;
      }

      Uri data = intent.getData();
      if (!UriUtils.isFileUri(data)) {
        Log.d(TAG, "Invalid data: " + intent);
        finish();
        return;
      }
      path = data.getPath();
    }

    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof ImportAsyncTask) {
      importAsyncTask = (ImportAsyncTask) retained;
      importAsyncTask.setActivity(this);
    } else {
      importAsyncTask = new ImportAsyncTask(this, importAll, path);
      importAsyncTask.execute();
    }
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    importAsyncTask.setActivity(null);
    return importAsyncTask;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_PROGRESS_ID:
        progressDialog = DialogUtils.createHorizontalProgressDialog(
            this, R.string.import_progress_message, new DialogInterface.OnCancelListener() {
              @Override
              public void onCancel(DialogInterface dialog) {
                importAsyncTask.cancel(true);
                finish();
              }
            });
        return progressDialog;
      case DIALOG_RESULT_ID:
        String message;
        if (successCount == 0) {
          message = getString(R.string.import_no_file, path);
        } else {
          String totalFiles = getResources()
              .getQuantityString(R.plurals.importGpxFiles, totalCount, totalCount);
          message = getString(R.string.import_success, successCount, totalFiles, path);
        }
        return new AlertDialog.Builder(this)
            .setCancelable(true)
            .setMessage(message)
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
              @Override
              public void onCancel(DialogInterface dialog) {
                finish();
              }
            })
            .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                if (!importAll && trackId != -1L) {
                  Intent intent = new Intent(ImportActivity.this, TrackDetailActivity.class)
                      .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                      .putExtra(TrackDetailActivity.TRACK_ID, trackId);
                  startActivity(intent);
                }
                finish();
              }
            })
            .create();
      default:
        return null;
    }
  }

  /**
   * Invokes when the associated AsyncTask completes.
   *
   * @param success true if the AsyncTask is successful
   * @param imported the number of files successfully imported
   * @param total the total number of files to import
   * @param id the last successfully imported track id
   */
  public void onAsyncTaskCompleted(boolean success, int imported, int total, long id) {
    successCount = imported;
    totalCount = total;
    trackId = id;
    removeDialog(DIALOG_PROGRESS_ID);
    if (success) {
      showDialog(DIALOG_RESULT_ID);
    } else {
      Toast.makeText(this, getString(R.string.import_error, path), Toast.LENGTH_LONG).show();
      finish();
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
   * @param number the number of files imported
   * @param max the maximum number of files
   */
  public void setProgressDialogValue(int number, int max) {
    if (progressDialog != null) {
      progressDialog.setIndeterminate(false);
      progressDialog.setMax(max);
      progressDialog.setProgress(Math.min(number, max));
    }
  }
}
