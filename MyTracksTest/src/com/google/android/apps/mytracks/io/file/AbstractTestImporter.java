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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.Factory;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.testing.TestingProviderUtilsFactory;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.content.ContentUris;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.test.AndroidTestCase;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.SimpleTimeZone;

import org.easymock.Capture;

/**
 * Abstract class for testing importer.
 * 
 * @author Jimmy Shih.
 */
public class AbstractTestImporter extends AndroidTestCase {

  protected static final String TRACK_NAME_0 = "blablub";
  protected static final String TRACK_DESCRIPTION_0 = "s'Laebe isch koi Schlotzer";

  protected static final String TRACK_NAME_1 = "another track";
  protected static final String TRACK_DESCRIPTION_1 = "another description";

  protected static final double TRACK_LATITUDE = 48.768364;
  protected static final double TRACK_LONGITUDE = 9.177886;
  protected static final double TRACK_ELEVATION = 324.0;

  protected static final String TRACK_TIME_0 = "2010-04-22T18:21:00Z";
  protected static final String TRACK_TIME_1 = "2010-04-22T18:21:50.123";
  protected static final String TRACK_TIME_2 = "2010-04-22T18:23:00.123";
  protected static final String TRACK_TIME_3 = "2010-04-22T18:24:50.123";

  protected static final SimpleDateFormat DATE_FORMAT_0 = new SimpleDateFormat(
      "yyyy-MM-dd'T'hh:mm:ss'Z'", Locale.US);
  protected static final SimpleDateFormat DATE_FORMAT_1 = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);

  static {
    /*
     * We can't omit the timezones in the test, otherwise it'll use the local
     * timezone and fail depending on where the test runner is.
     */
    SimpleTimeZone utc = new SimpleTimeZone(0, "UTC");
    DATE_FORMAT_0.setTimeZone(utc);
    DATE_FORMAT_1.setTimeZone(utc);
  }

  protected static final long TRACK_ID_0 = 1;
  protected static final long TRACK_ID_1 = 2;
  protected static final long TRACK_POINT_ID_0 = 1;
  protected static final long TRACK_POINT_ID_1 = 2;
  protected static final long TRACK_POINT_ID_3 = 4;
  protected static final long WAYPOINT_ID_0 = 1;

  protected static final Uri TRACK_ID_0_URI = ContentUris.appendId(
      TracksColumns.CONTENT_URI.buildUpon(), TRACK_ID_0).build();
  protected static final Uri TRACK_ID_1_URI = ContentUris.appendId(
      TracksColumns.CONTENT_URI.buildUpon(), TRACK_ID_1).build();
  protected static final Uri WAYPOINT_ID_O_URI = ContentUris.appendId(
      WaypointsColumns.CONTENT_URI.buildUpon(), WAYPOINT_ID_0).build();

  protected MyTracksProviderUtils myTracksProviderUtils;

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
  
  protected Location createLocation(int index, long time) {
    Location location = new Location(LocationManager.GPS_PROVIDER);
    location.setLatitude(TRACK_LATITUDE + index);
    location.setLongitude(TRACK_LONGITUDE + index);
    location.setAltitude(TRACK_ELEVATION + index);
    location.setTime(time);
    return location;
  }

  /**
   * Expects the first track point to be added.
   * 
   * @param location the location
   * @param trackId the track id
   * @param trackPointId the track point id
   */
  protected void expectFirstTrackPoint(Location location, long trackId, long trackPointId) {
    expect(myTracksProviderUtils.bulkInsertTrackPoint(
        location != null ? LocationsMatcher.eqLoc(location) : (Location[]) AndroidMock.anyObject(),
        eq(1), eq(trackId))).andReturn(1);
    expect(myTracksProviderUtils.getFirstTrackPointId(trackId)).andReturn(trackPointId);
    expect(myTracksProviderUtils.getLastTrackPointId(trackId)).andReturn(trackPointId);
  }

  /**
   * Expects the track to be updated.
   * 
   * @param track the track
   * @param lastTrack true if it is the last track in the gpx
   * @param trackId the track id
   */
  protected void expectUpdateTrack(Capture<Track> track, boolean lastTrack, long trackId) {
    myTracksProviderUtils.updateTrack(AndroidMock.capture(track));
    expect(myTracksProviderUtils.insertWaypoint((Waypoint) AndroidMock.anyObject()))
        .andReturn(WAYPOINT_ID_O_URI);
    if (lastTrack) {
      // Return null to not add waypoints
      expect(myTracksProviderUtils.getTrack(trackId)).andReturn(null);
    }
  }

  protected void verifyTrack(Track track, String name, String description, long time) {
    assertEquals(name, track.getName());
    assertEquals(description, track.getDescription());
    if (time != -1L) {
      assertEquals(time, track.getTripStatistics().getStartTime());
    }
    assertNotSame(-1, track.getStartId());
    assertNotSame(-1, track.getStopId());
  }  
}
