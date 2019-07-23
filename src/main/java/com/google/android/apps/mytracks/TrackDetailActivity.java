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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ShareActionProvider;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.fragments.ChartFragment;
import com.google.android.apps.mytracks.fragments.ChooseActivityTypeDialogFragment;
import com.google.android.apps.mytracks.fragments.ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller;
import com.google.android.apps.mytracks.fragments.ConfirmDeleteDialogFragment;
import com.google.android.apps.mytracks.fragments.FrequencyDialogFragment;
import com.google.android.apps.mytracks.fragments.StatsFragment;
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.settings.SettingsActivity;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackIconUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.apps.mytracks.util.TrackUtils;
import com.google.android.maps.mytracks.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An activity to show the track detail.
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class TrackDetailActivity extends AbstractTrackActivity implements ChooseActivityTypeCaller, ConfirmDeleteDialogFragment.ConfirmDeleteCaller {

  private static final String TAG = TrackDetailActivity.class.getSimpleName();

  public static final String EXTRA_TRACK_ID = "track_id";
  public static final String EXTRA_MARKER_ID = "marker_id";

  private static final String CURRENT_TAB_TAG_KEY = "current_tab_tag_key";
  private static final String PHOTO_URI_KEY = "photo_uri_key";
  private static final String HAS_PHOTO_KEY = "has_photo_key";
  private static final String JPEG_EXTENSION = "jpeg";

  private static final int CAMERA_REQUEST_CODE = 5;
  private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 6;

  // The following are set in onCreate
  private boolean hasCamera;
  private Uri photoUri;
  private boolean hasPhoto;
  private MyTracksProviderUtils myTracksProviderUtils;
  private SharedPreferences sharedPreferences;
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private TrackDataHub trackDataHub;
  private TabHost tabHost;
  private TrackController trackController;

  // From intent
  private long trackId;

  // Preferences
  private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
  private boolean recordingTrackPaused = PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT;

  private MenuItem insertMarkerMenuItem;
  private MenuItem insertPhotoMenuItem;
  private MenuItem markerListMenuItem;
  private MenuItem shareMenuItem;
  private MenuItem voiceFrequencyMenuItem;
  private MenuItem splitFrequencyMenuItem;

  private final Runnable bindChangedCallback = new Runnable() {
      @Override
    public void run() {
      // After binding changes (is available), update the total time in
      // trackController.
      runOnUiThread(new Runnable() {
          @Override
        public void run() {
          trackController.update(isRecording(), recordingTrackPaused);
          if (hasPhoto && photoUri != null) {
            hasPhoto = false;
            WaypointCreationRequest waypointCreationRequest = new WaypointCreationRequest(WaypointType.WAYPOINT, false, null, null, null, null, photoUri.toString());
            long markerId = TrackRecordingServiceConnectionUtils.addMarker(TrackDetailActivity.this, trackRecordingServiceConnection, waypointCreationRequest);
            if (markerId != -1L) {
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
          if (key == null || key.equals(PreferencesUtils.getKey(TrackDetailActivity.this, R.string.recording_track_id_key))) {
            recordingTrackId = PreferencesUtils.getLong(TrackDetailActivity.this, R.string.recording_track_id_key);
          }

          if (key == null || key.equals(PreferencesUtils.getKey(TrackDetailActivity.this, R.string.recording_track_paused_key))) {
            recordingTrackPaused = PreferencesUtils.getBoolean(TrackDetailActivity.this, R.string.recording_track_paused_key, PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
          }

          if (key == null) return;

          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              TrackDetailActivity.this.invalidateOptionsMenu();
              boolean isRecording = isRecording();
              trackController.update(isRecording, recordingTrackPaused);
            }
          });
        }
      };

  private final OnClickListener recordListener = new OnClickListener() {
      @Override
    public void onClick(View v) {
      if (recordingTrackPaused) {
        // Paused -> Resume
        updateMenuItems(false);
        TrackRecordingServiceConnectionUtils.resumeTrack(trackRecordingServiceConnection);
        trackController.update(true, false);
      } else {
        // Recording -> Paused
        updateMenuItems(true);
        TrackRecordingServiceConnectionUtils.pauseTrack(trackRecordingServiceConnection);
        trackController.update(true, true);
      }
    }
  };

  private final OnClickListener stopListener = new OnClickListener() {
      @Override
    public void onClick(View v) {
      TrackRecordingServiceConnectionUtils.stopRecording(TrackDetailActivity.this, trackRecordingServiceConnection, true);
        updateMenuItems(true);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    photoUri = savedInstanceState != null ? (Uri) savedInstanceState.getParcelable(PHOTO_URI_KEY) : null;
    hasPhoto = savedInstanceState != null && savedInstanceState.getBoolean(HAS_PHOTO_KEY, false);
       
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    handleIntent(getIntent());

    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);

    trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, bindChangedCallback);
    trackDataHub = TrackDataHub.newInstance(this);

    tabHost = findViewById(android.R.id.tabhost);
    tabHost.setup();

    ViewPager viewPager = findViewById(R.id.pager);
    TabsAdapter tabsAdapter = new TabsAdapter(this, tabHost, viewPager);

    TabSpec statsTabSpec = tabHost.newTabSpec(StatsFragment.STATS_FRAGMENT_TAG).setIndicator(getString(R.string.track_detail_stats_tab), getResources().getDrawable(R.drawable.ic_tab_stats));
    tabsAdapter.addTab(statsTabSpec, StatsFragment.class, null);

    TabSpec chartTabSpec = tabHost.newTabSpec(ChartFragment.CHART_FRAGMENT_TAG).setIndicator(getString(R.string.track_detail_chart_tab),getResources().getDrawable(R.drawable.ic_tab_chart));
    tabsAdapter.addTab(chartTabSpec, ChartFragment.class, null);

    if (savedInstanceState != null) {
      tabHost.setCurrentTabByTag(savedInstanceState.getString(CURRENT_TAB_TAG_KEY));
    }
    
    // Set the background after all three tabs are added
    for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
      tabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.tab_indicator_mytracks);
    }

    trackController = new TrackController(this, trackRecordingServiceConnection, false, recordListener, stopListener);
  }

  @Override
  protected void onStart() {
    super.onStart();

    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);

    TrackRecordingServiceConnectionUtils.startConnection(this, trackRecordingServiceConnection);
    trackDataHub.start();
  }

  @Override
  protected void onResume() {
    super.onResume();
    trackDataHub.loadTrack(trackId);

    // Update UI
    this.invalidateOptionsMenu();
    trackController.onResume(isRecording(), recordingTrackPaused);
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
        return;
      }
      hasPhoto = resultCode == RESULT_OK;

      if (hasPhoto) {
        //Register photo in media scanner
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(photoUri);
        this.sendBroadcast(mediaScanIntent);
        return;
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.track_detail;
  }

  @Override
  protected void onHomeSelected() {
    /*
     * TODO: Investigate
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

    insertMarkerMenuItem = menu.findItem(R.id.track_detail_insert_marker);
    insertPhotoMenuItem = menu.findItem(R.id.track_detail_insert_photo);
    insertPhotoMenuItem.setVisible(new Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(getPackageManager()) != null);

    shareMenuItem = menu.findItem(R.id.track_detail_share);
    ShareActionProvider shareActionProvider = (ShareActionProvider) shareMenuItem.getActionProvider();
    //TODO: Share the actual track when track is finished? How to get the file path or create a new file?
    shareActionProvider.setShareIntent(IntentUtils.newShareFileIntent(this, trackId, "", TrackFileFormat.KML));

    voiceFrequencyMenuItem = menu.findItem(R.id.track_detail_voice_frequency);
    splitFrequencyMenuItem = menu.findItem(R.id.track_detail_split_frequency);
    markerListMenuItem = menu.findItem(R.id.track_detail_markers);

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    updateMenuItems(recordingTrackPaused);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case R.id.track_detail_insert_marker:
        intent = IntentUtils
                .newIntent(this, MarkerEditActivity.class)
                .putExtra(MarkerEditActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.track_detail_insert_photo:
        if (!FileUtils.isExternalStorageWriteable() || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
          ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
          return false;
        }
        takePicture();
        return true;
      case R.id.track_detail_markers:
        intent = IntentUtils.newIntent(this, MarkerListActivity.class)
            .putExtra(MarkerListActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.track_detail_voice_frequency:
        FrequencyDialogFragment.newInstance(R.string.voice_frequency_key, PreferencesUtils.VOICE_FREQUENCY_DEFAULT, R.string.menu_voice_frequency)
            .show(getSupportFragmentManager(), FrequencyDialogFragment.FREQUENCY_DIALOG_TAG);
        return true;
      case R.id.track_detail_split_frequency:
        FrequencyDialogFragment.newInstance(R.string.split_frequency_key, PreferencesUtils.SPLIT_FREQUENCY_DEFAULT, R.string.menu_split_frequency)
            .show(getSupportFragmentManager(), FrequencyDialogFragment.FREQUENCY_DIALOG_TAG);
        return true;
      case R.id.track_detail_edit:
        intent = IntentUtils.newIntent(this, TrackEditActivity.class)
            .putExtra(TrackEditActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.track_detail_delete:
        deleteTracks(new long[] { trackId });
        return true;
      case R.id.track_detail_settings:
        intent = IntentUtils.newIntent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void takePicture() {
    File dir = FileUtils.getPhotoDir(trackId);
    FileUtils.ensureDirectoryExists(dir);

    String fileName = SimpleDateFormat.getDateTimeInstance().format(new Date());
    File file = new File(dir, FileUtils.buildUniqueFileName(dir, fileName, JPEG_EXTENSION));

    if (file != null) {
      photoUri = FileProvider.getUriForFile(this, "com.google.android.apps.mytracks.fileprovider", file);
      Log.d(TAG, "Taking photo to URI: " + photoUri);
      Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
              .putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
      startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
        Toast.makeText(this, R.string.external_storage_not_writable, Toast.LENGTH_LONG).show();
      } else {
        takePicture();
      }
      return;
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
   * Handles the data in the intent.
   */
  private void handleIntent(Intent intent) {
    trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1L);
    long markerId = intent.getLongExtra(EXTRA_MARKER_ID, -1L);
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
    }
  }

  /**
   * Updates the menu items.
   */
  private void updateMenuItems(boolean isPaused) {
    insertMarkerMenuItem.setVisible(isRecording() && !isPaused);
    insertPhotoMenuItem.setVisible(hasCamera && isRecording() && !isPaused);
    shareMenuItem.setVisible(!isRecording());
    markerListMenuItem.setShowAsAction(isRecording() ? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_IF_ROOM);
    voiceFrequencyMenuItem.setVisible(isRecording());
    splitFrequencyMenuItem.setVisible(isRecording());
    String title;
    if (isRecording()) {
      title = getString(isPaused ? R.string.generic_paused : R.string.generic_recording);
    } else {
      Track track = myTracksProviderUtils.getTrack(trackId);
      title = track != null ? track.getName() : "";
    }
    setTitle(title);
  }
  
  public void chooseActivityType(String category) {
    ChooseActivityTypeDialogFragment.newInstance(category).show(getSupportFragmentManager(), ChooseActivityTypeDialogFragment.CHOOSE_ACTIVITY_TYPE_DIALOG_TAG);
  }

  @Override
  public void onChooseActivityTypeDone(String iconValue) {
    Track track = myTracksProviderUtils.getTrack(trackId);
    String category = getString(TrackIconUtils.getIconActivityType(iconValue));
    TrackUtils.updateTrack(this, track, null, category, null, myTracksProviderUtils);
  }

  private boolean isRecording() {
    return trackId == recordingTrackId;
  }
}