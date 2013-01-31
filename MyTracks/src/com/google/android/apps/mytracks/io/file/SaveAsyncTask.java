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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Async Task to save tracks to the external storage.
 * 
 * @author Jimmy Shih
 */
public class SaveAsyncTask extends AsyncTask<Void, Integer, Boolean> {

  private static final String TAG = SaveAsyncTask.class.getSimpleName();

  private SaveActivity saveActivity;
  private final TrackFileFormat trackFileFormat;
  private final long trackId;
  private final File directory;
  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;

  private WakeLock wakeLock;
  private TrackWriter trackWriter;

  // true if the AsyncTask has completed
  private boolean completed;

  // the number of tracks successfully saved
  private int successCount;

  // the number of tracks to save
  private int totalCount;

  // the last successfully saved path
  private String savedPath;

  /**
   * Creates an AsyncTask.
   * 
   * @param saveActivity the activity currently associated with this task
   * @track id the track id to save, -1L to save all tracks
   * @param trackFileFormat the track file format
   * @param directory the directory to save to
   */
  public SaveAsyncTask(
      SaveActivity saveActivity, TrackFileFormat trackFileFormat, long trackId, File directory) {
    this.saveActivity = saveActivity;
    this.trackFileFormat = trackFileFormat;
    this.trackId = trackId;
    this.directory = directory;
    context = saveActivity.getApplicationContext();
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);

    // Get the wake lock if not recording or paused
    if (PreferencesUtils.getLong(saveActivity, R.string.recording_track_id_key)
        == PreferencesUtils.RECORDING_TRACK_ID_DEFAULT || PreferencesUtils.getBoolean(saveActivity,
        R.string.recording_track_paused_key, PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT)) {
      wakeLock = SystemUtils.acquireWakeLock(saveActivity, wakeLock);
    }

    completed = false;
    successCount = 0;
    totalCount = 0;
    savedPath = null;
  }

  /**
   * Sets the current activity associated with this AyncTask.
   * 
   * @param saveActivity the current activity, can be null
   */
  public void setActivity(SaveActivity saveActivity) {
    this.saveActivity = saveActivity;
    if (completed && saveActivity != null) {
      saveActivity.onAsyncTaskCompleted(successCount, totalCount, savedPath);
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
    try {
      if (trackId != -1L) {
        totalCount = 1;
        if (saveOneTrack(trackId)) {
          successCount = 1;
          return true;
        } else {
          return false;
        }
      } else {
        return saveAllTracks();
      }
    } finally {
      // Release the wake lock if obtained
      if (wakeLock != null && wakeLock.isHeld()) {
        wakeLock.release();
      }
    }
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    if (saveActivity != null) {
      saveActivity.setProgressDialogValue(values[0], values[1]);
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    completed = true;
    if (saveActivity != null) {
      saveActivity.onAsyncTaskCompleted(successCount, totalCount, savedPath);
    }
  }

  @Override
  protected void onCancelled() {
    if (trackWriter != null) {
      trackWriter.stopWriteTrack();
    }
  }

  /**
   * Saves one track.
   * 
   * @param id the track id
   */
  private Boolean saveOneTrack(long id) {   
    Track track = myTracksProviderUtils.getTrack(id);
    if (track == null) {
      Log.d(TAG, "No track for " + id);
      return false;
    }
  
    // Make sure the file doesn't exist yet (possibly by changing the filename)
    String fileName = FileUtils.buildUniqueFileName(
        directory, track.getName(), trackFileFormat.getExtension());
    if (fileName == null) {
      Log.d(TAG, "Unable to get a unique filename for " + track.getName());
      return false;
    }
    
    trackWriter = new TrackWriter(
        context, myTracksProviderUtils, track, trackFileFormat, new TrackWriter.OnWriteListener() {
            @Override
          public void onWrite(int number, int max) {
            /*
             * If only saving one track, update the progress dialog once every
             * 500 points
             */
            if (trackId != -1L && number % 500 == 0) {
              publishProgress(number, max);
            }
          }
        });
  
    File file = null;
    try {
      file = new File(directory, fileName);
      OutputStream outputStream = new FileOutputStream(file);
      trackWriter.writeTrack(outputStream);
    } catch (FileNotFoundException e) {
      Log.d(TAG, "File not found " + fileName, e);
      return false;
    }   
  
    if (trackWriter.wasSuccess()) {
      savedPath = file.getAbsolutePath();
    } else {
      if (!file.delete()) {
        Log.w(TAG, "Failed to delete file " + file.getAbsolutePath());
      }
    }
    return trackWriter.wasSuccess();
  }

  /**
   * Saves all the tracks.
   */
  private Boolean saveAllTracks() {
    Cursor cursor = null;
    try {
      cursor = myTracksProviderUtils.getTrackCursor(null, null, TracksColumns._ID);
      if (cursor == null) {
        return false;
      }
      totalCount = cursor.getCount();
      int idIndex = cursor.getColumnIndexOrThrow(TracksColumns._ID);
      for (int i = 0; i < totalCount; i++) {
        if (isCancelled()) {
          return false;
        }
        cursor.moveToPosition(i);
        long id = cursor.getLong(idIndex);
        if (saveOneTrack(id)) {
          successCount++;
        }
        publishProgress(i + 1, totalCount);
      }
      return true;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }
}
