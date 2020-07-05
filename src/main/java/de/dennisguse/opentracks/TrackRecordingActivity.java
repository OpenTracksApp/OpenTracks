package de.dennisguse.opentracks;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.fragments.ChartFragment;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
import de.dennisguse.opentracks.fragments.StatisticsRecordingFragment;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.services.TrackRecordingServiceInterface;
import de.dennisguse.opentracks.settings.SettingsActivity;
import de.dennisguse.opentracks.util.IntentDashboardUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackUtils;

/**
 * An activity to show the track detail, record a new track or resumes an existing one.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class TrackRecordingActivity extends AbstractActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller, TrackActivityDataHubInterface {

    public static final String EXTRA_TRACK_ID = "track_id";

    private static final String TAG = TrackRecordingActivity.class.getSimpleName();

    private static final String CURRENT_TAB_TAG_KEY = "current_tab_tag_key";

    // The following are set in onCreate
    private ContentProviderUtils contentProviderUtils;
    private SharedPreferences sharedPreferences;
    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private TrackDataHub trackDataHub;
    private ViewPager pager;
    private TrackController trackController;

    // Initialized from Intent; if a new track recording is started new TrackId will be provided by TrackRecordingService
    private long trackId;

    // Preferences
    @Deprecated //TODO Do we really need two trackIds here?
    private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    private boolean recordingTrackPaused;

    private final Runnable bindChangedCallback = new Runnable() {
        @Override
        public void run() {
            // After binding changes (is available), update the total time in trackController.
            runOnUiThread(() -> trackController.update(true, recordingTrackPaused));

            if (recordingTrackId == -1L) {
                TrackRecordingServiceInterface service = trackRecordingServiceConnection.getServiceIfBound();
                if (service == null) {
                    Log.d(TAG, "could not get TrackRecordingService");
                    return;
                }

                // Starts or resumes a track.
                int msg;
                if (trackId == -1L) {
                    // trackId isn't initialized -> leads a new recording.
                    trackId = service.startNewTrack();
                    recordingTrackId = trackId;
                    msg = R.string.track_detail_record_success;
                } else {
                    // trackId is initialized -> resumes the track.
                    recordingTrackId = trackId;
                    service.resumeTrack(trackId);
                    msg = R.string.track_detail_resume_success;
                }

                // A recording track is on.
                Toast.makeText(TrackRecordingActivity.this, msg, Toast.LENGTH_SHORT).show();
                trackDataHub.loadTrack(trackId);
                trackController.update(true, false);
                trackController.onResume(true, recordingTrackPaused);
            }
        }
    };

    private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (PreferencesUtils.isKey(TrackRecordingActivity.this, R.string.recording_track_id_key, key)) {
                recordingTrackId = PreferencesUtils.getRecordingTrackId(TrackRecordingActivity.this);
                setLockscreenPolicy();
                setScreenOnPolicy();
            }

            if (PreferencesUtils.isKey(TrackRecordingActivity.this, R.string.recording_track_paused_key, key)) {
                recordingTrackPaused = PreferencesUtils.isRecordingTrackPaused(TrackRecordingActivity.this);
                setLockscreenPolicy();
                setScreenOnPolicy();
            }

            if (PreferencesUtils.isKey(TrackRecordingActivity.this, R.string.stats_show_on_lockscreen_while_recording_key, key)) {
                setLockscreenPolicy();
            }

            if (PreferencesUtils.isKey(TrackRecordingActivity.this, R.string.stats_keep_screen_on_while_recording_key, key)) {
                setScreenOnPolicy();
            }

            if (key == null) return;

            runOnUiThread(() -> {
                TrackRecordingActivity.this.invalidateOptionsMenu();
                trackController.update(true, recordingTrackPaused);
            });
        }
    };

    private MenuItem insertMarkerMenuItem;
    private MenuItem markerListMenuItem;

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
            trackRecordingServiceConnection.stopRecording(TrackRecordingActivity.this, true);
            Intent newIntent = IntentUtils.newIntent(TrackRecordingActivity.this, TrackRecordedActivity.class)
                    .putExtra(TrackRecordedActivity.EXTRA_TRACK_ID, trackId);
            startActivity(newIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            updateMenuItems(true);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recordingTrackPaused = PreferencesUtils.isRecordingTrackPausedDefault(this);

        contentProviderUtils = new ContentProviderUtils(this);
        handleIntent(getIntent());

        sharedPreferences = PreferencesUtils.getSharedPreferences(this);

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindChangedCallback);
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
                    case 0:
                        return new StatisticsRecordingFragment();
                    case 1:
                        return ChartFragment.newInstance(false);
                    case 2:
                        return ChartFragment.newInstance(true);
                    default:
                        throw new RuntimeException("There isn't Fragment associated with the position: " + position);
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
                    default:
                        throw new RuntimeException("There isn't Fragment associated with the position: " + position);
                }
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
        boolean showOnLockScreen = PreferencesUtils.shouldShowStatsOnLockscreen(TrackRecordingActivity.this)
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
        boolean keepScreenOn = PreferencesUtils.shouldKeepScreenOn(TrackRecordingActivity.this)
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

        // Update UI
        this.invalidateOptionsMenu();

        //TODO Temporary fix, so that the TrackController is initialized properly after rotation when a new recording was started.
        if (trackId == -1L && trackId != recordingTrackId) {
            trackId = recordingTrackId;
        }

        if (trackId != -1L) {
            trackDataHub.loadTrack(trackId);
            trackController.onResume(true, recordingTrackPaused);
        }

        /*
         * If the binding has happened, then invoke the callback to start a new recording.
         * If the binding hasn't happened, then invoking the callback will have no effect.
         * But when the binding occurs, the callback will get invoked.
         */
        trackRecordingServiceConnection.startAndBind(this);
        bindChangedCallback.run();
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
        return R.layout.track_record;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.track_record, menu);

        insertMarkerMenuItem = menu.findItem(R.id.track_detail_insert_marker);
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
            case R.id.track_detail_menu_show_on_map:
                IntentDashboardUtils.startDashboard(this, trackId);
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
            case R.id.track_detail_settings:
                intent = IntentUtils.newIntent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Gets the {@link TrackDataHub}.
     */
    @Override
    public TrackDataHub getTrackDataHub() {
        return trackDataHub;
    }

    private void handleIntent(Intent intent) {
        trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1L);
        if (trackId == -1L) {
            return;
        }
        Track track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            track = contentProviderUtils.getLastTrack();
            if (track != null) {
                trackId = track.getId();
                return;
            }
            finish();
        }
    }

    /**
     * Updates the menu items.
     */
    private void updateMenuItems(boolean isPaused) {
        insertMarkerMenuItem.setVisible(!isPaused);
        markerListMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        setTitle(getString(isPaused ? R.string.generic_paused : R.string.generic_recording));
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
}