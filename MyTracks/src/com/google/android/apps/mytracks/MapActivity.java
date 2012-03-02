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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.TrackDataHub.ListenerDataType;
import com.google.android.apps.mytracks.content.TrackDataListener;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.file.SaveActivity;
import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.apps.mytracks.io.sendtogoogle.UploadServiceChooserActivity;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.GeoRect;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.PlayTrackUtils;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
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

import java.util.EnumSet;

/**
 * The map view activity of the MyTracks application.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class MapActivity extends com.google.android.maps.MapActivity
    implements View.OnTouchListener, View.OnClickListener,
        TrackDataListener {

  private static final int DIALOG_INSTALL_EARTH = 0;

  // Saved instance state keys:
  // ---------------------------

  private static final String KEY_CURRENT_LOCATION = "currentLocation";
  private static final String KEY_KEEP_MY_LOCATION_VISIBLE = "keepMyLocationVisible";

  private TrackDataHub dataHub;

  /**
   * True if the map should be scrolled so that the pointer is always in the
   * visible area.
   */
  private boolean keepMyLocationVisible;

  /**
   * The ID of a track on which we want to show a waypoint.
   * The waypoint will be shown as soon as the track is loaded.
   */
  private long showWaypointTrackId;

  /**
   * The ID of a waypoint which we want to show.
   * The waypoint will be shown as soon as its track is loaded.
   */
  private long showWaypointId;

  /**
   * The track that's currently selected.
   * This differs from {@link TrackDataHub#getSelectedTrackId} in that this one is only set after
   * actual track data has been received.
   */
  private long selectedTrackId;

  /**
   * The current pointer location.
   * This is kept to quickly center on it when the user requests.
   */
  private Location currentLocation;

  // UI elements:
  // -------------

  private RelativeLayout screen;
  private MapView mapView;
  private MapOverlay mapOverlay;
  private LinearLayout messagePane;
  private TextView messageText;
  private LinearLayout busyPane;
  private ImageButton optionsBtn;

  private MenuItem myLocation;
  private MenuItem toggleLayers;

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
  protected void onCreate(Bundle bundle) {
    Log.d(TAG, "MapActivity.onCreate");
    super.onCreate(bundle);

    // The volume we want to control is the Text-To-Speech volume
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);

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
    mapOverlay = new MapOverlay(this);
    mapView.getOverlays().add(mapOverlay);
    mapView.setOnTouchListener(this);
    mapView.setBuiltInZoomControls(true);
    messagePane = (LinearLayout) findViewById(R.id.messagepane);
    messageText = (TextView) findViewById(R.id.messagetext);
    busyPane = (LinearLayout) findViewById(R.id.busypane);
    optionsBtn = (ImageButton) findViewById(R.id.showOptions);

    optionsBtn.setOnCreateContextMenuListener(contextMenuListener);
    optionsBtn.setOnClickListener(this);
  }

  @Override
  protected void onRestoreInstanceState(Bundle bundle) {
    Log.d(TAG, "MapActivity.onRestoreInstanceState");
    if (bundle != null) {
      super.onRestoreInstanceState(bundle);
      keepMyLocationVisible =
          bundle.getBoolean(KEY_KEEP_MY_LOCATION_VISIBLE, false);
      if (bundle.containsKey(KEY_CURRENT_LOCATION)) {
        currentLocation = (Location) bundle.getParcelable(KEY_CURRENT_LOCATION);
        if (currentLocation != null) {
          showCurrentLocation();
        }
      } else {
        currentLocation = null;
      }
    }
  }

  @Override
  protected void onResume() {
    Log.d(TAG, "MapActivity.onResume");
    super.onResume();

    dataHub = ((MyTracksApplication) getApplication()).getTrackDataHub();
    dataHub.registerTrackDataListener(this, EnumSet.of(
        ListenerDataType.SELECTED_TRACK_CHANGED,
        ListenerDataType.POINT_UPDATES,
        ListenerDataType.WAYPOINT_UPDATES,
        ListenerDataType.LOCATION_UPDATES,
        ListenerDataType.COMPASS_UPDATES));
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    Log.d(TAG, "MapActivity.onSaveInstanceState");
    outState.putBoolean(KEY_KEEP_MY_LOCATION_VISIBLE, keepMyLocationVisible);
    if (currentLocation != null) {
      outState.putParcelable(KEY_CURRENT_LOCATION, currentLocation);
    }
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onPause() {
    Log.d(TAG, "MapActivity.onPause");

    dataHub.unregisterTrackDataListener(this);
    dataHub = null;

    super.onPause();
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_INSTALL_EARTH:
        return PlayTrackUtils.createInstallEarthDialog(this);
      default:
        return null;
    }
  }

  // Utility functions:
  // -------------------

  /**
   * Shows the options button if a track is selected, or hide it if not.
   */
  private void updateOptionsButton(boolean trackSelected) {
    optionsBtn.setVisibility(
        trackSelected ? View.VISIBLE : View.INVISIBLE);
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

    GeoPoint geoPoint = LocationUtils.getGeoPoint(location);
    return r.contains(geoPoint);
  }

  /**
   * Moves the location pointer to the current location and center the map if
   * the current location is outside the visible area.
   */
  private void showCurrentLocation() {
    if (mapOverlay == null || mapView == null) {
      return;
    }

    mapOverlay.setMyLocation(currentLocation);
    mapView.postInvalidate();

    if (currentLocation != null && keepMyLocationVisible && !locationIsVisible(currentLocation)) {
      GeoPoint geoPoint = LocationUtils.getGeoPoint(currentLocation);
      MapController controller = mapView.getController();
      controller.animateTo(geoPoint);
    }
  }

  @Override
  public void onTrackUpdated(Track track) {
    // We don't care.
  }

  /**
   * Zooms and pans the map so that the given track is visible.
   *
   * @param track the track
   */
  private void zoomMapToBoundaries(Track track) {
    if (mapView == null) {
      return;
    }

    if (track == null || track.getNumberOfPoints() < 2) {
      return;
    }

    TripStatistics stats = track.getStatistics();
    int bottom = stats.getBottom();
    int left = stats.getLeft();
    int latSpanE6 = stats.getTop() - bottom;
    int lonSpanE6 = stats.getRight() - left;
    if (latSpanE6 > 0
        && latSpanE6 < 180E6
        && lonSpanE6 > 0
        && lonSpanE6 < 360E6) {
      keepMyLocationVisible = false;
      GeoPoint center = new GeoPoint(
          bottom + latSpanE6 / 2,
          left + lonSpanE6 / 2);
      if (LocationUtils.isValidGeoPoint(center)) {
        mapView.getController().setCenter(center);
        mapView.getController().zoomToSpan(latSpanE6, lonSpanE6);
      }
    }
  }

  /**
   * Zooms and pans the map so that the given waypoint is visible.
   */
  public void showWaypoint(long waypointId) {
    MyTracksProviderUtils providerUtils = MyTracksProviderUtils.Factory.get(this);
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
   * Zooms and pans the map so that the given waypoint is visible, when the given track is loaded.
   * If the track is already loaded, it does that immediately.
   *
   * @param trackId the ID of the track on which to show the waypoint
   * @param waypointId the ID of the waypoint to show
   */
  public void showWaypoint(long trackId, long waypointId) {
    synchronized (this) {
      if (trackId == selectedTrackId) {
        showWaypoint(waypointId);
        return;
      }

      showWaypointTrackId = trackId;
      showWaypointId = waypointId;
    }
  }

  /**
   * Does the proper zooming/panning for a just-loaded track.
   * This may be either zooming to a waypoint that has been previously selected, or
   * zooming to the whole track.
   *
   * @param track the loaded track
   */
  private void zoomLoadedTrack(Track track) {
    synchronized (this) {
      if (track.getId() == showWaypointTrackId) {
        // There's a waypoint to show in this track.
        showWaypoint(showWaypointId);

        showWaypointId = 0L;
        showWaypointTrackId = 0L;
      } else {
        // Zoom out to show the whole track.
        zoomMapToBoundaries(track);
      }
    }
  }

  @Override
  public void onSelectedTrackChanged(final Track track, final boolean isRecording) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        boolean trackSelected = track != null;
        updateOptionsButton(trackSelected);

        mapOverlay.setTrackDrawingEnabled(trackSelected);

        if (trackSelected) {
          busyPane.setVisibility(View.VISIBLE);

          synchronized (this) {
            // Need to get the track ID only at this point, to prevent a race condition
            // among showWaypoint, zoomLoadedTrack and dataHub.loadTrack.
            selectedTrackId = track.getId();
            zoomLoadedTrack(track);
          }

          mapOverlay.setShowEndMarker(!isRecording);
          busyPane.setVisibility(View.GONE);
        }
        mapView.invalidate();
      }
    });
  }

  private final OnCreateContextMenuListener contextMenuListener =
      new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
          menu.setHeaderTitle(R.string.track_list_context_menu_title);
          menu.add(Menu.NONE, Constants.MENU_EDIT, Menu.NONE, R.string.track_list_edit_track);
          if (!dataHub.isRecordingSelected()) {
            String saveFileFormat = getString(R.string.track_list_save_file);
            String shareFileFormat = getString(R.string.track_list_share_file);
            String fileTypes[] = getResources().getStringArray(R.array.file_types);

            menu.add(Menu.NONE, Constants.MENU_PLAY, Menu.NONE, R.string.track_list_play);
            menu.add(Menu.NONE, Constants.MENU_SEND_TO_GOOGLE, Menu.NONE,
                R.string.track_list_send_google);
            SubMenu share = menu.addSubMenu(
                Menu.NONE, Constants.MENU_SHARE, Menu.NONE, R.string.track_list_share_track);
            share.add(
                Menu.NONE, Constants.MENU_SHARE_MAP, Menu.NONE, R.string.track_list_share_map);
            share.add(Menu.NONE, Constants.MENU_SHARE_FUSION_TABLE, Menu.NONE,
                R.string.track_list_share_fusion_table);
            share.add(Menu.NONE, Constants.MENU_SHARE_GPX_FILE, Menu.NONE,
                String.format(shareFileFormat, fileTypes[0]));
            share.add(Menu.NONE, Constants.MENU_SHARE_KML_FILE, Menu.NONE,
                String.format(shareFileFormat, fileTypes[1]));
            share.add(Menu.NONE, Constants.MENU_SHARE_CSV_FILE, Menu.NONE,
                String.format(shareFileFormat, fileTypes[2]));
            share.add(Menu.NONE, Constants.MENU_SHARE_TCX_FILE, Menu.NONE,
                String.format(shareFileFormat, fileTypes[3]));
            SubMenu save = menu.addSubMenu(
                Menu.NONE, Constants.MENU_WRITE_TO_SD_CARD, Menu.NONE, R.string.track_list_save_sd);
            save.add(Menu.NONE, Constants.MENU_SAVE_GPX_FILE, Menu.NONE,
                String.format(saveFileFormat, fileTypes[0]));
            save.add(Menu.NONE, Constants.MENU_SAVE_KML_FILE, Menu.NONE,
                String.format(saveFileFormat, fileTypes[1]));
            save.add(Menu.NONE, Constants.MENU_SAVE_CSV_FILE, Menu.NONE,
                String.format(saveFileFormat, fileTypes[2]));
            save.add(Menu.NONE, Constants.MENU_SAVE_TCX_FILE, Menu.NONE,
                String.format(saveFileFormat, fileTypes[3]));
            menu.add(Menu.NONE, Constants.MENU_CLEAR_MAP, Menu.NONE, R.string.track_list_clear_map);
            menu.add(Menu.NONE, Constants.MENU_DELETE, Menu.NONE, R.string.track_list_delete_track);
          }
        }
      };

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    Intent intent;
    long trackId = dataHub.getSelectedTrackId();
    switch (item.getItemId()) {
      case Constants.MENU_EDIT: 
        intent = new Intent(this, TrackDetail.class).putExtra(TrackDetail.TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case Constants.MENU_PLAY:
        if (PlayTrackUtils.isEarthInstalled(this)) {
          PlayTrackUtils.playTrack(this, trackId);
          return true;
        } else {
          showDialog(DIALOG_INSTALL_EARTH);
          return true;
        }
      case Constants.MENU_SEND_TO_GOOGLE:
        intent = new Intent(this, UploadServiceChooserActivity.class)
            .putExtra(SendRequest.SEND_REQUEST_KEY, new SendRequest(trackId, true, true, true));
        startActivity(intent);
        return true;
      case Constants.MENU_SHARE_MAP:
        intent = new Intent(this, UploadServiceChooserActivity.class)
            .putExtra(SendRequest.SEND_REQUEST_KEY, new SendRequest(trackId, true, false, false));
        startActivity(intent);
        return true;
      case Constants.MENU_SHARE_FUSION_TABLE:
        intent = new Intent(this, UploadServiceChooserActivity.class)
            .putExtra(SendRequest.SEND_REQUEST_KEY, new SendRequest(trackId, false, true, false));
        startActivity(intent);
        return true;
      case Constants.MENU_SHARE_GPX_FILE:
      case Constants.MENU_SHARE_KML_FILE:
      case Constants.MENU_SHARE_CSV_FILE:
      case Constants.MENU_SHARE_TCX_FILE:
      case Constants.MENU_SAVE_GPX_FILE:
      case Constants.MENU_SAVE_KML_FILE:
      case Constants.MENU_SAVE_CSV_FILE:
      case Constants.MENU_SAVE_TCX_FILE:
        SaveActivity.handleExportTrackAction(
            this, trackId, Constants.getActionFromMenuId(item.getItemId()));
        return true;
      case Constants.MENU_CLEAR_MAP:
        dataHub.unloadCurrentTrack();
        return true;
      case Constants.MENU_DELETE:
        Uri uri = ContentUris.withAppendedId(TracksColumns.CONTENT_URI, trackId);
        intent = new Intent(Intent.ACTION_DELETE, uri);
        startActivity(intent);
        return true;
      default:
        return super.onMenuItemSelected(featureId, item);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    myLocation = menu.add(
        Menu.NONE, Constants.MENU_MY_LOCATION, Menu.NONE, R.string.menu_map_view_my_location);
    myLocation.setIcon(android.R.drawable.ic_menu_mylocation);
    toggleLayers = menu.add(
        Menu.NONE, Constants.MENU_TOGGLE_LAYERS, Menu.NONE, R.string.menu_map_view_satellite_mode);
    toggleLayers.setIcon(android.R.drawable.ic_menu_mapmode);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    toggleLayers.setTitle(mapView.isSatellite() ?
        R.string.menu_map_view_map_mode : R.string.menu_map_view_satellite_mode);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case Constants.MENU_MY_LOCATION: {
        dataHub.forceUpdateLocation();
        keepMyLocationVisible = true;
        if (mapView.getZoomLevel() < 18) {
          mapView.getController().setZoom(18);
        }
        if (currentLocation != null) {
          showCurrentLocation();
        }
        return true;
      }
      case Constants.MENU_TOGGLE_LAYERS: {
        mapView.setSatellite(!mapView.isSatellite());
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onClick(View v) {
    if (v == messagePane) {
      startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    } else if (v == optionsBtn) {
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
  public void onProviderStateChange(ProviderState state) {
    final int messageId;
    final boolean isGpsDisabled;
    switch (state) {
      case DISABLED:
        messageId = R.string.gps_need_to_enable;
        isGpsDisabled = true;
        break;
      case NO_FIX:
      case BAD_FIX:
        messageId = R.string.gps_wait_for_fix;
        isGpsDisabled = false;
        break;
      case GOOD_FIX:
        // Nothing to show.
        messageId = -1;
        isGpsDisabled = false;
        break;
      default:
        throw new IllegalArgumentException("Unexpected state: " + state);
    }

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (messageId != -1) {
          messageText.setText(messageId);
          messagePane.setVisibility(View.VISIBLE);

          if (isGpsDisabled) {
            // Give a warning about this state.
            Toast.makeText(MapActivity.this,
                R.string.gps_not_found,
                Toast.LENGTH_LONG).show();

            // Make clicking take the user to the location settings.
            messagePane.setOnClickListener(MapActivity.this);
          } else {
            messagePane.setOnClickListener(null);
          }
        } else {
          messagePane.setVisibility(View.GONE);
        }

        screen.requestLayout();
      }
    });
  }

  @Override
  public void onCurrentLocationChanged(Location location) {
    currentLocation = location;
    showCurrentLocation();
  }

  @Override
  public void onCurrentHeadingChanged(double heading) {
    synchronized (this) {
      if (mapOverlay.setHeading((float) heading)) {
        mapView.postInvalidate();
      }
    }
  }

  @Override
  public void clearWaypoints() {
    mapOverlay.clearWaypoints();
  }

  @Override
  public void onNewWaypoint(Waypoint waypoint) {
    if (LocationUtils.isValidLocation(waypoint.getLocation())) {
      // TODO: Optimize locking inside addWaypoint
      mapOverlay.addWaypoint(waypoint);
    }
  }

  @Override
  public void onNewWaypointsDone() {
    mapView.postInvalidate();
  }

  @Override
  public void clearTrackPoints() {
    mapOverlay.clearPoints();
  }

  @Override
  public void onNewTrackPoint(Location loc) {
    mapOverlay.addLocation(loc);
  }

  @Override
  public void onSegmentSplit() {
    mapOverlay.addSegmentSplit();
  }

  @Override
  public void onSampledOutTrackPoint(Location loc) {
    // We don't care.
  }

  @Override
  public void onNewTrackPointsDone() {
    mapView.postInvalidate();
  }

  @Override
  public boolean onUnitsChanged(boolean metric) {
    // We don't care.
    return false;
  }

  @Override
  public boolean onReportSpeedChanged(boolean reportSpeed) {
    // We don't care.
    return false;
  }
}
