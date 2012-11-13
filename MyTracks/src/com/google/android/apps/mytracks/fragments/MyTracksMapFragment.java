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

package com.google.android.apps.mytracks.fragments;

import com.google.android.apps.mytracks.MapOverlay;
import com.google.android.apps.mytracks.MarkerDetailActivity;
import com.google.android.apps.mytracks.TrackDetailActivity;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.Factory;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.TrackDataListener;
import com.google.android.apps.mytracks.content.TrackDataType;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.GoogleLocationUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * A fragment to display map to the user.
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class MyTracksMapFragment extends SupportMapFragment implements TrackDataListener {

  public static final String MAP_FRAGMENT_TAG = "mapFragment";

  private static final String CURRENT_LOCATION_KEY = "current_location_key";
  private static final String
      KEEP_CURRENT_LOCATION_VISIBLE_KEY = "keep_current_location_visible_key";
  private static final String ZOOM_TO_CURRENT_LOCATION_KEY = "zoom_to_current_location_key";
  private static final String MAP_TYPE = "map_type";
  
  private static final float DEFAULT_ZOOM_LEVEL = 18f;

  // Google's latitude and longitude
  private static final double DEFAULT_LATITUDE = 37.423;
  private static final double DEFAULT_LONGITUDE = -122.084;

  private static final int MAP_VIEW_PADDING = 32;

  private TrackDataHub trackDataHub;

  // Current location
  private Location currentLocation;

  // True to keep the currentLocation visible
  private boolean keepCurrentLocationVisible;

  // True to zoom to currentLocation when it is available
  private boolean zoomToCurrentLocation;

  private OnLocationChangedListener onLocationChangedListener;

  // For showing a marker
  private long markerTrackId = -1L;
  private long markerId = -1L;

  // Current track
  private Track currentTrack;

  // Current paths
  private ArrayList<Polyline> paths = new ArrayList<Polyline>();
  boolean reloadPaths = true;

  // UI elements
  private GoogleMap googleMap;
  private MapOverlay mapOverlay;
  private View mapView;
  private ImageButton myLocationImageButton;
  private TextView messageTextView;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setHasOptionsMenu(true);
    ApiAdapterFactory.getApiAdapter().invalidMenu(getActivity());
    mapOverlay = new MapOverlay(getActivity());
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mapView = super.onCreateView(inflater, container, savedInstanceState);
    View layout = inflater.inflate(R.layout.map, container, false);
    RelativeLayout mapContainer = (RelativeLayout) layout.findViewById(R.id.map_container);
    mapContainer.addView(mapView, 0);

    myLocationImageButton = (ImageButton) layout.findViewById(R.id.map_my_location);
    myLocationImageButton.setOnClickListener(new View.OnClickListener() {
        @Override
      public void onClick(View v) {
        forceUpdateLocation();
        keepCurrentLocationVisible = true;
        zoomToCurrentLocation = true;
        updateCurrentLocation();
      }
    });
    messageTextView = (TextView) layout.findViewById(R.id.map_message);

    /*
     * onCreateView can be called multiple times. E.g., when the user switches
     * tab. We only want to initialize the camera position if googleMap was
     * null.
     */
    boolean initCamera = googleMap == null;
    
    googleMap = getMap();
    googleMap.setMyLocationEnabled(true);
    googleMap.getUiSettings().setMyLocationButtonEnabled(false);
    googleMap.setIndoorEnabled(true);
    googleMap.setOnMarkerClickListener(new OnMarkerClickListener() {

        @Override
      public boolean onMarkerClick(Marker marker) {
        if (isResumed()) {
          String title = marker.getTitle();
          if (title != null && title.length() > 0) {
            long id = Long.valueOf(title);
            Context context = getActivity();
            Intent intent = IntentUtils.newIntent(context, MarkerDetailActivity.class)
                .putExtra(MarkerDetailActivity.EXTRA_MARKER_ID, id);
            context.startActivity(intent);
          }
        }
        return true;
      }
    });
    googleMap.setLocationSource(new LocationSource() {

        @Override
      public void activate(OnLocationChangedListener listener) {
        onLocationChangedListener = listener;
      }

        @Override
      public void deactivate() {
        onLocationChangedListener = null;
      }
    });
    googleMap.setOnCameraChangeListener(new OnCameraChangeListener() {

        @Override
      public void onCameraChange(CameraPosition cameraPosition) {
        if (isResumed() && keepCurrentLocationVisible && currentLocation != null
            && !isLocationVisible(currentLocation)) {
          keepCurrentLocationVisible = false;
          zoomToCurrentLocation = false;
        }
      }
    });
    if (initCamera) {
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(getDefaultLatLng(), googleMap.getMinZoomLevel()));
    }
    return layout;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (savedInstanceState != null) {
      keepCurrentLocationVisible = savedInstanceState.getBoolean(
          KEEP_CURRENT_LOCATION_VISIBLE_KEY, false);
      zoomToCurrentLocation = savedInstanceState.getBoolean(ZOOM_TO_CURRENT_LOCATION_KEY, false);
      currentLocation = (Location) savedInstanceState.getParcelable(CURRENT_LOCATION_KEY);
      updateCurrentLocation();
      if (googleMap != null) {
        googleMap.setMapType(savedInstanceState.getInt(MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL));
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    resumeTrackDataHub();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (currentLocation != null) {
      outState.putParcelable(CURRENT_LOCATION_KEY, currentLocation);
    }
    outState.putBoolean(KEEP_CURRENT_LOCATION_VISIBLE_KEY, keepCurrentLocationVisible);
    outState.putBoolean(ZOOM_TO_CURRENT_LOCATION_KEY, zoomToCurrentLocation);
    if (googleMap != null) {
      outState.putInt(MAP_TYPE, googleMap.getMapType());
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    pauseTrackDataHub();
  }

  /**
   * Shows the marker on the map.
   * 
   * @param trackId the track id
   * @param id the marker id
   */
  public void showMarker(long trackId, long id) {
    /*
     * Synchronize to prevent race condition in changing markerTrackId and
     * markerId variables.
     */
    synchronized (this) {
      if (currentTrack != null && currentTrack.getId() == trackId) {
        showMarker(id);
        markerTrackId = -1L;
        markerId = -1L;
        return;
      }
      markerTrackId = trackId;
      markerId = id;
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflator) {
    menuInflator.inflate(R.menu.map, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    if (googleMap != null) {
      int id;
      switch (googleMap.getMapType()) {
        case GoogleMap.MAP_TYPE_NORMAL:
          id = R.id.menu_map;
          break;
        case GoogleMap.MAP_TYPE_SATELLITE:
          id = R.id.menu_satellite;
          break;
        case GoogleMap.MAP_TYPE_HYBRID:
          id = R.id.menu_satellite_with_streets;
          break;
        case GoogleMap.MAP_TYPE_TERRAIN:
          id = R.id.menu_terrain;
          break;
        default:
          id = R.id.menu_map;
      }
      MenuItem menuItem = menu.findItem(id);
      if (menuItem != null) {
        menuItem.setChecked(true);
      }
    }
    super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem menuItem) {
    int type = GoogleMap.MAP_TYPE_NORMAL;
    switch (menuItem.getItemId()) {
      case R.id.menu_map:
        type = GoogleMap.MAP_TYPE_NORMAL;
        break;
      case R.id.menu_satellite:
        type = GoogleMap.MAP_TYPE_SATELLITE;
        break;
      case R.id.menu_satellite_with_streets:
        type = GoogleMap.MAP_TYPE_HYBRID;
        break;
      case R.id.menu_terrain:
        type = GoogleMap.MAP_TYPE_TERRAIN;
        break;
      default:
        return super.onOptionsItemSelected(menuItem);
    }
    if (googleMap != null) {
      googleMap.setMapType(type);
      menuItem.setChecked(true);
    }
    return true;
  }

  @Override
  public void onLocationStateChanged(final LocationState state) {
    if (isResumed()) {
      getActivity().runOnUiThread(new Runnable() {
          @Override
        public void run() {
          if (!isResumed() || googleMap == null) {
            return;
          }
          boolean myLocationEnabled = true;
          if (state == LocationState.DISABLED) {
            currentLocation = null;
            myLocationEnabled = false;
          }
          googleMap.setMyLocationEnabled(myLocationEnabled);

          String message;
          boolean isGpsDisabled;
          if (!isSelectedTrackRecording()) {
            message = null;
            isGpsDisabled = false;
          } else {
            switch (state) {
              case DISABLED:
                String setting = getString(GoogleLocationUtils.isAvailable(getActivity())
                    ? R.string.gps_google_location_settings
                    : R.string.gps_location_access);
                message = getString(R.string.gps_disabled, setting);
                isGpsDisabled = true;
                break;
              case NO_FIX:
                message = getString(R.string.gps_wait_for_signal);
                isGpsDisabled = false;
                break;
              case BAD_FIX:
                message = getString(R.string.gps_wait_for_better_signal);
                isGpsDisabled = false;
                break;
              case GOOD_FIX:
                message = null;
                isGpsDisabled = false;
                break;
              default:
                throw new IllegalArgumentException("Unexpected state: " + state);
            }
          }
          if (message == null) {
            messageTextView.setVisibility(View.GONE);
            return;
          }
          messageTextView.setText(message);
          messageTextView.setVisibility(View.VISIBLE);
          if (isGpsDisabled) {
            Toast.makeText(getActivity(), R.string.gps_not_found, Toast.LENGTH_LONG).show();

            // Click to show the location source settings
            messageTextView.setOnClickListener(new OnClickListener() {

                @Override
              public void onClick(View v) {
                Intent intent = GoogleLocationUtils.isAvailable(getActivity()) ? new Intent(
                    GoogleLocationUtils.ACTION_GOOGLE_LOCATION_SETTINGS)
                    : new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
              }
            });
          } else {
            messageTextView.setOnClickListener(null);
          }
        }
      });
    }
  }

  @Override
  public void onLocationChanged(final Location location) {
    if (isResumed()) {
      if (isSelectedTrackRecording() && currentLocation == null && location != null) {
        zoomToCurrentLocation = true;
      }
      currentLocation = location;
      updateCurrentLocation();
    }
  }

  @Override
  public void onHeadingChanged(double heading) {
    // We don't care.
  }

  @Override
  public void onSelectedTrackChanged(final Track track) {
    if (isResumed()) {
      currentTrack = track;
      boolean hasTrack = track != null;
      if (hasTrack) {
        mapOverlay.setShowEndMarker(!isSelectedTrackRecording());
        synchronized (this) {
          /*
           * Synchronize to prevent race condition in changing markerTrackId and
           * markerId variables.
           */
          if (track.getId() == markerTrackId) {
            // Show the marker
            showMarker(markerId);

            markerTrackId = -1L;
            markerId = -1L;
          } else {
            // Show the track
            showTrack();
          }
        }
      }
    }
  }

  @Override
  public void onTrackUpdated(Track track) {
    // We don't care.
  }

  @Override
  public void clearTrackPoints() {
    if (isResumed()) {
      mapOverlay.clearPoints();
      reloadPaths = true;
    }
  }

  @Override
  public void onSampledInTrackPoint(final Location location) {
    if (isResumed()) {
      mapOverlay.addLocation(location);
    }
  }

  @Override
  public void onSampledOutTrackPoint(Location location) {
    // We don't care.
  }

  @Override
  public void onSegmentSplit(Location location) {
    if (isResumed()) {
      mapOverlay.addSegmentSplit();
    }
  }

  @Override
  public void onNewTrackPointsDone() {
    if (isResumed()) {
      getActivity().runOnUiThread(new Runnable() {
        public void run() {
          if (isResumed() && googleMap != null) {
            mapOverlay.update(googleMap, paths, reloadPaths);
            reloadPaths = false;
          }
        }
      });
    }
  }

  @Override
  public void clearWaypoints() {
    if (isResumed()) {
      mapOverlay.clearWaypoints();
    }
  }

  @Override
  public void onNewWaypoint(Waypoint waypoint) {
    if (isResumed() && waypoint != null && LocationUtils.isValidLocation(waypoint.getLocation())) {
      mapOverlay.addWaypoint(waypoint);
    }
  }

  @Override
  public void onNewWaypointsDone() {
    if (isResumed()) {
      getActivity().runOnUiThread(new Runnable() {
        public void run() {
          if (isResumed() && googleMap != null) {
            mapOverlay.update(googleMap, paths, true);
          }
        }
      });
    }
  }

  @Override
  public boolean onMetricUnitsChanged(boolean metric) {
    // We don't care.
    return false;
  }

  @Override
  public boolean onReportSpeedChanged(boolean reportSpeed) {
    // We don't care.
    return false;
  }

  @Override
  public boolean onMinRecordingDistanceChanged(int minRecordingDistance) {
    // We don't care.
    return false;
  }

  /**
   * Resumes the trackDataHub. Needs to be synchronized because the trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized void resumeTrackDataHub() {
    trackDataHub = ((TrackDetailActivity) getActivity()).getTrackDataHub();
    trackDataHub.registerTrackDataListener(this, EnumSet.of(TrackDataType.SELECTED_TRACK,
        TrackDataType.WAYPOINTS_TABLE, TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE,
        TrackDataType.LOCATION));
  }

  /**
   * Pauses the trackDataHub. Needs to be synchronized because the trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized void pauseTrackDataHub() {
    if (trackDataHub != null) {
      trackDataHub.unregisterTrackDataListener(this);
    }
    trackDataHub = null;
  }

  /**
   * Returns true if the selected track is recording. Needs to be synchronized
   * because the trackDataHub can be accessed by multiple threads.
   */
  private synchronized boolean isSelectedTrackRecording() {
    return trackDataHub != null && trackDataHub.isSelectedTrackRecording();
  }

  /**
   * Forces update location. Needs to be synchronized because the trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized void forceUpdateLocation() {
    if (trackDataHub != null) {
      trackDataHub.forceUpdateLocation();
    }
  }

  /**
   * Updates the current location and zoom to it if necessary.
   */
  private void updateCurrentLocation() {
    getActivity().runOnUiThread(new Runnable() {
      public void run() {
        if (!isResumed() || googleMap == null || onLocationChangedListener == null
            || currentLocation == null) {
          return;
        }
        onLocationChangedListener.onLocationChanged(currentLocation);
        if (zoomToCurrentLocation
            || (keepCurrentLocationVisible && !isLocationVisible(currentLocation))) {
          LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
          googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL));
          zoomToCurrentLocation = false;
        }
      };
    });
  }

  /**
   * Sets the camera over a track.
   */
  private void showTrack() {
    getActivity().runOnUiThread(new Runnable() {
        @Override
      public void run() {
        if (!isResumed() || googleMap == null || currentTrack == null
            || currentTrack.getNumberOfPoints() < 2) {
          return;
        }

        if (mapView == null || mapView.getWidth() == 0 || mapView.getHeight() == 0) {
          return;
        }

        TripStatistics tripStatistics = currentTrack.getTripStatistics();
        int latitudeSpanE6 = tripStatistics.getTop() - tripStatistics.getBottom();
        int longitudeSpanE6 = tripStatistics.getRight() - tripStatistics.getLeft();
        if (latitudeSpanE6 > 0 && latitudeSpanE6 < 180E6 && longitudeSpanE6 > 0
            && longitudeSpanE6 < 360E6) {
          LatLng southWest = new LatLng(
              tripStatistics.getBottomDegrees(), tripStatistics.getLeftDegrees());
          LatLng northEast = new LatLng(
              tripStatistics.getTopDegrees(), tripStatistics.getRightDegrees());
          LatLngBounds bounds = LatLngBounds.builder()
              .include(southWest).include(northEast).build();
          CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(
              bounds, mapView.getWidth(), mapView.getHeight(), MAP_VIEW_PADDING);
          googleMap.moveCamera(cameraUpdate);
        }
      }
    });
  }

  /**
   * Sets the camera over a marker.
   * 
   * @param id the marker id
   */
  private void showMarker(final long id) {
    getActivity().runOnUiThread(new Runnable() {
        @Override
      public void run() {
        if (!isResumed() || googleMap == null) {
          return;
        }
        MyTracksProviderUtils MyTracksProviderUtils = Factory.get(getActivity());
        Waypoint waypoint = MyTracksProviderUtils.getWaypoint(id);
        if (waypoint != null && waypoint.getLocation() != null) {
          Location location = waypoint.getLocation();
          LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
          keepCurrentLocationVisible = false;
          zoomToCurrentLocation = false;
          CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL);
          googleMap.moveCamera(cameraUpdate);
        }
      }
    });
  }

  /**
   * Gets the default LatLng.
   */
  private LatLng getDefaultLatLng() {
    MyTracksProviderUtils myTracksProviderUtils = MyTracksProviderUtils.Factory.get(getActivity());
    Location location = myTracksProviderUtils.getLastValidTrackPoint();
    if (location != null) {
      return new LatLng(location.getLatitude(), location.getLongitude());
    } else {
      return new LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
    }
  }

  /**
   * Returns true if the location is visible. Needs to run on the UI thread.
   * 
   * @param location the location
   */
  private boolean isLocationVisible(Location location) {
    if (location == null || googleMap == null) {
      return false;
    }
    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
    return googleMap.getProjection().getVisibleRegion().latLngBounds.contains(latLng);
  }
}