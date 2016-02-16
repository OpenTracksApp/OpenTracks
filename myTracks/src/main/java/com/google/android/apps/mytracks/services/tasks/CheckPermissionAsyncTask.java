/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.mytracks.services.tasks;

import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

/**
 * An async task to check permission.
 * 
 * @author Jimmy Shih
 */
public class CheckPermissionAsyncTask extends AsyncTask<Void, Void, Boolean> {

  public interface CheckPermissionCaller {
    public void onCheckPermissionDone(String scope, boolean success, Intent userRecoverableIntent);
  }

  private static final String TAG = CheckPermissionAsyncTask.class.getSimpleName();

  private Activity activity;
  private final String accountName;
  private final String scope;

  /**
   * True if the AsyncTask result is success.
   */
  private boolean success;

  /**
   * User recoverable intent if failed.
   */
  private Intent userRecoverableIntent;

  /**
   * True if the AsyncTask has completed.
   */
  private boolean completed;

  /**
   * True if can retry the AsyncTask.
   */
  private boolean canRetry;

  public CheckPermissionAsyncTask(Activity activity, String accountName, String scope) {
    this.activity = activity;
    this.accountName = accountName;
    this.scope = scope;
    success = false;
    userRecoverableIntent = null;
    completed = false;
    canRetry = true;
  }

  public void setActivity(Activity activity) {
    this.activity = activity;
    if (completed && activity != null) {
      ((CheckPermissionCaller) activity).onCheckPermissionDone(
          scope, success, userRecoverableIntent);
    }
  }

  @Override
  protected Boolean doInBackground(Void... params) {
    return performTask();
  }

  @Override
  protected void onPostExecute(Boolean result) {
    success = result;
    completed = true;
    if (activity != null) {
      ((CheckPermissionCaller) activity).onCheckPermissionDone(
          scope, success, userRecoverableIntent);
    }
  }

  private boolean performTask() {
    try {
      SendToGoogleUtils.getGoogleAccountCredential(activity, accountName, scope);
      return true;
    } catch (UserRecoverableAuthException e) {
      try {
        // HACK: UserRecoverableAuthException.getIntent can throw a null pointer
        // exception.
        userRecoverableIntent = e.getIntent();
        return false;
      } catch (Exception e1) {
        Log.e(TAG, "Exception in getIntent", e1);
        userRecoverableIntent = null;
        return retryTask();
      }
    } catch (GoogleAuthException e) {
      Log.e(TAG, "GoogleAuthException", e);
      return retryTask();
    } catch (UserRecoverableAuthIOException e) {
      userRecoverableIntent = e.getIntent();
      return false;
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
      return retryTask();
    }
  }

  private boolean retryTask() {
    if (isCancelled()) {
      return false;
    }

    if (canRetry) {
      canRetry = false;
      return performTask();
    }
    return false;
  }
}