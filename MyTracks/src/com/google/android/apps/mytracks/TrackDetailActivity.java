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

import static com.google.android.apps.mytracks.Constants.CHART_TAB_TAG;
import static com.google.android.apps.mytracks.Constants.MAP_TAB_TAG;
import static com.google.android.apps.mytracks.Constants.STATS_TAB_TAG;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.io.file.SaveActivity;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.apps.mytracks.io.sendtogoogle.UploadServiceChooserActivity;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.ServiceUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.Toast;

import java.util.List;

/**
 * An activity to show the track detail.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
@SuppressWarnings("deprecation")
public class TrackDetailActivity extends TabActivity implements OnTouchListener {

  public static final String EXTRA_TRACK_ID = "track_id";
  public static final String EXTRA_WAYPOINT_ID = "waypoint_id";

  private static final String TAG = TrackDetailActivity.class.getSimpleName();
  private static final String MARKET_URL_PREFIX = "market://details?id=";
  
  private static final int DIALOG_INSTALL_EARTH_ID = 0;
  private static final int DIALOG_DELETE_CURRENT_ID = 1;

  private SharedPreferences sharedPreferences;
  private TrackDataHub trackDataHub;
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private NavControls navControls;
  private long trackId;
  
  private MenuItem stopRecordingMenuItem;
  private MenuItem insertMarkerMenuItem;
  private MenuItem playMenuItem;
  private MenuItem shareMenuItem;
  private MenuItem sendGoogleMenuItem;
  private MenuItem saveMenuItem;
  private MenuItem editMenuItem;
  private MenuItem deleteMenuItem;

  private final Runnable changeTab = new Runnable() {
    public void run() {
      getTabHost().setCurrentTab(navControls.getCurrentIcons());
    }
  };

  /*
   * Note that sharedPreferenceChangeListener cannot be an anonymous inner
   * class. Anonymous inner class will get garbage collected.
   */
  private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
    new OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
      // Note that key can be null
      if (getString(R.string.recording_track_key).equals(key)) {
        updateMenu();      
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);
    ApiAdapterFactory.getApiAdapter().configureActionBarHomeAsUp(this);
    
    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    trackDataHub = ((MyTracksApplication) getApplication()).getTrackDataHub();
    trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, null);

    final Resources res = getResources();
    final TabHost tabHost = getTabHost();
    tabHost.addTab(tabHost.newTabSpec(MAP_TAB_TAG)
        .setIndicator("Map", res.getDrawable(android.R.drawable.ic_menu_mapmode))
        .setContent(new Intent(this, MapActivity.class)));
    tabHost.addTab(tabHost.newTabSpec(STATS_TAB_TAG)
        .setIndicator("Stats", res.getDrawable(R.drawable.ic_menu_statistics))
        .setContent(new Intent(this, StatsActivity.class)));
    tabHost.addTab(tabHost.newTabSpec(CHART_TAB_TAG)
        .setIndicator("Chart", res.getDrawable(R.drawable.menu_elevation))
        .setContent(new Intent(this, ChartActivity.class)));

    // Hide the tab widget itself. We'll use overlayed prev/next buttons to
    // switch between the tabs:
    tabHost.getTabWidget().setVisibility(View.GONE);

    RelativeLayout layout = new RelativeLayout(this);
    LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    layout.setLayoutParams(params);
    navControls = new NavControls(this, layout, getResources().obtainTypedArray(R.array.left_icons),
        getResources().obtainTypedArray(R.array.right_icons), changeTab);
    navControls.show();
    tabHost.addView(layout);
    layout.setOnTouchListener(this);
    
    handleIntent(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    handleIntent(intent);
  }

  @Override
  protected void onStart() {
    super.onStart();
    trackDataHub.start();
  }

  @Override
  protected void onResume() {
    super.onResume();
    trackRecordingServiceConnection.bindIfRunning();
  }

  @Override
  protected void onStop() {
    super.onStop();
    trackDataHub.stop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    trackRecordingServiceConnection.unbind();
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_INSTALL_EARTH_ID:
        return new AlertDialog.Builder(this).setCancelable(true)
            .setMessage(R.string.track_detail_install_earth_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent()
                    .setData(Uri.parse(MARKET_URL_PREFIX + SaveActivity.GOOGLE_EARTH_PACKAGE));
                startActivity(intent);
              }
            })
            .create();
      case DIALOG_DELETE_CURRENT_ID:
        return DialogUtils.createConfirmationDialog(this,
            R.string.track_detail_delete_confirm_message, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                deleteCurrentTrack();
                startTrackListActivity();
              }
            });
      default:
        return null;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.track_detail, menu);
    String fileTypes[] = getResources().getStringArray(R.array.file_types);
    menu.findItem(R.id.track_detail_save_gpx)
        .setTitle(getString(R.string.menu_save_format, fileTypes[0]));
    menu.findItem(R.id.track_detail_save_kml)
        .setTitle(getString(R.string.menu_save_format, fileTypes[1]));
    menu.findItem(R.id.track_detail_save_csv)
        .setTitle(getString(R.string.menu_save_format, fileTypes[2]));
    menu.findItem(R.id.track_detail_save_tcx)
        .setTitle(getString(R.string.menu_save_format, fileTypes[3]));

    menu.findItem(R.id.track_detail_share_gpx)
        .setTitle(getString(R.string.menu_share_file, fileTypes[0]));
    menu.findItem(R.id.track_detail_share_kml)
        .setTitle(getString(R.string.menu_share_file, fileTypes[1]));
    menu.findItem(R.id.track_detail_share_csv)
        .setTitle(getString(R.string.menu_share_file, fileTypes[2]));
    menu.findItem(R.id.track_detail_share_tcx)
        .setTitle(getString(R.string.menu_share_file, fileTypes[3]));

    stopRecordingMenuItem = menu.findItem(R.id.track_detail_stop_recording);
    insertMarkerMenuItem = menu.findItem(R.id.track_detail_insert_marker);
    playMenuItem = menu.findItem(R.id.track_detail_play);
    shareMenuItem = menu.findItem(R.id.track_detail_share);
    sendGoogleMenuItem = menu.findItem(R.id.track_detail_send_google);
    saveMenuItem = menu.findItem(R.id.track_detail_save);
    editMenuItem = menu.findItem(R.id.track_detail_edit);
    deleteMenuItem = menu.findItem(R.id.track_detail_delete);

    updateMenu();
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    String currentTabTag = getTabHost().getCurrentTabTag();
    menu.findItem(R.id.track_detail_chart_settings).setVisible(CHART_TAB_TAG.equals(currentTabTag));
    menu.findItem(R.id.track_detail_my_location).setVisible(MAP_TAB_TAG.equals(currentTabTag));

    // Set map or satellite mode
    MapActivity mapActivity = getMapActivity();
    boolean isSatelliteMode = mapActivity != null ? mapActivity.isSatelliteView() : false;
    menu.findItem(R.id.track_detail_satellite_mode).setVisible(MAP_TAB_TAG.equals(currentTabTag))
        .setTitle(isSatelliteMode ? R.string.menu_map_mode : R.string.menu_satellite_mode);

    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    MapActivity mapActivity;
    Intent intent;
    switch (item.getItemId()) {
      case android.R.id.home:
        startTrackListActivity();
        return true;
      case R.id.track_detail_stop_recording:
        updateMenuItems(false);
        stopRecording();
        return true;
      case R.id.track_detail_insert_marker:
        // TODO: Add insert marker when updating WaypointList to ICS
        return true;
      case R.id.track_detail_play:
        if (isEarthInstalled()) {
          AnalyticsUtils.sendPageViews(this, "/action/play");
          intent = new Intent(this, SaveActivity.class)
              .putExtra(SaveActivity.EXTRA_TRACK_ID, trackId)
              .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) TrackFileFormat.KML)
              .putExtra(SaveActivity.EXTRA_PLAY_TRACK, true);
          startActivity(intent);
        } else {
          showDialog(DIALOG_INSTALL_EARTH_ID);
        }
        return true;
      case R.id.track_detail_share_map:
        intent = new Intent(this, UploadServiceChooserActivity.class)
            .putExtra(SendRequest.SEND_REQUEST_KEY, new SendRequest(trackId, true, false, false));
        startActivity(intent);
        return true;
      case R.id.track_detail_share_fusion_table:
        intent = new Intent(this, UploadServiceChooserActivity.class)
            .putExtra(SendRequest.SEND_REQUEST_KEY, new SendRequest(trackId, false, true, false));
        startActivity(intent);
        return true;
      case R.id.track_detail_share_gpx:
        startSaveActivity(TrackFileFormat.GPX, true);
        return true;
      case R.id.track_detail_share_kml:
        startSaveActivity(TrackFileFormat.KML, true);
        return true;
      case R.id.track_detail_share_csv:
        startSaveActivity(TrackFileFormat.CSV, true);
        return true;
      case R.id.track_detail_share_tcx:
        startSaveActivity(TrackFileFormat.TCX, true);
        return true;
      case R.id.track_detail_markers:
        intent = new Intent(this, WaypointsList.class)
            .putExtra("trackid", trackDataHub.getSelectedTrackId());
        startActivityForResult(intent, Constants.SHOW_WAYPOINT);
        return true;
      case R.id.track_detail_send_google:
        intent = new Intent(this, UploadServiceChooserActivity.class)
            .putExtra(SendRequest.SEND_REQUEST_KEY, new SendRequest(trackId, true, true, true));
        startActivity(intent);
        return true;
      case R.id.track_detail_save_gpx:
        startSaveActivity(TrackFileFormat.GPX, false);
        return true;
      case R.id.track_detail_save_kml:
        startSaveActivity(TrackFileFormat.KML, false);
        return true;
      case R.id.track_detail_save_csv:
        startSaveActivity(TrackFileFormat.CSV, false);
        return true;
      case R.id.track_detail_save_tcx:
        startSaveActivity(TrackFileFormat.TCX, false);
        return true;
      case R.id.track_detail_edit:
        startActivity(new Intent(this, TrackEditActivity.class)
            .putExtra(TrackEditActivity.EXTRA_TRACK_ID, trackId));
        return true;
      case R.id.track_detail_delete:
        showDialog(DIALOG_DELETE_CURRENT_ID);
        return true;
      case R.id.track_detail_my_location:
        mapActivity = getMapActivity();
        if (mapActivity != null) {
          mapActivity.showMyLocation();
        }
        return true;
      case R.id.track_detail_satellite_mode:
        mapActivity = getMapActivity();
        if (mapActivity != null) {
          mapActivity.setSatelliteView(!mapActivity.isSatelliteView());
        }
        return true;
      case R.id.track_detail_chart_settings:
        ChartActivity chartActivity = getChartActivity();
        if (chartActivity != null) {
          chartActivity.showChartSettingsDialog();
        }
        return true;
      case R.id.track_detail_sensor_state:
        startActivity(new Intent(this, SensorStateActivity.class));
        return true;
      case R.id.track_detail_settings:
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
      case R.id.track_detail_help:
        startActivity(new Intent(this, WelcomeActivity.class));
        return true;
      default:
        return false;
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, final Intent results) {
    if (requestCode != Constants.SHOW_WAYPOINT) {
      Log.d(TAG, "Invalid request code: " + requestCode);
      return;
    }
    if (results != null) {
      long waypointId = results.getLongExtra(WaypointDetails.WAYPOINT_ID_EXTRA, -1L);
      if (waypointId != -1L) {
        MapActivity mapActivity = (MapActivity) getLocalActivityManager().getActivity(MAP_TAB_TAG);
        if (mapActivity != null) {
          getTabHost().setCurrentTab(0);
          mapActivity.showWaypoint(waypointId);
        }
      }
    }
  }

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      if (isRecording()) {
        ITrackRecordingService trackRecordingService = trackRecordingServiceConnection
            .getServiceIfBound();
        if (trackRecordingService == null) {
          Log.e(TAG, "The track recording service is null");
          return true;
        }
        boolean success = false;
        try {
          long waypointId = trackRecordingService.insertWaypoint(
              WaypointCreationRequest.DEFAULT_STATISTICS);
          if (waypointId != -1L) {
            success = true;
          }
        } catch (RemoteException e) {
          Log.e(TAG, "Unable to insert waypoint", e);
        }
        Toast.makeText(this,
            success ? R.string.marker_insert_success : R.string.marker_insert_error,
            success ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG)
            .show();
        return true;
      }
    }
    return super.onTrackballEvent(event);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      navControls.show();
    }
    return false;
  }

  /**
   * Handles the data in the intent.
   */
  private void handleIntent(Intent intent) {
    // Get the trackid
    trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1L);
    if (trackId == -1L) {
      startTrackListActivity();
      finish();
    }
    trackDataHub.loadTrack(trackId);
    
    // Get the waypointId
    long waypointId = intent.getLongExtra(EXTRA_WAYPOINT_ID, -1L);
    if (waypointId != -1L) {
      MapActivity mapActivity = getMapActivity();
      if (mapActivity != null) {
        getTabHost().setCurrentTab(0);
        mapActivity.showWaypoint(trackId, waypointId);
      } else {
        Log.e(TAG, "MapActivity is null");
      }
    }
  }

  /**
   * Updates the menu.
   */
  private void updateMenu() {
    updateMenuItems(
        trackId == sharedPreferences.getLong(getString(R.string.recording_track_key), -1L));
  }

  /**
   * Updates the menu items.
   *
   * @param isRecording true if recording
   */
  private void updateMenuItems(boolean isRecording) {
    if (stopRecordingMenuItem != null) {
      stopRecordingMenuItem.setVisible(isRecording);
    }
    if (insertMarkerMenuItem != null) {
      insertMarkerMenuItem.setVisible(isRecording);
    }
    if (playMenuItem != null) {
      playMenuItem.setVisible(!isRecording);
    }
    if (shareMenuItem != null) {
      shareMenuItem.setVisible(!isRecording);
    }
    if (sendGoogleMenuItem != null) {
      sendGoogleMenuItem.setVisible(!isRecording);
    }
    if (saveMenuItem != null) {
      saveMenuItem.setVisible(!isRecording);
    }
    if (editMenuItem != null) {
      editMenuItem.setVisible(!isRecording);
    }
    if (deleteMenuItem != null) {
      deleteMenuItem.setVisible(!isRecording);
    }
  }

  /**
   * Deletes the current track.
   */
  private void deleteCurrentTrack() {
    long trackId = trackDataHub.getSelectedTrackId();
    MyTracksProviderUtils.Factory.get(TrackDetailActivity.this).deleteTrack(trackId);
  }

  /**
   * Starts the {@link TrackListActivity}.
   */
  private void startTrackListActivity() {
    startActivity(new Intent(this, TrackListActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
  }

  /**
   * Starts the {@link SaveActivity} to save a track.
   *
   * @param trackFileFormat the track file format
   * @param shareTrack true to share the track after saving
   */
  private void startSaveActivity(TrackFileFormat trackFileFormat, boolean shareTrack) {
    Intent intent = new Intent(this, SaveActivity.class)
        .putExtra(SaveActivity.EXTRA_TRACK_ID, trackId)
        .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) trackFileFormat)
        .putExtra(SaveActivity.EXTRA_SHARE_TRACK, shareTrack);
    startActivity(intent);
  }

  /**
   * Returns true if recording.
   */
  private boolean isRecording() {
    return ServiceUtils.isRecording(
        this, trackRecordingServiceConnection.getServiceIfBound(), sharedPreferences);
  }

  /**
   * Stops the recording and the track recording service connection and shows
   * {@link TrackEditActivity}.
   */
  private void stopRecording() {
    ITrackRecordingService trackRecordingService = trackRecordingServiceConnection
        .getServiceIfBound();
    if (trackRecordingService != null) {
      try {
        trackRecordingService.endCurrentTrack();
      } catch (Exception e) {
        Log.e(TAG, "Unable to stop recording.", e);
      }
    }
    trackRecordingServiceConnection.stop();

    long recordingTrackId = sharedPreferences.getLong(getString(R.string.recording_track_key), -1L);
    if (recordingTrackId != -1L) {
      Intent intent = new Intent(this, TrackEditActivity.class)
          .putExtra(TrackEditActivity.EXTRA_SHOW_CANCEL, false)
          .putExtra(TrackEditActivity.EXTRA_TRACK_ID, recordingTrackId);
      startActivity(intent);
    }
  }

  /**
   * Returns true if Google Earth app is installed.
   */
  private boolean isEarthInstalled() {
    List<ResolveInfo> infos = getPackageManager().queryIntentActivities(
        new Intent().setType(SaveActivity.KML_MIME_TYPE), PackageManager.MATCH_DEFAULT_ONLY);
    for (ResolveInfo info : infos) {
      if (info.activityInfo != null && info.activityInfo.packageName != null
          && info.activityInfo.packageName.equals(SaveActivity.GOOGLE_EARTH_PACKAGE)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Gets the map activity, can be null.
   */
  private MapActivity getMapActivity() {
    return (MapActivity) getLocalActivityManager().getActivity(MAP_TAB_TAG);
  }

  /**
   * Gets the chart activity, can be null.
   */
  private ChartActivity getChartActivity() {
    return (ChartActivity) getLocalActivityManager().getActivity(CHART_TAB_TAG);
  }
}