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
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;

import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.databinding.TrackRecordingBinding;
import de.dennisguse.opentracks.fragments.ChartFragment;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
import de.dennisguse.opentracks.fragments.IntervalsFragment;
import de.dennisguse.opentracks.fragments.StatisticsRecordingFragment;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
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
//NOTE: This activity does NOT react to preference changes of R.string.recording_track_id_key.
//This mode of communication should be removed anyhow.
public class TrackRecordingActivity extends AbstractActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller, TrackActivityDataHubInterface, ControllerFragment.Callback {

    public static final String EXTRA_TRACK_ID = "track_id";

    private static final String TAG = TrackRecordingActivity.class.getSimpleName();

    private static final String CURRENT_TAB_TAG_KEY = "current_tab_tag_key";

    // The following are setFrequency in onCreate
    private ContentProviderUtils contentProviderUtils;
    private SharedPreferences sharedPreferences;
    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private TrackDataHub trackDataHub;

    private TrackRecordingBinding viewBinding;

    // Initialized from Intent; if a new track recording is started, a new TrackId will be provided by TrackRecordingService
    @Deprecated
    private Track.Id trackId;

    private TrackRecordingService.RecordingStatus recordingStatus = TrackRecordingService.STATUS_DEFAULT;

    private final Runnable bindChangedCallback = new Runnable() {
        @Override
        public void run() {
            TrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
            if (service == null) {
                Log.w(TAG, "could not get TrackRecordingService");
                return;
            }

            service.getRecordingStatusObservable()
                    .observe(TrackRecordingActivity.this, status -> onRecordingStatusChanged(status));

            if (!service.isRecording()) {
                if (trackId == null) {
                    // trackId isn't initialized -> leads a new recording.
                    trackId = service.startNewTrack();
                } else {
                    // trackId is initialized -> resumes the track.
                    service.resumeTrack(trackId);
                }

                // A recording track is on.
                trackDataHub.loadTrack(trackId);
                trackDataHub.setRecordingStatus(recordingStatus);
            }
        }
    };

    private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PreferencesUtils.isKey(TrackRecordingActivity.this, R.string.stats_show_on_lockscreen_while_recording_key, key)) {
                setLockscreenPolicy();
            }
            if (PreferencesUtils.isKey(TrackRecordingActivity.this, R.string.stats_keep_screen_on_while_recording_key, key)) {
                setScreenOnPolicy();
            }
            if (PreferencesUtils.isKey(TrackRecordingActivity.this, R.string.stats_fullscreen_while_recording_key, key)) {
                setFullscreenPolicy();
            }
            if (PreferencesUtils.isKey(TrackRecordingActivity.this, R.string.recording_distance_interval_key, key)) {
                trackDataHub.setRecordingDistanceInterval(PreferencesUtils.getRecordingDistanceInterval(sharedPreferences, TrackRecordingActivity.this));
            }
            if (key == null) return;

            runOnUiThread(TrackRecordingActivity.this::invalidateOptionsMenu);
        }
    };

    private MenuItem insertMarkerMenuItem;
    private MenuItem markerListMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentProviderUtils = new ContentProviderUtils(this);
        sharedPreferences = PreferencesUtils.getSharedPreferences(this);
        trackId = null;
        if (savedInstanceState != null) {
            //Activity was recreated.
            trackId = savedInstanceState.getParcelable(EXTRA_TRACK_ID);
        } else {
            // Resume a track
            trackId = getIntent().getParcelableExtra(EXTRA_TRACK_ID);
            if (trackId != null && contentProviderUtils.getTrack(trackId) == null) {
                Log.w(TAG, "TrackId does not exists; cannot continue the recording.");
                finish();
            }
        }

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindChangedCallback);
        trackDataHub = new TrackDataHub(this);

        CustomFragmentPagerAdapter pagerAdapter = new CustomFragmentPagerAdapter(this);
        viewBinding.trackDetailActivityViewPager.setAdapter(pagerAdapter);
        new TabLayoutMediator(viewBinding.trackDetailActivityTablayout, viewBinding.trackDetailActivityViewPager,
                (tab, position) -> tab.setText(pagerAdapter.getPageTitle(position))).attach();
        if (savedInstanceState != null) {
            viewBinding.trackDetailActivityViewPager.setCurrentItem(savedInstanceState.getInt(CURRENT_TAB_TAG_KEY));
        }
    }

    @Override
    public void onAttachedToWindow() {
        setLockscreenPolicy();
        setScreenOnPolicy();
        setFullscreenPolicy();
        super.onAttachedToWindow();
    }

    private void setLockscreenPolicy() {
        boolean showOnLockScreen = PreferencesUtils.shouldShowStatsOnLockscreen(sharedPreferences, TrackRecordingActivity.this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(showOnLockScreen);
        } else if (showOnLockScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    private void setScreenOnPolicy() {
        boolean keepScreenOn = PreferencesUtils.shouldKeepScreenOn(sharedPreferences, TrackRecordingActivity.this);

        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void setFullscreenPolicy() {
        boolean keepScreenOn = PreferencesUtils.shouldUseFullscreen(sharedPreferences, TrackRecordingActivity.this);

        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

        trackRecordingServiceConnection.startConnection(this);
        trackDataHub.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update UI
        invalidateOptionsMenu();

        if (trackId != null) {
            //TODO Pass recordingStatus directly to them
            trackDataHub.loadTrack(trackId);
            trackDataHub.setRecordingStatus(recordingStatus);
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_TAB_TAG_KEY, viewBinding.trackDetailActivityViewPager.getCurrentItem());
        outState.putParcelable(EXTRA_TRACK_ID, trackId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        trackRecordingServiceConnection.unbind(this);
        trackDataHub.stop();
    }

    @Override
    protected View getRootView() {
        viewBinding = TrackRecordingBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
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
        updateMenuItems(recordingStatus.isPaused());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.track_detail_insert_marker) {
            Intent intent = IntentUtils
                    .newIntent(this, MarkerEditActivity.class)
                    .putExtra(MarkerEditActivity.EXTRA_TRACK_ID, trackId);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.track_detail_menu_show_on_map) {
            IntentDashboardUtils.startDashboard(this, true, trackId);
            return true;
        }

        if (item.getItemId() == R.id.track_detail_markers) {
            Intent intent = IntentUtils.newIntent(this, MarkerListActivity.class)
                    .putExtra(MarkerListActivity.EXTRA_TRACK_ID, trackId);
            startActivity(intent);
            return true;

        }

        if (item.getItemId() == R.id.track_detail_edit) {
            Intent intent = IntentUtils.newIntent(this, TrackEditActivity.class)
                    .putExtra(TrackEditActivity.EXTRA_TRACK_ID, trackId);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.track_detail_settings) {
            Intent intent = IntentUtils.newIntent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Gets the {@link TrackDataHub}.
     */
    @Override
    public TrackDataHub getTrackDataHub() {
        return trackDataHub;
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

    @Override
    public void recordStart() {
        updateMenuItems(false);
        trackRecordingServiceConnection.resumeTrack();
    }

    @Override
    public void recordPause() {
        updateMenuItems(true);
        trackRecordingServiceConnection.pauseTrack();
    }

    @Override
    public void recordStop() {
        trackRecordingServiceConnection.stopRecording(TrackRecordingActivity.this);
        Intent newIntent = IntentUtils.newIntent(TrackRecordingActivity.this, TrackRecordedActivity.class)
                .putExtra(TrackRecordedActivity.EXTRA_TRACK_ID, trackId);
        startActivity(newIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        updateMenuItems(true);
        finish();
    }

    private class CustomFragmentPagerAdapter extends FragmentStateAdapter {

        public CustomFragmentPagerAdapter(@NonNull FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return StatisticsRecordingFragment.newInstance();
                case 1:
                    return IntervalsFragment.newInstance(trackId, false);
                case 2:
                    return ChartFragment.newInstance(false);
                case 3:
                    return ChartFragment.newInstance(true);
                default:
                    throw new RuntimeException("There isn't Fragment associated with the position: " + position);
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }

        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.track_detail_stats_tab);
                case 1:
                    return getString(R.string.track_detail_intervals_tab);
                case 2:
                    return getString(R.string.settings_chart_by_time);
                case 3:
                    return getString(R.string.settings_chart_by_distance);
                default:
                    throw new RuntimeException("There isn't Fragment associated with the position: " + position);
            }
        }
    }

    private void onRecordingStatusChanged(TrackRecordingService.RecordingStatus status) {
        recordingStatus = status;

        trackDataHub.setRecordingStatus(recordingStatus);

        setLockscreenPolicy();
        setScreenOnPolicy();
    }
}
