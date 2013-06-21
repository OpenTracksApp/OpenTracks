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

import com.google.android.apps.mytracks.content.Track;
import com.google.android.testing.mocking.AndroidMock;

import android.location.Location;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.easymock.Capture;
import org.xml.sax.SAXException;

/**
 * Tests for {@link GpxImporter}.
 * 
 * @author Steffen Horlacher
 */
public class GpxImporterTest extends AbstractTestImporter {

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

  /**
   * Tests one track with one segment.
   */
  public void testOneTrackOneSegment() throws Exception {
    Capture<Track> track = new Capture<Track>();

    Location location0 = createLocation(0, DATE_FORMAT_0.parse(TRACK_TIME_0).getTime());
    Location location1 = createLocation(1, DATE_FORMAT_1.parse(TRACK_TIME_1).getTime());

    expect(myTracksProviderUtils.insertTrack((Track) AndroidMock.anyObject()))
        .andReturn(TRACK_ID_0_URI);
    expectFirstTrackPoint(location0, TRACK_ID_0, TRACK_POINT_ID_0);

    // A flush happens at the end
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        LocationsMatcher.eqLoc(location1), eq(1), eq(TRACK_ID_0))).andReturn(1);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID_0)).andReturn(TRACK_POINT_ID_1);

    expectUpdateTrack(track, true, TRACK_ID_0);
    AndroidMock.replay(myTracksProviderUtils);

    InputStream inputStream = new ByteArrayInputStream(VALID_ONE_TRACK_ONE_SEGMENT_GPX.getBytes());
    GpxImporter gpxImporter = new GpxImporter(getContext(), myTracksProviderUtils);
    long[] trackIds = gpxImporter.importFile(inputStream);
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
    expectFirstTrackPoint(location0, TRACK_ID_0, TRACK_POINT_ID_0);

    // A flush happens at the end
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        (Location[]) AndroidMock.anyObject(), eq(5), eq(TRACK_ID_0))).andStubReturn(5);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID_0)).andReturn(TRACK_POINT_ID_3);

    expectUpdateTrack(track, true, TRACK_ID_0);
    AndroidMock.replay(myTracksProviderUtils);

    InputStream inputStream = new ByteArrayInputStream(VALID_ONE_TRACK_TWO_SEGMENTS_GPX.getBytes());
    GpxImporter gpxImporter = new GpxImporter(getContext(), myTracksProviderUtils);
    long[] trackIds = gpxImporter.importFile(inputStream);
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
    Capture<Track> track = new Capture<Track>();

    expect(myTracksProviderUtils.insertTrack((Track) AndroidMock.anyObject()))
        .andReturn(TRACK_ID_0_URI);
    expectFirstTrackPoint(null, TRACK_ID_0, TRACK_POINT_ID_0);

    // A flush happens at the end
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        (Location[]) AndroidMock.anyObject(), eq(5), eq(TRACK_ID_0))).andStubReturn(5);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID_0)).andReturn(TRACK_POINT_ID_3);

    expectUpdateTrack(track, true, TRACK_ID_0);
    AndroidMock.replay(myTracksProviderUtils);

    InputStream inputStream = new ByteArrayInputStream(
        VALID_ONE_TRACK_TWO_SEGMENTS_NO_TIME_GPX.getBytes());
    GpxImporter gpxImporter = new GpxImporter(getContext(), myTracksProviderUtils);
    long[] trackIds = gpxImporter.importFile(inputStream);
    
    assertEquals(1, trackIds.length);
    assertEquals(TRACK_ID_0, trackIds[0]);

    assertEquals(0, track.getValue().getTripStatistics().getTotalTime());

    AndroidMock.verify(myTracksProviderUtils);
    verifyTrack(track.getValue(), TRACK_NAME_0, TRACK_DESCRIPTION_0, -1L);
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
    expectFirstTrackPoint(location0, TRACK_ID_0, TRACK_POINT_ID_0);
    expectUpdateTrack(track0, false, TRACK_ID_0);

    expect(myTracksProviderUtils.insertTrack((Track) AndroidMock.anyObject()))
        .andReturn(TRACK_ID_1_URI);
    expectFirstTrackPoint(location1, TRACK_ID_1, TRACK_POINT_ID_1);
    expectUpdateTrack(track1, true, TRACK_ID_1);

    AndroidMock.replay(myTracksProviderUtils);

    InputStream inputStream = new ByteArrayInputStream(VALID_TWO_TRACKS_GPX.getBytes());
    GpxImporter gpxImporter = new GpxImporter(getContext(), myTracksProviderUtils);
    long[] trackIds = gpxImporter.importFile(inputStream);
    assertEquals(2, trackIds.length);
    assertEquals(TRACK_ID_0, trackIds[0]);
    assertEquals(TRACK_ID_1, trackIds[1]);

    AndroidMock.verify(myTracksProviderUtils);
  }

  /**
   * Test an invalid xml input.
   */
  public void testInvalidXml() throws Exception {
    testInvalidGpx(INVALID_XML_GPX);
  }

  /**
   * Test an invalid location.
   */
  public void testInvalidLocation() throws Exception {
    testInvalidGpx(INVALID_LOCATION_GPX);
  }

  /**
   * Test an invalid time.
   */
  public void testInvalidTime() throws Exception {
    testInvalidGpx(INVALID_TIME_GPX);
  }

  /**
   * Test an invalid altitude.
   */
  public void testInvalidAltitude() throws Exception {
    testInvalidGpx(INVALID_ALTITUDE_GPX);
  }

  /**
   * Test an invalid latitude.
   */
  public void testInvalidLatitude() throws Exception {
    testInvalidGpx(INVALID_LATITUDE_GPX);
  }

  /**
   * Test an invalid longitude.
   */
  public void testInvalidLongitude() throws Exception {
    testInvalidGpx(INVALID_LONGITUDE_GPX);
  }

  private void testInvalidGpx(String xml) throws Exception {
    expect(myTracksProviderUtils.insertTrack((Track) AndroidMock.anyObject()))
        .andReturn(TRACK_ID_0_URI);

    // For the following, use StubReturn since we don't care whether they are
    // invoked or not.
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        (Location[]) AndroidMock.anyObject(), AndroidMock.anyInt(), AndroidMock.anyLong()))
        .andStubReturn(1);
    expect(myTracksProviderUtils.getFirstTrackPointId(TRACK_ID_0)).andStubReturn(TRACK_POINT_ID_0);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID_0)).andStubReturn(TRACK_POINT_ID_0);
    myTracksProviderUtils.deleteTrack(TRACK_ID_0);
    AndroidMock.replay(myTracksProviderUtils);

    InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
    try {
      GpxImporter gpxImporter = new GpxImporter(getContext(), myTracksProviderUtils);
      gpxImporter.importFile(inputStream);
    } catch (SAXException e) {
      // expected
    }
    AndroidMock.verify(myTracksProviderUtils);
  }
}
