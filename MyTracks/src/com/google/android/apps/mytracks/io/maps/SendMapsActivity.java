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

import com.google.android.maps.mytracks.R;

import android.accounts.Account;
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

  // parameters in the input intent
  public static final String ACCOUNT = "account";
  public static final String TRACK_ID = "trackId";
  public static final String MAP_ID = "mapId";

  // parameters in the output intent
  public static final String SUCCESS = "success";

  private static final int PROGRESS_DIALOG = 1;

  private SendMapsAsyncTask asyncTask;
  private ProgressDialog progressDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof SendMapsAsyncTask) {
      asyncTask = (SendMapsAsyncTask) retained;
      asyncTask.setActivity(this);
    } else {
      Intent intent = getIntent();
      Account account = intent.getParcelableExtra(ACCOUNT);
      if (account == null) {
        setResult(RESULT_OK, new Intent().putExtra(SUCCESS, false));
        finish();
        return;
      }
      long trackId = intent.getLongExtra(TRACK_ID, -1L);
      if (trackId == -1L) {
        setResult(RESULT_OK, new Intent().putExtra(SUCCESS, false));
        finish();
        return;
      }
      String mapId = intent.getStringExtra(MAP_ID);

      asyncTask = new SendMapsAsyncTask(this, account, trackId, mapId);
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
            setResult(RESULT_CANCELED);
            finish();
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
    setResult(RESULT_OK, new Intent().putExtra(SUCCESS, success));
    finish();
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
}
