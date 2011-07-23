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
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.Factory;
import com.google.android.apps.mytracks.io.file.GpxImporter;
import com.google.android.apps.mytracks.testing.TestingProviderUtilsFactory;
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

  private static final String TRACK_NAME = "blablub";
  private static final String TRACK_DESC = "s'Laebe isch koi Schlotzer";

  private static final String TRACK_LAT_1 = "48.768364";
  private static final String TRACK_LON_1 = "9.177886";
  private static final String TRACK_ELE_1 = "324.0";
  private static final String TRACK_TIME_1 = "2010-04-22T18:21:00Z";

  private static final String TRACK_LAT_2 = "48.768374";
  private static final String TRACK_LON_2 = "9.177816";
  private static final String TRACK_ELE_2 = "333.0";
  private static final String TRACK_TIME_2 = "2010-04-22T18:21:50.123";

  private static final SimpleDateFormat DATE_FORMAT1 =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
  private static final SimpleDateFormat DATE_FORMAT2 =
      new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
  static {
    // We can't omit the timezones in the test, otherwise it'll use the local
    // timezone and fail depending on where the test runner is.
    SimpleTimeZone utc = new SimpleTimeZone(0, "UTC");
    DATE_FORMAT1.setTimeZone(utc);
    DATE_FORMAT2.setTimeZone(utc);
  }

  // TODO: use real files from different sources with more track points.
  private static final String VALID_TEST_GPX = "<gpx><trk><name><![CDATA["
      + TRACK_NAME + "]]></name><desc><![CDATA[" + TRACK_DESC
      + "]]></desc><trkseg>" + "<trkpt lat=\"" + TRACK_LAT_1 + "\" lon=\""
      + TRACK_LON_1 + "\"><ele>" + TRACK_ELE_1 + "</ele><time>" + TRACK_TIME_1
      + "</time></trkpt> +" + "<trkpt lat=\"" + TRACK_LAT_2 + "\" lon=\""
      + TRACK_LON_2 + "\"><ele>" + TRACK_ELE_2 + "</ele><time>" + TRACK_TIME_2
      + "</time></trkpt>" + "</trkseg></trk></gpx>";

  // invalid xml
  private static final String INVALID_XML_TEST_GPX = VALID_TEST_GPX.substring(
      0, VALID_TEST_GPX.length() - 50);
  private static final String INVALID_LOCATION_TEST_GPX = VALID_TEST_GPX
      .replaceAll(TRACK_LAT_1, "1000.0");
  private static final String INVALID_TIME_TEST_GPX = VALID_TEST_GPX
      .replaceAll(TRACK_TIME_1, "invalid");
  private static final String INVALID_ALTITUDE_TEST_GPX = VALID_TEST_GPX
      .replaceAll(TRACK_ELE_1, "invalid");
  private static final String INVALID_LATITUDE_TEST_GPX = VALID_TEST_GPX
      .replaceAll(TRACK_LAT_1, "invalid");
  private static final String INVALID_LONGITUDE_TEST_GPX = VALID_TEST_GPX
      .replaceAll(TRACK_LON_1, "invalid");

  private static final long TRACK_ID = 1;
  private static final long TRACK_POINT_ID_1 = 1;
  private static final long TRACK_POINT_ID_2 = 2;

  private static final Uri TRACK_ID_URI = ContentUris.appendId(
      TracksColumns.CONTENT_URI.buildUpon(), TRACK_ID).build();

  private MyTracksProviderUtils providerUtils;

  private Factory oldProviderUtilsFactory;

  @UsesMocks(MyTracksProviderUtils.class)
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    providerUtils = AndroidMock.createMock(MyTracksProviderUtils.class);
    oldProviderUtilsFactory =
        TestingProviderUtilsFactory.installWithInstance(providerUtils);
  }

  @Override
  protected void tearDown() throws Exception {
    TestingProviderUtilsFactory.restoreOldFactory(oldProviderUtilsFactory);
    super.tearDown();
  }

  /**
   * Test import success.
   */
  public void testImportSuccess() throws Exception {
    Capture<Track> trackParam = new Capture<Track>();

    Location loc1 = new Location(LocationManager.GPS_PROVIDER);
    loc1.setTime(DATE_FORMAT2.parse(TRACK_TIME_1).getTime());
    loc1.setLatitude(Double.parseDouble(TRACK_LAT_1));
    loc1.setLongitude(Double.parseDouble(TRACK_LON_1));
    loc1.setAltitude(Double.parseDouble(TRACK_ELE_1));

    Location loc2 = new Location(LocationManager.GPS_PROVIDER);
    loc2.setTime(DATE_FORMAT1.parse(TRACK_TIME_2).getTime());
    loc2.setLatitude(Double.parseDouble(TRACK_LAT_2));
    loc2.setLongitude(Double.parseDouble(TRACK_LON_2));
    loc2.setAltitude(Double.parseDouble(TRACK_ELE_2));

    expect(providerUtils.insertTrack(AndroidMock.capture(trackParam)))
        .andReturn(TRACK_ID_URI);

    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(TRACK_POINT_ID_1).andReturn(TRACK_POINT_ID_2);

    // A flush happens after the first insertion to get the starting point ID,
    // which is why we get two calls
    expect(providerUtils.bulkInsertTrackPoints(LocationsMatcher.eqLoc(loc1),
        eq(1), eq(TRACK_ID))).andReturn(1);
    expect(providerUtils.bulkInsertTrackPoints(LocationsMatcher.eqLoc(loc2),
        eq(1), eq(TRACK_ID))).andReturn(1);

    providerUtils.updateTrack(AndroidMock.capture(trackParam));

    AndroidMock.replay(providerUtils);

    InputStream is = new ByteArrayInputStream(VALID_TEST_GPX.getBytes());
    GpxImporter.importGPXFile(is, providerUtils);

    AndroidMock.verify(providerUtils);

    // verify track parameter
    Track track = trackParam.getValue();
    assertEquals(TRACK_NAME, track.getName());
    assertEquals(TRACK_DESC, track.getDescription());
    assertEquals(DATE_FORMAT2.parse(TRACK_TIME_1).getTime(), track.getStatistics()
        .getStartTime());
    assertNotSame(-1, track.getStartId());
    assertNotSame(-1, track.getStopId());
  }

  /**
   * Test with invalid location - track should be deleted.
   */
  public void testImportLocationFailure() throws ParserConfigurationException, IOException {
    testInvalidXML(INVALID_LOCATION_TEST_GPX);
  }

  /**
   * Test with invalid time - track should be deleted.
   */
  public void testImportTimeFailure() throws ParserConfigurationException, IOException {
    testInvalidXML(INVALID_TIME_TEST_GPX);
  }

  /**
   * Test with invalid xml - track should be deleted.
   */
  public void testImportXMLFailure() throws ParserConfigurationException, IOException {
    testInvalidXML(INVALID_XML_TEST_GPX);
  }

  /**
   * Test with invalid altitude - track should be deleted.
   */
  public void testImportInvalidAltitude() throws ParserConfigurationException, IOException {
    testInvalidXML(INVALID_ALTITUDE_TEST_GPX);
  }

  /**
   * Test with invalid latitude - track should be deleted.
   */
  public void testImportInvalidLatitude() throws ParserConfigurationException, IOException {
    testInvalidXML(INVALID_LATITUDE_TEST_GPX);
  }

  /**
   * Test with invalid longitude - track should be deleted.
   */
  public void testImportInvalidLongitude() throws ParserConfigurationException, IOException {
    testInvalidXML(INVALID_LONGITUDE_TEST_GPX);
  }

  private void testInvalidXML(String xml) throws ParserConfigurationException,
      IOException {
    expect(providerUtils.insertTrack((Track) AndroidMock.anyObject()))
        .andReturn(TRACK_ID_URI);

    expect(providerUtils.bulkInsertTrackPoints((Location[]) AndroidMock.anyObject(),
        AndroidMock.anyInt(), AndroidMock.anyLong())).andStubReturn(1);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andStubReturn(TRACK_POINT_ID_1);

    providerUtils.deleteTrack(TRACK_ID);

    AndroidMock.replay(providerUtils);

    InputStream is = new ByteArrayInputStream(xml.getBytes());
    try {
      GpxImporter.importGPXFile(is, providerUtils);
    } catch (SAXException e) {
      // expected exception
    }

    AndroidMock.verify(providerUtils);
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
      return eqLoc(new Location[] { expected});
    }

    @Override
    public void appendTo(StringBuffer buf) {
      buf.append("eqLoc(").append(Arrays.toString(matchLocs)).append(")");
    }

    @Override
    public boolean matches(Object obj) {
      if (! (obj instanceof Location[])) { return false; }
      Location[] locs = (Location[]) obj;
      if (locs.length < matchLocs.length) { return false; }

      // Only check the first elements (those that will be taken into account)
      for (int i = 0; i < matchLocs.length; i++) {
        if (!locationsMatch(locs[i], matchLocs[i])) {
          return false;
        }
      }

      return true;
    }

    private boolean locationsMatch(Location loc1, Location loc2) {
      return (loc1.getTime() == loc2.getTime()) &&
             (loc1.getLatitude() == loc2.getLatitude()) &&
             (loc1.getLongitude() == loc2.getLongitude()) &&
             (loc1.getAltitude() == loc2.getAltitude());
    }
  }
}
