/*
 * Copyright 2008 Google Inc.
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

package com.google.android.apps.mytracks;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.android.apps.mytracks.maps.TrackPath;
import com.google.android.apps.mytracks.maps.TrackPathFactory;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A map overlay that displays my location arrow, error circle, and track info.
 * 
 * @author Leif Hendrik Wilden
 */
public class MapOverlay {

  public static final float WAYPOINT_X_ANCHOR = 13f / 48f;

  private static final float WAYPOINT_Y_ANCHOR = 43f / 48f;
  private static final float MARKER_X_ANCHOR = 50f / 96f;
  private static final float MARKER_Y_ANCHOR = 90f / 96f;
  private static final int INITIAL_LOCATIONS_SIZE = 1024;

  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
          @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
          if (key == null
              || key.equals(PreferencesUtils.getKey(context, R.string.track_color_mode_key))) {
            trackColorMode = PreferencesUtils.getString(
                context, R.string.track_color_mode_key, PreferencesUtils.TRACK_COLOR_MODE_DEFAULT);
            trackPath = TrackPathFactory.getTrackPath(context, trackColorMode);
          }
        }
      };

  private final Context context;
  private final List<CachedLocation> locations;
  private final BlockingQueue<CachedLocation> pendingLocations;
  private final List<Waypoint> waypoints;

  private String trackColorMode = PreferencesUtils.TRACK_COLOR_MODE_DEFAULT;

  private boolean showEndMarker = true;
  private TrackPath trackPath;

  /**
   * A pre-processed {@link Location} to speed up drawing.
   * 
   * @author Jimmy Shih
   */
  public static class CachedLocation {

    private final boolean valid;
    private final LatLng latLng;
    private final int speed;

    /**
     * Constructor for an invalid cached location.
     */
    public CachedLocation() {
      this.valid = false;
      this.latLng = null;
      this.speed = -1;
    }

    /**
     * Constructor for a potentially valid cached location.
     */
    public CachedLocation(Location location) {
      this.valid = LocationUtils.isValidLocation(location);
      this.latLng = valid ? new LatLng(location.getLatitude(), location.getLongitude()) : null;
      this.speed = location.hasSpeed() ? (int) Math.floor(
          location.getSpeed() * UnitConversions.MS_TO_KMH)
          : -1;
    }

    /**
     * Returns true if the location is valid.
     */
    public boolean isValid() {
      return valid;
    }

    /**
     * Gets the speed in kilometers per hour.
     */
    public int getSpeed() {
      return speed;
    }

    /**
     * Gets the LatLng.
     */
    public LatLng getLatLng() {
      return latLng;
    }
  };

  public MapOverlay(Context context) {
    this.context = context;
    this.waypoints = new ArrayList<Waypoint>();
    this.locations = new ArrayList<CachedLocation>(INITIAL_LOCATIONS_SIZE);
    this.pendingLocations = new ArrayBlockingQueue<CachedLocation>(
        Constants.MAX_DISPLAYED_TRACK_POINTS, true);

    context.getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE)
        .registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
  }

  /**
   * Add a location.
   * 
   * @param location the location
   */
  public void addLocation(Location location) {
    // Queue up in the pendingLocations until it's merged with locations
    if (!pendingLocations.offer(new CachedLocation(location))) {
      Log.e(TAG, "Unable to add to pendingLocations.");
    }
  }

  /**
   * Adds a segment split.
   */
  public void addSegmentSplit() {
    // Queue up in the pendingLocations until it's merged with locations
    if (!pendingLocations.offer(new CachedLocation())) {
      Log.e(TAG, "Unable to add to pendingLocations.");
    }
  }

  /**
   * Clears the locations.
   */
  public void clearPoints() {
    synchronized (locations) {
      locations.clear();
      pendingLocations.clear();
    }
  }

  /**
   * Adds a waypoint.
   * 
   * @param waypoint the waypoint
   */
  public void addWaypoint(Waypoint waypoint) {
    synchronized (waypoints) {
      waypoints.add(waypoint);
    }
  }

  /**
   * Clears the waypoints.
   */
  public void clearWaypoints() {
    synchronized (waypoints) {
      waypoints.clear();
    }
  }

  /**
   * Sets whether to show the end marker.
   * 
   * @param show true to show the end marker
   */
  public void setShowEndMarker(boolean show) {
    showEndMarker = show;
  }

  /**
   * Updates the track, start and end markers, and waypoints.
   * 
   * @param googleMap the google map
   * @param paths the paths
   * @param tripStatistics the trip statistics
   * @param reload true to reload all points
   * @return true if has the start marker
   */
  public boolean update(GoogleMap googleMap, ArrayList<Polyline> paths,
      TripStatistics tripStatistics, boolean reload) {
    synchronized (locations) {
      boolean hasStartMarker = false;
      // Merge pendingLocations with locations
      int newLocations = pendingLocations.drainTo(locations);
      // Call updateState first because we want to update its state each time
      // (for dynamic coloring)
      if (trackPath.updateState(tripStatistics) || reload) {
        googleMap.clear();
        paths.clear();
        trackPath.updatePath(googleMap, paths, 0, locations);
        hasStartMarker = updateStartAndEndMarkers(googleMap);
        updateWaypoints(googleMap);
      } else {
        if (newLocations != 0) {
          int numLocations = locations.size();
          trackPath.updatePath(googleMap, paths, numLocations - newLocations, locations);
        }
      }
      return hasStartMarker;
    }
  }

  /**
   * Updates the start and end markers.
   * 
   * @param googleMap the google map
   * @return true if has the start marker
   */
  private boolean updateStartAndEndMarkers(GoogleMap googleMap) {
    // Add the end marker
    if (showEndMarker) {
      for (int i = locations.size() - 1; i >= 0; i--) {
        CachedLocation cachedLocation = locations.get(i);
        if (cachedLocation.valid) {
          MarkerOptions markerOptions = new MarkerOptions().position(cachedLocation.getLatLng())
              .anchor(MARKER_X_ANCHOR, MARKER_Y_ANCHOR).draggable(false).visible(true)
              .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_red_paddle));
          googleMap.addMarker(markerOptions);
          break;
        }
      }
    }

    // Add the start marker
    boolean hasStartMarker = false;
    for (int i = 0; i < locations.size(); i++) {
      CachedLocation cachedLocation = locations.get(i);
      if (cachedLocation.valid) {
        MarkerOptions markerOptions = new MarkerOptions().position(cachedLocation.getLatLng())
            .anchor(MARKER_X_ANCHOR, MARKER_Y_ANCHOR).draggable(false).visible(true)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_green_paddle));
        googleMap.addMarker(markerOptions);
        hasStartMarker = true;
        break;
      }
    }
    return hasStartMarker;
  }

  /**
   * Updates the waypoints.
   * 
   * @param googleMap the google map.
   */
  private void updateWaypoints(GoogleMap googleMap) {
    synchronized (waypoints) {
      for (Waypoint waypoint : waypoints) {
        Location location = waypoint.getLocation();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        int drawableId = waypoint.getType() == WaypointType.STATISTICS 
            ? R.drawable.ic_marker_yellow_pushpin : R.drawable.ic_marker_blue_pushpin;
        MarkerOptions markerOptions = new MarkerOptions().position(latLng)
            .anchor(WAYPOINT_X_ANCHOR, WAYPOINT_Y_ANCHOR).draggable(false).visible(true)
            .icon(BitmapDescriptorFactory.fromResource(drawableId))
            .title(String.valueOf(waypoint.getId()));
        googleMap.addMarker(markerOptions);
      }
    }
  }
}
