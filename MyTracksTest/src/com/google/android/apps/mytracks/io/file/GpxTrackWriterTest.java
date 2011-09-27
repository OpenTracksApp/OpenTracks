// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.file;

import com.google.android.apps.mytracks.io.file.GpxTrackWriter;
import com.google.android.apps.mytracks.io.file.TrackFormatWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
    List<Element> segPointTags = getChildElements(segmentTags.get(0), "trkpt", 2);
    assertTagMatchesLocation(segPointTags.get(0),
        "0", "0", "1970-01-01T02:46:40Z", "0");
    assertTagMatchesLocation(segPointTags.get(1),
        "1", "-1", "1970-01-01T02:46:41Z", "5000000");

    segPointTags = getChildElements(segmentTags.get(1), "trkpt", 2);
    assertTagMatchesLocation(segPointTags.get(0),
        "2", "-2", "1970-01-01T02:46:42Z", "10000000");
    assertTagMatchesLocation(segPointTags.get(1),
        "3", "-3", "1970-01-01T02:46:43Z", "15000000");

    List<Element> waypointTags = getChildElements(gpxTag, "wpt", 2);
    Element wptTag = waypointTags.get(0);
    assertEquals(WAYPOINT1_NAME, getChildTextValue(wptTag, "name"));
    assertEquals(WAYPOINT1_DESCRIPTION, getChildTextValue(wptTag, "desc"));
    assertTagMatchesLocation(wptTag,
        "1", "-1", "1970-01-01T02:46:41Z", "5000000");

    wptTag = waypointTags.get(1);
    assertEquals(WAYPOINT2_NAME, getChildTextValue(wptTag, "name"));
    assertEquals(WAYPOINT2_DESCRIPTION, getChildTextValue(wptTag, "desc"));
    assertTagMatchesLocation(wptTag,
        "2", "-2", "1970-01-01T02:46:42Z", "10000000");
  }

  /**
   * Asserts that the given tag describes the location given by the
   * Strings lat, lon, time, and ele.
   */
  private void assertTagMatchesLocation(Element tag, String lat,
      String lon, String time, String ele) {
    assertEquals(lat, tag.getAttribute("lat"));
    assertEquals(lon, tag.getAttribute("lon"));
    assertEquals(time, getChildTextValue(tag, "time"));
    assertEquals(ele, getChildTextValue(tag, "ele"));
  }
}
