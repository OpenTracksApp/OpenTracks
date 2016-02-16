// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.io.file.exporter;

import com.google.android.apps.mytracks.content.MyTracksProvider;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.Factory;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.services.TrackRecordingServiceTest.MockContext;
import com.google.android.apps.mytracks.testing.TestingProviderUtilsFactory;

import android.content.Context;
import android.location.Location;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;

/**
 * Tests for {@link FileTrackExporter}.
 * 
 * @author Rodrigo Damazio
 */
public class FileTrackExporterTest extends AndroidTestCase {

  private static final long TRACK_ID = 1234567L;
  private static final String TRACK_NAME = "Swimming across the pacific";

  private MyTracksProviderUtils myTracksProviderUtils;
  private Factory oldProviderUtilsFactory;

  private IMocksControl mocksControl;

  private TrackWriter trackWriter;
  private Track[] tracks;
  private OutputStream outputStream;
  private FileTrackExporter fileTrackExporter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    MockContentResolver mockContentResolver = new MockContentResolver();
    RenamingDelegatingContext targetContext = new RenamingDelegatingContext(
        getContext(), getContext(), "test.");
    Context context = new MockContext(mockContentResolver, targetContext);
    MyTracksProvider myTracksProvider = new MyTracksProvider();
    myTracksProvider.attachInfo(context, null);
    mockContentResolver.addProvider(MyTracksProviderUtils.AUTHORITY, myTracksProvider);
    setContext(context);
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
    oldProviderUtilsFactory = TestingProviderUtilsFactory.installWithInstance(
        myTracksProviderUtils);

    mocksControl = EasyMock.createStrictControl();
    trackWriter = mocksControl.createMock(TrackWriter.class);

    Track track = new Track();
    track.setName(TRACK_NAME);
    track.setId(TRACK_ID);

    tracks = new Track[] {track};
    
    outputStream = new ByteArrayOutputStream();
    TrackExporterListener trackExporterListener = new TrackExporterListener() {
      
        @Override
      public void onProgressUpdate(int number, int max) {
        // Safe to ignore

      }
    };
    fileTrackExporter = new FileTrackExporter(
        myTracksProviderUtils, tracks, trackWriter, trackExporterListener);
  }

  @Override
  protected void tearDown() throws Exception {
    TestingProviderUtilsFactory.restoreOldFactory(oldProviderUtilsFactory);
    super.tearDown();
  }

  /**
   * Tests write track with an empty track.
   */
  public void testWriteTrack_emptyTrack() throws Exception {

    // Set expected mock behavior
    trackWriter.prepare(outputStream);
    trackWriter.writeHeader(tracks);
    trackWriter.writeBeginTracks();
    trackWriter.writeBeginTrack(tracks[0], null);
    trackWriter.writeEndTrack(tracks[0], null);
    trackWriter.writeEndTracks();
    trackWriter.writeFooter();
    trackWriter.close();
 
    mocksControl.replay();
    assertTrue(fileTrackExporter.writeTrack(outputStream));
    mocksControl.verify();
  }

  /**
   * Tests write track with invalid locations. Make sure an empty track is
   * written.
   */
  public void testWriteTrack_oneInvalidLocation() throws Exception {

    // Add two locations
    Location[] locations = { new Location("fake0"), new Location("fake1") };
    fillLocations(locations);

    // Make locations invalid
    locations[0].setLatitude(100.0);
    locations[1].setLatitude(100.0);

    assertEquals(locations.length,
        myTracksProviderUtils.bulkInsertTrackPoint(locations, locations.length, TRACK_ID));

    // Set expected mock behavior
    trackWriter.prepare(outputStream);
    trackWriter.writeHeader(tracks);
    trackWriter.writeBeginTracks();
    trackWriter.writeBeginTrack(tracks[0], null);
    trackWriter.writeEndTrack(tracks[0], null);
    trackWriter.writeEndTracks();
    trackWriter.writeFooter();
    trackWriter.close();
    
    mocksControl.replay();
    
    assertTrue(fileTrackExporter.writeTrack(outputStream));
    mocksControl.verify();
  }

  /**
   * Tests write track.
   */
  public void testWriteTrack() throws Exception {

    // Add six locations
    Location[] locations = { new Location("fake0"), new Location("fake1"), new Location("fake2"),
        new Location("fake3"), new Location("fake4"), new Location("fake5") };
    fillLocations(locations);

    // Make location 3 invalid
    locations[2].setLatitude(100.0);

    assertEquals(locations.length,
        myTracksProviderUtils.bulkInsertTrackPoint(locations, locations.length, TRACK_ID));

    Waypoint[] waypoints = { new Waypoint(), new Waypoint(), new Waypoint() };

    for (int i = 0; i < waypoints.length; i++) {
      Waypoint waypoint = waypoints[i];
      waypoint.setTrackId(TRACK_ID);
      assertNotNull(myTracksProviderUtils.insertWaypoint(waypoint));
      waypoint.setId(i + 1);
    }

    trackWriter.prepare(outputStream);
    trackWriter.writeHeader(tracks);

    // Expect reading/writing of the waypoints (except the first)
    trackWriter.writeBeginWaypoints(tracks[0]);
    trackWriter.writeWaypoint(waypointEq(waypoints[1]));
    trackWriter.writeWaypoint(waypointEq(waypoints[2]));
    trackWriter.writeEndWaypoints();

    // Begin the track
    trackWriter.writeBeginTracks();
    trackWriter.writeBeginTrack(trackEq(tracks[0]), locationEq(locations[0]));

    // Write locations 1-2
    trackWriter.writeOpenSegment();
    trackWriter.writeLocation(locationEq(locations[0]));
    trackWriter.writeLocation(locationEq(locations[1]));
    trackWriter.writeCloseSegment();

    // Location 3 is not written - it's invalid

    // Write locations 4-6
    trackWriter.writeOpenSegment();
    trackWriter.writeLocation(locationEq(locations[3]));
    trackWriter.writeLocation(locationEq(locations[4]));
    trackWriter.writeLocation(locationEq(locations[5]));
    trackWriter.writeCloseSegment();

    // End the track
    trackWriter.writeEndTrack(trackEq(tracks[0]), locationEq(locations[5]));
    trackWriter.writeEndTracks();
    
    trackWriter.writeFooter();
    trackWriter.close();
    
    mocksControl.replay();
    
    assertTrue(fileTrackExporter.writeTrack(outputStream));
    mocksControl.verify();
  }

  /**
   * Waypoint equals.
   * 
   * @param waypoint the waypoint
   */
  private Waypoint waypointEq(final Waypoint waypoint) {
    EasyMock.reportMatcher(new IArgumentMatcher() {
        @Override
      public boolean matches(Object object) {
        if (object == null || waypoint == null) {
          return waypoint == object;
        }
        Waypoint waypoint2 = (Waypoint) object;

        return waypoint.getId() == waypoint2.getId();
      }

        @Override
      public void appendTo(StringBuffer buffer) {
        buffer.append("waypointEq(");
        buffer.append(waypoint);
        buffer.append(")");
      }
    });
    return null;
  }

  /**
   * Location equals.
   *  
   * @param location the location
   */
  private Location locationEq(final Location location) {
    EasyMock.reportMatcher(new IArgumentMatcher() {
        @Override
      public boolean matches(Object object) {
        if (object == null || location == null) {
          return location == object;
        }
        Location location2 = (Location) object;

        return location.hasAccuracy() == location2.hasAccuracy()
            && (!location.hasAccuracy() || location.getAccuracy() == location2.getAccuracy())
            && location.hasAltitude() == location2.hasAltitude()
            && (!location.hasAltitude() || location.getAltitude() == location2.getAltitude())
            && location.hasBearing() == location2.hasBearing()
            && (!location.hasBearing() || location.getBearing() == location2.getBearing())
            && location.hasSpeed() == location2.hasSpeed()
            && (!location.hasSpeed() || location.getSpeed() == location2.getSpeed())
            && location.getLatitude() == location2.getLatitude()
            && location.getLongitude() == location2.getLongitude()
            && location.getTime() == location2.getTime();
      }

        @Override
      public void appendTo(StringBuffer buffer) {
        buffer.append("locationEq(");
        buffer.append(location);
        buffer.append(")");
      }
    });
    return null;
  }

  /**
   * Track equals.
   * 
   * @param track1 the track
   */
  private Track trackEq(final Track track1) {
    EasyMock.reportMatcher(new IArgumentMatcher() {
        @Override
      public boolean matches(Object object) {
        if (object == null || track1 == null) {
          return track1 == object;
        }
        Track track2 = (Track) object;

        return track1.getName().equals(track2.getName());
      }

        @Override
      public void appendTo(StringBuffer buffer) {
        buffer.append("trackEq(");
        buffer.append(track1);
        buffer.append(")");
      }
    });
    return null;
  }

  /**
   * Fills the locations.
   * 
   * @param locations the locations
   */
  private void fillLocations(Location... locations) {
    assertTrue(locations.length < 90);
    for (int i = 0; i < locations.length; i++) {
      Location location = locations[i];
      location.setLatitude(i + 1);
      location.setLongitude(i + 1);
      location.setTime(i + 1000);
    }
  }
}
