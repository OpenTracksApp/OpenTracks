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
package com.google.android.apps.mytracks.io;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.TrackWriterFactory.TrackFileFormat;

import android.location.Location;
import android.os.Build;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Write out a a track in the Garmin training center database, tcx format.
 * As defined by:
 * http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2
 *
 * @author Sandor Dornbush
 */
public class TcxTrackWriter implements TrackFormatWriter {

  private PrintWriter pw = null;
  private Track track;

  static final SimpleDateFormat TIMESTAMP_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  static {
    TIMESTAMP_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public TcxTrackWriter() {
  }

  @Override
  public void prepare(Track track, OutputStream out) {
    this.track = track;
    this.pw = new PrintWriter(out);

  }

  @Override
  public void close() {
    if (pw != null) {
      pw.close();
      pw = null;
    }
  }

  @Override
  public String getExtension() {
    return TrackFileFormat.TCX.getExtension();
  }

  @Override
  public void writeHeader() {
    if (pw == null) {
      return;
    }
    pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
    pw.print("<TrainingCenterDatabase ");
    pw.print("xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\" ");
    pw.print("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
    pw.print("xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2 ");
    pw.println("http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\">");
    pw.println();
  }

  @Override
  public void writeBeginTrack(Location firstPoint) {
    if (pw == null) {
      return;
    }
    pw.println("  <Activities>");
    pw.print("    <Activity Sport=\"");
    if (track.getCategory() != null) {
      pw.print(track.getCategory());
    }
    pw.println("\">");
    pw.print("      <Id>");
    pw.print(TIMESTAMP_FORMAT.format(track.getStatistics().getStartTime()));
    pw.println("</Id>");
    pw.println("      <Lap>");
    pw.print("        <TotalTimeSeconds>");
    pw.print(track.getStatistics().getTotalTime() / 1000);
    pw.println("</TotalTimeSeconds>");
    pw.print("        <DistanceMeters>");
    pw.print(track.getStatistics().getTotalDistance());
    pw.println("</DistanceMeters>");
    // TODO max speed etc.
    // Calories are a required element just put in 0.
    pw.print("<Calories>0</Calories>");
    pw.println("<Intensity>Active</Intensity>");
    pw.println("<TriggerMethod>Manual</TriggerMethod>)");
  }

  @Override
  public void writeOpenSegment() {
    if (pw != null) {
      pw.println("      <Track>");
    }
  }

  @Override
  public void writeLocation(Location location) {
    if (pw == null) {
      return;
    }
    pw.println("        <Trackpoint>");
    Date d = new Date(location.getTime());
    pw.println("          <Time>" + TIMESTAMP_FORMAT.format(d) + "</Time>");
    pw.println("          <Position>");

    pw.print("            <LatitudeDegrees>");
    pw.print(location.getLatitude());
    pw.println("</LatitudeDegrees>");

    pw.print("            <LongitudeDegrees>");
    pw.print(location.getLongitude());
    pw.println("</LongitudeDegrees>");

    pw.println("          </Position>");
    pw.print("          <AltitudeMeters>");
    pw.print(location.getAltitude());
    pw.println("</AltitudeMeters>");

    pw.println("        </Trackpoint>");
  }

  @Override
  public void writeCloseSegment() {
    if (pw != null) {
      pw.println("      </Track>");
    }
  }

  @Override
  public void writeEndTrack(Location lastPoint) {
    if (pw == null) {
      return;
    }
    pw.println("      </Lap>");
    pw.print("      <Creator xsi:type=\"Device_t\">");
    pw.print("<Name>");
    pw.print(Build.MODEL);
    pw.print("</Name>");
    pw.println("</Creator>)");
    pw.println("    </Activity>");
    pw.println("  </Activities>");
  }

  @Override
  public void writeFooter() {
    if (pw == null) {
      return;
    }
    pw.print("  <Author xsi:type=\"Application_t\">");
    pw.print("<Name>My Tracks by Google</Name>");
    pw.println("</Author>");
    pw.println("</TrainingCenterDatabase>");
  }

  @Override
  public void writeWaypoint(Waypoint waypoint) {
    // TODO Write out the waypoints somewhere.
  }
}
