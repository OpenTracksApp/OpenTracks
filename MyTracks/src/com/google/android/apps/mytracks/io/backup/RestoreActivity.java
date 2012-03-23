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

package com.google.android.apps.mytracks.io.backup;

import com.google.android.apps.mytracks.MyTracks;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

/**
 * An activity to restore data from the SD card.
 * 
 * @author Jimmy Shih
 */
public class RestoreActivity extends Activity {

  public static final String EXTRA_DATE = "date";
  
  private static final String TAG = RestoreActivity.class.getSimpleName();
  private static final int DIALOG_PROGRESS_ID = 0;
  
  private RestoreAsyncTask restoreAsyncTask;
  private ProgressDialog progressDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof RestoreAsyncTask) {
      restoreAsyncTask = (RestoreAsyncTask) retained;
      restoreAsyncTask.setActivity(this);
    } else {
      long date = getIntent().getLongExtra(EXTRA_DATE, -1L);
      if (date == -1L) {
        Log.d(TAG, "Invalid date");
        finish();
      }
      restoreAsyncTask = new RestoreAsyncTask(this, new Date(date));
      restoreAsyncTask.execute();
    }
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    restoreAsyncTask.setActivity(null);
    return restoreAsyncTask;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_PROGRESS_ID:
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.settings_backup_restore_progress_message));
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            finish();
          }
        });
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle(R.string.generic_progress_title);
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
    startActivity(new Intent(this, MyTracks.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    finish();
  }

  /**
   * Shows the progress dialog.
   */
  public void showProgressDialog() {
    showDialog(DIALOG_PROGRESS_ID);
  }
}
