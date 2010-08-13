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
package com.google.android.apps.mytracks.io;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackPointsColumns;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.Factory;
import com.google.android.apps.mytracks.testing.TestingProviderUtilsFactory;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.content.ContentUris;
import android.location.Location;
import android.net.Uri;
import android.test.AndroidTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

import javax.xml.parsers.ParserConfigurationException;

import org.easymock.Capture;
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
  private static final String TRACK_TIME_2 = "2010-04-22T18:21:50Z";

  // TODO use real files from different sources with more track points
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

  private static final long TRACK_ID = 1;
  private static final long TRACK_POINT_ID_1 = 1;
  private static final long TRACK_POINT_ID_2 = 1;

  private static final Uri TRACK_ID_URI = ContentUris.appendId(
      TracksColumns.CONTENT_URI.buildUpon(), TRACK_ID).build();
  private static final Uri TRACK_POINT_ID_URI_1 = ContentUris.appendId(
      TrackPointsColumns.CONTENT_URI.buildUpon(), TRACK_POINT_ID_1).build();
  private static final Uri TRACK_POINT_ID_URI_2 = ContentUris.appendId(
      TrackPointsColumns.CONTENT_URI.buildUpon(), TRACK_POINT_ID_2).build();

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
   * Test import success
   */
  public void testImportSuccess() throws Exception {
    Capture<Track> trackParam = new Capture<Track>();
    Capture<Location> locParam1 = new MyLocationCapture();
    Capture<Location> locParam2 = new MyLocationCapture();

    AndroidMock.expect(
        providerUtils.insertTrack(AndroidMock.capture(trackParam))).andReturn(
        TRACK_ID_URI);

    AndroidMock.expect(
        providerUtils.insertTrackPoint(AndroidMock.capture(locParam1),
            AndroidMock.anyLong())).andReturn(TRACK_POINT_ID_URI_1);

    AndroidMock.expect(
        providerUtils.insertTrackPoint(AndroidMock.capture(locParam2),
            AndroidMock.anyLong())).andReturn(TRACK_POINT_ID_URI_2);

    providerUtils.updateTrack(AndroidMock.capture(trackParam));

    AndroidMock.replay(providerUtils);

    InputStream is = new ByteArrayInputStream(VALID_TEST_GPX.getBytes());
    GpxImporter.importGPXFile(is, providerUtils);

    AndroidMock.verify();

    SimpleDateFormat format = GpxImporter.DATE_FORMAT2;

    // verify track parameter
    Track track = trackParam.getValue();
    assertEquals(TRACK_NAME, track.getName());
    assertEquals(TRACK_DESC, track.getDescription());
    assertEquals(format.parse(TRACK_TIME_1).getTime(), track.getStatistics()
        .getStartTime());
    assertNotSame(-1, track.getStartId());
    assertNotSame(-1, track.getStopId());

    // verify last location parameter
    Location loc1 = locParam1.getValue();
    assertEquals(Double.parseDouble(TRACK_LAT_1), loc1.getLatitude());
    assertEquals(Double.parseDouble(TRACK_LON_1), loc1.getLongitude());
    assertEquals(Double.parseDouble(TRACK_ELE_1), loc1.getAltitude());
    assertEquals(format.parse(TRACK_TIME_1).getTime(), loc1.getTime());

    Location loc2 = locParam2.getValue();
    assertEquals(Double.parseDouble(TRACK_LAT_2), loc2.getLatitude());
    assertEquals(Double.parseDouble(TRACK_LON_2), loc2.getLongitude());
    assertEquals(Double.parseDouble(TRACK_ELE_2), loc2.getAltitude());
    assertEquals(format.parse(TRACK_TIME_2).getTime(), loc2.getTime());
  }

  /**
   * Test with invalid location - track should be deleted
   */
  public void testImportLocationFailure() throws ParserConfigurationException,
      SAXException, IOException {
    testInvalidXML(INVALID_LOCATION_TEST_GPX);
  }

  /**
   * Test with invalid time - track should be deleted
   */
  public void testImportTimeFailure() throws ParserConfigurationException,
      SAXException, IOException {
    testInvalidXML(INVALID_TIME_TEST_GPX);
  }

  /**
   * Test with invalid xml - track should be deleted
   */
  public void testImportXMLFailure() throws ParserConfigurationException,
      SAXException, IOException {
    testInvalidXML(INVALID_XML_TEST_GPX);
  }

  private void testInvalidXML(String xml) throws ParserConfigurationException,
      IOException {
    AndroidMock.expect(
        providerUtils.insertTrack((Track) AndroidMock.anyObject())).andReturn(
        TRACK_ID_URI);

    AndroidMock.expect(
        providerUtils.insertTrackPoint((Location) AndroidMock.anyObject(),
            AndroidMock.anyLong())).andStubReturn(TRACK_POINT_ID_URI_1);

    providerUtils.deleteTrack(TRACK_ID);

    AndroidMock.replay(providerUtils);

    InputStream is = new ByteArrayInputStream(xml.getBytes());
    try {
      GpxImporter.importGPXFile(is, providerUtils);
    } catch (SAXException e) {
      // expected exception
    }

    AndroidMock.verify();
  }

  /**
   * Workaround because of capture bug 2617107 in easymock:
   * http://sourceforge.net
   * /tracker/?func=detail&aid=2617107&group_id=82958&atid=567837
   */
  @SuppressWarnings("serial")
  class MyLocationCapture extends Capture<Location> {
    @Override
    public void setValue(Location value) {
      if (!hasCaptured()) {
        super.setValue(value);
      }
    }
  }
}
