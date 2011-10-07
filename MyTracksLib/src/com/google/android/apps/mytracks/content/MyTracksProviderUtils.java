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
package com.google.android.apps.mytracks.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;

import java.util.Iterator;
import java.util.List;

/**
 * Utility to access data from the mytracks content provider.
 *
 * @author Rodrigo Damazio
 */
public interface MyTracksProviderUtils {

  /**
   * Authority (first part of URI) for the MyTracks content provider:
   */
  public static final String AUTHORITY = "com.google.android.maps.mytracks";

  /**
   * Authority for the Database content provider. The Database content provider
   * is configured, through the MyTracks AndroidManifest.xml, to only allow the
   * MyTracks app access.
   */
  public static final String DATABASE_AUTHORITY = "com.google.android.apps.mytracks.content";

  /**
   * Deletes all tracks (including track points) from the provider.
   */
  void deleteAllTracks();

  /**
   * Deletes a track with the given track id.
   *
   * @param trackId the unique track id
   */
  void deleteTrack(long trackId);

  /**
   * Deletes a way point with the given way point id.
   * This will also correct the next statistics way point after the deleted one
   * to reflect the deletion.
   * The generator is needed to stitch together statistics waypoints.
   *
   * @param waypointId the unique way point id
   * @param descriptionGenerator the class to generate descriptions
   */
  void deleteWaypoint(long waypointId,
      DescriptionGenerator descriptionGenerator);

  /**
   * Finds the next statistics waypoint after the given waypoint.
   *
   * @param waypoint a given waypoint
   * @return the next statistics waypoint, or null if none found.
   */
  Waypoint getNextStatisticsWaypointAfter(Waypoint waypoint);

  /**
   * Updates the waypoint in the provider.
   *
   * @param waypoint
   * @return true if successful
   */
  boolean updateWaypoint(Waypoint waypoint);

  /**
   * Finds the last recorded location from the location provider.
   *
   * @return the last location, or null if no locations available
   */
  Location getLastLocation();

  /**
   * Finds the first recorded waypoint for a given track from the location
   * provider.
   * This is a special waypoint that holds the stats for current segment.
   *
   * @param trackId the id of the track the waypoint belongs to
   * @return the first waypoint, or null if no waypoints available
   */
  Waypoint getFirstWaypoint(long trackId);

  /**
   * Finds the given waypoint from the location provider.
   *
   * @param waypointId
   * @return the waypoint, or null if it does not exist
   */
  Waypoint getWaypoint(long waypointId);

  /**
   * Finds the last recorded location id from the track points provider.
   *
   * @param trackId find last location on this track
   * @return the location id, or -1 if no locations available
   */
  long getLastLocationId(long trackId);

  /**
   * Finds the id of the 1st waypoint for a given track.
   * The 1st waypoint is special as it contains the stats for the current
   * segment.
   *
   * @param trackId find last location on this track
   * @return the waypoint id, or -1 if no waypoints are available
   */
  long getFirstWaypointId(long trackId);

  /**
   * Finds the id of the 1st waypoint for a given track.
   * The 1st waypoint is special as it contains the stats for the current
   * segment.
   *
   * @param trackId find last location on this track
   * @return the waypoint id, or -1 if no waypoints are available
   */
  long getLastWaypointId(long trackId);

  /**
   * Finds the last recorded track from the track provider.
   *
   * @return the last track, or null if no tracks available
   */
  Track getLastTrack();

  /**
   * Finds the last recorded track id from the tracks provider.
   *
   * @return the track id, or -1 if no tracks available
   */
  long getLastTrackId();

  /**
   * Finds a location by given unique id.
   *
   * @param id the desired id
   * @return a Location object, or null if not found
   */
  Location getLocation(long id);

  /**
   * Creates a cursor over the locations in the track points provider which
   * iterates over a given range of unique ids.
   * Caller gets to own the returned cursor. Don't forget to close it.
   *
   * @param trackId the id of the track for which to get the points
   * @param minTrackPointId the minimum id for the track points
   * @param maxLocations maximum number of locations retrieved
   * @param descending if true the results will be returned in descending id
   *        order (latest location first)
   * @return A cursor over the selected range of locations
   */
  Cursor getLocationsCursor(long trackId, long minTrackPointId,
      int maxLocations, boolean descending);

  /**
   * Creates a cursor over the waypoints of a track.
   * Caller gets to own the returned cursor. Don't forget to close it.
   *
   * @param trackId the id of the track for which to get the points
   * @param minWaypointId the minimum id for the track points
   * @param maxWaypoints the maximum number of waypoints to return
   * @return A cursor over the selected range of locations
   */
  Cursor getWaypointsCursor(long trackId, long minWaypointId,
      int maxWaypoints);

  /**
   * Finds a track by given unique track id.
   * Note that the returned track object does not have any track points attached.
   * Use {@link #getLocationIterator(long, long, boolean, LocationFactory)} to load
   * the track points.
   *
   * @param id desired unique track id
   * @return a Track object, or null if not found
   */
  Track getTrack(long id);
  
  /**
   * Retrieves all tracks without track points. If no tracks exist, an empty
   * list will be returned.  Use {@link #getLocationIterator(long, long, boolean, LocationFactory)}
   * to load the track points.
   * 
   * @return a list of all the recorded tracks
   */
  List<Track> getAllTracks();

  /**
   * Creates a cursor over the tracks provider with a given selection.
   * Caller gets to own the returned cursor. Don't forget to close it.
   *
   * @param selection a given selection
   * @return a cursor of the selected tracks
   */
  Cursor getTracksCursor(String selection);

  /**
   * Inserts a track in the tracks provider.
   * Note: This does not insert any track points.
   * Use {@link #insertTrackPoint(Location, long)} to insert them.
   *
   * @param track the track to insert
   * @return the content provider URI for the inserted track
   */
  Uri insertTrack(Track track);

  /**
   * Inserts a track point in the tracks provider.
   *
   * @param location the location to insert
   * @return the content provider URI for the inserted track point
   */
  Uri insertTrackPoint(Location location, long trackId);

  /**
   * Inserts multiple track points in a single operation.
   *
   * @param locations an array of locations to insert
   * @param length the number of locations (from the beginning of the array)
   *        to actually insert, or -1 for all of them
   * @param trackId the ID of the track to insert the points into
   * @return the number of points inserted
   */
  int bulkInsertTrackPoints(Location[] locations, int length, long trackId);

  /**
   * Inserts a waypoint in the provider.
   *
   * @param waypoint the waypoint to insert
   * @return the content provider URI for the inserted track
   */
  Uri insertWaypoint(Waypoint waypoint);

  /**
   * Tests if a track with given id exists.
   *
   * @param id the unique id
   * @return true if track exists
   */
  boolean trackExists(long id);

  /**
   * Updates a track in the content provider.
   * Note: This will not update any track points.
   *
   * @param track a given track
   */
  void updateTrack(Track track);

  /**
   * Creates a Track object from a given cursor.
   *
   * @param cursor a cursor pointing at a db or provider with tracks
   * @return a new Track object
   */
  Track createTrack(Cursor cursor);

  /**
   * Creates the ContentValues for a given Track object.
   *
   * Note: If the track has an id<0 the id column will not be filled.
   *
   * @param track a given track object
   * @return a filled in ContentValues object
   */
  ContentValues createContentValues(Track track);

  /**
   * Creates a location object from a given cursor.
   *
   * @param cursor a cursor pointing at a db or provider with locations
   * @return a new location object
   */
  Location createLocation(Cursor cursor);

  /**
   * Fill a location object with values from a given cursor.
   * 
   * @param cursor a cursor pointing at a db or provider with locations
   * @param location a location object to be overwritten
   */
  void fillLocation(Cursor cursor, Location location);

  /**
   * Creates a waypoint object from a given cursor.
   *
   * @param cursor a cursor pointing at a db or provider with waypoints.
   * @return a new waypoint object
   */
  Waypoint createWaypoint(Cursor cursor);

  /**
   * A lightweight wrapper around the original {@link Cursor} with a method to clean up.
   */
  interface LocationIterator extends Iterator<Location> {
    /**
     * Returns ID of the most recently retrieved track point through a call to {@link #next()}.
     * 
     * @return the ID of the most recent track point ID.
     */
    long getLocationId();

    /**
     * Should be called in case the underlying iterator hasn't reached the last record.
     * Calling it if it has reached the last record is a no-op.
     */
    void close();
  }

  /**
   * A factory for creating new {@class Location}s.
   */
  interface LocationFactory {
    /**
     * Creates a new {@link Location} object to be populated from the underlying database record.
     * It's up to the implementing class to decide whether to create a new instance or reuse
     * existing to optimize for speed.
     * 
     * @return a {@link Location} to be populated from the database.
     */
    Location createLocation();
  }
  
  /**
   * The default {@class Location}s factory, which creates a new location of 'gps' type.
   */
  LocationFactory DEFAULT_LOCATION_FACTORY = new LocationFactory() {
    @Override
    public Location createLocation() {
      return new Location("gps");
    }
  };

  /**
   * A location factory which uses two location instances (one for the current location,
   * and one for the previous), useful when we need to keep the last location.
   */
  public class DoubleBufferedLocationFactory implements LocationFactory {
    private final Location locs[] = new MyTracksLocation[] {
      new MyTracksLocation("gps"),
      new MyTracksLocation("gps")
    };
    private int lastLoc = 0;

    @Override
    public Location createLocation() {
      lastLoc = (lastLoc + 1) % locs.length;
      return locs[lastLoc];
    }
  }
  
  /**
   * Creates a new read-only iterator over all track points for the given track.  It provides
   * a lightweight way of iterating over long tracks without failing due to the underlying cursor
   * limitations. Since it's a read-only iterator, {@link Iterator#remove()} always throws
   * {@class UnsupportedOperationException}.
   * 
   * Each call to {@link LocationIterator#next()} may advance to the next DB record, and if so,
   * the iterator calls {@link LocationFactory#createLocation()} and populates it with information
   * retrieved from the record.
   * 
   * When done with iteration, you must call {@link LocationIterator#close()} to make sure that all
   * resources are properly deallocated.
   * 
   * Example use:
   * <code>
   *   ...
   *   LocationIterator it = providerUtils.getLocationIterator(
   *       1, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
   *   try {
   *     for (Location loc : it) {
   *       ...  // Do something useful with the location.
   *     }
   *   } finally {
   *     it.close();
   *   }
   *   ...
   * </code>
   * 
   * @param trackId the ID of a track to retrieve locations for.
   * @param startTrackPointId the ID of the first track point to load, or -1 to start from
   *        the first point.
   * @param descending if true the results will be returned in descending ID
   *        order (latest location first).
   * @param locationFactory the factory for creating new locations.
   * 
   * @return the read-only iterator over the given track's points.
   */
  LocationIterator getLocationIterator(long trackId, long startTrackPointId, boolean descending,
      LocationFactory locationFactory);

  /**
   * A factory which can produce instances of {@link MyTracksProviderUtils},
   * and can be overridden in tests (a.k.a. poor man's guice).
   */
  public static class Factory {
    private static Factory instance = new Factory();

    /**
     * Creates and returns an instance of {@link MyTracksProviderUtils} which
     * uses the given context to access its data.
     */
    public static MyTracksProviderUtils get(Context context) {
      return instance.newForContext(context);
    }

    /**
     * Returns the global instance of this factory.
     */
    public static Factory getInstance() {
      return instance;
    }

    /**
     * Overrides the global instance for this factory, to be used for testing.
     * If used, don't forget to set it back to the original value after the
     * test is run.
     */
    public static void overrideInstance(Factory factory) {
      instance = factory;
    }

    /**
     * Creates an instance of {@link MyTracksProviderUtils}.
     */
    protected MyTracksProviderUtils newForContext(Context context) {
      return new MyTracksProviderUtilsImpl(context.getContentResolver(), AUTHORITY);
    }
  }
}
