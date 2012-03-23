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

package com.google.android.apps.mytracks.io.file;

import android.os.AsyncTask;

/**
 * Async Task to save a track to the SD card.
 *
 * @author Jimmy Shih
 */
public class SaveAsyncTask extends AsyncTask<Void, Integer, Boolean> {

  private SaveActivity saveActivity;
  private final TrackWriter trackWriter;
  
  // true if the AsyncTask result is success
  private boolean success;

  // true if the AsyncTask has completed
  private boolean completed;

  /**
   * Creates an AsyncTask.
   *
   * @param saveActivity the {@link SaveActivity} currently associated with this
   *          AsyncTask
   * @param trackWriter the track writer
   */
  public SaveAsyncTask(SaveActivity saveActivity, TrackWriter trackWriter) {
    this.saveActivity = saveActivity;
    this.trackWriter = trackWriter;
    success = false;
    completed = false;
  }

  /**
   * Sets the current {@link SaveActivity} associated with this AyncTask.
   *
   * @param saveActivity the current {@link SaveActivity}, can be null
   */
  public void setActivity(SaveActivity saveActivity) {
    this.saveActivity = saveActivity;
    if (completed && saveActivity != null) {
      saveActivity.onAsyncTaskCompleted(
          success, trackWriter.getErrorMessage(), trackWriter.getAbsolutePath());
    }
  }

  @Override
  protected void onPreExecute() {
    if (saveActivity != null) {
      saveActivity.showProgressDialog();
    }
  }

  @Override
  protected Boolean doInBackground(Void... params) {  
    trackWriter.setOnWriteListener(new TrackWriter.OnWriteListener() {
      @Override
      public void onWrite(int number, int max) {
        // Update the progress dialog once every 500 points
        if (number % 500 == 0) {
          publishProgress(number, max);
        }
      }
    });
    trackWriter.writeTrack();
    return trackWriter.wasSuccess();
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    if (saveActivity != null) {
      saveActivity.setProgressDialogValue(values[0], values[1]);
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    success = result;
    completed = true;
    if (saveActivity != null) {
      saveActivity.onAsyncTaskCompleted(
          success, trackWriter.getErrorMessage(), trackWriter.getAbsolutePath());
    }
  }

  @Override
  protected void onCancelled() {
    trackWriter.stopWriteTrack();
  }
}
