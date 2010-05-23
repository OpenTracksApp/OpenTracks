package com.google.android.apps.mytracks.io;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

import javax.xml.parsers.ParserConfigurationException;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.xml.sax.SAXException;

import android.content.ContentUris;
import android.location.Location;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackPointsColumns;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.Factory;
import com.google.android.apps.mytracks.testing.TestingProviderUtilsFactory;

/**
 * Tests for the GPX importer.
 * 
 * @author Steffen (steffen.horlacher@gmail.com)
 */
public class GpxImporterTest extends AndroidTestCase {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
      "yyyy-MM-dd'T'hh:mm:ss'Z'");

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
  private static final String INVALID_TEST_GPX = VALID_TEST_GPX.substring(0,
      VALID_TEST_GPX.length() - 50);

  private static final long TRACK_ID = 1;
  private static final long TRACK_POINT_ID = 1;

  private static final Uri TRACK_ID_URI = ContentUris.appendId(
      TracksColumns.CONTENT_URI.buildUpon(), TRACK_ID).build();
  private static final Uri TRACK_POINT_ID_URI = ContentUris.appendId(
      TrackPointsColumns.CONTENT_URI.buildUpon(), TRACK_POINT_ID).build();

  private MyTracksProviderUtils providerUtils;

  private Factory oldProviderUtilsFactory;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    providerUtils = EasyMock.createMock(MyTracksProviderUtils.class);
    oldProviderUtilsFactory = TestingProviderUtilsFactory
        .installWithInstance(providerUtils);
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
    Capture<Location> locParam = new Capture<Location>();
    Capture<Long> idParam = new Capture<Long>();

    expect(providerUtils.insertTrack(capture(trackParam)))
      .andReturn(TRACK_ID_URI);

    expect(providerUtils.insertTrackPoint(capture(locParam), capture(idParam)))
      .andReturn(TRACK_POINT_ID_URI);
    expectLastCall().times(2);

    providerUtils.updateTrack(capture(trackParam));

    replay(providerUtils);

    InputStream is = new ByteArrayInputStream(VALID_TEST_GPX.getBytes());
    GpxSaxImporter.importGPXFile(is, providerUtils);

    verify();

    // verify track parameter
    Track track = trackParam.getValue();
    assertEquals(TRACK_NAME, track.getName());
    assertEquals(TRACK_DESC, track.getDescription());
    assertEquals(DATE_FORMAT.parse(TRACK_TIME_1).getTime(), track.getStartTime());
    assertNotSame(-1, track.getStartId());
    assertNotSame(-1, track.getStopId());

    // verify last location parameter
    Location loc = locParam.getValue();
    assertEquals(Double.parseDouble(TRACK_LAT_2), loc.getLatitude());
    assertEquals(Double.parseDouble(TRACK_LON_2), loc.getLongitude());
    assertEquals(Double.parseDouble(TRACK_ELE_2), loc.getAltitude());
    assertEquals(DATE_FORMAT.parse(TRACK_TIME_2).getTime(), loc.getTime());

  }

  /**
   * Test if created track will be deleted on parsing errors
   */
  public void testImportFailure() throws ParserConfigurationException,
      SAXException, IOException {

    expect(
        providerUtils.insertTrack((Track) EasyMock.anyObject())).andReturn(TRACK_ID_URI);
    expect(
        providerUtils.insertTrackPoint((Location) EasyMock.anyObject(),
            EasyMock.anyLong())).andReturn(TRACK_POINT_ID_URI);

    expectLastCall().anyTimes();
    providerUtils.deleteTrack(TRACK_ID);

    replay(providerUtils);

    InputStream is = new ByteArrayInputStream(INVALID_TEST_GPX.getBytes());

    try {
      GpxSaxImporter.importGPXFile(is, providerUtils);
    } catch (SAXException e) {
      // expected exception
    }

    verify();

  }

}
