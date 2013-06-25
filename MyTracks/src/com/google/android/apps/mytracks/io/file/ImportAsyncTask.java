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

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * AsyncTask to import files from the external storage.
 * 
 * @author Jimmy Shih
 */
public class ImportAsyncTask extends AsyncTask<Void, Integer, Boolean> {

  private static final String TAG = ImportAsyncTask.class.getSimpleName();

  private ImportActivity importActivity;
  private final boolean importAll;
  private final TrackFileFormat trackFileFormat;
  private final String path;
  private final Context context;
  private WakeLock wakeLock;

  // true if the AsyncTask has completed
  private boolean completed;

  // the number of files successfully imported
  private int successCount;

  // the number of files to import
  private int totalCount;

  // the last successfully imported track id
  private long trackId;

  /**
   * Creates an AsyncTask.
   * 
   * @param importActivity the activity currently associated with this AsyncTask
   * @param importAll true to import all GPX files
   * @param trackFileFormat the track file format
   * @param path path to import GPX files
   */
  public ImportAsyncTask(ImportActivity importActivity, boolean importAll,
      TrackFileFormat trackFileFormat, String path) {
    this.importActivity = importActivity;
    this.importAll = importAll;
    this.trackFileFormat = trackFileFormat;
    this.path = path;
    context = importActivity.getApplicationContext();

    completed = false;
    successCount = 0;
    totalCount = 0;
    trackId = -1L;
  }

  /**
   * Sets the current {@link ImportActivity} associated with this AyncTask.
   * 
   * @param importActivity the current {@link ImportActivity}, can be null
   */
  public void setActivity(ImportActivity importActivity) {
    this.importActivity = importActivity;
    if (completed && importActivity != null) {
      importActivity.onAsyncTaskCompleted(successCount, totalCount, trackId);
    }
  }

  @Override
  protected void onPreExecute() {
    if (importActivity != null) {
      importActivity.showProgressDialog();
    }
  }

  @Override
  protected Boolean doInBackground(Void... params) {
    try {
      // Get the wake lock if not recording or paused
      boolean isRecording =
          PreferencesUtils.getLong(importActivity, R.string.recording_track_id_key)
          != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
      boolean isPaused = PreferencesUtils.getBoolean(importActivity,
          R.string.recording_track_paused_key, PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
      if (!isRecording || isPaused) {
        wakeLock = SystemUtils.acquireWakeLock(importActivity, wakeLock);
      }

      List<File> files = getFiles();
      totalCount = files.size();
      if (totalCount == 0) {
        return true;
      }

      for (int i = 0; i < totalCount; i++) {
        if (isCancelled()) {
          // If cancelled, return true to show the number of files imported
          return true;
        }
        if (importFile(files.get(i))) {
          successCount++;
        }
        publishProgress(i + 1, totalCount);
      }
      return true;
    } finally {
      if (wakeLock != null && wakeLock.isHeld()) {
        wakeLock.release();
      }
    }
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    if (importActivity != null) {
      importActivity.setProgressDialogValue(values[0], values[1]);
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    completed = true;
    if (importActivity != null) {
      importActivity.onAsyncTaskCompleted(successCount, totalCount, trackId);
    }
  }

  /**
   * Imports a file.
   * 
   * @param file the file
   */
  private boolean importFile(final File file) {
    try {
      AbstractImporter importer = trackFileFormat == TrackFileFormat.KML ? new KmlImporter(
          context, -1L)
          : new GpxImporter(context, -1L);
      long trackIds[] = importer.importFile(new FileInputStream(file));
      int length = trackIds.length;
      if (length > 0) {
        trackId = trackIds[length - 1];
      }
      return true;
    } catch (Exception e) {
      Log.d(TAG, "file: " + file.getAbsolutePath(), e);
      return false;
    }
  }

  /**
   * Gets a list of files. If importAll is true, returns a list of the files
   * under the path directory. If importAll is false, returns a list containing
   * just the path file.
   */
  private List<File> getFiles() {
    List<File> files = new ArrayList<File>();
    File file = new File(path);
    if (importAll) {
      File[] candidates = file.listFiles();
      if (candidates != null) {
        for (File candidate : candidates) {
          if (!candidate.isDirectory() && candidate.getName()
              .endsWith(trackFileFormat == TrackFileFormat.KML ? ".kml" : ".gpx")) {
            files.add(candidate);
          }
        }
      }
    } else {
      files.add(file);
    }
    return files;
  }
}
