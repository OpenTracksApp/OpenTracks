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
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;

import android.location.Location;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Exports a track as a CSV file, according to RFC 4180.
 *
 * The first field is a type:
 * TRACK - track description
 * P - point
 * WAYPOINT - waypoint
 *
 * For each type, the fields are:
 *
 * TRACK,,,,,,,,name,description,
 * P,time,lat,lon,alt,bearing,accurancy,speed,,,segmentIdx
 * WAYPOINT,time,lat,lon,alt,bearing,accuracy,speed,name,description,
 *
 * @author Rodrigo Damazio
 */
public class CsvTrackWriter implements TrackFormatWriter {

  static final NumberFormat SHORT_FORMAT = NumberFormat.getInstance();
  static final SimpleDateFormat TIMESTAMP_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

  static {
    SHORT_FORMAT.setMaximumFractionDigits(4);
    TIMESTAMP_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private int segmentIdx = 0;
  private int numFields = -1;
  private PrintWriter pw;
  private Track track;

  @Override
  public String getExtension() {
    return TrackFileFormat.CSV.getExtension();
  }

  @SuppressWarnings("hiding")
  @Override
  public void prepare(Track track, OutputStream out) {
    this.track = track;
    this.pw = new PrintWriter(out);
  }

  @Override
  public void writeHeader() {
    writeCommaSeparatedLine("TYPE", "TIME", "LAT", "LON", "ALT", "BEARING",
        "ACCURACY", "SPEED", "NAME", "DESCRIPTION", "SEGMENT");
  }

  @Override
  public void writeBeginTrack(Location firstPoint) {
    writeCommaSeparatedLine("TRACK",
        null, null, null, null, null, null, null,
        track.getName(), track.getDescription(),
        null);
  }

  @Override
  public void writeOpenSegment() {
    // Do nothing
  }

  @Override
  public void writeLocation(Location location) {
    String timeStr = TIMESTAMP_FORMAT.format(new Date(location.getTime()));
    writeCommaSeparatedLine("P",
        timeStr,
        Double.toString(location.getLatitude()),
        Double.toString(location.getLongitude()),
        Double.toString(location.getAltitude()),
        Double.toString(location.getBearing()),
        SHORT_FORMAT.format(location.getAccuracy()),
        SHORT_FORMAT.format(location.getSpeed()),
        null, null,
        Integer.toString(segmentIdx));
  }

  @Override
  public void writeWaypoint(Waypoint waypoint) {
    Location location = waypoint.getLocation();
    String timeStr = TIMESTAMP_FORMAT.format(new Date(location.getTime()));
    writeCommaSeparatedLine("WAYPOINT",
        timeStr,
        Double.toString(location.getLatitude()),
        Double.toString(location.getLongitude()),
        Double.toString(location.getAltitude()),
        Double.toString(location.getBearing()),
        SHORT_FORMAT.format(location.getAccuracy()),
        SHORT_FORMAT.format(location.getSpeed()),
        waypoint.getName(),
        waypoint.getDescription(),
        null);
  }

  /**
   * Writes a single line of a comma-separated-value file.
   *
   * @param strs the values to be written as comma-separated
   */
  private void writeCommaSeparatedLine(String... strs) {
    if (numFields == -1) {
      numFields = strs.length;
    } else if (strs.length != numFields) {
      throw new IllegalArgumentException(
          "CSV lines with different number of fields");
    }

    boolean isFirst = true;
    for (String str : strs) {
      if (!isFirst) {
        pw.print(',');
      }
      isFirst = false;

      if (str != null) {
        pw.print('"');
        pw.print(str.replaceAll("\"", "\"\""));
        pw.print('"');
      }
    }
    pw.println();
  }

  @Override
  public void writeCloseSegment() {
    segmentIdx++;
  }

  @Override
  public void writeEndTrack(Location lastPoint) {
    // Do nothing
  }

  @Override
  public void writeFooter() {
    // Do nothing
  }

  @Override
  public void close() {
    pw.close();
  }
}
