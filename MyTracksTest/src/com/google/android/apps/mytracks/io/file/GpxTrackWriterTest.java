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

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests for {@link GpxTrackWriter}.
 *
 * @author Rodrigo Damazio
 */
public class GpxTrackWriterTest extends TrackFormatWriterTest {

  public void testXmlOutput() throws Exception {
    TrackFormatWriter writer = new GpxTrackWriter(getContext());
    String result = writeTrack(writer);
    Document doc = parseXmlDocument(result);

    Element gpxTag = getChildElement(doc, "gpx");
    Element trackTag = getChildElement(gpxTag, "trk");
    assertEquals(TRACK_NAME, getChildTextValue(trackTag, "name"));
    assertEquals(TRACK_DESCRIPTION, getChildTextValue(trackTag, "desc"));
    List<Element> segmentTags = getChildElements(trackTag, "trkseg", 2);
    List<Element> segPointTags = getChildElements(segmentTags.get(0), "trkpt", 2);
    assertTagMatchesLocation(segPointTags.get(0), "0", "0", "1970-01-01T00:00:00.000Z", "0");
    assertTagMatchesLocation(segPointTags.get(1), "1", "-1", "1970-01-01T00:01:40.000Z", "10");

    segPointTags = getChildElements(segmentTags.get(1), "trkpt", 2);
    assertTagMatchesLocation(segPointTags.get(0), "2", "-2", "1970-01-01T00:03:20.000Z", "20");
    assertTagMatchesLocation(segPointTags.get(1), "3", "-3", "1970-01-01T00:05:00.000Z", "30");

    List<Element> waypointTags = getChildElements(gpxTag, "wpt", 2);
    Element wptTag = waypointTags.get(0);
    assertEquals(WAYPOINT1_NAME, getChildTextValue(wptTag, "name"));
    assertEquals(WAYPOINT1_DESCRIPTION, getChildTextValue(wptTag, "desc"));
    assertTagMatchesLocation(wptTag, "1", "-1", "1970-01-01T00:01:40.000Z", "10");

    wptTag = waypointTags.get(1);
    assertEquals(WAYPOINT2_NAME, getChildTextValue(wptTag, "name"));
    assertEquals(WAYPOINT2_DESCRIPTION, getChildTextValue(wptTag, "desc"));
    assertTagMatchesLocation(wptTag, "2", "-2", "1970-01-01T00:03:20.000Z", "20");
  }

  /**
   * Asserts that the given tag describes a location.
   *
   * @param tag the tag
   * @param latitude the location's latitude
   * @param longitude the location's longitude
   * @param time the location's time
   * @param elevation the location's elevation
   */
  private void assertTagMatchesLocation(
      Element tag, String latitude, String longitude, String time, String elevation) {
    assertEquals(latitude, tag.getAttribute("lat"));
    assertEquals(longitude, tag.getAttribute("lon"));
    assertEquals(time, getChildTextValue(tag, "time"));
    assertEquals(elevation, getChildTextValue(tag, "ele"));
  }
}
