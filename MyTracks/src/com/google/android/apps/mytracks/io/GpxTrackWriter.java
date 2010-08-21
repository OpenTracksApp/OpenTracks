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

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.util.StringUtils;

import android.location.Location;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Log of one track.
 *
 * @author Sandor Dornbush
 */
public class GpxTrackWriter implements TrackFormatWriter {

  static final SimpleDateFormat TIMESTAMP_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  static {
    TIMESTAMP_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private PrintWriter pw = null;
  private Track track;

  @Override
  public void prepare(Track track, OutputStream out) {
    this.track = track;
    this.pw = new PrintWriter(out);
  }

  @Override
  public String getExtension() {
    return TrackFileFormat.GPX.getExtension();
  }

  @Override
  public void writeHeader() {
    if (pw != null) {
      pw.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ");
      pw.println("standalone=\"yes\"?>");
      pw.println("<?xml-stylesheet type=\"text/xsl\" href=\"details.xsl\"?>");
      pw.println("<gpx");
      pw.println(" version=\"1.0\"");
      pw.println(" creator=\"My Tracks for the G1 running Android\"");
      pw.println(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
      pw.println(" xmlns=\"http://www.topografix.com/GPX/1/0\"");
      pw.print(" xmlns:topografix=\"http://www.topografix.com/GPX/Private/"
          + "TopoGrafix/0/1\"");
      pw.print(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 ");
      pw.print("http://www.topografix.com/GPX/1/0/gpx.xsd ");
      pw.print("http://www.topografix.com/GPX/Private/TopoGrafix/0/1 ");
      pw.println("http://www.topografix.com/GPX/Private/TopoGrafix/0/1/"
          + "topografix.xsd\">");
      // TODO: Author etc.
    }
  }

  @Override
  public void writeFooter() {
    if (pw != null) {
      pw.println("</gpx>");
    }
  }

  @Override
  public void writeBeginTrack(Location firstPoint) {
    if (pw != null) {
      pw.println("<trk>");
      pw.println("<name>" + StringUtils.stringAsCData(track.getName())
          + "</name>");
      pw.println("<desc>" + StringUtils.stringAsCData(track.getDescription())
          + "</desc>");
      pw.println("<number>" + track.getId() + "</number>");
      pw.println("<topografix:color>c0c0c0</topografix:color>");
    }
  }

  @Override
  public void writeEndTrack(Location lastPoint) {
    if (pw != null) {
      pw.println("</trk>");
    }
  }

  @Override
  public void writeOpenSegment() {
    pw.println("<trkseg>");
  }

  @Override
  public void writeCloseSegment() {
    pw.println("</trkseg>");
  }

  @Override
  public void writeLocation(Location l) {
    if (pw != null) {
      pw.println("<trkpt lat=\"" + l.getLatitude() + "\" lon=\""
          + l.getLongitude() + "\">");
      Date d = new Date(l.getTime());
      pw.println("<ele>" + l.getAltitude() + "</ele>");
      pw.println("<time>" + TIMESTAMP_FORMAT.format(d) + "</time>");
      pw.println("</trkpt>");
    }
  }

  @Override
  public void close() {
    if (pw != null) {
      pw.close();
      pw = null;
    }
  }

  @Override
  public void writeWaypoint(Waypoint waypoint) {
    if (pw != null) {
      // TODO: The gpx spec says waypoints should come *before* tracks
      Location l = waypoint.getLocation();
      if (l != null) {
        pw.println("<wpt lat=\"" + l.getLatitude() + "\" lon=\""
            + l.getLongitude() + "\">");
        pw.println("<name>" + StringUtils.stringAsCData(waypoint.getName())
            + "</name>");
        pw.println("<desc>"
            + StringUtils.stringAsCData(waypoint.getDescription()) + "</desc>");
        pw.println("<time>" + TIMESTAMP_FORMAT.format(l.getTime()) + "</time>");
        pw.println("<ele>" + l.getAltitude() + "</ele>");
        pw.println("</wpt>");
      }
    }
  }
}
