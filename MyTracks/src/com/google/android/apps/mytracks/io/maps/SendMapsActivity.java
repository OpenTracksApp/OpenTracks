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
package com.google.android.apps.mytracks.io.maps;

import com.google.android.apps.mytracks.io.docs.SendDocsActivity;
import com.google.android.apps.mytracks.io.fusiontables.SendFusionTablesActivity;
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
 * An activity to send a track to Google Maps.
 *
 * @author Jimmy Shih
 */
public class SendMapsActivity extends Activity {

  private static final int PROGRESS_DIALOG = 1;

  private SendRequest sendRequest;
  private SendMapsAsyncTask asyncTask;
  private ProgressDialog progressDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);
    
    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof SendMapsAsyncTask) {
      asyncTask = (SendMapsAsyncTask) retained;
      asyncTask.setActivity(this);
    } else {
      asyncTask = new SendMapsAsyncTask(
          this, sendRequest.getTrackId(), sendRequest.getAccount(), sendRequest.getMapId());
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
        progressDialog.setTitle(
            getString(R.string.send_google_progress_title, getString(R.string.send_google_maps)));
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            asyncTask.cancel(true);
            startNextActivity(false, true);
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
    startNextActivity(success, false);
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
   * @param success true if sendMaps is success
   * @param isCancel true if it is a cancel request
   */
  private void startNextActivity(boolean success, boolean isCancel) {
    sendRequest.setMapsSuccess(success);
    
    Class<?> next;
    if (isCancel) {
      next = UploadResultActivity.class;
    } else {
      if (sendRequest.isSendFusionTables()) {
        next = SendFusionTablesActivity.class;
      } else if (sendRequest.isSendDocs()) {
        next = SendDocsActivity.class;
      } else {
        next = UploadResultActivity.class;
      }
    }
    Intent intent = new Intent(this, next)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
    finish();
  }
}
