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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.fragments.ChartFragment;
import com.google.android.apps.mytracks.fragments.ChooseActivityTypeDialogFragment;
import com.google.android.apps.mytracks.fragments.ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller;
import com.google.android.apps.mytracks.fragments.ExportDialogFragment;
import com.google.android.apps.mytracks.fragments.ExportDialogFragment.ExportCaller;
import com.google.android.apps.mytracks.fragments.ExportDialogFragment.ExportType;
import com.google.android.apps.mytracks.fragments.FrequencyDialogFragment;
import com.google.android.apps.mytracks.fragments.MapLayerDialogFragment;
import com.google.android.apps.mytracks.fragments.MyTracksMapFragment;
import com.google.android.apps.mytracks.fragments.PlayMultipleDialogFragment;
import com.google.android.apps.mytracks.fragments.PlayMultipleDialogFragment.PlayMultipleCaller;
import com.google.android.apps.mytracks.fragments.StatsFragment;
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.io.file.exporter.SaveActivity;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.settings.SettingsActivity;
import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.CalorieUtils;
import com.google.android.apps.mytracks.util.CalorieUtils.ActivityType;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackIconUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.apps.mytracks.util.TrackUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An activity to show the track detail.
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class TrackDetailActivity extends AbstractSendToGoogleActivity
    implements ChooseActivityTypeCaller, ExportCaller, PlayMultipleCaller {

  public static final String EXTRA_TRACK_ID = "track_id";
  public static final String EXTRA_MARKER_ID = "marker_id";

  private static final String CURRENT_TAB_TAG_KEY = "current_tab_tag_key";
  private static final String PHOTO_URI_KEY = "photo_uri_key";
  private static final String HAS_PHOTO_KEY = "has_photo_key";
  private static final String JPEG_EXTENSION = "jpeg";
  
  // The following are set in onCreate
  private boolean hasCamera;
  private Uri photoUri;
  private boolean hasPhoto;
  private MyTracksProviderUtils myTracksProviderUtils;
  private SharedPreferences sharedPreferences;
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private TrackDataHub trackDataHub;
  private TabHost tabHost;
  private ViewPager viewPager;
  private TabsAdapter tabsAdapter;
  private TrackController trackController;

  // From intent
  private long trackId;
  private long markerId;

  // Preferences
  private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
  private boolean recordingTrackPaused = PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT;
  private String sensorType = PreferencesUtils.SENSOR_TYPE_DEFAULT;
  
  private MenuItem insertMarkerMenuItem;
  private MenuItem insertPhotoMenuItem;
  private MenuItem playMenuItem;
  private MenuItem shareMenuItem;
  private MenuItem exportMenuItem;
  private MenuItem voiceFrequencyMenuItem;
  private MenuItem splitFrequencyMenuItem;
  private MenuItem sensorStateMenuItem;

  private final Runnable bindChangedCallback = new Runnable() {
      @Override
    public void run() {
      // After binding changes (is available), update the total time in
      // trackController.
      runOnUiThread(new Runnable() {
          @Override
        public void run() {
          trackController.update(trackId == recordingTrackId, recordingTrackPaused);
          if (hasPhoto && photoUri != null) {
            hasPhoto = false;
            WaypointCreationRequest waypointCreationRequest = new WaypointCreationRequest(
                WaypointType.WAYPOINT, false, null, null, null, null, photoUri.toString());
            long id = TrackRecordingServiceConnectionUtils.addMarker(
                TrackDetailActivity.this, trackRecordingServiceConnection, waypointCreationRequest);

            if (id != -1L) {
              FileUtils.updateMediaScanner(TrackDetailActivity.this, photoUri);
            }
          }
        }
      });
    }
  };

  /*
   * Note that sharedPreferenceChangeListener cannot be an anonymous inner
   * class. Anonymous inner class will get garbage collected.
   */
  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
          @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
          // Note that key can be null
          if (key == null || key.equals(
              PreferencesUtils.getKey(TrackDetailActivity.this, R.string.recording_track_id_key))) {
            recordingTrackId = PreferencesUtils.getLong(
                TrackDetailActivity.this, R.string.recording_track_id_key);
          }
          if (key == null || key.equals(PreferencesUtils.getKey(
              TrackDetailActivity.this, R.string.recording_track_paused_key))) {
            recordingTrackPaused = PreferencesUtils.getBoolean(TrackDetailActivity.this,
                R.string.recording_track_paused_key,
                PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
          }
          if (key == null || key.equals(
              PreferencesUtils.getKey(TrackDetailActivity.this, R.string.sensor_type_key))) {
            sensorType = PreferencesUtils.getString(TrackDetailActivity.this,
                R.string.sensor_type_key, PreferencesUtils.SENSOR_TYPE_DEFAULT);
          }
          if (key != null) {
            runOnUiThread(new Runnable() {
                @Override
              public void run() {
                ApiAdapterFactory.getApiAdapter().invalidMenu(TrackDetailActivity.this);
                boolean isRecording = trackId == recordingTrackId;
                trackController.update(isRecording, recordingTrackPaused);
              }
            });
          }
        }
      };

  private final OnClickListener recordListener = new OnClickListener() {
      @Override
    public void onClick(View v) {
      if (recordingTrackPaused) {
        // Paused -> Resume
        AnalyticsUtils.sendPageViews(TrackDetailActivity.this, AnalyticsUtils.ACTION_RESUME_TRACK);
        updateMenuItems(true, false);
        TrackRecordingServiceConnectionUtils.resumeTrack(trackRecordingServiceConnection);
        trackController.update(true, false);
      } else {
        // Recording -> Paused
        AnalyticsUtils.sendPageViews(TrackDetailActivity.this, AnalyticsUtils.ACTION_PAUSE_TRACK);
        updateMenuItems(true, true);
        TrackRecordingServiceConnectionUtils.pauseTrack(trackRecordingServiceConnection);
        trackController.update(true, true);
      }
    }
  };

  private final OnClickListener stopListener = new OnClickListener() {
      @Override
    public void onClick(View v) {
      AnalyticsUtils.sendPageViews(TrackDetailActivity.this, AnalyticsUtils.ACTION_STOP_RECORDING);
      updateMenuItems(false, true);
      TrackRecordingServiceConnectionUtils.stopRecording(
          TrackDetailActivity.this, trackRecordingServiceConnection, true);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    photoUri = savedInstanceState != null ? (Uri) savedInstanceState.getParcelable(PHOTO_URI_KEY)
        : null;
    hasPhoto = savedInstanceState != null ? savedInstanceState.getBoolean(HAS_PHOTO_KEY, false)
        : false;
       
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    handleIntent(getIntent());

    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);

    trackRecordingServiceConnection = new TrackRecordingServiceConnection(
        this, bindChangedCallback);
    trackDataHub = TrackDataHub.newInstance(this);

    tabHost = (TabHost) findViewById(android.R.id.tabhost);
    tabHost.setup();

    viewPager = (ViewPager) findViewById(R.id.pager);

    tabsAdapter = new TabsAdapter(this, tabHost, viewPager);

    TabSpec mapTabSpec = tabHost.newTabSpec(MyTracksMapFragment.MAP_FRAGMENT_TAG).setIndicator(
        getString(R.string.track_detail_map_tab),
        getResources().getDrawable(R.drawable.ic_tab_map));
    tabsAdapter.addTab(mapTabSpec, MyTracksMapFragment.class, null);

    TabSpec chartTabSpec = tabHost.newTabSpec(ChartFragment.CHART_FRAGMENT_TAG).setIndicator(
        getString(R.string.track_detail_chart_tab),
        getResources().getDrawable(R.drawable.ic_tab_chart));
    tabsAdapter.addTab(chartTabSpec, ChartFragment.class, null);

    TabSpec statsTabSpec = tabHost.newTabSpec(StatsFragment.STATS_FRAGMENT_TAG).setIndicator(
        getString(R.string.track_detail_stats_tab),
        getResources().getDrawable(R.drawable.ic_tab_stats));
    tabsAdapter.addTab(statsTabSpec, StatsFragment.class, null);

    if (savedInstanceState != null) {
      tabHost.setCurrentTabByTag(savedInstanceState.getString(CURRENT_TAB_TAG_KEY));
    }
    
    // Set the background after all three tabs are added
    ApiAdapterFactory.getApiAdapter().setTabBackground(tabHost.getTabWidget());
    
    trackController = new TrackController(
        this, trackRecordingServiceConnection, false, recordListener, stopListener);
  }

  @Override
  protected void onStart() {
    super.onStart();

    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);

    TrackRecordingServiceConnectionUtils.startConnection(this, trackRecordingServiceConnection);
    trackDataHub.start();
    AnalyticsUtils.sendPageViews(this, AnalyticsUtils.PAGE_TRACK_DETAIL);
  }

  @Override
  protected void onResume() {
    super.onResume();
    trackDataHub.loadTrack(trackId);

    // Update UI
    ApiAdapterFactory.getApiAdapter().invalidMenu(this);
    boolean isRecording = trackId == recordingTrackId;
    trackController.onResume(isRecording, recordingTrackPaused);
  }

  @Override
  protected void onPause() {
    super.onPause();
    trackController.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    trackRecordingServiceConnection.unbind();
    trackDataHub.stop();
    AnalyticsUtils.dispatch();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(CURRENT_TAB_TAG_KEY, tabHost.getCurrentTabTag());
    if (photoUri != null) {
      outState.putParcelable(PHOTO_URI_KEY, photoUri);
    }
    outState.putBoolean(HAS_PHOTO_KEY, hasPhoto);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == CAMERA_REQUEST_CODE) {
      if (resultCode == RESULT_CANCELED) {
        Toast.makeText(this, R.string.marker_add_canceled, Toast.LENGTH_LONG).show();
      }
      hasPhoto = resultCode == RESULT_OK;
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.track_detail;
  }

  @Override
  protected boolean hideTitle() {
    return true;
  }

  @Override
  protected void onHomeSelected() {
    /*
     * According to
     * http://developer.android.com/training/implementing-navigation
     * /ancestral.html, we should use NavUtils.shouldUpRecreateTask instead of
     * always creating a new back stack. However, NavUtils.shouldUpRecreateTask
     * seems to always return false.
     */
    TaskStackBuilder.create(this).addParentStack(TrackDetailActivity.class).startActivities();
    finish();
  }

  @Override
  public void onNewIntent(Intent intent) {
    setIntent(intent);
    handleIntent(intent);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.track_detail, menu);

    Track track = myTracksProviderUtils.getTrack(trackId);
    boolean isSharedWithMe = track != null ? track.isSharedWithMe() : true;

    menu.findItem(R.id.track_detail_edit).setVisible(!isSharedWithMe);  
    menu.findItem(R.id.track_detail_help_feedback).setTitle(
        ApiAdapterFactory.getApiAdapter().isGoogleFeedbackAvailable() ? R.string.menu_help_feedback
            : R.string.menu_help);

    insertMarkerMenuItem = menu.findItem(R.id.track_detail_insert_marker);
    insertPhotoMenuItem = menu.findItem(R.id.track_detail_insert_photo);
    playMenuItem = menu.findItem(R.id.track_detail_play);

    shareMenuItem = menu.findItem(R.id.track_detail_share);
    shareMenuItem.setEnabled(!isSharedWithMe);
    shareMenuItem.setVisible(!isSharedWithMe);

    exportMenuItem = menu.findItem(R.id.track_detail_export);
    voiceFrequencyMenuItem = menu.findItem(R.id.track_detail_voice_frequency);
    splitFrequencyMenuItem = menu.findItem(R.id.track_detail_split_frequency);
    sensorStateMenuItem = menu.findItem(R.id.track_detail_sensor_state);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    updateMenuItems(trackId == recordingTrackId, recordingTrackPaused);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case R.id.track_detail_insert_marker:
        AnalyticsUtils.sendPageViews(this, AnalyticsUtils.ACTION_INSERT_MARKER);
        intent = IntentUtils.newIntent(this, MarkerEditActivity.class)
            .putExtra(MarkerEditActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.track_detail_insert_photo:
        if (!FileUtils.isExternalStorageWriteable()) {
          Toast.makeText(this, R.string.external_storage_not_writable, Toast.LENGTH_LONG).show();
          return false;
        }

        File dir = FileUtils.getPhotoDir(trackId);
        FileUtils.ensureDirectoryExists(dir);

        String fileName = SimpleDateFormat.getDateTimeInstance().format(new Date());
        File file = new File(dir, FileUtils.buildUniqueFileName(dir, fileName, JPEG_EXTENSION));

        photoUri = Uri.fromFile(file);
        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(
            MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
        return true;
      case R.id.track_detail_play:
        playTracks(new long[] {trackId});
        return true;
      case R.id.track_detail_share:
        shareTrack(trackId);
        return true;
      case R.id.track_detail_markers:
        intent = IntentUtils.newIntent(this, MarkerListActivity.class)
            .putExtra(MarkerListActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.track_detail_play_multiple:
        PlayMultipleDialogFragment.newInstance(trackId)
            .show(getSupportFragmentManager(), PlayMultipleDialogFragment.PLAY_MULTIPLE_DIALOG_TAG);
        return true;
      case R.id.track_detail_voice_frequency:
        FrequencyDialogFragment.newInstance(R.string.voice_frequency_key,
            PreferencesUtils.VOICE_FREQUENCY_DEFAULT, R.string.menu_voice_frequency)
            .show(getSupportFragmentManager(), FrequencyDialogFragment.FREQUENCY_DIALOG_TAG);
        return true;
      case R.id.track_detail_split_frequency:
        FrequencyDialogFragment.newInstance(R.string.split_frequency_key,
            PreferencesUtils.SPLIT_FREQUENCY_DEFAULT, R.string.menu_split_frequency)
            .show(getSupportFragmentManager(), FrequencyDialogFragment.FREQUENCY_DIALOG_TAG);
        return true;
      case R.id.track_detail_export:
        Track track = myTracksProviderUtils.getTrack(trackId);
        boolean hideDrive = track != null ? track.isSharedWithMe() : true;
        ExportDialogFragment.newInstance(hideDrive)
            .show(getSupportFragmentManager(), ExportDialogFragment.EXPORT_DIALOG_TAG);
        return true;
      case R.id.track_detail_edit:
        intent = IntentUtils.newIntent(this, TrackEditActivity.class)
            .putExtra(TrackEditActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.track_detail_delete:
        deleteTracks(new long[] { trackId });
        return true;
      case R.id.track_detail_sensor_state:
        intent = IntentUtils.newIntent(this, SensorStateActivity.class);
        startActivity(intent);
        return true;
      case R.id.track_detail_settings:
        intent = IntentUtils.newIntent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
      case R.id.track_detail_help_feedback:
        intent = IntentUtils.newIntent(this, HelpActivity.class);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      if (trackId == recordingTrackId && !recordingTrackPaused) {
        TrackRecordingServiceConnectionUtils.addMarker(
            this, trackRecordingServiceConnection, WaypointCreationRequest.DEFAULT_WAYPOINT);
        return true;
      }
    }
    return super.onTrackballEvent(event);
  }

  @Override
  public void onExportDone(
      ExportType exportType, TrackFileFormat trackFileFormat, Account account) {
    if (exportType == ExportType.EXTERNAL_STORAGE) {
      AnalyticsUtils.sendPageViews(
          this, AnalyticsUtils.ACTION_EXPORT_PREFIX + trackFileFormat.getExtension());
      Intent intent = IntentUtils.newIntent(this, SaveActivity.class)
          .putExtra(SaveActivity.EXTRA_TRACK_IDS, new long[] { trackId })
          .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) trackFileFormat);
      startActivity(intent);
    } else {
      exportTrackToGoogle(trackId, exportType, account);
    }
  }

  @Override
  protected TrackRecordingServiceConnection getTrackRecordingServiceConnection() {
    return trackRecordingServiceConnection;
  }

  @Override
  protected void onDeleted() {
    runOnUiThread(new Runnable() {
        @Override
      public void run() {
        finish();
      }
    });
  }

  /**
   * Gets the {@link TrackDataHub}.
   */
  public TrackDataHub getTrackDataHub() {
    return trackDataHub;
  }

  /**
   * Gets the track id.
   */
  public long getTrackId() {
    return trackId;
  }
  
  /**
   * Gets the marker id.
   */
  public long getMarkerId() {
    return markerId;
  }
  
  /**
   * Handles the data in the intent.
   */
  private void handleIntent(Intent intent) {
    trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1L);
    markerId = intent.getLongExtra(EXTRA_MARKER_ID, -1L);
    if (markerId != -1L) {
      // Use the trackId from the marker
      Waypoint waypoint = myTracksProviderUtils.getWaypoint(markerId);
      if (waypoint == null) {
        finish();
        return;
      }
      trackId = waypoint.getTrackId();
    }
    if (trackId == -1L) {
      finish();
      return;
    }
    Track track = myTracksProviderUtils.getTrack(trackId);
    if (track == null) {
      // Use the last track if markerId is not set
      if (markerId == -1L) {
        track = myTracksProviderUtils.getLastTrack();
        if (track != null) {
          trackId = track.getId();
          return;
        }
      }
      finish();
      return;
    }
  }

  /**
   * Updates the menu items.
   * 
   * @param isRecording true if recording
   */
  private void updateMenuItems(boolean isRecording, boolean isPaused) {
    if (insertMarkerMenuItem != null) {
      insertMarkerMenuItem.setVisible(isRecording && !isPaused);
    }
    if (insertPhotoMenuItem != null) {
      insertPhotoMenuItem.setVisible(hasCamera && isRecording && !isPaused);
    }
    if (playMenuItem != null) {
      playMenuItem.setVisible(!isRecording);
    }
    if (shareMenuItem != null && shareMenuItem.isEnabled()) {
      shareMenuItem.setVisible(!isRecording);
    }
    if (exportMenuItem != null) {
      exportMenuItem.setVisible(!isRecording);
    }
    if (voiceFrequencyMenuItem != null) {
      voiceFrequencyMenuItem.setVisible(isRecording);
    }
    if (splitFrequencyMenuItem != null) {
      splitFrequencyMenuItem.setVisible(isRecording);
    }    
    if (sensorStateMenuItem != null) {
      sensorStateMenuItem.setVisible(!PreferencesUtils.SENSOR_TYPE_DEFAULT.equals(sensorType));
    }

    String title;
    if (isRecording) {
      title = getString(isPaused ? R.string.generic_paused : R.string.generic_recording);
    } else {
      Track track = myTracksProviderUtils.getTrack(trackId);
      title = track != null ? track.getName() : "";
    }
    setTitle(title);
  }
  
  public void chooseActivityType(String category) {
    ChooseActivityTypeDialogFragment.newInstance(category).show(getSupportFragmentManager(),
        ChooseActivityTypeDialogFragment.CHOOSE_ACTIVITY_TYPE_DIALOG_TAG);
  }

  @Override
  public void onChooseActivityTypeDone(String iconValue, boolean newWeight) {
    Track track = myTracksProviderUtils.getTrack(trackId);
    String category = getString(TrackIconUtils.getIconActivityType(iconValue));
    TrackUtils.updateTrack(
        this, track, null, category, null, myTracksProviderUtils, trackRecordingServiceConnection, newWeight);

    // Add toast if cannot calculate calorie
    if (CalorieUtils.getActivityType(this, category) == ActivityType.INVALID) {
      String message = getString(R.string.stats_calorie_no_calculation, category);
      Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
  }
  
  @Override
  public void onPlayMultipleDone(long[] trackIds) {
    playTracks(trackIds);
  }
  
  /**
   * Shows the map layer dialog.
   */
  public void showMapLayerDialog() {
    new MapLayerDialogFragment().show(
        getSupportFragmentManager(), MapLayerDialogFragment.MAP_LAYER_DIALOG_TAG);
  }
}