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

import static com.google.android.testing.mocking.AndroidMock.eq;
import static com.google.android.testing.mocking.AndroidMock.expect;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.Factory;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.testing.TestingProviderUtilsFactory;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.content.ContentUris;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.test.AndroidTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.SimpleTimeZone;

import javax.xml.parsers.ParserConfigurationException;

import org.easymock.Capture;
import org.easymock.IArgumentMatcher;
import org.xml.sax.SAXException;

/**
 * Tests for the GPX importer.
 * 
 * @author Steffen Horlacher
 */
public class GpxImporterTest extends AndroidTestCase {

  private static final String TRACK_NAME_0 = "blablub";
  private static final String TRACK_DESCRIPTION_0 = "s'Laebe isch koi Schlotzer";

  private static final String TRACK_NAME_1 = "another track";
  private static final String TRACK_DESCRIPTION_1 = "another description";

  private static final double TRACK_LATITUDE = 48.768364;
  private static final double TRACK_LONGITUDE = 9.177886;
  private static final double TRACK_ELEVATION = 324.0;

  private static final String TRACK_TIME_0 = "2010-04-22T18:21:00Z";
  private static final String TRACK_TIME_1 = "2010-04-22T18:21:50.123";
  private static final String TRACK_TIME_2 = "2010-04-22T18:23:00.123";
  private static final String TRACK_TIME_3 = "2010-04-22T18:24:50.123";

  private static final SimpleDateFormat DATE_FORMAT_0 = new SimpleDateFormat(
      "yyyy-MM-dd'T'hh:mm:ss'Z'");
  private static final SimpleDateFormat DATE_FORMAT_1 = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSS");

  static {
    // We can't omit the timezones in the test, otherwise it'll use the local
    // timezone and fail depending on where the test runner is.
    SimpleTimeZone utc = new SimpleTimeZone(0, "UTC");
    DATE_FORMAT_0.setTimeZone(utc);
    DATE_FORMAT_1.setTimeZone(utc);
  }

  private static String getNameAndDescription(String name, String description) {
    return "<name><![CDATA[" + name + "]]></name>" + "<desc><![CDATA[" + description + "]]></desc>";
  }

  private static String getTrackPoint(int index, String time) {
    String latitude = Double.toString(TRACK_LATITUDE + index);
    String longitude = Double.toString(TRACK_LONGITUDE + index);
    String elevation = Double.toString(TRACK_ELEVATION + index);
    StringBuffer buffer = new StringBuffer();
    buffer.append(
        "<trkpt lat=\"" + latitude + "\" lon=\"" + longitude + "\"><ele>" + elevation + "</ele>");
    if (time != null) {
      buffer.append("<time>" + time + "</time>");
    }
    buffer.append("</trkpt>");
    return buffer.toString();
  }

  private static final String VALID_ONE_TRACK_ONE_SEGMENT_GPX = "<gpx><trk>"
      + getNameAndDescription(TRACK_NAME_0, TRACK_DESCRIPTION_0) + "<trkseg>"
      + getTrackPoint(0, TRACK_TIME_0) + getTrackPoint(1, TRACK_TIME_1) + "</trkseg></trk></gpx>";
  private static final String VALID_ONE_TRACK_TWO_SEGMENTS_GPX = "<gpx><trk>"
      + getNameAndDescription(TRACK_NAME_0, TRACK_DESCRIPTION_0) + "<trkseg>"
      + getTrackPoint(0, TRACK_TIME_0) + getTrackPoint(1, TRACK_TIME_1) + "</trkseg><trkseg>"
      + getTrackPoint(2, TRACK_TIME_2) + getTrackPoint(3, TRACK_TIME_3) + "</trkseg></trk></gpx>";
  private static final String VALID_ONE_TRACK_TWO_SEGMENTS_NO_TIME_GPX = "<gpx><trk>"
      + getNameAndDescription(TRACK_NAME_0, TRACK_DESCRIPTION_0) + "<trkseg>"
      + getTrackPoint(0, null) + getTrackPoint(1, null) + "</trkseg><trkseg>"
      + getTrackPoint(2, null) + getTrackPoint(3, null) + "</trkseg></trk></gpx>";
  private static final String VALID_TWO_TRACKS_GPX = "<gpx><trk>"
      + getNameAndDescription(TRACK_NAME_0, TRACK_DESCRIPTION_0) + "<trkseg>"
      + getTrackPoint(0, TRACK_TIME_0) + "</trkseg></trk><trk>"
      + getNameAndDescription(TRACK_NAME_1, TRACK_DESCRIPTION_1) + "<trkseg>"
      + getTrackPoint(1, TRACK_TIME_1) + "</trkseg></trk></gpx>";

  private static final String INVALID_XML_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.substring(
      0, VALID_ONE_TRACK_ONE_SEGMENT_GPX.length() - 50);
  private static final String INVALID_LOCATION_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.replaceAll(
      Double.toString(TRACK_LATITUDE), "1000.0");
  private static final String INVALID_TIME_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.replaceAll(
      TRACK_TIME_0, "invalid");
  private static final String INVALID_ALTITUDE_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.replaceAll(
      Double.toString(TRACK_ELEVATION), "invalid");
  private static final String INVALID_LATITUDE_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.replaceAll(
      Double.toString(TRACK_LATITUDE), "invalid");
  private static final String INVALID_LONGITUDE_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.replaceAll(
      Double.toString(TRACK_LONGITUDE), "invalid");

  private static final long TRACK_ID_0 = 1;
  private static final long TRACK_ID_1 = 2;
  private static final long TRACK_POINT_ID_0 = 1;
  private static final long TRACK_POINT_ID_1 = 2;
  private static final long TRACK_POINT_ID_3 = 4;

  private static final Uri TRACK_ID_0_URI = ContentUris.appendId(
      TracksColumns.CONTENT_URI.buildUpon(), TRACK_ID_0).build();
  private static final Uri TRACK_ID_1_URI = ContentUris.appendId(
      TracksColumns.CONTENT_URI.buildUpon(), TRACK_ID_1).build();

  private MyTracksProviderUtils myTracksProviderUtils;

  private Factory oldMyTracksProviderUtilsFactory;

  @UsesMocks(MyTracksProviderUtils.class)
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myTracksProviderUtils = AndroidMock.createMock(MyTracksProviderUtils.class);
    oldMyTracksProviderUtilsFactory = TestingProviderUtilsFactory.installWithInstance(
        myTracksProviderUtils);
  }

  @Override
  protected void tearDown() throws Exception {
    TestingProviderUtilsFactory.restoreOldFactory(oldMyTracksProviderUtilsFactory);
    super.tearDown();
  }

  /**
   * Tests one track with one segment.
   */
  public void testOneTrackOneSegment() throws Exception {
    Capture<Track> track = new Capture<Track>();

    Location location0 = createLocation(0, DATE_FORMAT_0.parse(TRACK_TIME_0).getTime());
    Location location1 = createLocation(1, DATE_FORMAT_1.parse(TRACK_TIME_1).getTime());

    expect(myTracksProviderUtils.insertTrack((Track) AndroidMock.anyObject()))
        .andReturn(TRACK_ID_0_URI);

    // A flush happens before getting the start point ID
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        LocationsMatcher.eqLoc(location0), eq(1), eq(TRACK_ID_0))).andReturn(1);
    expect(myTracksProviderUtils.getFirstTrackPointId(TRACK_ID_0)).andReturn(TRACK_POINT_ID_0);

    // A flush happens at the end
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        LocationsMatcher.eqLoc(location1), eq(1), eq(TRACK_ID_0))).andReturn(1);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID_0)).andReturn(TRACK_POINT_ID_1);

    myTracksProviderUtils.updateTrack(AndroidMock.capture(track));

    AndroidMock.replay(myTracksProviderUtils);

    InputStream inputStream = new ByteArrayInputStream(VALID_ONE_TRACK_ONE_SEGMENT_GPX.getBytes());
    long[] trackIds = GpxImporter.importGPXFile(
        inputStream, myTracksProviderUtils, PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT);
    assertEquals(1, trackIds.length);
    assertEquals(TRACK_ID_0, trackIds[0]);

    long time0 = DATE_FORMAT_0.parse(TRACK_TIME_0).getTime();
    long time1 = DATE_FORMAT_1.parse(TRACK_TIME_1).getTime();
    assertEquals(time1 - time0, track.getValue().getTripStatistics().getTotalTime());
    AndroidMock.verify(myTracksProviderUtils);
    verifyTrack(track.getValue(), TRACK_NAME_0, TRACK_DESCRIPTION_0, time0);
  }

  /**
   * Tests one track with two segments.
   */
  public void testOneTrackTwoSegments() throws Exception {
    Capture<Track> track = new Capture<Track>();

    Location location0 = createLocation(0, DATE_FORMAT_0.parse(TRACK_TIME_0).getTime());

    expect(myTracksProviderUtils.insertTrack((Track) AndroidMock.anyObject()))
        .andReturn(TRACK_ID_0_URI);
    // A flush happens before getting the start point ID
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        LocationsMatcher.eqLoc(location0), eq(1), eq(TRACK_ID_0))).andReturn(1);
    expect(myTracksProviderUtils.getFirstTrackPointId(TRACK_ID_0)).andReturn(TRACK_POINT_ID_0);

    // A flush happens at the end
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        (Location[]) AndroidMock.anyObject(), eq(5), eq(TRACK_ID_0))).andStubReturn(5);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID_0)).andReturn(TRACK_POINT_ID_3);

    myTracksProviderUtils.updateTrack(AndroidMock.capture(track));

    AndroidMock.replay(myTracksProviderUtils);

    InputStream inputStream = new ByteArrayInputStream(VALID_ONE_TRACK_TWO_SEGMENTS_GPX.getBytes());
    long[] trackIds = GpxImporter.importGPXFile(
        inputStream, myTracksProviderUtils, PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT);
    assertEquals(1, trackIds.length);
    assertEquals(TRACK_ID_0, trackIds[0]);

    long time0 = DATE_FORMAT_0.parse(TRACK_TIME_0).getTime();
    long time1 = DATE_FORMAT_1.parse(TRACK_TIME_1).getTime();
    long time2 = DATE_FORMAT_1.parse(TRACK_TIME_2).getTime();
    long time3 = DATE_FORMAT_1.parse(TRACK_TIME_3).getTime();
    assertEquals(
        time1 - time0 + time3 - time2, track.getValue().getTripStatistics().getTotalTime());

    AndroidMock.verify(myTracksProviderUtils);
    verifyTrack(track.getValue(), TRACK_NAME_0, TRACK_DESCRIPTION_0,
        DATE_FORMAT_0.parse(TRACK_TIME_0).getTime());
  }

  /**
   * Tests one track with two segments, but no time in the track points.
   */
  public void testOneTrackTwoSegmentsNoTime() throws Exception {
    Capture<Track> capturedTrack = new Capture<Track>();

    expect(myTracksProviderUtils.insertTrack((Track) AndroidMock.anyObject()))
        .andReturn(TRACK_ID_0_URI);
    // A flush happens before getting the start point ID
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
      (Location[]) AndroidMock.anyObject(), eq(1), eq(TRACK_ID_0))).andReturn(1);
    expect(myTracksProviderUtils.getFirstTrackPointId(TRACK_ID_0)).andReturn(TRACK_POINT_ID_0);

    // A flush happens at the end
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        (Location[]) AndroidMock.anyObject(), eq(5), eq(TRACK_ID_0))).andStubReturn(5);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID_0)).andReturn(TRACK_POINT_ID_3);

    myTracksProviderUtils.updateTrack(AndroidMock.capture(capturedTrack));

    AndroidMock.replay(myTracksProviderUtils);

    InputStream inputStream = new ByteArrayInputStream(
        VALID_ONE_TRACK_TWO_SEGMENTS_NO_TIME_GPX.getBytes());
    long[] trackIds = GpxImporter.importGPXFile(
        inputStream, myTracksProviderUtils, PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT);
    assertEquals(1, trackIds.length);
    assertEquals(TRACK_ID_0, trackIds[0]);

    assertEquals(0, capturedTrack.getValue().getTripStatistics().getTotalTime());

    AndroidMock.verify(myTracksProviderUtils);
    verifyTrack(capturedTrack.getValue(), TRACK_NAME_0, TRACK_DESCRIPTION_0, -1L);
  }

  /**
   * Tests two tracks.
   */
  public void testTwoTracks() throws Exception {
    Capture<Track> track0 = new Capture<Track>();
    Capture<Track> track1 = new Capture<Track>();

    Location location0 = createLocation(0, DATE_FORMAT_0.parse(TRACK_TIME_0).getTime());
    Location location1 = createLocation(1, DATE_FORMAT_1.parse(TRACK_TIME_1).getTime());

    expect(myTracksProviderUtils.insertTrack((Track) AndroidMock.anyObject()))
        .andReturn(TRACK_ID_0_URI);
    // A flush happens before getting the start point ID
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        LocationsMatcher.eqLoc(location0), eq(1), eq(TRACK_ID_0))).andReturn(1);
    expect(myTracksProviderUtils.getFirstTrackPointId(TRACK_ID_0)).andReturn(TRACK_POINT_ID_0);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID_0)).andReturn(TRACK_POINT_ID_0);
    myTracksProviderUtils.updateTrack(AndroidMock.capture(track0));

    expect(myTracksProviderUtils.insertTrack((Track) AndroidMock.anyObject()))
        .andReturn(TRACK_ID_1_URI);
    // A flush happens before getting the start point ID
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        LocationsMatcher.eqLoc(location1), eq(1), eq(TRACK_ID_1))).andReturn(1);
    expect(myTracksProviderUtils.getFirstTrackPointId(TRACK_ID_1)).andReturn(TRACK_POINT_ID_1);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID_1)).andReturn(TRACK_POINT_ID_1);
    myTracksProviderUtils.updateTrack(AndroidMock.capture(track1));

    AndroidMock.replay(myTracksProviderUtils);

    InputStream inputStream = new ByteArrayInputStream(VALID_TWO_TRACKS_GPX.getBytes());
    long[] trackIds = GpxImporter.importGPXFile(
        inputStream, myTracksProviderUtils, PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT);
    assertEquals(2, trackIds.length);
    assertEquals(TRACK_ID_0, trackIds[0]);
    assertEquals(TRACK_ID_1, trackIds[1]);

    AndroidMock.verify(myTracksProviderUtils);
  }

  private Location createLocation(int index, long time) {
    Location location = new Location(LocationManager.GPS_PROVIDER);
    location.setLatitude(TRACK_LATITUDE + index);
    location.setLongitude(TRACK_LONGITUDE + index);
    location.setAltitude(TRACK_ELEVATION + index);
    location.setTime(time);
    return location;
  }

  private void verifyTrack(Track track, String name, String description, long time) {
    assertEquals(name, track.getName());
    assertEquals(description, track.getDescription());
    if (time != -1L) {
      assertEquals(time, track.getTripStatistics().getStartTime());
    }
    assertNotSame(-1, track.getStartId());
    assertNotSame(-1, track.getStopId());
  }

  /**
   * Test an invalid xml input.
   */
  public void testInvalidXml() throws ParserConfigurationException, IOException {
    testInvalidGpx(INVALID_XML_GPX);
  }

  /**
   * Test an invalid location.
   */
  public void testInvalidLocation() throws ParserConfigurationException, IOException {
    testInvalidGpx(INVALID_LOCATION_GPX);
  }

  /**
   * Test an invalid time.
   */
  public void testInvalidTime() throws ParserConfigurationException, IOException {
    testInvalidGpx(INVALID_TIME_GPX);
  }

  /**
   * Test an invalid altitude.
   */
  public void testInvalidAltitude() throws ParserConfigurationException, IOException {
    testInvalidGpx(INVALID_ALTITUDE_GPX);
  }

  /**
   * Test an invalid latitude.
   */
  public void testInvalidLatitude() throws ParserConfigurationException, IOException {
    testInvalidGpx(INVALID_LATITUDE_GPX);
  }

  /**
   * Test an invalid longitude.
   */
  public void testInvalidLongitude() throws ParserConfigurationException, IOException {
    testInvalidGpx(INVALID_LONGITUDE_GPX);
  }

  private void testInvalidGpx(String xml) throws ParserConfigurationException, IOException {
    expect(myTracksProviderUtils.insertTrack((Track) AndroidMock.anyObject()))
        .andReturn(TRACK_ID_0_URI);
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        (Location[]) AndroidMock.anyObject(), AndroidMock.anyInt(), AndroidMock.anyLong()))
        .andStubReturn(1);
    expect(myTracksProviderUtils.getFirstTrackPointId(TRACK_ID_0)).andStubReturn(TRACK_POINT_ID_0);
    myTracksProviderUtils.deleteTrack(TRACK_ID_0);
    AndroidMock.replay(myTracksProviderUtils);

    InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
    try {
      GpxImporter.importGPXFile(
          inputStream, myTracksProviderUtils, PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT);
    } catch (SAXException e) {
      // expected
    }
    AndroidMock.verify(myTracksProviderUtils);
  }

  /**
   * Workaround because of capture bug 2617107 in easymock:
   * http://sourceforge.net
   * /tracker/?func=detail&aid=2617107&group_id=82958&atid=567837
   */
  private static class LocationsMatcher implements IArgumentMatcher {
    private final Location[] matchLocs;

    private LocationsMatcher(Location[] expected) {
      this.matchLocs = expected;
    }

    public static Location[] eqLoc(Location[] expected) {
      IArgumentMatcher matcher = new LocationsMatcher(expected);
      AndroidMock.reportMatcher(matcher);
      return null;
    }

    public static Location[] eqLoc(Location expected) {
      return eqLoc(new Location[] { expected });
    }

    @Override
    public void appendTo(StringBuffer buf) {
      buf.append("eqLoc(").append(Arrays.toString(matchLocs)).append(")");
    }

    @Override
    public boolean matches(Object obj) {
      if (!(obj instanceof Location[])) {
        return false;
      }
      Location[] locs = (Location[]) obj;
      if (locs.length < matchLocs.length) {
        return false;
      }

      // Only check the first elements (those that will be taken into account)
      for (int i = 0; i < matchLocs.length; i++) {
        if (!locationsMatch(locs[i], matchLocs[i])) {
          return false;
        }
      }

      return true;
    }

    private boolean locationsMatch(Location loc1, Location loc2) {
      return (loc1.getTime() == loc2.getTime()) && (loc1.getLatitude() == loc2.getLatitude())
          && (loc1.getLongitude() == loc2.getLongitude())
          && (loc1.getAltitude() == loc2.getAltitude());
    }
  }
}
