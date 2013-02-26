/*
 * Copyright 2011 Google Inc.
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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.util.Log;

import java.io.OutputStream;

/**
 * Track Writer for writing a track to an {@link OutputStream}.
 * 
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class TrackWriter {

  /**
   * Listener for when a track location is written.
   */
  public interface OnWriteListener {

    /**
     * When a track location is written.
     * 
     * @param number the location number
     * @param max the maximum number of locations, for calculation of completion
     *          percentage
     */
    public void onWrite(int number, int max);
  }

  private final static String TAG = TrackWriter.class.getSimpleName();

  private final MyTracksProviderUtils myTracksProviderUtils;
  private final Track track;
  private final TrackFormatWriter trackFormatWriter;
  private final OnWriteListener onWriteListener;

  private Thread writeThread;
  private boolean success = false;

  /**
   * Constructor.
   * 
   * @param context the context
   * @param myTracksProviderUtils the my tracks provider utils
   * @param track the track
   * @param trackFileFormat the track file format
   * @param onWriteListener the on write listener
   */
  public TrackWriter(Context context, MyTracksProviderUtils myTracksProviderUtils, Track track,
      TrackFileFormat trackFileFormat, OnWriteListener onWriteListener) {
    this(myTracksProviderUtils, track, trackFileFormat.newFormatWriter(context), onWriteListener);
  }

  @VisibleForTesting
  public TrackWriter(MyTracksProviderUtils myTracksProviderUtils, Track track,
      TrackFormatWriter trackFormatWriter, OnWriteListener onWriteListener) {
    this.myTracksProviderUtils = myTracksProviderUtils;
    this.track = track;
    this.trackFormatWriter = trackFormatWriter;
    this.onWriteListener = onWriteListener;
  }
  
  /**
   * Returns true if the write completed successfully.
   */
  public boolean wasSuccess() {
    return success;
  }

  /**
   * Writes the given track to the output stream.
   * 
   * @param outputStream the output stream.
   */
  public void writeTrack(final OutputStream outputStream) {
    writeThread = new Thread() {
        @Override
      public void run() {
        try {
          trackFormatWriter.prepare(track, outputStream);
          trackFormatWriter.writeHeader();
          writeWaypoints();
          writeLocations();
          trackFormatWriter.writeFooter();
          trackFormatWriter.close();
          success = true;
        } catch (InterruptedException e) {
          success = false;
        }
      }
    };
    writeThread.start();
    try {
      writeThread.join();
    } catch (InterruptedException e) {
      Log.e(TAG, "Interrupted while waiting for write to complete", e);
      success = false;
    }
  }

  /**
   * Stops any in-progress writes.
   */
  public void stopWriteTrack() {
    if (writeThread != null && writeThread.isAlive()) {
      Log.i(TAG, "Attempting to stop track write");
      writeThread.interrupt();

      try {
        writeThread.join();
        Log.i(TAG, "Track write stopped");
      } catch (InterruptedException e) {
        Log.e(TAG, "Interrupted while waiting for writer to stop", e);
        success = false;
      }
    }
  }

  /**
   * Writes the waypoints.
   */
  private void writeWaypoints() {
    /*
     * TODO: Stream through the waypoints in chunks. I am leaving the number of
     * waypoints very high which should not be a problem because we don't try to
     * load them into objects all at the same time.
     */
    boolean hasWaypoints = false;
    Cursor cursor = null;
    try {
      cursor = myTracksProviderUtils.getWaypointCursor(
          track.getId(), -1L, Constants.MAX_LOADED_WAYPOINTS_POINTS);
      if (cursor != null && cursor.moveToFirst()) {
        /*
         * Yes, this will skip the first waypoint and that is intentional as the
         * first waypoint holds the stats for the track.
         */
        while (cursor.moveToNext()) {
          if (!hasWaypoints) {
            trackFormatWriter.writeBeginWaypoints();
            hasWaypoints = true;
          }
          Waypoint waypoint = myTracksProviderUtils.createWaypoint(cursor);
          trackFormatWriter.writeWaypoint(waypoint);
        }
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    if (hasWaypoints) {
      trackFormatWriter.writeEndWaypoints();
    }
  }

  /**
   * Writes the locations.
   */
  private void writeLocations() throws InterruptedException {
    boolean wroteTrack = false;
    boolean wroteSegment = false;
    boolean isLastLocationValid = false;
    TrackWriterLocationFactory locationFactory = new TrackWriterLocationFactory();
    LocationIterator iterator = myTracksProviderUtils.getTrackPointLocationIterator(
        track.getId(), -1L, false, locationFactory);
 
    try {
      int locationNumber = 0;
      while (iterator.hasNext()) {
        Location location = iterator.next();
        if (Thread.interrupted()) {
          throw new InterruptedException();
        }
        locationNumber++;

        boolean isLocationValid = LocationUtils.isValidLocation(location);
        boolean isSegmentValid = isLocationValid && isLastLocationValid;
        if (!wroteTrack && isSegmentValid) {
          // Found the first two consecutive locations that are valid
          trackFormatWriter.writeBeginTrack(locationFactory.lastLocation);
          wroteTrack = true;
        }

        if (isSegmentValid) {
          if (!wroteSegment) {
            // Start a segment
            trackFormatWriter.writeOpenSegment();
            wroteSegment = true;

            // Write the previous location, which we had previously skipped
            trackFormatWriter.writeLocation(locationFactory.lastLocation);
          }

          // Write the current location
          trackFormatWriter.writeLocation(location);
          if (onWriteListener != null) {
            onWriteListener.onWrite(locationNumber, track.getNumberOfPoints());
          }
        } else {
          if (wroteSegment) {
            trackFormatWriter.writeCloseSegment();
            wroteSegment = false;
          }
        }
        locationFactory.swapLocations();
        isLastLocationValid = isLocationValid;
      }
      
      if (wroteSegment) {
        trackFormatWriter.writeCloseSegment();
        wroteSegment = false;
      }
      if (wroteTrack) {
        trackFormatWriter.writeEndTrack(locationFactory.lastLocation);
      } else {
        // Write an empty track
        trackFormatWriter.writeBeginTrack(null);
        trackFormatWriter.writeEndTrack(null);
      }
    } finally {
      iterator.close();
    }
  }

  /**
   * Track writer location factory. Keeping the last two locations.
   * 
   * @author Jimmy Shih
   */
  private class TrackWriterLocationFactory implements MyTracksProviderUtils.LocationFactory {
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
      Location tempLocation = lastLocation;
      lastLocation = currentLocation;
      currentLocation = tempLocation;
      if (currentLocation != null) {
        currentLocation.reset();
      }
    }
  }
}
