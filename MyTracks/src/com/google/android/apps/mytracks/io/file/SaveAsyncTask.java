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
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
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

/**
 * Async Task to save tracks to the SD card.
 * 
 * @author Jimmy Shih
 */
public class SaveAsyncTask extends AsyncTask<Void, Integer, Boolean> {

  private static final String TAG = SaveAsyncTask.class.getSimpleName();

  private SaveActivity saveActivity;
  private final TrackFileFormat trackFileFormat;
  private final long trackId;
  private final boolean useTempDir;

  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;
  private WakeLock wakeLock;
  private TrackWriter trackWriter;

  // true if the AsyncTask result is success
  private boolean success;

  // true if the AsyncTask has completed
  private boolean completed;

  // message id to return to the activity
  private int messageId;

  // saved file path to return to the activity
  private String savedPath;

  /**
   * Creates an AsyncTask.
   * 
   * @param saveActivity the activity currently associated with this task.
   * @param trackFileFormat the track format
   * @param trackId the track id
   * @param useTempDir true to use the temp directory
   */
  public SaveAsyncTask(SaveActivity saveActivity, TrackFileFormat trackFileFormat, long trackId,
      boolean useTempDir) {
    this.saveActivity = saveActivity;
    this.trackFileFormat = trackFileFormat;
    this.trackId = trackId;
    this.useTempDir = useTempDir;
    context = saveActivity.getApplicationContext();
    
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(saveActivity);

    // Get the wake lock if not recording or paused
    if (PreferencesUtils.getLong(saveActivity, R.string.recording_track_id_key)
        == PreferencesUtils.RECORDING_TRACK_ID_DEFAULT || PreferencesUtils.getBoolean(saveActivity,
        R.string.recording_track_paused_key, PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT)) {
      wakeLock = SystemUtils.acquireWakeLock(saveActivity, wakeLock);
    }
    
    trackWriter = null;
    success = false;
    completed = false;
    messageId = R.string.external_storage_save_error;
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
      saveActivity.onAsyncTaskCompleted(success, messageId, savedPath);
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
        return saveOneTrack(trackId, true);
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

  /**
   * Saves one track.
   * 
   * @param id the track id
   * @param updateSavingProgress true to update the saving progress
   */
  private Boolean saveOneTrack(long id, final boolean updateSavingProgress) {
    trackWriter = TrackWriterFactory.newWriter(
        context, myTracksProviderUtils, id, trackFileFormat);
    if (trackWriter == null) {
      Log.e(TAG, "Track writer is null");
      return false;
    }
    if (useTempDir) {
      String dirName = FileUtils.buildExternalDirectoryPath(trackFileFormat.getExtension(), "tmp");
      trackWriter.setDirectory(new File(dirName));
    }
    trackWriter.setOnWriteListener(new TrackWriter.OnWriteListener() {
        @Override
      public void onWrite(int number, int max) {
        // Update the progress dialog once every 500 points
        if (updateSavingProgress && number % 500 == 0) {
          publishProgress(number, max);
        }
      }
    });
    trackWriter.writeTrack();
    messageId = trackWriter.getErrorMessage();
    savedPath = trackWriter.getAbsolutePath();
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
        messageId = R.string.external_storage_save_error_no_track;
        return false;
      }
      int count = cursor.getCount();
      if (count == 0) {
        messageId = R.string.external_storage_save_error_no_track;
        return false;
      }
      int idIndex = cursor.getColumnIndexOrThrow(TracksColumns._ID);
      for (int i = 0; i < count; i++) {
        if (isCancelled()) {
          return false;
        }
        cursor.moveToPosition(i);
        long id = cursor.getLong(idIndex);
        if (!saveOneTrack(id, false)) {
          return false;
        }        
        publishProgress(i + 1, count);
      }
      messageId = R.string.external_storage_save_success;
      return true;
    } finally {
      if (cursor != null) {
        cursor.close();
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
    success = result;
    completed = true;
    if (saveActivity != null) {
      saveActivity.onAsyncTaskCompleted(success, messageId, savedPath);
    }
  }

  @Override
  protected void onCancelled() {
    if (trackWriter != null) {
      trackWriter.stopWriteTrack();
    }
  }
}
