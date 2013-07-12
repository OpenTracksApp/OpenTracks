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
import com.google.android.apps.mytracks.services.MyTracksLocationManager;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.GoogleLocationUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.gms.location.LocationListener;
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
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
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

  private static final float DEFAULT_ZOOM_LEVEL = 18f;

  // Google's latitude and longitude
  private static final double DEFAULT_LATITUDE = 37.423;
  private static final double DEFAULT_LONGITUDE = -122.084;

  private static final int MAP_VIEW_PADDING = 32;

  // States from TrackDetailActivity, set in onResume
  private long trackId;
  private long markerId;
  private TrackDataHub trackDataHub;
  
  // Current location
  private Location currentLocation;
  private Location lastTrackPoint;
  private int recordingGpsAccuracy = PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT;

  /**
   * True to continue keeping the current location visible on the screen.
   * <p>
   * Set to true when <br>
   * 1. user clicks on the my location button <br>
   * 2. first location during a recording <br>
   * Set to false when <br>
   * 1. showing a marker <br>
   * 2. user manually zooms/pans
   */
  private boolean keepCurrentLocationVisible;

  private OnLocationChangedListener onLocationChangedListener;

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
    mapOverlay = new MapOverlay(getActivity());
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mapView = super.onCreateView(inflater, container, savedInstanceState);
    View layout = inflater.inflate(R.layout.map, container, false);
    RelativeLayout mapContainer = (RelativeLayout) layout.findViewById(R.id.map_container);
    mapContainer.addView(mapView, 0);

    /*
     * For Froyo (2.2) and Gingerbread (2.3), need a transparent FrameLayout on
     * top for view pager to work correctly.
     */
    FrameLayout frameLayout = new FrameLayout(getActivity());
    frameLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    mapContainer.addView(frameLayout,
        new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));  
    
    myLocationImageButton = (ImageButton) layout.findViewById(R.id.map_my_location);
    myLocationImageButton.setOnClickListener(new View.OnClickListener() {
        @Override
      public void onClick(View v) {
        final MyTracksLocationManager myTracksLocationManager = new MyTracksLocationManager(
            getActivity(), Looper.myLooper(), true);
        if (!myTracksLocationManager.isAllowed()) {
          String setting = getString(
              GoogleLocationUtils.isAvailable(getActivity()) ? R.string.gps_google_location_settings
                  : R.string.gps_location_access);
          Toast.makeText(
              getActivity(), getString(R.string.my_location_no_gps, setting), Toast.LENGTH_LONG)
              .show();
          myTracksLocationManager.close();
        } else {
          myTracksLocationManager.requestLastLocation(new LocationListener() {
              @Override
            public void onLocationChanged(Location location) {
              myTracksLocationManager.close();
              keepCurrentLocationVisible = true;
              setCurrentLocation(location);
              updateCurrentLocation(true);
            }
          });
        }
      }
    });
    messageTextView = (TextView) layout.findViewById(R.id.map_message);
    return layout;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (savedInstanceState != null) {
      keepCurrentLocationVisible = savedInstanceState.getBoolean(
          KEEP_CURRENT_LOCATION_VISIBLE_KEY, false);
      if (keepCurrentLocationVisible) {
        Location location = (Location) savedInstanceState.getParcelable(CURRENT_LOCATION_KEY);
        if (location != null) {
          setCurrentLocation(location);
        }
      }
    }
    
    /*
     * At this point, after onCreateView, getMap will not return null and we can
     * initialize googleMap. However, onActivityCreated can be called multiple
     * times, e.g., when the user switches tabs. With
     * GoogleMapOptions.useViewLifecycleInFragment == false, googleMap lifecycle
     * is tied to the fragment lifecycle and the same googleMap object is
     * returned in getMap. Thus we only need to initialize googleMap once, when
     * it is null.
     */
    if (googleMap == null) {
      googleMap = getMap();
      googleMap.setMyLocationEnabled(true);

      /*
       * My Tracks needs to handle the onClick event when the my location button
       * is clicked. Currently, the API doesn't allow handling onClick event,
       * thus hiding the default my location button and providing our own.
       */
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
          }
        }
      });
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    // First obtain the states from TrackDetailActivity
    trackId = ((TrackDetailActivity) getActivity()).getTrackId();
    markerId = ((TrackDetailActivity) getActivity()).getMarkerId();
    resumeTrackDataHub();

    MyTracksLocationManager myTracksLocationManager = new MyTracksLocationManager(
        getActivity(), Looper.myLooper(), false);
    boolean isGpsProviderEnabled = myTracksLocationManager.isGpsProviderEnabled();
    myTracksLocationManager.close();

    if (googleMap != null) {

      // Update map type
      int mapType = PreferencesUtils.getInt(
          getActivity(), R.string.map_type_key, PreferencesUtils.MAP_TYPE_DEFAUlT);
      googleMap.setMapType(mapType);
      ApiAdapterFactory.getApiAdapter().invalidMenu(getActivity());

      // Disable my location if gps is disabled
      googleMap.setMyLocationEnabled(isGpsProviderEnabled);
    }

    // setWarningMessage depends on resumeTrackDataHub being invoked beforehand
    setWarningMessage(isGpsProviderEnabled);
    
    currentTrack = MyTracksProviderUtils.Factory.get(getActivity()).getTrack(trackId);
    mapOverlay.setShowEndMarker(!isSelectedTrackRecording());
    if (markerId != -1L) {
      showMarker(markerId);
    } else {
      if (keepCurrentLocationVisible && currentLocation != null && isSelectedTrackRecording()) {
        updateCurrentLocation(true);
      } else {
        showTrack();
      }      
    }   
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putBoolean(KEEP_CURRENT_LOCATION_VISIBLE_KEY, keepCurrentLocationVisible);
    if (currentLocation != null) {
      /*
       * currentLocation is a MyTracksLocation object, which cannot be
       * unmarshalled. Thus creating a Location object before placing it in the
       * bundle.
       */
      outState.putParcelable(CURRENT_LOCATION_KEY, new Location(currentLocation));
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    pauseTrackDataHub();
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
      PreferencesUtils.setInt(getActivity(), R.string.map_type_key, type);
    }
    return true;
  }

  @Override
  public void onTrackUpdated(Track track) {
    currentTrack = track;
  }

  @Override
  public void clearTrackPoints() {
    lastTrackPoint = null;
    if (isResumed()) {
      mapOverlay.clearPoints();
      reloadPaths = true;
    }
  }

  @Override
  public void onSampledInTrackPoint(final Location location) {
    lastTrackPoint = location;
    if (isResumed()) {
      mapOverlay.addLocation(location);
    }
  }

  @Override
  public void onSampledOutTrackPoint(Location location) {
    lastTrackPoint = location;
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
          if (isResumed() && googleMap != null && currentTrack != null) {
            boolean hasStartMarker = mapOverlay.update(
                googleMap, paths, currentTrack.getTripStatistics(), reloadPaths);

            /*
             * If has the start marker, then don't need to reload the paths each
             * time
             */
            if (hasStartMarker) {
              reloadPaths = false;
            }

            if (lastTrackPoint != null && isSelectedTrackRecording()) {
              boolean firstLocation = setCurrentLocation(lastTrackPoint);
              if (firstLocation) {
                keepCurrentLocationVisible = true;
              }
              updateCurrentLocation(firstLocation);
              setWarningMessage(true);
            }
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
          if (isResumed() && googleMap != null && currentTrack != null) {
            mapOverlay.update(googleMap, paths, currentTrack.getTripStatistics(), true);
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
  public boolean onRecordingGpsAccuracy(int newValue) {
    recordingGpsAccuracy = newValue;
    return false;
  }

  @Override
  public boolean onRecordingDistanceIntervalChanged(int minRecordingDistance) {
    // We don't care.
    return false;
  }

  /**
   * Resumes the trackDataHub. Needs to be synchronized because the trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized void resumeTrackDataHub() {
    trackDataHub = ((TrackDetailActivity) getActivity()).getTrackDataHub();
    trackDataHub.registerTrackDataListener(this, EnumSet.of(TrackDataType.TRACKS_TABLE,
        TrackDataType.WAYPOINTS_TABLE, TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE,
        TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE, TrackDataType.PREFERENCE));
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
   * Sets the current location.
   * 
   * @param location the location
   * @return true if this is the first location  
   */
  private boolean setCurrentLocation(Location location) {
    boolean firstLocation = false;
    if (currentLocation == null && location != null) {
      firstLocation = true;
    }
    currentLocation = location;
    return firstLocation;
  }

  /**
   * Updates the current location.
   * 
   * @param forceZoom true to force zoom to the current location regardless of
   *          the keepCurrentLocationVisible policy
   */
  private void updateCurrentLocation(final boolean forceZoom) {
    getActivity().runOnUiThread(new Runnable() {
      public void run() {
        if (!isResumed() || googleMap == null || onLocationChangedListener == null
            || currentLocation == null) {
          return;
        }
        onLocationChangedListener.onLocationChanged(currentLocation);
        if (forceZoom || (keepCurrentLocationVisible && !isLocationVisible(currentLocation))) {
          LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
          googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL));
        }
      };
    });
  }

  /**
   * Shows the current track by moving the camera over the track.
   */
  private void showTrack() {
    if (googleMap == null || currentTrack == null) {
      return;
    }
    if (currentTrack.getNumberOfPoints() < 2) {
      googleMap.moveCamera(
          CameraUpdateFactory.newLatLngZoom(getDefaultLatLng(), googleMap.getMinZoomLevel()));
      return;
    }

    if (mapView == null) {
      return;
    }
    
    if (mapView.getWidth() == 0 || mapView.getHeight() == 0) {
      if (mapView.getViewTreeObserver().isAlive()) {
        mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
          public void onGlobalLayout() {
            ApiAdapterFactory.getApiAdapter()
                .removeGlobalLayoutListener(mapView.getViewTreeObserver(), this);
            getActivity().runOnUiThread(new Runnable() {
                @Override
              public void run() {
                if (isResumed()) {
                  moveCameraOverTrack();
                }
              }
            });
          }
        });
      }
      return;
    }
    moveCameraOverTrack();
  }

  /**
   * Moves the camera over the current track.
   */
  private void moveCameraOverTrack() {
    /**
     * Check all the required variables.
     */
    if (googleMap == null || currentTrack == null || currentTrack.getNumberOfPoints() < 2
        || mapView == null || mapView.getWidth() == 0 || mapView.getHeight() == 0) {
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
      LatLngBounds bounds = LatLngBounds.builder().include(southWest).include(northEast).build();

      /**
       * Note cannot call CameraUpdateFactory.newLatLngBounds(LatLngBounds
       * bounds, int padding) if the map view has not undergone layout. Thus
       * calling CameraUpdateFactory.newLatLngBounds(LatLngBounds bounds, int
       * width, int height, int padding) after making sure that mapView is valid
       * in the above code.
       */
      CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(
          bounds, mapView.getWidth(), mapView.getHeight(), MAP_VIEW_PADDING);
      googleMap.moveCamera(cameraUpdate);
    }
  }

  /**
   * Shows a marker by moving the camera over the marker.
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
    }
    return new LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
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

  /**
   * Sets the warning message.
   * 
   * @param isGpsProviderEnabled true if gps provider is enabled
   */
  private void setWarningMessage(boolean isGpsProviderEnabled) {
    String message;
    if (!isSelectedTrackRecording()) {
      message = null;
    } else {
      if (isGpsProviderEnabled) {
        boolean hasFix;
        boolean hasGoodFix;
        if (currentLocation == null) {
          hasFix = false;
          hasGoodFix = false;
        } else {
          hasFix = !LocationUtils.isLocationOld(currentLocation);
          hasGoodFix = currentLocation.hasAccuracy()
              && currentLocation.getAccuracy() < recordingGpsAccuracy;
        }
        if (!hasFix) {
          message = getString(R.string.gps_wait_for_signal);
        } else if (!hasGoodFix) {
          message = getString(R.string.gps_wait_for_better_signal);
        } else {
          message = null;
        }
      } else {
        String setting = getString(
            GoogleLocationUtils.isAvailable(getActivity()) ? R.string.gps_google_location_settings
                : R.string.gps_location_access);
        message = getString(R.string.gps_disabled, setting);
      }
    }

    if (message == null) {
      messageTextView.setVisibility(View.GONE);
      return;
    }
    messageTextView.setText(message);
    messageTextView.setVisibility(View.VISIBLE);
    if (isGpsProviderEnabled) {
      messageTextView.setOnClickListener(null);
    } else {
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
    }
  }
}