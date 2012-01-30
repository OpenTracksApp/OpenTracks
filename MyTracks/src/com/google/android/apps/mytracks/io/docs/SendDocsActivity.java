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
package com.google.android.apps.mytracks.io.docs;

import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.apps.mytracks.io.sendtogoogle.UploadResultActivity;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

/**
 * An activity to send a track to Google Docs.
 *
 * @author jshih@google.com (Jimmy Shih)
 */
public class SendDocsActivity extends Activity {

  private static final int PROGRESS_DIALOG = 1;

  private SendRequest sendRequest;
  private SendDocsAsyncTask asyncTask;
  private ProgressDialog progressDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);
    
    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof SendDocsAsyncTask) {
      asyncTask = (SendDocsAsyncTask) retained;
      asyncTask.setActivity(this);
    } else {
      asyncTask = new SendDocsAsyncTask(this, sendRequest.getTrackId(), sendRequest.getAccount());
      asyncTask.execute();
    }
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    asyncTask.setActivity(null);
    return asyncTask;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case PROGRESS_DIALOG:
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
        progressDialog.setTitle(getString(
            R.string.send_google_progress_title, getString(R.string.send_google_docs)));
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            asyncTask.cancel(true);
            startNextActivity(false);
          }
        });
        return progressDialog;
      default:
        return null;
    }
  }

  /**
   * Invokes when the associated AsyncTask completes.
   *
   * @param success true if success
   */
  public void onAsyncTaskCompleted(boolean success) {
    startNextActivity(success);
  }

  /**
   * Shows the progress dialog.
   */
  public void showProgressDialog() {
    showDialog(PROGRESS_DIALOG);
  }

  /**
   * Sets the progress dialog value.
   */
  public void setProgressDialogValue(int value) {
    if (progressDialog != null) {
      progressDialog.setProgress(value);
    }
  }
  
  /**
   * Starts the next activity.
   * 
   * @param success true if sendDocs is success
   */
  private void startNextActivity(boolean success) {
    sendRequest.setDocsSuccess(success);
    Intent intent = new Intent(this, UploadResultActivity.class)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
    finish();
  }
}
