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

import android.os.AsyncTask;

/**
 * The abstract class for AsyncTasks sending a track to Google.
 * 
 * @author Jimmy Shih
 */
public abstract class AbstractSendAsyncTask extends AsyncTask<Void, Integer, Boolean> {

  /**
   * The activity associated with this AsyncTask.
   */
  private AbstractSendActivity activity;

  /**
   * True if the AsyncTask result is success.
   */
  private boolean success;

  /**
   * The share url from the AsyncTask.
   */
  protected String shareUrl;

  /**
   * True if the AsyncTask has completed.
   */
  private boolean completed;

  /**
   * True if can retry the AsyncTask.
   */
  private boolean canRetry;

  /**
   * Creates an AsyncTask.
   * 
   * @param activity the activity currently associated with this AsyncTask
   */
  public AbstractSendAsyncTask(AbstractSendActivity activity) {
    this.activity = activity;
    success = false;
    shareUrl = null;
    completed = false;
    canRetry = true;
  }

  /**
   * Sets the current activity associated with this AyncTask.
   * 
   * @param activity the current activity, can be null
   */
  public void setActivity(AbstractSendActivity activity) {
    this.activity = activity;
    if (completed && activity != null) {
      activity.onAsyncTaskCompleted(success, shareUrl);
    }
  }

  @Override
  protected void onPreExecute() {
    activity.showProgressDialog();
  }

  @Override
  protected Boolean doInBackground(Void... params) {
    try {
      return performTask();
    } finally {
      closeConnection();
    }
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    if (activity != null) {
      activity.setProgressDialogValue(values[0]);
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    success = result;
    completed = true;
    if (activity != null) {
      activity.onAsyncTaskCompleted(success, shareUrl);
    }
  }

  /**
   * Retries the task. First, invalidates the auth token. If can retry, invokes
   * {@link #performTask()}. Returns false if cannot retry.
   * 
   * @return the result of the retry.
   */
  protected boolean retryTask() {
    if (isCancelled()) {
      return false;
    }

    invalidateToken();
    if (canRetry) {
      canRetry = false;
      return performTask();
    }
    return false;
  }

  /**
   * Closes any AsyncTask connection.
   */
  protected abstract void closeConnection();

  /**
   * Performs the AsyncTask.
   * 
   * @return true if success
   */
  protected abstract boolean performTask();

  /**
   * Invalidates the auth token.
   */
  protected abstract void invalidateToken();
}
