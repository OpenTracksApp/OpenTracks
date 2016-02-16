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

package com.google.android.apps.mytracks.io.sendtogoogle;

import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * The abstract class for activities sending a track to Google.
 * <p>
 * The activity gets recreated when the screen rotates. To support the activity
 * displaying a progress dialog, we do the following:
 * <ul>
 * <li>use one instance of an AyncTask to send the track</li>
 * <li>save that instance as the last non configuration instance of the activity
 * </li>
 * <li>when a new activity is created, pass the activity to the AsyncTask so
 * that the AsyncTask can update the progress dialog of the activity</li>
 * </ul>
 *
 * @author Jimmy Shih
 */
public abstract class AbstractSendActivity extends Activity {

  private static final int DIALOG_PROGRESS_ID = 0;

  protected SendRequest sendRequest;
  private AbstractSendAsyncTask asyncTask;
  private ProgressDialog progressDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);

    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof AbstractSendAsyncTask) {
      asyncTask = (AbstractSendAsyncTask) retained;
      asyncTask.setActivity(this);
    } else {
      asyncTask = createAsyncTask();
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
    if (id != DIALOG_PROGRESS_ID) {
      return null;
    }
    progressDialog = DialogUtils.createHorizontalProgressDialog(
        this, R.string.send_google_progress_message, new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            asyncTask.cancel(true);
            startNextActivity(false, true);
          }
        }, getServiceName());
    return progressDialog;
  }

  /**
   * Invokes when the associated AsyncTask completes.
   *
   * @param success true if the AsyncTask is successful
   */
  public void onAsyncTaskCompleted(boolean success, String shareUrl) {
    sendRequest.setShareUrl(shareUrl);
    startNextActivity(success, false);
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
   * @param value the dialog value
   */
  public void setProgressDialogValue(int value) {
    if (progressDialog != null) {
      progressDialog.setIndeterminate(false);
      progressDialog.setProgress(value);
      progressDialog.setMax(100);
    }
  }

  /**
   * Creates the AsyncTask.
   */
  protected abstract AbstractSendAsyncTask createAsyncTask();

  /**
   * Gets the service name.
   */
  protected abstract String getServiceName();

  /**
   * Starts the next activity.
   *
   * @param success true if this activity is successful
   * @param isCancel true if it is a cancel request
   */
  protected abstract void startNextActivity(boolean success, boolean isCancel);
}
