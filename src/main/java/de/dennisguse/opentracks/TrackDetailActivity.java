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

package de.dennisguse.opentracks;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.Waypoint;
import de.dennisguse.opentracks.content.Waypoint.WaypointType;
import de.dennisguse.opentracks.content.WaypointCreationRequest;
import de.dennisguse.opentracks.fragments.ChartDistanceFragment;
import de.dennisguse.opentracks.fragments.ChartTimeFragment;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
import de.dennisguse.opentracks.fragments.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.fragments.StatsFragment;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.SettingsActivity;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackUtils;

/**
 * An activity to show the track detail.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class TrackDetailActivity extends AbstractListActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller, ConfirmDeleteDialogFragment.ConfirmDeleteCaller {

    public static final String EXTRA_TRACK_ID = "track_id";
    public static final String EXTRA_MARKER_ID = "marker_id";

    private static final String TAG = TrackDetailActivity.class.getSimpleName();

    private static final String CURRENT_TAB_TAG_KEY = "current_tab_tag_key";
    private static final String PHOTO_URI_KEY = "photo_uri_key";
    private static final String HAS_PHOTO_KEY = "has_photo_key";

    private static final int CAMERA_REQUEST_CODE = 5;
    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 6;

    // The following are set in onCreate
    private boolean hasCamera;
    private Uri photoUri;
    private boolean hasPhoto;
    private ContentProviderUtils contentProviderUtils;
    private SharedPreferences sharedPreferences;
    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private TrackDataHub trackDataHub;
    private TabHost tabHost;
    private TrackController trackController;

    // From intent
    private long trackId;

    // Preferences
    private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    private boolean recordingTrackPaused;

    private final Runnable bindChangedCallback = new Runnable() {
        @Override
        public void run() {
            // After binding changes (is available), update the total time in trackController.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    trackController.update(isRecording(), recordingTrackPaused);
                    if (hasPhoto && photoUri != null) {
                        hasPhoto = false;
                        WaypointCreationRequest waypointCreationRequest = new WaypointCreationRequest(WaypointType.WAYPOINT, false, null, null, null, null, photoUri.toString());
                        long markerId = trackRecordingServiceConnection.addMarker(TrackDetailActivity.this, waypointCreationRequest);
                        if (markerId != -1L) {
                            //TODO: Make configurable.
                            FileUtils.updateMediaScanner(TrackDetailActivity.this, photoUri);
                        }
                    }
                }
            });
        }
    };

    // Note that sharedPreferenceChangeListener cannot be an anonymous inner class. Anonymous inner class will get garbage collected.
    private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (PreferencesUtils.isKey(TrackDetailActivity.this, R.string.recording_track_id_key, key)) {
                recordingTrackId = PreferencesUtils.getRecordingTrackId(TrackDetailActivity.this);
                setLockscreenPolicy();
                setScreenOnPolicy();
            }

            if (PreferencesUtils.isKey(TrackDetailActivity.this, R.string.recording_track_paused_key, key)) {
                recordingTrackPaused = PreferencesUtils.isRecordingTrackPaused(TrackDetailActivity.this);
                setLockscreenPolicy();
                setScreenOnPolicy();
            }

            if (PreferencesUtils.isKey(TrackDetailActivity.this, R.string.stats_show_on_lockscreen_while_recording_key, key)) {
                setLockscreenPolicy();
            }

            if (PreferencesUtils.isKey(TrackDetailActivity.this, R.string.stats_keep_screen_on_while_recording_key, key)) {
                setScreenOnPolicy();
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

    private MenuItem insertMarkerMenuItem;
    private MenuItem insertPhotoMenuItem;
    private MenuItem markerListMenuItem;
    private MenuItem shareMenuItem;

    private final OnClickListener recordListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (recordingTrackPaused) {
                // Paused -> Resume
                updateMenuItems(false);
                trackRecordingServiceConnection.resumeTrack();
                trackController.update(true, false);
            } else {
                // Recording -> Paused
                updateMenuItems(true);
                trackRecordingServiceConnection.pauseTrack();
                trackController.update(true, true);
            }
        }
    };

    private final OnClickListener stopListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            trackRecordingServiceConnection.stopRecording(TrackDetailActivity.this, true);
            updateMenuItems(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recordingTrackPaused = PreferencesUtils.isRecordingTrackPausedDefault(this);

        hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        photoUri = savedInstanceState != null ? (Uri) savedInstanceState.getParcelable(PHOTO_URI_KEY) : null;
        hasPhoto = savedInstanceState != null && savedInstanceState.getBoolean(HAS_PHOTO_KEY, false);

        contentProviderUtils = new ContentProviderUtils(this);
        handleIntent(getIntent());

        sharedPreferences = PreferencesUtils.getSharedPreferences(this);

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, bindChangedCallback);
        trackDataHub = TrackDataHub.newInstance(this);

        tabHost = findViewById(R.id.tackdetail_tabhost);
        tabHost.setup();

        ViewPager viewPager = findViewById(R.id.pager);
        TabsAdapter tabsAdapter = new TabsAdapter(this, tabHost, viewPager);

        TabSpec statsTabSpec = tabHost.newTabSpec(StatsFragment.STATS_FRAGMENT_TAG).setIndicator(getString(R.string.track_detail_stats_tab));
        tabsAdapter.addTab(statsTabSpec, StatsFragment.class, null);

        TabSpec chartTimeTabSpec = tabHost.newTabSpec(ChartTimeFragment.CHART_FRAGMENT_TAG).setIndicator(getString(R.string.settings_chart_by_time));
        tabsAdapter.addTab(chartTimeTabSpec, ChartTimeFragment.class, null);

        TabSpec chartDistanceTabSpec = tabHost.newTabSpec(ChartDistanceFragment.CHART_FRAGMENT_TAG).setIndicator(getString(R.string.settings_chart_by_distance));
        tabsAdapter.addTab(chartDistanceTabSpec, ChartDistanceFragment.class, null);

        if (savedInstanceState != null) {
            tabHost.setCurrentTabByTag(savedInstanceState.getString(CURRENT_TAB_TAG_KEY));
        }

        // Set the background after all three tabs are added
        for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
            tabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.tab_indicator);
        }

        trackController = new TrackController(this, trackRecordingServiceConnection, false, recordListener, stopListener);
    }

    @Override
    public void onAttachedToWindow() {
        setLockscreenPolicy();
        setScreenOnPolicy();
        super.onAttachedToWindow();
    }

    private void setLockscreenPolicy() {
        boolean showOnLockScreen = PreferencesUtils.shouldShowStatsOnLockscreen(TrackDetailActivity.this)
                && PreferencesUtils.isRecording(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(showOnLockScreen);
        } else if (showOnLockScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    private void setScreenOnPolicy() {
        boolean keepScreenOn = PreferencesUtils.shouldKeepScreenOn(TrackDetailActivity.this)
                && PreferencesUtils.isRecording(this);

        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);

        trackRecordingServiceConnection.startConnection(this);
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
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
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.track_detail;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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
            case R.id.track_detail_share:
                intent = IntentUtils.newShareFileIntent(this, new long[]{trackId});
                intent = Intent.createChooser(intent, null);
                startActivity(intent);
                return true;
            case R.id.track_detail_insert_photo:
                if (!FileUtils.isExternalStorageWriteable() || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                    return false;
                }
                createWaypointWithPicture();
                return true;
            case R.id.track_detail_menu_show_on_map:
                IntentUtils.showTrackOnMap(this, new long[]{trackId});
                return true;
            case R.id.track_detail_markers:
                intent = IntentUtils.newIntent(this, MarkerListActivity.class)
                        .putExtra(MarkerListActivity.EXTRA_TRACK_ID, trackId);
                startActivity(intent);
                return true;
            case R.id.track_detail_edit:
                intent = IntentUtils.newIntent(this, TrackEditActivity.class)
                        .putExtra(TrackEditActivity.EXTRA_TRACK_ID, trackId);
                startActivity(intent);
                return true;
            case R.id.track_detail_delete:
                deleteTracks(new long[]{trackId});
                return true;
            case R.id.track_detail_settings:
                intent = IntentUtils.newIntent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.external_storage_not_writable, Toast.LENGTH_LONG).show();
            } else {
                createWaypointWithPicture();
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

    private void handleIntent(Intent intent) {
        trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1L);
        long markerId = intent.getLongExtra(EXTRA_MARKER_ID, -1L);
        if (markerId != -1L) {
            // Use the trackId from the marker
            Waypoint waypoint = contentProviderUtils.getWaypoint(markerId);
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
        Track track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            // Use the last track if markerId is not set
            if (markerId == -1L) {
                track = contentProviderUtils.getLastTrack();
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
        String title;
        if (isRecording()) {
            title = getString(isPaused ? R.string.generic_paused : R.string.generic_recording);
        } else {
            Track track = contentProviderUtils.getTrack(trackId);
            title = track != null ? track.getName() : "";
        }
        setTitle(title);
    }

    private void createWaypointWithPicture() {
        Pair<Intent, Uri> intentAndPhotoUri = IntentUtils.createTakePictureIntent(this, trackId);
        photoUri = intentAndPhotoUri.second;
        startActivityForResult(intentAndPhotoUri.first, CAMERA_REQUEST_CODE);
    }

    public void chooseActivityType(String category) {
        ChooseActivityTypeDialogFragment.showDialog(getSupportFragmentManager(), category);
    }

    @Override
    public void onChooseActivityTypeDone(String iconValue) {
        Track track = contentProviderUtils.getTrack(trackId);
        String category = getString(TrackIconUtils.getIconActivityType(iconValue));
        TrackUtils.updateTrack(this, track, null, category, null, contentProviderUtils);
    }

    private boolean isRecording() {
        return trackId == recordingTrackId;
    }
}