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

import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.util.StringUtils;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests for {@link TcxTrackWriter}.
 *
 * @author Sandor Dornbush
 */
public class TcxTrackWriterTest extends TrackFormatWriterTest {

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
   * Asserts that the given tags describe the given locations in the same order.
   *
   * @param tags list of tags
   * @param locations list of locations
   */
  private void assertTagsMatchPoints(List<Element> tags, MyTracksLocation... locations) {
    assertEquals(locations.length, tags.size());
    for (int i = 0; i < locations.length; i++) {
      assertTagMatchesLocation(tags.get(i), locations[i]);
    }
  }

  /**
   * Asserts that the given tag describes the given location.
   *
   * @param tag the tag
   * @param location the location
   */
  private void assertTagMatchesLocation(Element tag, MyTracksLocation location) {
    assertEquals(StringUtils.formatDateTimeIso8601(location.getTime()), getChildTextValue(tag, "Time"));

    Element positionTag = getChildElement(tag, "Position");
    assertEquals(
        Double.toString(location.getLatitude()), getChildTextValue(positionTag, "LatitudeDegrees"));
    assertEquals(Double.toString(location.getLongitude()),
        getChildTextValue(positionTag, "LongitudeDegrees"));

    assertEquals(Double.toString(location.getAltitude()), getChildTextValue(tag, "AltitudeMeters"));
    assertTrue(location.getSensorDataSet() != null);
    Sensor.SensorDataSet sds = location.getSensorDataSet();

    List<Element> heartRate = getChildElements(tag, "HeartRateBpm", 1);
    assertEquals(Integer.toString(sds.getHeartRate().getValue()),
        getChildTextValue(heartRate.get(0), "Value"));

    List<Element> extensions = getChildElements(tag, "Extensions", 1);
    List<Element> tpx = getChildElements(extensions.get(0), "TPX", 1);
    assertEquals(
        Integer.toString(sds.getCadence().getValue()), getChildTextValue(tpx.get(0), "RunCadence"));
    assertEquals(
        Integer.toString(sds.getPower().getValue()), getChildTextValue(tpx.get(0), "Watts"));
  }
}
