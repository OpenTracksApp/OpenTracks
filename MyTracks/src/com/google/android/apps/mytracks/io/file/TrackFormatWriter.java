/*
 * Copyright 2010 Google Inc.
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

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;

import android.location.Location;

import java.io.OutputStream;

/**
 * Interface for writing data to a specific track file format.
 *
 * The expected sequence of calls is:
 * <ol>
 *   <li>{@link #prepare}
 *   <li>{@link #writeHeader}
 *   <li>{@link #writeBeginTrack}
 *   <li>For each segment:
 *   <ol>
 *     <li>{@link #writeOpenSegment}
 *     <li>For each location in the segment: {@link #writeLocation}
 *     <li>{@link #writeCloseSegment}
 *   </ol>
 *   <li>{@link #writeEndTrack}
 *   <li>For each waypoint: {@link #writeWaypoint}
 *   <li>{@link #writeFooter}
 * </ol>
 *
 * @author Rodrigo Damazio
 */
public interface TrackFormatWriter {

  /**
   * Sets up the writer to write the given track to the given output.
   *
   * @param track the track to write
   * @param out the stream to write the track contents to
   */
  void prepare(Track track, OutputStream out);

  /**
   * @return The file extension (i.e. gpx, kml, ...)
   */
  String getExtension();

  /**
   * Writes the header.
   * This is chance for classes to write out opening information.
   */
  void writeHeader();

  /**
   * Writes the footer.
   * This is chance for classes to write out closing information.
   */
  void writeFooter();

  /**
   * Write the given location object.
   *
   * TODO Add some flexible handling of other sensor data.
   *
   * @param location the location to write
   */
  void writeLocation(Location location) throws InterruptedException;

  /**
   * Write a way point.
   *
   * @param waypoint
   */
  void writeWaypoint(Waypoint waypoint);

  /**
   * Write the beginning of a track.
   */
  void writeBeginTrack(Location firstPoint);

  /**
   * Write the end of a track.
   */
  void writeEndTrack(Location lastPoint);

  /**
   * Write the statements necessary to open a new segment.
   */
  void writeOpenSegment();

  /**
   * Write the statements necessary to close a segment.
   */
  void writeCloseSegment();

  /**
   * Close the underlying file handle.
   */
  void close();
}
