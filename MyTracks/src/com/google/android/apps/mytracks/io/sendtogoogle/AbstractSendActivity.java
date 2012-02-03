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

import com.google.android.maps.mytracks.R;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

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

  private static final int PROGRESS_DIALOG = 1;

  /**
   * A callback after prompting the user for permission to access a Google
   * service.
   *
   * @author Jimmy Shih
   */
  public interface PermissionCallback {

    /**
     * Invoke when the permission is granted.
     */
    public void onSuccess();

    /**
     * Invoke when the permission is not granted.
     */
    public void onFailure();
  }
  
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
      promptPermission(getAuthTokenType(), getPermissionCallback());
    }
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    if (asyncTask == null) {
      return null;
    }
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
        progressDialog.setTitle(getString(R.string.send_google_progress_title, getServiceName()));
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
   * @param success true if the AsyncTask is successful
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
   *
   * @param value the dialog value
   */
  public void setProgressDialogValue(int value) {
    if (progressDialog != null) {
      progressDialog.setProgress(value);
    }
  }

  /**
   * Gets the logging TAG.
   */
  protected abstract String getTag();

  /**
   * Gets the auth token type.
   */
  protected abstract String getAuthTokenType();
  
  /**
   * Gets the callback for requesting permission to access a service.
   */
  protected abstract PermissionCallback getPermissionCallback();

  /**
   * Prompts the user for permission to access the service.
   *
   * @param authTokenType the auth token type
   * @param callback the callback
   */
  protected void promptPermission(String authTokenType, final PermissionCallback callback) {
    AccountManager.get(this).getAuthToken(
        sendRequest.getAccount(), authTokenType, null, this, new AccountManagerCallback<Bundle>() {
          @Override
          public void run(AccountManagerFuture<Bundle> future) {
            try {
              if (future.getResult().getString(AccountManager.KEY_AUTHTOKEN) != null) {
                callback.onSuccess();
              } else {
                Log.d(getTag(), "auth token is null");
                callback.onFailure();
              }
            } catch (OperationCanceledException e) {
              Log.d(getTag(), "Unable to get auth token", e);
              callback.onFailure();
            } catch (AuthenticatorException e) {
              Log.d(getTag(), "Unable to get auth token", e);
              callback.onFailure();
            } catch (IOException e) {
              Log.d(getTag(), "Unable to get auth token", e);
              callback.onFailure();
            }
          }
        }, null);
  }

  /**
   * Executes the AsyncTask after obtaining user permission to access the
   * service.
   */
  protected void executeAsyncTask() {
    asyncTask = createAsyncTask();
    asyncTask.execute();
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
