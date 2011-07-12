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
package com.google.android.apps.mytracks.io.file;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.LocationUtils;
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
 * This class exports tracks to the SD card.  It is intended to be format-
 * neutral -- it handles creating the output file and reading the track to be
 * exported, but requires an instance of {@link TrackFormatWriter} to actually
 * format the data.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
class TrackWriterImpl implements TrackWriter {
  private final Context context;
  private final MyTracksProviderUtils providerUtils;
  private final Track track;
  private final TrackFormatWriter writer;
  private final FileUtils fileUtils;
  private boolean success = false;
  private int errorMessage = -1;
  private File directory = null;
  private File file = null;
  private OnCompletionListener onCompletionListener;
  private OnWriteListener onWriteListener;
  private Thread writeThread;

  TrackWriterImpl(Context context, MyTracksProviderUtils providerUtils,
      Track track, TrackFormatWriter writer) {
    this.context = context;
    this.providerUtils = providerUtils;
    this.track = track;
    this.writer = writer;
    this.fileUtils = new FileUtils();
  }

  @Override
  public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
    this.onCompletionListener = onCompletionListener;
  }

  @Override
  public void setOnWriteListener(OnWriteListener onWriteListener) {
    this.onWriteListener = onWriteListener;
  }

  @Override
  public void setDirectory(File directory) {
    this.directory = directory;
  }

  @Override
  public String getAbsolutePath() {
    return file.getAbsolutePath();
  }

  @Override
  public void writeTrackAsync() {
    writeThread = new Thread() {
      @Override
      public void run() {
        doWriteTrack();
      }
    };
    writeThread.start();
  }

  @Override
  public void writeTrack() {
    writeTrackAsync();
    try {
      writeThread.join();
    } catch (InterruptedException e) {
      Log.e(Constants.TAG, "Interrupted waiting for write to complete", e);
    }
  }

  private void doWriteTrack() {
    // Open the input and output
    success = false;
    errorMessage = R.string.error_track_does_not_exist;
    if (track != null) {
      if (openFile()) {
        try {
          writeDocument();
        } catch (InterruptedException e) {
          Log.i(Constants.TAG, "The track write was interrupted");
          if (file != null) {
            if (!file.delete()) {
              Log.w(TAG, "Failed to delete file " + file.getAbsolutePath());
            }
          }
          success = false;
          errorMessage = R.string.error_operation_cancelled;
        }
      }
    }
    finished();
  }

  public void stopWriteTrack() {
    if (writeThread != null && writeThread.isAlive()) {
      Log.i(Constants.TAG, "Attempting to stop track write");
      writeThread.interrupt();

      try {
        writeThread.join();
        Log.i(Constants.TAG, "Track write stopped");
      } catch (InterruptedException e) {
        Log.e(Constants.TAG, "Failed to wait for writer to stop", e);
      }
    }
  }

  @Override
  public int getErrorMessage() {
    return errorMessage;
  }

  @Override
  public boolean wasSuccess() {
    return success;
  }

  /*
   * Helper methods:
   * ===============
   */

  private void finished() {
    if (onCompletionListener != null) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          onCompletionListener.onComplete();
        }
      });
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
      Log.e(Constants.TAG,
          "Unable to get a unique filename for " + track.getName());
      return false;
    }

    Log.i(Constants.TAG, "Writing track to: " + fileName);
    try {
      writer.prepare(track, newOutputStream(fileName));
    } catch (FileNotFoundException e) {
      Log.e(Constants.TAG, "Failed to open output file.", e);
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
      Log.i(Constants.TAG, "Could not find SD card.");
      errorMessage = R.string.io_no_external_storage_found;
      return false;
    }
    if (!fileUtils.ensureDirectoryExists(directory)) {
      Log.i(Constants.TAG, "Could not create export directory.");
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
        Constants.MAX_LOADED_WAYPOINTS_POINTS);
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
        cursor.close();
      }
    }
  }

  /**
   * Does the actual work of writing the track to the now open file.
   */
  void writeDocument() throws InterruptedException {
    Log.d(Constants.TAG, "Started writing track.");
    writer.writeHeader();
    writeWaypoints(track.getId());
    writeLocations();
    writer.writeFooter();
    writer.close();
    success = true;
    Log.d(Constants.TAG, "Done writing track.");
    errorMessage = R.string.io_write_finished;
  }

  private void writeLocations() throws InterruptedException {
    boolean wroteFirst = false;
    boolean segmentOpen = false;
    boolean isLastValid = false;

    class TrackWriterLocationFactory implements MyTracksProviderUtils.LocationFactory {
      Location currentLocation;
      Location lastLocation;

      @Override
      public Location createLocation() {
        if (currentLocation == null) {
          currentLocation = new MyTracksLocation("");
        }
        return currentLocation;
      }

      public void swapLocations() {
        Location tmpLoc = lastLocation;
        lastLocation = currentLocation;
        currentLocation = tmpLoc;
        if (currentLocation != null) {
          currentLocation.reset();
        }
      }
    };

    TrackWriterLocationFactory locationFactory = new TrackWriterLocationFactory();
    LocationIterator it = providerUtils.getLocationIterator(track.getId(), 0, false,
        locationFactory);
    try {
      if (!it.hasNext()) {
        Log.w(Constants.TAG, "Unable to get any points to write");
        return;
      }
      int pointNumber = 0;
      while (it.hasNext()) {
        Location loc = it.next();
        if (Thread.interrupted()) {
          throw new InterruptedException();
        }

        pointNumber++;

        boolean isValid = LocationUtils.isValidLocation(loc);
        boolean validSegment = isValid && isLastValid;
        if (!wroteFirst && validSegment) {
          // Found the first two consecutive points which are valid
          writer.writeBeginTrack(locationFactory.lastLocation);
          wroteFirst = true;
        }

        if (validSegment) {
          if (!segmentOpen) {
            // Start a segment for this point
            writer.writeOpenSegment();
            segmentOpen = true;

            // Write the previous point, which we had previously skipped
            writer.writeLocation(locationFactory.lastLocation);
          }

          // Write the current point
          writer.writeLocation(loc);
          if (onWriteListener != null) {
            onWriteListener.onWrite(pointNumber, track.getNumberOfPoints());
          }
        } else {
          if (segmentOpen) {
            writer.writeCloseSegment();
            segmentOpen = false;
          }
        }

        locationFactory.swapLocations();
        isLastValid = isValid;
      }
      if (segmentOpen) {
        writer.writeCloseSegment();
        segmentOpen = false;
      }
      if (wroteFirst) {
        writer.writeEndTrack(locationFactory.lastLocation);
      }
    } finally {
      it.close();
    }
  }
}
