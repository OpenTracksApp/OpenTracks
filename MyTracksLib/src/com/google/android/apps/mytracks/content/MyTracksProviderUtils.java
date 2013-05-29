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

import com.google.android.apps.mytracks.content.Waypoint.WaypointType;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;

import java.util.Iterator;
import java.util.List;

/**
 * Utilities to access data from the My Tracks content provider.
 * 
 * @author Rodrigo Damazio
 */
public interface MyTracksProviderUtils {

  /**
   * The authority (the first part of the URI) for the My Tracks content
   * provider.
   */
  public static final String AUTHORITY = "com.google.android.maps.mytracks";

  /**
   * Clears a track. Removes waypoints and trackpoints. Only keeps the track id.
   * 
   * @param trackId the track id
   */
  public void clearTrack(long trackId);

  /**
   * Creates a {@link Track} from a cursor.
   * 
   * @param cursor the cursor pointing to the track
   */
  public Track createTrack(Cursor cursor);

  /**
   * Deletes all tracks (including waypoints and track points).
   */
  public void deleteAllTracks();

  /**
   * Deletes a track.
   * 
   * @param trackId the track id
   */
  public void deleteTrack(long trackId);

  /**
   * Gets all the tracks. If no track exists, an empty list is returned.
   * <p>
   * Note that the returned tracks do not have any track points attached.
   */
  public List<Track> getAllTracks();

  /**
   * Gets the last track. Returns null if doesn't exist.
   */
  public Track getLastTrack();

  /**
   * Gets a track by a track id. Returns null if not found.
   * <p>
   * Note that the returned track doesn't have any track points attached.
   * 
   * @param trackId the track id.
   */
  public Track getTrack(long trackId);

  /**
   * Gets a track cursor. The caller owns the returned cursor and is responsible
   * for closing it.
   * 
   * @param selection the selection. Can be null
   * @param selectionArgs the selection arguments. Can be null
   * @param sortOrder the sort order. Can be null
   */
  public Cursor getTrackCursor(String selection, String[] selectionArgs, String sortOrder);

  /**
   * Inserts a track.
   * <p>
   * Note: This doesn't insert any track points.
   * 
   * @param track the track
   * @return the content provider URI of the inserted track.
   */
  public Uri insertTrack(Track track);

  /**
   * Updates a track.
   * <p>
   * Note: This doesn't update any track points.
   * 
   * @param track the track
   */
  public void updateTrack(Track track);

  /**
   * Creates a waypoint from a cursor.
   * 
   * @param cursor the cursor pointing to the waypoint
   */
  public Waypoint createWaypoint(Cursor cursor);

  /**
   * Deletes a waypoint. If deleting a statistics waypoint, this will also
   * correct the next statistics waypoint after the deleted one to reflect the
   * deletion. The generator is used to update the next statistics waypoint.
   * 
   * @param waypointId the waypoint id
   * @param descriptionGenerator the description generator. Can be null for
   *          waypoint marker
   */
  public void deleteWaypoint(long waypointId, DescriptionGenerator descriptionGenerator);

  /**
   * Gets the first waypoint id for a track. The first waypoint is special as it
   * contains the stats for the track. Returns -1L if it doesn't exist.
   * 
   * @param trackId the track id
   */
  public long getFirstWaypointId(long trackId);

  /**
   * Gets the last waypoint for a type. Returns null if it doesn't exist.
   * 
   * @param trackId the track id
   * @param waypointType the waypoint type
   */
  public Waypoint getLastWaypoint(long trackId, WaypointType waypointType);

  /**
   * Gets the next waypoint number for a type. Returns -1 if not able to get the
   * next waypoint number.
   * 
   * @param trackId the track id
   * @param waypointType the waypoint type
   */
  public int getNextWaypointNumber(long trackId, WaypointType waypointType);

  /**
   * Gets a waypoint from a waypoint id. Returns null if not found.
   * 
   * @param waypointId the waypoint id
   */
  public Waypoint getWaypoint(long waypointId);

  /**
   * Gets a waypoint cursor. The caller owns the returned cursor and is
   * responsible for closing it.
   * 
   * @param selection the selection. Can be null
   * @param selectionArgs the selection arguments. Can be null
   * @param sortOrder the sort order. Can be null
   * @param maxWaypoints the maximum number of waypoints to return. -1 for no
   *          limit
   */
  public Cursor getWaypointCursor(
      String selection, String[] selectionArgs, String sortOrder, int maxWaypoints);

  /**
   * Gets a waypoint cursor for a track. The caller owns the returned cursor and
   * is responsible for closing it.
   * 
   * @param trackId the track id
   * @param minWaypointId the minimum waypoint id. -1L to ignore
   * @param maxWaypoints the maximum number of waypoints to return. -1 for no
   *          limit
   */
  public Cursor getWaypointCursor(long trackId, long minWaypointId, int maxWaypoints);

  /**
   * Inserts a waypoint.
   * 
   * @param waypoint the waypoint
   * @return the content provider URI of the inserted waypoint.
   */
  public Uri insertWaypoint(Waypoint waypoint);

  /**
   * Updates a waypoint. Returns true if successful.
   * 
   * @param waypoint the waypoint
   */
  public boolean updateWaypoint(Waypoint waypoint);

  /**
   * Inserts multiple track points.
   * 
   * @param locations an array of locations
   * @param length the number of locations (from the beginning of the array) to
   *          insert, or -1 for all of them
   * @param trackId the track id
   * @return the number of points inserted
   */
  public int bulkInsertTrackPoint(Location[] locations, int length, long trackId);

  /**
   * Creates a location object from a cursor.
   * 
   * @param cursor the cursor pointing to the location
   */
  public Location createTrackPoint(Cursor cursor);

  /**
   * Gets the first location id for a track. Returns -1L if it doesn't exist.
   * 
   * @param trackId the track id
   */
  public long getFirstTrackPointId(long trackId);

  /**
   * Gets the last location id for a track. Returns -1L if it doesn't exist.
   * 
   * @param trackId the track id
   */
  public long getLastTrackPointId(long trackId);

  /**
   * Gets the first valid location for a track. Returns null if it doesn't
   * exist.
   * 
   * @param trackId the track id
   */
  public Location getFirstValidTrackPoint(long trackId);

  /**
   * Gets the last valid location for a track. Returns null if it doesn't exist.
   * 
   * @param trackId the track id
   */
  public Location getLastValidTrackPoint(long trackId);

  
  /**
   * Gets the last valid location.
   */
  public Location getLastValidTrackPoint();
  
  /**
   * Creates a location cursor. The caller owns the returned cursor and is
   * responsible for closing it.
   * 
   * @param trackId the track id
   * @param startTrackPointId the starting track point id. -1L to ignore
   * @param maxLocations maximum number of locations to return. -1 for no limit
   * @param descending true to sort the result in descending order (latest
   *          location first)
   */
  public Cursor getTrackPointCursor(
      long trackId, long startTrackPointId, int maxLocations, boolean descending);

  /**
   * Creates a new read-only iterator over a given track's points. It provides a
   * lightweight way of iterating over long tracks without failing due to the
   * underlying cursor limitations. Since it's a read-only iterator,
   * {@link Iterator#remove()} always throws
   * {@link UnsupportedOperationException}. Each call to
   * {@link LocationIterator#next()} may advance to the next DB record, and if
   * so, the iterator calls {@link LocationFactory#createLocation()} and
   * populates it with information retrieved from the record. When done with
   * iteration, {@link LocationIterator#close()} must be called.
   * 
   * @param trackId the track id
   * @param startTrackPointId the starting track point id. -1L to ignore
   * @param descending true to sort the result in descending order (latest
   *          location first)
   * @param locationFactory the location factory
   */
  public LocationIterator getTrackPointLocationIterator(
      long trackId, long startTrackPointId, boolean descending, LocationFactory locationFactory);

  /**
   * Inserts a track point.
   * 
   * @param location the location
   * @param trackId the track id
   * @return the content provider URI of the inserted track point
   */
  public Uri insertTrackPoint(Location location, long trackId);

  /**
   * A lightweight wrapper around the original {@link Cursor} with a method to
   * clean up.
   */
  public interface LocationIterator extends Iterator<Location> {

    /**
     * Gets the most recently retrieved track point id by {@link #next()}.
     */
    public long getLocationId();

    /**
     * Closes the iterator.
     */
    public void close();
  }

  /**
   * A factory for creating new {@link Location}.
   */
  public interface LocationFactory {

    /**
     * Creates a new {@link Location}. An implementation can create new
     * instances or reuse existing instances for optimization.
     */
    public Location createLocation();
  }

  /**
   * The default {@link LocationFactory} which creates a location each time.
   */
  public LocationFactory DEFAULT_LOCATION_FACTORY = new LocationFactory() {
      @Override
    public Location createLocation() {
      return new MyTracksLocation(LocationManager.GPS_PROVIDER);
    }
  };

  /**
   * A factory which can produce instances of {@link MyTracksProviderUtils}, and
   * can be overridden for testing.
   */
  public static class Factory {

    private static Factory instance = new Factory();

    /**
     * Creates an instance of {@link MyTracksProviderUtils}.
     * 
     * @param context the context
     */
    public static MyTracksProviderUtils get(Context context) {
      return instance.newForContext(context);
    }

    /**
     * Returns the factory instance.
     */
    public static Factory getInstance() {
      return instance;
    }

    /**
     * Overrides the factory instance for testing. Don't forget to set it back
     * to the original value after testing.
     * 
     * @param factory the factory
     */
    public static void overrideInstance(Factory factory) {
      instance = factory;
    }

    /**
     * Creates an instance of {@link MyTracksProviderUtils}. Allows subclasses
     * to override for testing.
     * 
     * @param context the context
     */
    protected MyTracksProviderUtils newForContext(Context context) {
      return new MyTracksProviderUtilsImpl(context.getContentResolver());
    }
  }
}
