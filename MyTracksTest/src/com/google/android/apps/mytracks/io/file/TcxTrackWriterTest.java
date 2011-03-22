// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.file;

import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.io.file.TcxTrackWriter;
import com.google.android.apps.mytracks.io.file.TrackFormatWriter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests for the GPX track exporter.
 *
 * @author Sandor Dornbush
 */
public class TcxTrackWriterTest extends TrackFormatWriterTest {
  private DateFormat timestampFormatter;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    timestampFormatter = new SimpleDateFormat(TcxTrackWriter.TIMESTAMP_FORMAT);
    timestampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public void testXmlOutput() throws Exception {
    TrackFormatWriter writer = new TcxTrackWriter(getContext());
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
  private void assertTagsMatchPoints(List<Element> tags, MyTracksLocation... locs) {
    assertEquals(locs.length, tags.size());
    for (int i = 0; i < locs.length; i++) {
      Element tag = tags.get(i);
      MyTracksLocation loc = locs[i];

      assertTagMatchesLocation(tag, loc);
    }
  }

  /**
   * Asserts that the given tag describes the given location.
   */
  private void assertTagMatchesLocation(Element tag, MyTracksLocation loc) {
    Element posTag = getChildElement(tag, "Position");
    assertEquals(Double.toString(loc.getLatitude()),
        getChildTextValue(posTag, "LatitudeDegrees"));
    assertEquals(Double.toString(loc.getLongitude()),
        getChildTextValue(posTag, "LongitudeDegrees"));

    assertEquals(timestampFormatter.format(loc.getTime()),
        getChildTextValue(tag, "Time"));
    assertEquals(Double.toString(loc.getAltitude()),
        getChildTextValue(tag, "AltitudeMeters"));
    assertTrue(loc.getSensorDataSet() != null);
    Sensor.SensorDataSet sds = loc.getSensorDataSet();

    List<Element> bpm = getChildElements(tag, "HeartRateBpm", 1);
    assertEquals(Integer.toString(sds.getHeartRate().getValue()),
        getChildTextValue(bpm.get(0), "Value"));

    List<Element> ext = getChildElements(tag, "Extensions", 1);
    List<Element> tpx = getChildElements(ext.get(0), "TPX", 1);
    assertEquals(Integer.toString(sds.getPower().getValue()),
        getChildTextValue(tpx.get(0), "Watts"));
  }
}
