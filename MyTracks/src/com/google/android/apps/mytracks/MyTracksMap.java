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

import com.google.android.apps.mymaps.GeoRect;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackPointsColumns;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.MyTracksUtils;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The map view activity of the MyTracks application.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracksMap extends MapActivity
    implements LocationListener, SensorEventListener, View.OnTouchListener,
    View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

  // Saved instance state keys:
  // ---------------------------

  public static final String KEY_CURRENT_LOCATION = "currentLocation";
  public static final String KEY_KEEP_MY_LOCATION_VISIBLE =
      "keepMyLocationVisible";
  public static final String KEY_HAVE_GOOD_FIX = "haveGoodFix";

  /**
   * The currently selected track (or null if nothing selected).
   */
  private Track selectedTrack;

  /**
   * The id of the currently recording track.
   */
  private long recordingTrackId = -1;

  /**
   * True if the map should be scrolled so that the pointer is always in the
   * visible area.
   */
  private boolean keepMyLocationVisible;

  /**
   * Id of the last location that was seen when reading tracks from the
   * provider. This is used to determine which locations are new compared to the
   * last time the mapOverlay was updated.
   */
  private long lastSeenLocationId = -1;

  /**
   * Magnetic variation.
   */
  private double variation;

  /**
   * From the shared preferences:
   */
  private int minRequiredAccuracy =
      MyTracksSettings.DEFAULT_MIN_REQUIRED_ACCURACY;

  /**
   * True, if the application thinks it has a good fix, i.e. accuracy is better
   * than the required accuracy.
   */
  private boolean haveGoodFix;

  /**
   * The current pointer location.
   */
  private Location currentLocation;

  /**
   * A thread with a looper. Post to updateTrackHandler to execute Runnables on
   * this thread.
   */
  private final HandlerThread updateTrackThread =
      new HandlerThread("updateTrackThread");

  /** Handler for updateTrackThread */
  private Handler updateTrackHandler;

  private MyTracksProviderUtils providerUtils;

  /**
   * A runnable that updates the track from the provider (looking for points
   * added after "lastSeenLocationId".
   */
  private final Runnable updateTrackRunnable = new Runnable() {
    @Override
    public void run() {
      if (selectedTrack == null) {
        return;
      }
      Cursor cursor = null;
      try {
        cursor = providerUtils.getLocationsCursor(
            recordingTrackId,
            lastSeenLocationId + 1,
            MyTracksConstants.MAX_DISPLAYED_TRACK_POINTS
                - selectedTrack.getLocations().size(),
            true);
        if (cursor != null) {
          if (cursor.moveToLast()) {
            final int idColumnIdx =
                cursor.getColumnIndexOrThrow(TrackPointsColumns._ID);
            do {
              lastSeenLocationId = cursor.getLong(idColumnIdx);
              Location location = providerUtils.createLocation(cursor);
              if (location != null) {
                selectedTrack.addLocation(location);
              }
            } while (cursor.moveToPrevious());
          }
        }
      } catch (RuntimeException e) {
        Log.w(MyTracksConstants.TAG, "Caught an unexpected exception: ", e);
      } finally {
        if (cursor != null) {
          cursor.close();
        }
        mapView.postInvalidate();
      }
    }
  };

  /**
   * A runnable that restores all track points from the provider.
   */
  private Runnable restoreTrackRunnable = new Runnable() {
    @Override
    public void run() {
      if (selectedTrack == null) {
        return;
      }
      lastSeenLocationId = providerUtils.getTrackPoints(selectedTrack,
          MyTracksConstants.MAX_DISPLAYED_TRACK_POINTS);
      mapView.postInvalidate();
    }
  };

  /**
   * A runnable that restores all waypoints from the provider.
   */
  private final Runnable restoreWaypointsRunnable = new Runnable() {
    @Override
    public void run() {
      if (selectedTrack == null) {
        return;
      }

      Cursor cursor = null;
      mapOverlay.clearWaypoints();
      try {
        // We will silently drop extra waypoints to make the app responsive.
        // TODO: Try to only load the waypoints in the view port.
        cursor = providerUtils.getWaypointsCursor(
            selectedTrack.getId(), 0,
            MyTracksConstants.MAX_DISPLAYED_WAYPOINTS_POINTS);
        if (cursor != null) {
          if (cursor.moveToFirst()) {
            do {
              Waypoint wpt = providerUtils.createWaypoint(cursor);
              mapOverlay.addWaypoint(wpt);
            } while (cursor.moveToNext());
          }
        }
      } catch (RuntimeException e) {
        Log.w(MyTracksConstants.TAG, "Caught an unexpected exception.", e);
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
      mapView.postInvalidate();
    }
  };

  /**
   * A runnable intended to be posted to the updateTrackThread after the
   * selected track changes. It will post to the ui thread to update the screen
   * elements and move the map to show the selected track.
   */
  private final Runnable setSelectedTrackRunnable = new Runnable() {
    @Override
    public void run() {
      uiHandler.post(new Runnable() {
        public void run() {
          showTrack(selectedTrack);
          mapOverlay.setSelectedTrack(selectedTrack);
          mapOverlay.setShowEndMarker(selectedTrack == null ||
              selectedTrack.getId() != recordingTrackId);
          mapView.invalidate();
          busyPane.setVisibility(View.GONE);
          updateOptionsButton();
        }
      });
    }
  };

  // UI elements:
  // -------------

  private RelativeLayout screen;
  private MapView mapView;
  private MyTracksOverlay mapOverlay;
  private LinearLayout messagePane;
  private TextView messageText;
  private LinearLayout busyPane;
  private ImageButton optionsBtn;

  private MenuItem myLocation;
  private MenuItem toggleLayers;

  private SensorManager sensorManager;
  private LocationManager locationManager;
  private ContentObserver observer;
  private ContentObserver waypointObserver;

  /** Handler for callbacks to the UI thread */
  private final Handler uiHandler = new Handler();

  /**
   * We are not displaying driving directions. Just an arbitrary track that is
   * not associated to any licensed mapping data. Therefore it should be okay to
   * return false here and still comply with the terms of service.
   */
  @Override
  protected boolean isRouteDisplayed() {
    return false;
  }

  /**
   * We are displaying a location. This needs to return true in order to comply
   * with the terms of service.
   */
  @Override
  protected boolean isLocationDisplayed() {
    return true;
  }

  // Application life cycle:
  // ------------------------

  @Override
  public void onCreate(Bundle bundle) {
    Log.d(MyTracksConstants.TAG, "MyTracksMap.onCreate");
    super.onCreate(bundle);

    providerUtils = MyTracksProviderUtils.Factory.get(this);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    // Inflate the layout:
    setContentView(R.layout.mytracks_layout);

    // Remove the window's background because the MapView will obscure it
    getWindow().setBackgroundDrawable(null);

    // Set up a map overlay:
    screen = (RelativeLayout) findViewById(R.id.screen);
    mapView = (MapView) findViewById(R.id.map);
    mapView.requestFocus();
    mapOverlay = new MyTracksOverlay(this);
    mapView.getOverlays().add(mapOverlay);
    mapView.setOnTouchListener(this);
    messagePane = (LinearLayout) findViewById(R.id.messagepane);
    messageText = (TextView) findViewById(R.id.messagetext);
    busyPane = (LinearLayout) findViewById(R.id.busypane);
    optionsBtn = (ImageButton) findViewById(R.id.showOptions);

    optionsBtn.setOnCreateContextMenuListener(contextMenuListener);
    optionsBtn.setOnClickListener(this);

    setupZoomControls();
    
    // Get the sensor and location managers:
    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    locationManager =
        (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    updateTrackThread.start();
    updateTrackHandler = new Handler(updateTrackThread.getLooper());

    // Register observer for the track point provider:
    Handler contentHandler = new Handler();
    observer = new ContentObserver(contentHandler) {
      @Override
      public void onChange(boolean selfChange) {
        Log.d(MyTracksConstants.TAG, "MyTracksMap: ContentObserver.onChange");
        // Check for any new locations and append them to the currently
        // recording track:
        if (!MyTracks.getInstance().isRecording()) {
          // No track is being recorded. We should not be here.
          return;
        }
        if (selectedTrack == null
            || selectedTrack.getId() != recordingTrackId) {
          // No track, or one other than the recording track is selected,
          // don't bother.
          return;
        }
        // Update can potentially be lengthy, put it in its own thread:
        updateTrackHandler.post(updateTrackRunnable);
        super.onChange(selfChange);
      }
    };

    waypointObserver = new ContentObserver(contentHandler) {
      @Override
      public void onChange(boolean selfChange) {
        Log.d(MyTracksConstants.TAG,
            "MyTracksMap: ContentObserver.onChange waypoints");
        if (selectedTrack == null) {
          return;
        }
        updateTrackHandler.post(restoreWaypointsRunnable);
        super.onChange(selfChange);
      }
    };

    // Read shared preferences and register change listener:
    SharedPreferences preferences =
        getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (preferences != null) {
      minRequiredAccuracy = preferences.getInt(
          MyTracksSettings.MIN_REQUIRED_ACCURACY,
          MyTracksSettings.DEFAULT_MIN_REQUIRED_ACCURACY);
      recordingTrackId =
          preferences.getLong(MyTracksSettings.RECORDING_TRACK, -1);
      long selectedTrackId =
          preferences.getLong(MyTracksSettings.SELECTED_TRACK, -1);
      if (selectedTrackId >= 0) {
        setSelectedTrack(selectedTrackId);
      }
      updateOptionsButton();
      preferences.registerOnSharedPreferenceChangeListener(this);
    }
  }

  @SuppressWarnings("deprecation")
  protected void setupZoomControls() {
    mapView.setBuiltInZoomControls(true);
  }

  @Override
  protected void onDestroy() {
    // Final cleanup before activity is destroyed.
    // May not be called at all in some situations.
    Log.d(MyTracksConstants.TAG, "MyTracksMap.onDestroy");
    super.onDestroy();
  }

  @Override
  protected void onStart() {
    // Called after onCreate or onStop.
    // Will be followed by onRestart.
    Log.d(MyTracksConstants.TAG, "MyTracksMap.onStart");
    super.onStart();
  }

  @Override
  protected void onStop() {
    // Called when activity is no longer visible to user.
    // Next either onStart, onDestroy or nothing will be called.
    // This method may never be called in low memory situations.
    Log.d(MyTracksConstants.TAG, "MyTracksMap.onStop");
    super.onStop();
  }

  @Override
  protected void onRestart() {
    // Called when the current activity is being re-displayed.
    // Will be followed by onResume.
    Log.d(MyTracksConstants.TAG, "MyTracksMap.onRestart");
    super.onRestart();
  }

  @Override
  protected void onPause() {
    // Called when activity is going into the background, but has not (yet) been
    // killed. Shouldn't block longer than approx. 2 seconds.
    Log.d(MyTracksConstants.TAG, "MyTracksMap.onPause");
    unregisterLocationAndSensorListeners();
    unregisterContentObservers();
    super.onPause();
  }

  @Override
  protected void onResume() {
    // Called when the current activity is being displayed or re-displayed
    // to the user.
    Log.d(MyTracksConstants.TAG, "MyTracksMap.onResume");
    super.onResume();

    // Make sure any updates that might have happened are propagated to the
    // Map overlay:
    observer.onChange(false);
    waypointObserver.onChange(false);

    registerContentObservers();
    registerLocationAndSensorListeners();

    if (locationManager.isProviderEnabled(MyTracksConstants.GPS_PROVIDER)) {
      messageText.setText(R.string.wait_for_fix);
      messagePane.setOnClickListener(null);
    } else {
      messageText.setText(R.string.status_enable_gps);
      messagePane.setVisibility(View.VISIBLE);
      messagePane.setOnClickListener(this);
      screen.requestLayout();
    }

    // While this activity was paused the user may have deleted the selected
    // track. In that case the map overlay needs to be cleared:
    Track track = mapOverlay.getSelectedTrack();
    if (track != null && !providerUtils.trackExists(track.getId())) {
      // The recording track must have been deleted meanwhile.
      mapOverlay.setSelectedTrack(null);
      mapView.invalidate();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    Log.d(MyTracksConstants.TAG, "MyTracksMap.onSaveInstanceState");
    outState.putBoolean(KEY_HAVE_GOOD_FIX, haveGoodFix);
    outState.putBoolean(KEY_KEEP_MY_LOCATION_VISIBLE, keepMyLocationVisible);
    if (currentLocation != null) {
      outState.putParcelable(KEY_CURRENT_LOCATION, currentLocation);
    }
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle bundle) {
    Log.d(MyTracksConstants.TAG, "MyTracksMap.onRestoreInstanceState");
    if (bundle != null) {
      super.onRestoreInstanceState(bundle);
      haveGoodFix = bundle.getBoolean(KEY_HAVE_GOOD_FIX, false);
      keepMyLocationVisible =
          bundle.getBoolean(KEY_KEEP_MY_LOCATION_VISIBLE, false);
      if (bundle.containsKey(KEY_CURRENT_LOCATION)) {
        currentLocation = (Location) bundle.getParcelable(KEY_CURRENT_LOCATION);
        if (currentLocation != null) {
          setVariation(currentLocation);
          showCurrentLocation();
        }
      } else {
        currentLocation = null;
      }
    }
  }

  // Utility functions:
  // -------------------

  /**
   * Toggles between satellite and map view.
   */
  public void toggleLayer() {
    mapView.setSatellite(!mapView.isSatellite());
  }

  /**
   * Registers to receive location updates from the GPS location provider and
   * sensor updated from the compass.
   */
  void registerLocationAndSensorListeners() {
    if (locationManager != null) {
      LocationProvider gpsProvider =
          locationManager.getProvider(MyTracksConstants.GPS_PROVIDER);
      if (gpsProvider == null) {
        alert(getString(R.string.error_no_gps_location_provider));
        return;
      } else {
        Log.d(MyTracksConstants.TAG, "MyTracksMap: Using location provider "
            + gpsProvider.getName());
      }
      locationManager.requestLocationUpdates(gpsProvider.getName(),
          0 /*minTime*/, 0 /*minDist*/, this);
      try {
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
            1000 * 60 * 5 /*minTime*/, 0 /*minDist*/, this);
      } catch (RuntimeException e) {
        // If anything at all goes wrong with getting a cell location do not
        // abort. Cell location is not essential to this app.
        Log.w(MyTracksConstants.TAG,
            "Could not register network location listener.");
      }
    }
    if (sensorManager == null) {
      return;
    }
    Sensor compass = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    if (compass == null) {
      return;
    }
    Log.d(MyTracksConstants.TAG,
        "MyTracksMap: Now registering sensor listeners.");
    sensorManager.registerListener(
        this, compass, SensorManager.SENSOR_DELAY_UI);
  }

  /**
   * Unregisters all location and sensor listeners
   */
  void unregisterLocationAndSensorListeners() {
    if (locationManager != null) {
      Log.d(MyTracksConstants.TAG,
          "MyTracksMap: Now unregistering location listeners.");
      locationManager.removeUpdates(this);
    }
    if (sensorManager != null) {
      Log.d(MyTracksConstants.TAG,
          "MyTracksMap: Now unregistering sensor listeners.");
      sensorManager.unregisterListener(this);
    }
  }

  /**
   * Registers the content observer for the map overlay.
   */
  private void registerContentObservers() {
    getContentResolver().registerContentObserver(
        TrackPointsColumns.CONTENT_URI, false /* notifyForDescendents */,
        observer);
    getContentResolver().registerContentObserver(
        WaypointsColumns.CONTENT_URI, false /* notifyForDescendents */,
        waypointObserver);
  }

  /**
   * Unregisters the content observer for the map overlay.
   */
  private void unregisterContentObservers() {
    getContentResolver().unregisterContentObserver(observer);
    getContentResolver().unregisterContentObserver(waypointObserver);
  }

  /**
   * Shows the options button if a track is selected, or hide it if not.
   */
  private void updateOptionsButton() {
    optionsBtn.setVisibility(
        selectedTrack != null ? View.VISIBLE : View.INVISIBLE);
  }

  /**
   * Tests if a location is visible.
   *
   * @param location a given location
   * @return true if the given location is within the visible map area
   */
  private boolean locationIsVisible(Location location) {
    if (location == null || mapView == null) {
      return false;
    }
    GeoPoint center = mapView.getMapCenter();
    int latSpan = mapView.getLatitudeSpan();
    int lonSpan = mapView.getLongitudeSpan();

    // Bottom of map view is obscured by zoom controls/buttons.
    // Subtract a margin from the visible area:
    GeoPoint marginBottom = mapView.getProjection().fromPixels(
        0, mapView.getHeight());
    GeoPoint marginTop = mapView.getProjection().fromPixels(0,
        mapView.getHeight()
            - mapView.getZoomButtonsController().getZoomControls().getHeight());
    int margin =
        Math.abs(marginTop.getLatitudeE6() - marginBottom.getLatitudeE6());
    GeoRect r = new GeoRect(center, latSpan, lonSpan);
    r.top += margin;

    GeoPoint geoPoint = MyTracksUtils.getGeoPoint(location);
    return r.contains(geoPoint);
  }

  /**
   * Moves the location pointer to the current location and center the map if
   * the current location is outside the visible area.
   */
  private void showCurrentLocation() {
    if (currentLocation == null || mapOverlay == null || mapView == null) {
      return;
    }
    mapOverlay.setMyLocation(currentLocation);
    mapView.invalidate();
    if (keepMyLocationVisible && !locationIsVisible(currentLocation)) {
      MapController controller = mapView.getController();
      GeoPoint geoPoint = MyTracksUtils.getGeoPoint(currentLocation);
      controller.animateTo(geoPoint);
    }
  }

  /**
   * Zooms and pans the map so that the given track is visible.
   *
   * @param track a given track
   */
  public void showTrack(Track track) {
    if (track == null || mapView == null || track.getNumberOfPoints() < 2) {
      return;
    }
    TripStatistics stats = track.getStatistics();
    int latSpanE6 = stats.getTop() - stats.getBottom();
    int lonSpanE6 = stats.getRight() - stats.getLeft();
    if (latSpanE6 > 0 && latSpanE6 < 180E6 && lonSpanE6 > 0
        && lonSpanE6 < 180E6) {
      keepMyLocationVisible = false;
      GeoPoint center = new GeoPoint(
          stats.getBottom() + latSpanE6 / 2,
          stats.getLeft() + lonSpanE6 / 2);
      if (MyTracksUtils.isValidGeoPoint(center)) {
        mapView.getController().setCenter(center);
        mapView.getController().zoomToSpan(latSpanE6, lonSpanE6);
      }
    }
  }

  /**
   * Zooms and pans the map so that the given waypoint is visible.
   */
  public void showWaypoint(long waypointId) {
    Waypoint wpt = providerUtils.getWaypoint(waypointId);
    if (wpt != null && wpt.getLocation() != null) {
      keepMyLocationVisible = false;
      GeoPoint center = new GeoPoint(
          (int) (wpt.getLocation().getLatitude() * 1E6),
          (int) (wpt.getLocation().getLongitude() * 1E6));
      mapView.getController().setCenter(center);
      mapView.getController().setZoom(20);
      mapView.invalidate();
    }
  }

  /**
   * Sets the selected track and zoom and pan the map so that it is visible.
   *
   * @param trackId a given track id
   */
  public void setSelectedTrack(final long trackId) {
    if (selectedTrack != null && selectedTrack.getId() == trackId) {
      // Selected track did not change, nothing to do.
      mapOverlay.setSelectedTrack(selectedTrack);
      mapView.invalidate();
      updateOptionsButton();
      return;
    }
    if (trackId < 0) {
      // Remove selection.
      selectedTrack = null;
      mapOverlay.setSelectedTrack(null);
      mapOverlay.clearWaypoints();
      updateOptionsButton();
      mapView.invalidate();
      return;
    }
    busyPane.setVisibility(View.VISIBLE);
    selectedTrack = providerUtils.getTrack(trackId);
    updateTrackHandler.post(restoreTrackRunnable);
    updateTrackHandler.post(restoreWaypointsRunnable);
    updateTrackHandler.post(setSelectedTrackRunnable);
  }

  /**
   * Displays an alert for a few seconds.
   *
   * @param txt The text to be displayed
   */
  public void alert(String txt) {
    Toast.makeText(this, txt, Toast.LENGTH_LONG).show();
  }

  public void launchMyLocationSettings() {
    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
  }

  public void setVariation(Location location) {
    long timestamp = location.getTime();
    if (timestamp == 0) {
      // Hack for Samsung phones which don't populate the time field
      timestamp = System.currentTimeMillis();
    }

    GeomagneticField field = new GeomagneticField(
        (float) location.getLatitude(),
        (float) location.getLongitude(),
        (float) location.getAltitude(),
        timestamp);
    variation = field.getDeclination();

    Log.d(MyTracksConstants.TAG,
        "MyTracksMap: Variation reset to " + variation + " degrees.");
  }

  public MyTracksOverlay getMapOverlay() {
    return mapOverlay;
  }

  public MapView getMapView() {
    return mapView;
  }

  // Event listeners:
  // -----------------

  private final OnCreateContextMenuListener contextMenuListener =
      new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
          menu.setHeaderTitle(R.string.tracklist_this_track);
          menu.add(0, MyTracksConstants.MENU_EDIT, 0,
              R.string.tracklist_edit_track);
          if (!MyTracks.getInstance().isRecording() || (selectedTrack != null
              && selectedTrack.getId() != recordingTrackId)) {
            menu.add(0, MyTracksConstants.MENU_SEND_TO_GOOGLE, 0,
                R.string.tracklist_send_to_google);
            SubMenu share = menu.addSubMenu(0, MyTracksConstants.MENU_SHARE, 0,
                R.string.tracklist_share_track);
            share.add(0, MyTracksConstants.MENU_SHARE_LINK, 0,
                R.string.tracklist_share_link);
            share.add(0, MyTracksConstants.MENU_SHARE_GPX_FILE, 0,
                R.string.tracklist_share_gpx_file);
            share.add(0, MyTracksConstants.MENU_SHARE_KML_FILE, 0,
                R.string.tracklist_share_kml_file);
            share.add(0, MyTracksConstants.MENU_SHARE_CSV_FILE, 0,
                R.string.tracklist_share_csv_file);
            SubMenu save = menu.addSubMenu(0,
                MyTracksConstants.MENU_WRITE_TO_SD_CARD, 0,
                R.string.tracklist_write_to_sd);
            save.add(0, MyTracksConstants.MENU_SAVE_GPX_FILE, 0,
                R.string.tracklist_save_as_gpx);
            save.add(0, MyTracksConstants.MENU_SAVE_KML_FILE, 0,
                R.string.tracklist_save_as_kml);
            save.add(0, MyTracksConstants.MENU_SAVE_CSV_FILE, 0,
                R.string.tracklist_save_as_csv);
            menu.add(0, MyTracksConstants.MENU_CLEAR_MAP, 0,
                R.string.tracklist_clear_map);
            menu.add(0, MyTracksConstants.MENU_DELETE, 0,
                R.string.tracklist_delete_track);
          }
        }
      };

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    if (!super.onMenuItemSelected(featureId, item)) {
      if (selectedTrack != null) {
        MyTracks.getInstance().onActivityResult(
            MyTracksConstants.getActionFromMenuId(item.getItemId()), RESULT_OK,
            new Intent());
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    myLocation = menu.add(0, MyTracksConstants.MENU_MY_LOCATION, 0,
        R.string.mylocation);
    myLocation.setIcon(android.R.drawable.ic_menu_mylocation);
    toggleLayers = menu.add(0, MyTracksConstants.MENU_TOGGLE_LAYERS, 0,
        R.string.switch_to_sat);
    toggleLayers.setIcon(android.R.drawable.ic_menu_mapmode);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    toggleLayers.setTitle(mapView.isSatellite() ?
        R.string.switch_to_map : R.string.switch_to_sat);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case MyTracksConstants.MENU_MY_LOCATION: {
        Location loc = MyTracks.getInstance().getCurrentLocation();
        if (loc != null) {
          currentLocation = loc;
          setVariation(currentLocation);
          mapOverlay.setMyLocation(loc);
          mapView.invalidate();
          GeoPoint geoPoint = MyTracksUtils.getGeoPoint(loc);
          MapController controller = mapView.getController();
          controller.animateTo(geoPoint);
          if (mapView.getZoomLevel() < 18) {
            controller.setZoom(18);
          }
          keepMyLocationVisible = true;
        }
        return true;
      }
      case MyTracksConstants.MENU_TOGGLE_LAYERS: {
        toggleLayer();
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onClick(View v) {
    if (v == messagePane) {
      launchMyLocationSettings();
    }
    if (v == optionsBtn) {
      optionsBtn.performLongClick();
    }
  }

  /**
   * We want the pointer to become visible again in case of the next location
   * update:
   */
  @Override
  public boolean onTouch(View view, MotionEvent event) {
    if (keepMyLocationVisible && event.getAction() == MotionEvent.ACTION_MOVE) {
      if (!locationIsVisible(currentLocation)) {
        keepMyLocationVisible = false;
      }
    }
    return false;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key != null) {
      if (key.equals(MyTracksSettings.MIN_REQUIRED_ACCURACY)) {
        minRequiredAccuracy = sharedPreferences.getInt(
            MyTracksSettings.MIN_REQUIRED_ACCURACY,
            MyTracksSettings.DEFAULT_MIN_REQUIRED_ACCURACY);
      } else if (key.equals(MyTracksSettings.SELECTED_TRACK)) {
        long selectedTrackId =
            sharedPreferences.getLong(MyTracksSettings.SELECTED_TRACK, -1);
        setSelectedTrack(selectedTrackId);
      } else if (key.equals(MyTracksSettings.RECORDING_TRACK)) {
        recordingTrackId =
            sharedPreferences.getLong(MyTracksSettings.RECORDING_TRACK, -1);
        if (selectedTrack != null) {
          mapOverlay.setShowEndMarker(
              selectedTrack.getId() != recordingTrackId);
          mapView.postInvalidate();
        }
      }
    }
  }

  @Override
  public void onProviderEnabled(String provider) {
    if (provider.equals(MyTracksConstants.GPS_PROVIDER)) {
      messageText.setText(R.string.wait_for_fix);
    }
  }

  @Override
  public void onProviderDisabled(String provider) {
    if (provider.equals(MyTracksConstants.GPS_PROVIDER)) {
      messageText.setText(R.string.status_enable_gps);
      messagePane.setVisibility(View.VISIBLE);
      messagePane.setOnClickListener(this);
      screen.requestLayout();
    }
  }

  @Override
  public void onLocationChanged(Location location) {
    if (location.getProvider().equals(MyTracksConstants.GPS_PROVIDER)) {
      // Recalculate the variation if there was a jump in location > 1km:
      if (currentLocation == null
          || location.distanceTo(currentLocation) > 1000) {
        setVariation(location);
      }
      currentLocation = location;
      boolean haveGoodFixNow =
          currentLocation.getAccuracy() < minRequiredAccuracy;
      if (haveGoodFixNow != haveGoodFix) {
        haveGoodFix = haveGoodFixNow;
        messagePane.setVisibility(haveGoodFix ? View.GONE : View.VISIBLE);
        screen.requestLayout();
      }
      showCurrentLocation();
    } else {
      Log.d(MyTracksConstants.TAG,
          "MyTracksMap: Network location update received.");
    }
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    if (provider.equals(MyTracksConstants.GPS_PROVIDER)) {
      switch (status) {
        case LocationProvider.OUT_OF_SERVICE:
        case LocationProvider.TEMPORARILY_UNAVAILABLE:
          haveGoodFix = false;
          messagePane.setVisibility(View.VISIBLE);
          screen.requestLayout();
          break;
      }
    }
  }

  @Override
  public void onSensorChanged(SensorEvent se) {
    synchronized (this) {
      float magneticHeading = se.values[0];
      double heading = magneticHeading + variation;
      if (mapOverlay.setHeading((float) heading)) {
        mapView.invalidate();
      }
    }
  }

  @Override
  public void onAccuracyChanged(Sensor s, int accuracy) {
    // do nothing
  }
}
