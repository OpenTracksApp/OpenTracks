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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.fragments.ChartFragment;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
import de.dennisguse.opentracks.fragments.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.fragments.StatsFragment;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.SettingsActivity;
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

    // The following are set in onCreate
    private ContentProviderUtils contentProviderUtils;
    private SharedPreferences sharedPreferences;
    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private TrackDataHub trackDataHub;
    private ViewPager pager;
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
                }
            });
        }
    };

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

        contentProviderUtils = new ContentProviderUtils(this);
        handleIntent(getIntent());

        sharedPreferences = PreferencesUtils.getSharedPreferences(this);

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, bindChangedCallback);
        trackDataHub = new TrackDataHub(this);

        FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager(), 1) {
            @Override
            public int getCount() {
                return 3;
            }

            @NonNull
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 1:
                        return ChartFragment.newInstance(false);
                    case 2:
                        return ChartFragment.newInstance(true);
                    default: //0
                        return new StatsFragment();
                }
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return getString(R.string.track_detail_stats_tab);
                    case 1:
                        return getString(R.string.settings_chart_by_time);
                    case 2:
                        return getString(R.string.settings_chart_by_distance);
                }
                return "Unknown Tab";
            }
        };
        pager = findViewById(R.id.track_detail_activity_view_pager);
        pager.setAdapter(adapter);
        TabLayout tabs = findViewById(R.id.track_detail_activity_tablayout);
        tabs.setupWithViewPager(pager);
        if (savedInstanceState != null) {
            pager.setCurrentItem(savedInstanceState.getInt(CURRENT_TAB_TAG_KEY));
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
        trackRecordingServiceConnection.unbind(this);
        trackDataHub.stop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_TAB_TAG_KEY, pager.getCurrentItem());
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