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

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.util.StringUtils;

import android.location.Location;
import android.os.Build;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Log of one track.
 *
 * @author Sandor Dornbush
 */
public class GpxTrackWriter implements TrackFormatWriter {
  private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  private final NumberFormat elevationFormatter;
  private final NumberFormat coordinateFormatter;
  private final SimpleDateFormat timestampFormatter;
  private PrintWriter pw = null;
  private Track track;

  public GpxTrackWriter() {
    // GPX readers expect to see fractional numbers with US-style punctuation.
    // That is, they want periods for decimal points, rather than commas.
    elevationFormatter = NumberFormat.getInstance(Locale.US);
    elevationFormatter.setMaximumFractionDigits(1);
    elevationFormatter.setGroupingUsed(false);

    coordinateFormatter = NumberFormat.getInstance(Locale.US);
    coordinateFormatter.setMaximumFractionDigits(5);
    coordinateFormatter.setMaximumIntegerDigits(3);
    coordinateFormatter.setGroupingUsed(false);

    timestampFormatter = new SimpleDateFormat(TIMESTAMP_FORMAT);
    timestampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private String formatLocation(Location l) {
    return "lat=\"" + coordinateFormatter.format(l.getLatitude())
      + "\" lon=\"" + coordinateFormatter.format(l.getLongitude()) + "\"";
  }

  @SuppressWarnings("hiding")
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
      pw.format("<?xml version=\"1.0\" encoding=\"%s\" standalone=\"yes\"?>\n",
          Charset.defaultCharset().name());
      pw.println("<?xml-stylesheet type=\"text/xsl\" href=\"details.xsl\"?>");
      pw.println("<gpx");
      pw.println(" version=\"1.1\"");
      pw.format(" creator=\"My Tracks running on %s\"\n", Build.MODEL);
      pw.println(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
      pw.println(" xmlns=\"http://www.topografix.com/GPX/1/1\"");
      pw.print(" xmlns:topografix=\"http://www.topografix.com/GPX/Private/"
          + "TopoGrafix/0/1\"");
      pw.print(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 ");
      pw.print("http://www.topografix.com/GPX/1/1/gpx.xsd ");
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
      pw.println("<extensions><topografix:color>c0c0c0</topografix:color></extensions>");
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
      pw.println("<trkpt " + formatLocation(l) + ">");
      Date d = new Date(l.getTime());
      pw.println("<ele>" + elevationFormatter.format(l.getAltitude()) + "</ele>");
      pw.println("<time>" + timestampFormatter.format(d) + "</time>");
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
      Location l = waypoint.getLocation();
      if (l != null) {
        pw.println("<wpt " + formatLocation(l) + ">");
        pw.println("<ele>" + elevationFormatter.format(l.getAltitude()) + "</ele>");
        pw.println("<time>" + timestampFormatter.format(l.getTime()) + "</time>");
        pw.println("<name>" + StringUtils.stringAsCData(waypoint.getName())
            + "</name>");
        pw.println("<desc>"
            + StringUtils.stringAsCData(waypoint.getDescription()) + "</desc>");
        pw.println("</wpt>");
      }
    }
  }
}
