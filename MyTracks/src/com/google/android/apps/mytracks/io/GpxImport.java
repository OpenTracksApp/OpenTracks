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
import com.google.android.apps.mytracks.stats.TripStatisticsBuilder;
import com.google.android.apps.mytracks.util.MyTracksUtils;

import android.location.Location;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Helper class to parse a GPX file or string and convert it into a track
 * object.
 *
 * TODO: See if we can use a SAX style parser as the DOM style
 * parsing uses too much memory and will not allow import of very large GPX
 * files (limit currently set to 500KB).
 *
 * @author Leif Hendrik Wilden
 */
public class GpxImport {
  private static final SimpleDateFormat DATE_FORMAT1 =
      new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");
  private static final SimpleDateFormat DATE_FORMAT2 =
      new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
  private static final SimpleDateFormat DATE_FORMAT3 =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  /**
   * Reads GPS tracks from a GPX file and append tracks and their coordinates to
   * the given list of tracks.
   *
   * @param tracks a list of tracks
   * @param filename a file name
   * @throws SAXException a parsing error
   * @throws ParserConfigurationException internal error
   * @throws IOException a file reading problem
   */
  public static void importGPXFile(
      final String filename, final ArrayList<Track> tracks)
      throws SAXException, ParserConfigurationException, IOException,
          OutOfMemoryError {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    File file = new File(filename);
    if (file.length() > 500 * 1024) {
      // Better to throw an error now then, to let the VM do it. If the VM does
      // it, it will throw another exception while throwing the OutOfMemoryError
      // and that will kill out app with a nasty error message. In that way we
      // can at least display a reasonable message to the user and let her know
      // what the problem is.
      throw new OutOfMemoryError();
    }
    Document doc = builder.parse(file);
    importGPXDocument(tracks, doc);
  }

  /**
   * This is a utility class with only static members.
   */
  private GpxImport() {
  }

  /**
   * Reads GPS tracks from a GPX document and append then to the given list of
   * tracks.
   *
   * TODO: Break this method down into smaller ones
   *
   * @param tracks an array list of tracks
   * @param doc a document
   */
  public static void importGPXDocument(
      final ArrayList<Track> tracks, final Document doc) {
    NodeList trackNodes = doc.getElementsByTagName("trk");
    for (int n = 0; n < trackNodes.getLength(); n++) {
      Track track = new Track();
      tracks.add(track);
      ArrayList<Location> locations = track.getLocations();
      Location lastLocation = null;

      Node trkNode = trackNodes.item(n);
      NodeList segmentNodes = trkNode.getChildNodes();
      ArrayList<Node> nodes = new ArrayList<Node>();
      int nSegments = 0;
      for (int k = 0; k < segmentNodes.getLength(); k++) {
        Node segmentNode = segmentNodes.item(k);
        if (segmentNode.getNodeName().equals("name")) {
          track.setName(segmentNode.getFirstChild().getNodeValue());
        } else if (segmentNode.getNodeName().equals("description")) {
          track.setDescription(segmentNode.getFirstChild().getNodeValue());
        } else if (segmentNode.getNodeName().equals("trkseg")) {
          if (nSegments > 0) {
            // Add a segment separator:
            Location location = new Location("gps");
            location.setLatitude(100.0);
            location.setLongitude(100.0);
            location.setAltitude(0);
            if (locations.size() > 0) {
              long pointTime = locations.get(locations.size() - 1).getTime();
              location.setTime(pointTime);
            }
            track.addLocation(location);
            lastLocation = null;
          }
          nSegments++;
          NodeList segmentChildren = segmentNode.getChildNodes();
          nodes.clear();
          for (int j = 0; j < segmentChildren.getLength(); j++) {
            Node child = segmentChildren.item(j);
            if (child.getNodeName().equals("trkpt")) {
              nodes.add(child);
            }
          }
          for (int i = 0; i < nodes.size(); i++) {
            NamedNodeMap namedNodes = nodes.get(i).getAttributes();
            double lat = Double.parseDouble(namedNodes.getNamedItem("lat")
                .getNodeValue());
            double lon = Double.parseDouble(namedNodes.getNamedItem("lon")
                .getNodeValue());
            NodeList children = nodes.get(i).getChildNodes();
            Node elementNode = null;
            Node timeNode = null;
            for (int j = 0; j < children.getLength(); j++) {
              Node child = children.item(j);
              if (child.getNodeName().equals("ele")) {
                elementNode = child;
              } else if (child.getNodeName().equals("time")) {
                timeNode = child;
              }
            }
            String altitudeStr = null;
            if (elementNode != null) {
              altitudeStr = elementNode.getFirstChild().getNodeValue();
            } else {
              altitudeStr = "0";
            }
            String timeContents = null;
            if (timeNode != null) {
              timeContents = timeNode.getFirstChild().getNodeValue();
            } else {
              timeContents = "";
            }
            double altitude = Double.parseDouble(altitudeStr);
            long t = -1;
            try {
              // 1st try with time zone at end a la "+0000"
              t = DATE_FORMAT1.parse(timeContents).getTime();
            } catch (ParseException e) {
              // if that fails, try with a literal "Z" at the end (this is not
              // according to xml standard, but some gpx files are like that):
              try {
                t = DATE_FORMAT2.parse(timeContents).getTime();
              } catch (ParseException ex) {
                // some gpx timestamps have 3 additional digits at the end.
                try {
                  t = DATE_FORMAT3.parse(timeContents).getTime();
                } catch (ParseException exc) {
                  t = 0;
                }
              }
            }

            Location location = new Location("gps");
            location.setLatitude(lat);
            location.setLongitude(lon);
            location.setAltitude(altitude);
            location.setTime(t);
            // We don't have a speed and bearing in GPX, make something up from
            // the last two points:
            if (lastLocation != null) {
              final long dt = location.getTime() - lastLocation.getTime();
              if (dt > 0) {
                final float speed =
                    location.distanceTo(lastLocation) / (dt / 1000);
                location.setSpeed(speed);
              }
              location.setBearing(lastLocation.bearingTo(location));
            }
            lastLocation = location;
            if (MyTracksUtils.isValidLocation(location)) {
              track.addLocation(location);
            }
          }

          if (locations.size() > 0) {
            long startTime = locations.get(0).getTime();

            // Calculate statistics for the imported track
            TripStatisticsBuilder statsBuilder = new TripStatisticsBuilder();
            statsBuilder.resumeAt(startTime);
            for (Location location : locations) {
              if (MyTracksUtils.isValidLocation(location)) {
                /* Any time works here. The totalTime will be set by "pauseAt" later: */
                statsBuilder.addLocation(location, location.getTime());
              }
            }

            long lastPointTime = locations.get(locations.size() - 1).getTime();
            statsBuilder.pauseAt(lastPointTime);

            track.setStatistics(statsBuilder.getStatistics());
          }
        }
      }
    }
  }
}
