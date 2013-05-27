/*
 * Copyright 2012 Google Inc.
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

/**
 * Tests for {@link KmlImporter}.
 * 
 * @author Jimmy Shih
 */
public class KmlImporterTest extends AbstractTestImporter {

  private static String getNameAndDescription(String name, String description) {
    return "<name><![CDATA[" + name + "]]></name><description><![CDATA[" + description
        + "]]></description>";
  }

  private static String getTrackPoint(int index, String time) {
    String latitude = Double.toString(TRACK_LATITUDE + index);
    String longitude = Double.toString(TRACK_LONGITUDE + index);
    String altitude = Double.toString(TRACK_ELEVATION + index);
    StringBuffer buffer = new StringBuffer();
    buffer.append("<when>" + time + "</when>" + "<gx:coord>" + longitude + " " + latitude + " "
        + altitude + "</gx:coord>");
    return buffer.toString();
  }

  private static final String VALID_ONE_TRACK_ONE_SEGMENT_GPX =
      "<kml xmlns:gx=\"http://www.google.com/kml/ext/2.2\"><Placemark>"
      + getNameAndDescription(TRACK_NAME_0, TRACK_DESCRIPTION_0) + "<gx:MultiTrack><gx:Track>"
      + getTrackPoint(0, TRACK_TIME_0) + getTrackPoint(1, TRACK_TIME_1)
      + "</gx:Track></gx:MultiTrack></Placemark></kml>";
  private static final String VALID_ONE_TRACK_TWO_SEGMENTS_GPX =
      "<kml xmlns:gx=\"http://www.google.com/kml/ext/2.2\"><Placemark>"
      + getNameAndDescription(TRACK_NAME_0, TRACK_DESCRIPTION_0) + "<gx:MultiTrack><gx:Track>"
      + getTrackPoint(0, TRACK_TIME_0) + getTrackPoint(1, TRACK_TIME_1) + "</gx:Track><gx:Track>"
      + getTrackPoint(2, TRACK_TIME_2) + getTrackPoint(3, TRACK_TIME_3)
      + "</gx:Track></gx:MultiTrack></Placemark></kml>";

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
    KmlImporter kmlImporter = new KmlImporter(getContext(), myTracksProviderUtils);
    long[] trackIds = kmlImporter.importFile(inputStream);
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
    KmlImporter kmlImporter = new KmlImporter(getContext(), myTracksProviderUtils);
    long[] trackIds = kmlImporter.importFile(inputStream);
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
}
