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

package com.google.android.apps.mytracks.services;

import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.common.annotations.VisibleForTesting;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;

/**
 * A service to remove My Tracks temp files older than one hour on the SD card.
 *
 * @author Jimmy Shih
 */
public class RemoveTempFilesService extends Service {

  private static final String TAG = RemoveTempFilesService.class.getSimpleName();
  private static final int ONE_HOUR_IN_MILLISECONDS = 60 * 60 * 1000;

  private RemoveTempFilesAsyncTask removeTempFilesAsyncTask = null;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

    // Setup an alarm to repeatedly call this service
    Intent alarmIntent = new Intent(this, RemoveTempFilesService.class);
    PendingIntent pendingIntent = PendingIntent.getService(
        this, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
        System.currentTimeMillis() + ONE_HOUR_IN_MILLISECONDS, AlarmManager.INTERVAL_HOUR,
        pendingIntent);

    // Invoke the AsyncTask to cleanup the temp files
    if (removeTempFilesAsyncTask == null
        || removeTempFilesAsyncTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
      removeTempFilesAsyncTask = new RemoveTempFilesAsyncTask();
      removeTempFilesAsyncTask.execute((Void[]) null);
    }
    return START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private class RemoveTempFilesAsyncTask extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void... params) {
      if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        // Can't do anything
        return null;
      }
      cleanTempDirectory(TrackFileFormat.KML.getExtension());
      cleanTempDirectory(TrackFileFormat.GPX.getExtension());
      cleanTempDirectory(TrackFileFormat.CSV.getExtension());
      cleanTempDirectory(TrackFileFormat.TCX.getExtension());
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      stopSelf();
    }
  }

  private void cleanTempDirectory(String name) {
    cleanTempDirectory(new File(FileUtils.getDirectoryPath(name, FileUtils.TEMP_DIR)));
  }
  
  /**
   * Removes temp files in a directory older than one hour.
   * 
   * @param dir the directory
   * @return the number of files removed.
   */
  @VisibleForTesting
  int cleanTempDirectory(File dir) {
    if (!dir.exists()) {
      return 0;
    }
    int count = 0;
    long oneHourAgo = System.currentTimeMillis() - ONE_HOUR_IN_MILLISECONDS;
    for (File f : dir.listFiles()) {
      if (f.lastModified() < oneHourAgo) {
        if (!f.delete()) {
          Log.e(TAG, "Unable to delete file: " + f.getAbsolutePath());
        } else {
          count++;
        }
      }
    }
    return count;
  }
}
