// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io;

import com.google.android.apps.mytracks.content.Waypoint;

import android.location.Location;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Date;
import java.util.List;

/**
 * Tests for the GPX track exporter.
 *
 * @author Rodrigo Damazio
 */
public class GpxTrackWriterTest extends TrackFormatWriterTest {

  public void testXmlOutput() throws Exception {
    TrackFormatWriter writer = new GpxTrackWriter();
    String result = writeTrack(writer);
    Document doc = parseXmlDocument(result);

    Element gpxTag = getChildElement(doc, "gpx");
    Element trackTag = getChildElement(gpxTag, "trk");
    assertEquals(TRACK_NAME, getChildTextValue(trackTag, "name"));
    assertEquals(TRACK_DESCRIPTION, getChildTextValue(trackTag, "desc"));
    assertEquals(Long.toString(TRACK_ID),
        getChildTextValue(trackTag, "number"));
    List<Element> segmentTags = getChildElements(trackTag, "trkseg", 2);
    Element segment1Tag = segmentTags.get(0);
    Element segment2Tag = segmentTags.get(1);
    List<Element> seg1PointTags = getChildElements(segment1Tag, "trkpt", 2);
    List<Element> seg2PointTags = getChildElements(segment2Tag, "trkpt", 2);
    assertTagsMatchPoints(seg1PointTags, location1, location2);
    assertTagsMatchPoints(seg2PointTags, location3, location4);
    List<Element> waypointTags = getChildElements(gpxTag, "wpt", 2);
    assertTagsMatchWaypoints(waypointTags, wp1, wp2);
  }

  /**
   * Asserts that the given tags describe the given waypoints, in the same
   * order.
   */
  protected void assertTagsMatchWaypoints(List<Element> tags, Waypoint... wps) {
    assertEquals(wps.length, tags.size());
    for (int i = 0; i < wps.length; i++) {
      Element tag = tags.get(i);
      Waypoint wp = wps[i];
      Location loc = wp.getLocation();

      assertTagMatchesLocation(tag, loc);

      assertEquals(wp.getName(), getChildTextValue(tag, "name"));
      assertEquals(wp.getDescription(), getChildTextValue(tag, "desc"));
    }
  }

  /**
   * Asserts that the given tags describe the given points, in the same order.
   */
  protected void assertTagsMatchPoints(List<Element> tags, Location... locs) {
    assertEquals(locs.length, tags.size());
    for (int i = 0; i < locs.length; i++) {
      Element tag = tags.get(i);
      Location loc = locs[i];

      assertTagMatchesLocation(tag, loc);
    }
  }

  /**
   * Asserts that the given tag describes the given location.
   */
  private void assertTagMatchesLocation(Element tag, Location loc) {
    assertEquals(Double.toString(loc.getLatitude()), tag.getAttribute("lat"));
    assertEquals(Double.toString(loc.getLongitude()), tag.getAttribute("lon"));
    assertEquals(
        GpxTrackWriter.TIMESTAMP_FORMAT.format(new Date(loc.getTime())),
        getChildTextValue(tag, "time"));
    assertEquals(Double.toString(loc.getAltitude()),
        getChildTextValue(tag, "ele"));
  }
}
