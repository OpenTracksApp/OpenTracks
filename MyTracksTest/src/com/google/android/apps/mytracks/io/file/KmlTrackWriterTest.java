// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.file;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.file.KmlTrackWriter;
import com.google.android.apps.mytracks.util.StringUtils;

import android.location.Location;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Vector;

/**
 * Tests for the KML track exporter.
 *
 * @author Rodrigo Damazio
 */
public class KmlTrackWriterTest extends TrackFormatWriterTest {
  private static final String FULL_TRACK_DESCRIPTION = "full track description";

  /**
   * A fake version of {@link StringUtils} which returns a fixed track
   * description, thus not depending on the context.
   */
  private class FakeStringUtils extends StringUtils {
    public FakeStringUtils() {
      super(null);
    }

    @Override
    public String generateTrackDescription(Track trackToDescribe,
        Vector<Double> distances, Vector<Double> elevations) {
      assertSame(KmlTrackWriterTest.super.track, trackToDescribe);
      assertTrue(distances.isEmpty());
      assertTrue(elevations.isEmpty());
      return FULL_TRACK_DESCRIPTION;
    }
  }

  public void testXmlOutput() throws Exception {
    KmlTrackWriter writer = new KmlTrackWriter(new FakeStringUtils());
    String result = writeTrack(writer);
    Document doc = parseXmlDocument(result);

    Element kmlTag = getChildElement(doc, "kml");
    Element docTag = getChildElement(kmlTag, "Document");
    assertEquals(TRACK_NAME, getChildTextValue(docTag, "name"));
    assertEquals(TRACK_DESCRIPTION, getChildTextValue(docTag, "description"));

    // There are 5 placemarks - start, segments, end, waypoint 1, waypoint 2
    List<Element> placemarkTags = getChildElements(docTag, "Placemark", 5);
    assertTagIsPlacemark(placemarkTags.get(0),
        "(Start)", TRACK_DESCRIPTION, location1);
    assertTagIsPlacemark(placemarkTags.get(2),
        "(End)", FULL_TRACK_DESCRIPTION, location4);
    assertTagIsPlacemark(placemarkTags.get(3),
        WAYPOINT1_NAME, WAYPOINT1_DESCRIPTION, location2);
    assertTagIsPlacemark(placemarkTags.get(4),
        WAYPOINT2_NAME, WAYPOINT2_DESCRIPTION, location3);

    Element trackPlacemarkTag = placemarkTags.get(1);
    assertEquals(TRACK_NAME, getChildTextValue(trackPlacemarkTag, "name"));
    assertEquals(TRACK_DESCRIPTION,
        getChildTextValue(trackPlacemarkTag, "description"));
    Element geometryTag = getChildElement(trackPlacemarkTag, "MultiGeometry");
    List<Element> segmentTags = getChildElements(geometryTag, "LineString", 2);
    assertTagHasPoints(segmentTags.get(0), location1, location2);
    assertTagHasPoints(segmentTags.get(1), location3, location4);
  }

  /**
   * Asserts that the given XML tag is a placemark with the given properties.
   *
   * @param tag the tag to analyze
   * @param name the expected name for the placemark
   * @param description the expected description for the placemark
   * @param location the expected location of the placemark
   */
  private void assertTagIsPlacemark(Element tag, String name,
      String description, Location location) {
    assertEquals(name, getChildTextValue(tag, "name"));
    assertEquals(description, getChildTextValue(tag, "description"));
    Element pointTag = getChildElement(tag, "Point");
    String expectedCoords =
        location.getLongitude() + "," + location.getLatitude();
    String actualCoords = getChildTextValue(pointTag, "coordinates");
    assertEquals(expectedCoords, actualCoords);
  }

  /**
   * Asserts that the given tag has a "coordinates" subtag with the given
   * locations.
   *
   * @param tag the tag to analyze
   * @param locs the locations to expect in the coordinates
   */
  private void assertTagHasPoints(Element tag, Location... locs) {
    StringBuilder expectedBuilder = new StringBuilder();
    for (Location loc : locs) {
      expectedBuilder.append(loc.getLongitude());
      expectedBuilder.append(',');
      expectedBuilder.append(loc.getLatitude());
      expectedBuilder.append(',');
      expectedBuilder.append(loc.getAltitude());
      expectedBuilder.append(' ');
    }
    String expectedCoordinates = expectedBuilder.toString().trim();
    String actualCoordinates = getChildTextValue(tag, "coordinates").trim();
    assertEquals(expectedCoordinates, actualCoordinates);
  }
}
