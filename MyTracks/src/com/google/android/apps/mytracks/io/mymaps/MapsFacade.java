// Copyright 2011 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.mymaps;

import android.location.Location;

import java.util.Collection;

/**
 * Single interface which abstracts all access to the Google Maps service.
 *
 * @author Rodrigo Damazio
 */
public interface MapsFacade {

  /**
   * Interface for receiving data back from getMapsList.
   * All calls to the interface will happen before getMapsList returns.
   */
  public interface MapsListCallback {
    void onReceivedMapListing(String mapId, String title, String description,
        boolean isPublic);
  }

  /**
   * Returns a list of all maps for the current user.
   *
   * @param callback callback to call for each map returned
   * @return true on success, false otherwise
   */
  boolean getMapsList(MapsListCallback callback);

  /**
   * Creates a new map for the current user.
   *
   * @param title title of the map
   * @param category category of the map
   * @param description description for the map
   * @param isPublic whether the map should be public
   * @param mapIdBuilder builder to append the map ID to
   * @return true on success, false otherwise
   */
  boolean createNewMap(String title, String category, String description,
      boolean isPublic, StringBuilder mapIdBuilder);

  /**
   * Uploads a single start or end marker to the given map.
   *
   * @param mapId ID of the map to upload to
   * @param trackName name of the track being started/ended
   * @param trackDescription description of the track being started/ended
   * @param location the location of the marker
   * @param isStart true to add a start marker, false to add an end marker
   * @return true on success, false otherwise
   */
  boolean uploadMarker(String mapId, String trackName, String trackDescription,
      Location location, boolean isStart);

  /** Plain data class for waypoints. */
  public class WaypointData {
    public String title;
    public String description;
    public String iconUrl;
    public Location location;

    public WaypointData(String title, String description, String iconUrl,
        Location location) {
      this.title = title;
      this.description = description;
      this.iconUrl = iconUrl;
      this.location = location;
    }
  }

  /**
   * Uploads a series of waypoints to the given map.
   *
   * @param mapId ID of the map to upload to
   * @param waypoints the waypoints to upload
   * @return true on success, false otherwise
   */
  boolean uploadWaypoints(String mapId, Iterable<WaypointData> waypoints);

  /**
   * Uploads a series of points to the given map.
   *
   * @param mapId ID of the map to upload to
   * @param trackName the name of the track
   * @param locations the locations to upload
   * @return true on success, false otherwise
   */
  boolean uploadTrackPoints(String mapId, String trackName,
      Collection<Location> locations);

  /**
   * Cleans up after a series of uploads.
   * This closes the connection to Maps and resets retry counters.
   */
  void cleanUp();
}
