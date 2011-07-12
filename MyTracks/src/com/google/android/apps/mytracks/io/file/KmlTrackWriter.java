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

import android.content.Context;
import android.location.Location;
import android.os.Build;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Vector;

/**
 * Write track as KML to a file.
 *
 * @author Leif Hendrik Wilden
 */
public class KmlTrackWriter implements TrackFormatWriter {

  private final Vector<Double> distances = new Vector<Double>();
  private final Vector<Double> elevations = new Vector<Double>();
  private final StringUtils stringUtils;
  private PrintWriter pw = null;
  private Track track;

  public KmlTrackWriter(Context context) {
    stringUtils = new StringUtils(context);
  }

  /**
   * Testing constructor.
   */
  KmlTrackWriter(StringUtils stringUtils) {
    this.stringUtils = stringUtils;
  }

  @SuppressWarnings("hiding")
  @Override
  public void prepare(Track track, OutputStream out) {
    this.track = track;
    this.pw = new PrintWriter(out);
  }

  @Override
  public String getExtension() {
    return TrackFileFormat.KML.getExtension();
  }

  @Override
  public void writeHeader() {
    if (pw != null) {
      pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      pw.print("<kml");
      pw.print(" xmlns=\"http://earth.google.com/kml/2.0\"");
      pw.println(" xmlns:atom=\"http://www.w3.org/2005/Atom\">");
      pw.println("<Document>");
      pw.format("<atom:author><atom:name>My Tracks running on %s"
          + "</atom:name></atom:author>\n", Build.MODEL);

      pw.println("<name>" + StringUtils.stringAsCData(track.getName())
          + "</name>");
      pw.println("<description>"
          + StringUtils.stringAsCData(track.getDescription())
          + "</description>");
      writeStyles();
    }
  }

  @Override
  public void writeFooter() {
    if (pw != null) {
      pw.println("</Document>");
      pw.println("</kml>");
    }
  }

  @Override
  public void writeBeginTrack(Location firstPoint) {
    if (pw != null) {
      writePlacemark("(Start)", track.getDescription(), "#sh_green-circle",
          firstPoint);
      pw.println("<Placemark>");
      pw.println("<name>" + StringUtils.stringAsCData(track.getName())
          + "</name>");
      pw.println("<description>"
          + StringUtils.stringAsCData(track.getDescription())
          + "</description>");
      pw.println("<styleUrl>#track</styleUrl>");
      pw.println("<MultiGeometry>");
    }
  }

  @Override
  public void writeEndTrack(Location lastPoint) {
    if (pw != null) {
      pw.println("</MultiGeometry>");
      pw.println("</Placemark>");
      String description = stringUtils.generateTrackDescription(
          track, distances, elevations);
      writePlacemark("(End)", description, "#sh_red-circle", lastPoint);
    }
  }

  @Override
  public void writeOpenSegment() {
    if (pw != null) {
      pw.print("<LineString><coordinates>");
    }
  }

  @Override
  public void writeCloseSegment() {
    if (pw != null) {
      pw.println("</coordinates></LineString>");
    }
  }

  @Override
  public void writeLocation(Location l) {
    if (pw != null) {
      pw.print(l.getLongitude() +  "," + l.getLatitude() + ","
          + l.getAltitude() + " ");
    }
  }

  private String getPinStyle(Waypoint waypoint) {
    if (waypoint.getType() == Waypoint.TYPE_STATISTICS) {
      return "#sh_ylw-pushpin";
    }
    // Try to find the icon color.
    // The string should be of the form:
    // "http://maps.google.com/mapfiles/ms/micons/XXX.png"
    int slash = waypoint.getIcon().lastIndexOf('/');
    int png = waypoint.getIcon().lastIndexOf('.');
    if ((slash != -1) && (slash < png)) {
      String color = waypoint.getIcon().substring(slash + 1, png);
      return "#sh_" + color + "-pushpin";
    }
    return "#sh_blue-pushpin";
  }

  @Override
  public void writeWaypoint(Waypoint waypoint) {
    if (pw != null) {
      writePlacemark(
          waypoint.getName(),
          waypoint.getDescription(),
          getPinStyle(waypoint),
          waypoint.getLocation());
    }
  }

  @Override
  public void close() {
    if (pw != null) {
      pw.close();
      pw = null;
    }
  }

  private void writeStyles() {
    pw.println("<Style id=\"track\"><LineStyle><color>7f0000ff</color>"
        + "<width>4</width></LineStyle></Style>");

    pw.print("<Style id=\"sh_green-circle\"><IconStyle><scale>1.3</scale>");
    pw.print("<Icon><href>http://maps.google.com/mapfiles/kml/paddle/"
        + "grn-circle.png</href></Icon>");
    pw.println("<hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>"
        + "</IconStyle></Style>");

    pw.print("<Style id=\"sh_red-circle\"><IconStyle><scale>1.3</scale>");
    pw.print("<Icon><href>http://maps.google.com/mapfiles/kml/paddle/"
        + "red-circle.png</href></Icon>");
    pw.println("<hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>"
        + "</IconStyle></Style>");

    pw.print("<Style id=\"sh_ylw-pushpin\"><IconStyle><scale>1.3</scale>");
    pw.print("<Icon><href>http://maps.google.com/mapfiles/kml/pushpin/"
        + "ylw-pushpin.png</href></Icon>");
    pw.println("<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>"
        + "</IconStyle></Style>");

    pw.print("<Style id=\"sh_blue-pushpin\"><IconStyle><scale>1.3</scale>");
    pw.print("<Icon><href>http://maps.google.com/mapfiles/kml/pushpin/"
        + "blue-pushpin.png</href></Icon>");
    pw.println("<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>"
        + "</IconStyle></Style>");

    pw.print("<Style id=\"sh_green-pushpin\"><IconStyle><scale>1.3</scale>");
    pw.print("<Icon><href>http://maps.google.com/mapfiles/kml/pushpin/"
        + "grn-pushpin.png</href></Icon>");
    pw.println("<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>"
        + "</IconStyle></Style>");

    pw.print("<Style id=\"sh_red-pushpin\"><IconStyle><scale>1.3</scale>");
    pw.print("<Icon><href>http://maps.google.com/mapfiles/kml/pushpin/"
        + "red-pushpin.png</href></Icon>");
    pw.println("<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>"
        + "</IconStyle></Style>");
  }

  private void writePlacemark(String name, String description, String style,
      Location location) {
    if (location != null) {
      pw.println("<Placemark>");
      pw.println("  <name>" + StringUtils.stringAsCData(name) + "</name>");
      pw.println("  <description>" + StringUtils.stringAsCData(description)
          + "</description>");
      pw.println("  <styleUrl>" + style + "</styleUrl>");
      pw.println("  <Point>");
      pw.println("    <coordinates>" + location.getLongitude() + ","
          + location.getLatitude() + "</coordinates>");
      pw.println("  </Point>");
      pw.println("</Placemark>");
    }
  }
}
