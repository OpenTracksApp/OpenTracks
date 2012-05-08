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

package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.io.file.GpxImporter;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.os.AsyncTask;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * AsyncTask to import GPX files from the SD card.
 *
 * @author Jimmy Shih
 */
public class ImportAsyncTask extends AsyncTask<Void, Integer, Boolean> {

  private static final String TAG = ImportAsyncTask.class.getSimpleName();

  private ImportActivity importActivity;
  private final boolean importAll;
  private final String path;
  private final MyTracksProviderUtils myTracksProviderUtils;
  private WakeLock wakeLock;

  // true if the AsyncTask result is success
  private boolean success;

  // true if the AsyncTask has completed
  private boolean completed;

  // number of files successfully imported
  private int successCount;

  // number of files to import
  private int totalCount;

  // the last successfully imported track id
  private long trackId;

  /**
   * Creates an AsyncTask.
   *
   * @param importActivity the activity currently associated with this AsyncTask
   * @param importAll true to import all GPX files
   * @param path path to import GPX files
   */
  public ImportAsyncTask(ImportActivity importActivity, boolean importAll, String path) {
    this.importActivity = importActivity;
    this.importAll = importAll;
    this.path = path;

    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(importActivity);

    // Get the wake lock if not recording
    if (PreferencesUtils.getLong(importActivity, R.string.recording_track_id_key)
        == PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
      wakeLock = SystemUtils.acquireWakeLock(importActivity, wakeLock);
    }

    success = false;
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
      importActivity.onAsyncTaskCompleted(success, successCount, totalCount, trackId);
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
      if (!FileUtils.isSdCardAvailable()) {
        return false;
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
        File file = files.get(i);
        if (importFile(file)) {
          successCount++;
        }
        publishProgress(i + 1, totalCount);
      }
      return true;
    } finally {
      // Release the wake lock if obtained
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
    success = result;
    completed = true;
    if (importActivity != null) {
      importActivity.onAsyncTaskCompleted(success, successCount, totalCount, trackId);
    }
  }

  /**
   * Imports a GPX file.
   *
   * @param file the file
   */
  private boolean importFile(final File file) {
    try {
      long trackIds[] = GpxImporter.importGPXFile(new FileInputStream(file), myTracksProviderUtils);
      int length = trackIds.length;
      if (length > 0) {
        trackId = trackIds[length - 1];
      }
      return true;
    } catch (FileNotFoundException e) {
      Log.d(TAG, "file: " + file.getAbsolutePath(), e);
      return false;
    } catch (ParserConfigurationException e) {
      Log.d(TAG, "file: " + file.getAbsolutePath(), e);
      return false;
    } catch (SAXException e) {
      Log.d(TAG, "file: " + file.getAbsolutePath(), e);
      return false;
    } catch (IOException e) {
      Log.d(TAG, "file: " + file.getAbsolutePath(), e);
      return false;
    }
  }

  /**
   * Gets a list of GPX files. If importAll is true, returns a list of GPX files
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
          if (!candidate.isDirectory() && candidate.getName().endsWith(".gpx")) {
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
