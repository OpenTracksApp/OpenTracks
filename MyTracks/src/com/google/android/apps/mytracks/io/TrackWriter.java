/*
 * Copyright 2008 Google Inc.
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
package com.google.android.apps.mytracks.io;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackBuffer;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.MyTracksUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * The class which exports tracks to the SD card.
 * This writer is format-neutral - it handles creating the output file
 * and reading the track to be exported, but requires an instance of
 * {@link TrackFormatWriter} to actually format the data.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class TrackWriter {

  private final Context context;
  private final MyTracksProviderUtils providerUtils;
  private final Track track;
  private final TrackFormatWriter writer;
  private final FileUtils fileUtils;
  private Runnable onCompletion = null;
  private boolean success = false;
  private int errorMessage = -1;
  private File directory = null;
  private File file = null;

  public TrackWriter(Context context, MyTracksProviderUtils providerUtils,
      Track track, TrackFormatWriter writer) {
    this.context = context;
    this.providerUtils = providerUtils;
    this.track = track;
    this.writer = writer;
    this.fileUtils = new FileUtils();
  }

  /**
   * Sets a completion callback.
   *
   * @param onCompletion Runnable that will be executed when finished
   */
  public void setOnCompletion(Runnable onCompletion) {
    this.onCompletion = onCompletion;
  }

  /**
   * Sets a custom directory where the file will be written.
   */
  public void setDirectory(File directory) {
    this.directory = directory;
  }

  public String getAbsolutePath() {
    return file.getAbsolutePath();
  }

  /**
   * Writes the given track id to the SD card.
   * This is non-blocking.
   */
  public void writeTrackAsync() {
    Thread t = new Thread() {
      @Override
      public void run() {
        writeTrack();
      }
    };
    t.start();
  }

  /**
   * Writes the given track id to the SD card.
   * This is blocking.
   */
  public void writeTrack() {
    // Open the input and output
    success = false;
    errorMessage = R.string.error_track_does_not_exist;
    if (track != null) {
      if (openFile()) {
        writeDocument();
      }
    }
    finished();
  }

  public int getErrorMessage() {
    return errorMessage;
  }

  public boolean wasSuccess() {
    return success;
  }

  /*
   * Helper methods:
   * ===============
   */

  private void finished() {
    if (onCompletion != null) {
      runOnUiThread(onCompletion);
      return;
    }
  }

  /**
   * Runs the given runnable in the UI thread.
   */
  protected void runOnUiThread(Runnable runnable) {
    if (context instanceof Activity) {
      ((Activity) context).runOnUiThread(runnable);
    }
  }

  /**
   * Opens the file and prepares the format writer for it. 
   * 
   * @return true on success, false otherwise (and errorMessage is set)
   */
  protected boolean openFile() {
    if (!canWriteFile()) {
      return false;
    }

    // Make sure the file doesn't exist yet (possibly by changing the filename)
    String fileName = fileUtils.buildUniqueFileName(
        directory, track.getName(), writer.getExtension());
    if (fileName == null) {
      Log.e(MyTracksConstants.TAG,
          "Unable to get a unique filename for " + fileName);
      return false;
    }

    Log.i(MyTracksConstants.TAG, "Writing track to: " + fileName);
    try {
      writer.prepare(track, newOutputStream(fileName));
    } catch (FileNotFoundException e) {
      Log.e(MyTracksConstants.TAG, "Failed to open output file.", e);
      errorMessage = R.string.io_write_failed;
      return false;
    }
    return true;
  }

  /**
   * Checks and returns whether we're ready to create the output file.
   */
  protected boolean canWriteFile() {
    if (directory == null) {
      String dirName =
          fileUtils.buildExternalDirectoryPath(writer.getExtension());
      directory = newFile(dirName);
    }

    if (!fileUtils.isSdCardAvailable()) {
      Log.i(MyTracksConstants.TAG, "Could not find SD card.");
      errorMessage = R.string.io_no_external_storage_found;
      return false;
    }
    if (!fileUtils.ensureDirectoryExists(directory)) {
      Log.i(MyTracksConstants.TAG, "Could not create export directory.");
      errorMessage = R.string.io_create_dir_failed;
      return false;
    }

    return true;
  }

  /**
   * Creates a new output stream to write to the given filename.
   *
   * @throws FileNotFoundException if the file could't be created
   */
  protected OutputStream newOutputStream(String fileName)
      throws FileNotFoundException {
    file = new File(directory, fileName);
    return new FileOutputStream(file);
  }

  /**
   * Creates a new file object for the given path.
   */
  protected File newFile(String path) {
    return new File(path);
  }

  /**
   * Writes the waypoints for the given track.
   *
   * @param trackId the ID of the track to write waypoints for
   */
  private void writeWaypoints(long trackId) {
    // TODO: Stream through he waypoints in chunks.
    // I am leaving the number of waypoints very high which should not be a
    // problem because we don't try to load them into objects all at the
    // same time.
    Cursor cursor = null;
    cursor = providerUtils.getWaypointsCursor(trackId, 0,
        MyTracksConstants.MAX_LOADED_WAYPOINTS_POINTS);
    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {
          // Yes, this will skip the 1st way point and that is intentional
          // as the 1st points holds the stats for the current/last segment.
          while (cursor.moveToNext()) {
            Waypoint wpt = providerUtils.createWaypoint(cursor);
            writer.writeWaypoint(wpt);
          }
        }
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
    }
  }

  /**
   * Does the actual work of writing the track to the now open file.
   */
  void writeDocument() {
    Log.d(MyTracksConstants.TAG, "Started writing track.");
    writer.writeHeader();
    TrackBuffer buffer = new TrackBuffer(1024);
    Location last = null;
    boolean wroteFirst = false;
    boolean segmentOpen = false;
    int nValidLocations = 0;

    // Fetch small pieces of the track.
    while (buffer.getLastLocationRead() < track.getStopId()) {
      Log.d(MyTracksConstants.TAG,
          "Reading track points starting at: " + buffer.getLastLocationRead());
      providerUtils.fillTrackPoints(track, buffer);
      if (!wroteFirst) {
        Location first = buffer.findStartLocation();
        writer.writeBeginTrack(first);
        wroteFirst = true;
      }

      Log.d(MyTracksConstants.TAG,
            "Reading " + buffer.getLocationsLoaded()
            + " Ending at: " + buffer.getLastLocationRead());
      for (int i = 0; i < buffer.getLocationsLoaded(); i++) {
        Location location = buffer.get(i);
        if (MyTracksUtils.isValidLocation(location)) {
          nValidLocations++;
          if (!segmentOpen) {
            writer.writeOpenSegment();
            segmentOpen = true;
          }
          writer.writeLocation(location);
          if (nValidLocations >= 2) {
            last = location;
          }
        } else {
          nValidLocations = 0;
          last = null;
          if (segmentOpen) {
            writer.writeCloseSegment();
            segmentOpen = false;
          }
        }
      }
    }
    if (segmentOpen) {
      writer.writeCloseSegment();
      segmentOpen = false;
    }
    if (wroteFirst) {
      writer.writeEndTrack(last);
    }
    writeWaypoints(track.getId());
    writer.writeFooter();
    writer.close();
    success = true;
    Log.d(MyTracksConstants.TAG, "Done writing track.");
    errorMessage = R.string.io_write_finished;
  }
}
