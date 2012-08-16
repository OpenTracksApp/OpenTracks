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

import com.google.android.apps.mytracks.content.DescriptionGenerator;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;

import android.annotation.TargetApi;
import android.location.Location;

import java.util.List;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests for {@link KmlTrackWriter}.
 *
 * @author Rodrigo Damazio
 */
public class KmlTrackWriterTest extends TrackFormatWriterTest {

  private static final String FULL_TRACK_DESCRIPTION = "full track description";

  /**
   * A fake version of {@link DescriptionGenerator} which returns a fixed track
   * description, thus not depending on the context.
   */
  private class FakeDescriptionGenerator implements DescriptionGenerator {
    @Override
    public String generateTrackDescription(
        Track aTrack, Vector<Double> distances, Vector<Double> elevations, boolean html) {
      return FULL_TRACK_DESCRIPTION;
    }

    @Override
    public String generateWaypointDescription(TripStatistics tripStatistics) {
      return null;
    }
  }

  public void testXmlOutput() throws Exception {
    KmlTrackWriter writer = new KmlTrackWriter(getContext(), new FakeDescriptionGenerator());
    String result = writeTrack(writer);
    Document doc = parseXmlDocument(result);

    Element kmlTag = getChildElement(doc, "kml");
    Element docTag = getChildElement(kmlTag, "Document");
    assertEquals(TRACK_NAME, getChildTextValue(docTag, "name"));
    assertEquals(TRACK_DESCRIPTION, getChildTextValue(docTag, "description"));

    // There are 3 placemarks - start, track, and end
    List<Element> placemarkTags = getChildElements(docTag, "Placemark", 3);
    assertTagIsPlacemark(
        placemarkTags.get(0), TRACK_NAME + " (Start)", TRACK_DESCRIPTION, location1);
    assertTagIsPlacemark(
        placemarkTags.get(2), TRACK_NAME + " (End)", FULL_TRACK_DESCRIPTION, location4);

    List<Element> folderTag = getChildElements(docTag, "Folder", 1);
    List<Element> folderPlacemarkTags = getChildElements(folderTag.get(0), "Placemark", 2);
    assertTagIsPlacemark(
        folderPlacemarkTags.get(0), WAYPOINT1_NAME, WAYPOINT1_DESCRIPTION, location2);
    assertTagIsPlacemark(
        folderPlacemarkTags.get(1), WAYPOINT2_NAME, WAYPOINT2_DESCRIPTION, location3);

    Element trackPlacemarkTag = placemarkTags.get(1);
    assertEquals(TRACK_NAME, getChildTextValue(trackPlacemarkTag, "name"));
    assertEquals(TRACK_DESCRIPTION, getChildTextValue(trackPlacemarkTag, "description"));
    Element multiTrackTag = getChildElement(trackPlacemarkTag, "gx:MultiTrack");
    List<Element> trackTags = getChildElements(multiTrackTag, "gx:Track", 2);
    assertTagHasPoints(trackTags.get(0), location1, location2);
    assertTagHasPoints(trackTags.get(1), location3, location4);
  }

  /**
   * Asserts that the given tag is a placemark with the given properties.
   *
   * @param tag the tag
   * @param name the expected placemark name
   * @param description the expected placemark description
   * @param location the expected placemark location
   */
  private void assertTagIsPlacemark(
      Element tag, String name, String description, Location location) {
    assertEquals(name, getChildTextValue(tag, "name"));
    assertEquals(description, getChildTextValue(tag, "description"));
    Element pointTag = getChildElement(tag, "Point");
    String expected = location.getLongitude() + "," + location.getLatitude() + ","
        + location.getAltitude();
    String actual = getChildTextValue(pointTag, "coordinates");
    assertEquals(expected, actual);
  }

  /**
   * Asserts that the given tag has a list of "gx:coord" subtags matching the
   * expected locations.
   *
   * @param tag the parent tag
   * @param locations list of expected locations
   */
  @TargetApi(8)
  private void assertTagHasPoints(Element tag, Location... locations) {
    List<Element> coordTags = getChildElements(tag, "gx:coord", locations.length);
    for (int i = 0; i < locations.length; i++) {
      Location location = locations[i];
      String expected = location.getLongitude() + " " + location.getLatitude() + " "
          + location.getAltitude();
      String actual = coordTags.get(i).getFirstChild().getTextContent();
      assertEquals(expected, actual);
    }
  }
}
