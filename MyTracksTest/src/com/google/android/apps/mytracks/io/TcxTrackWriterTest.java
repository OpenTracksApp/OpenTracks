// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io;

import android.location.Location;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Date;
import java.util.List;

/**
 * Tests for the GPX track exporter.
 *
 * @author Sandor Dornbush
 */
public class TcxTrackWriterTest extends TrackFormatWriterTest {

  public void testXmlOutput() throws Exception {
    TrackFormatWriter writer = new TcxTrackWriter();
    String result = writeTrack(writer);
    Document doc = parseXmlDocument(result);

    Element root = getChildElement(doc, "TrainingCenterDatabase");
    Element activitiesTag = getChildElement(root, "Activities");
    Element activityTag = getChildElement(activitiesTag, "Activity");
    Element lapTag = getChildElement(activityTag, "Lap");

    List<Element> segmentTags = getChildElements(lapTag, "Track", 2);
    Element segment1Tag = segmentTags.get(0);
    Element segment2Tag = segmentTags.get(1);
    List<Element> seg1PointTags = getChildElements(segment1Tag, "Trackpoint", 2);
    List<Element> seg2PointTags = getChildElements(segment2Tag, "Trackpoint", 2);
    assertTagsMatchPoints(seg1PointTags, location1, location2);
    assertTagsMatchPoints(seg2PointTags, location3, location4);
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
    Element posTag = getChildElement(tag, "Position");
    assertEquals(Double.toString(loc.getLatitude()),
        getChildTextValue(posTag, "LatitudeDegrees"));
    assertEquals(Double.toString(loc.getLongitude()),
        getChildTextValue(posTag, "LongitudeDegrees"));
    assertEquals(
        TcxTrackWriter.TIMESTAMP_FORMAT.format(new Date(loc.getTime())),
        getChildTextValue(tag, "Time"));
    assertEquals(Double.toString(loc.getAltitude()),
        getChildTextValue(tag, "AltitudeMeters"));
  }
}
