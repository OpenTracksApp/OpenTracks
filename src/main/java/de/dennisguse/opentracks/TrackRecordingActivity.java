package de.dennisguse.opentracks;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;

import java.time.Duration;
import java.util.List;

import de.dennisguse.opentracks.chart.ChartFragment;
import de.dennisguse.opentracks.chart.TrackDataHubInterface;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.TrackDataHub;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.databinding.TrackRecordingBinding;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
import de.dennisguse.opentracks.fragments.StatisticsRecordingFragment;
import de.dennisguse.opentracks.sensors.GpsStatusValue;
import de.dennisguse.opentracks.services.RecordingStatus;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.SettingsActivity;
import de.dennisguse.opentracks.ui.intervals.IntervalsFragment;
import de.dennisguse.opentracks.ui.markers.MarkerEditActivity;
import de.dennisguse.opentracks.ui.markers.MarkerListActivity;
import de.dennisguse.opentracks.ui.util.ActivityUtils;
import de.dennisguse.opentracks.util.IntentDashboardUtils;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * An activity to show the track detail, record a new track or resumes an existing one.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class TrackRecordingActivity extends AbstractActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller, TrackDataHubInterface {

    public static final String EXTRA_TRACK_ID = "track_id";

    private static final String TAG = TrackRecordingActivity.class.getSimpleName();

    private static final String CURRENT_TAB_TAG_KEY = "current_tab_tag_key";

    private Snackbar snackbar;

    // The following are setFrequency in onCreate
    private ContentProviderUtils contentProviderUtils;
    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private TrackDataHub trackDataHub;

    private TrackRecordingBinding viewBinding;

    private Track.Id trackId;

    private RecordingStatus recordingStatus = TrackRecordingService.STATUS_DEFAULT;

    private final TrackRecordingServiceConnection.Callback bindChangedCallback = (service, unused) -> {
        service.getRecordingStatusObservable()
                .observe(TrackRecordingActivity.this, this::onRecordingStatusChanged);

        service.getGpsStatusObservable()
                .observe(TrackRecordingActivity.this, this::onGpsStatusChanged);

        if (!service.isRecording()) {
            finish();
            return;
        }

        trackDataHub.loadTrack(trackId);
        trackDataHub.setRecordingStatus(recordingStatus);
    };

    private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.stats_show_on_lockscreen_while_recording_key, key)) {
            setLockscreenPolicy();
        }
        if (PreferencesUtils.isKey(R.string.stats_keep_screen_on_while_recording_key, key)) {
            setScreenOnPolicy();
        }
        if (PreferencesUtils.isKey(R.string.stats_fullscreen_while_recording_key, key)) {
            setFullscreenPolicy();
        }
        if (key == null) return;

        runOnUiThread(TrackRecordingActivity.this::invalidateOptionsMenu); //TODO Should not be necessary
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentProviderUtils = new ContentProviderUtils(this);

        trackId = getIntent().getParcelableExtra(EXTRA_TRACK_ID);
        if (trackId == null) {
            throw new RuntimeException("TrackId is mandatory");
        }
        if (contentProviderUtils.getTrack(trackId) == null) {
            Log.w(TAG, "TrackId does not exists.");
            finish();
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

        viewBinding.trackRecordingFabAction.setImageResource(R.drawable.ic_baseline_stop_24);
        viewBinding.trackRecordingFabAction.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.opentracks));
        viewBinding.trackRecordingFabAction.setBackgroundColor(ContextCompat.getColor(this, R.color.opentracks));
        viewBinding.trackRecordingFabAction.setOnLongClickListener((view) -> {
            ActivityUtils.vibrate(this, Duration.ofSeconds(1));
            trackRecordingServiceConnection.stopRecording(TrackRecordingActivity.this);
            Intent newIntent = IntentUtils.newIntent(TrackRecordingActivity.this, TrackStoppedActivity.class)
                    .putExtra(TrackStoppedActivity.EXTRA_TRACK_ID, trackId);
            startActivity(newIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
            return true;
        });
        viewBinding.trackRecordingFabAction.setOnClickListener((view) -> Toast.makeText(TrackRecordingActivity.this, getString(R.string.hold_to_stop), Toast.LENGTH_LONG).show());

        setSupportActionBar(viewBinding.bottomAppBar);
    }

    @Override
    public void onAttachedToWindow() {
        setLockscreenPolicy();
        setScreenOnPolicy();
        setFullscreenPolicy();
        super.onAttachedToWindow();
    }

    private void setLockscreenPolicy() {
        boolean showOnLockScreen = PreferencesUtils.shouldShowStatsOnLockscreen();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(showOnLockScreen);
        } else if (showOnLockScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    private void setScreenOnPolicy() {
        boolean keepScreenOn = PreferencesUtils.shouldKeepScreenOn();

        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void setFullscreenPolicy() {
        boolean fullscreen = PreferencesUtils.shouldUseFullscreen();

        WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            if (fullscreen) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            } else {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
            }
            return;
        }

        if (fullscreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        trackRecordingServiceConnection.bind(this);
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

        trackRecordingServiceConnection.bind(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_TAB_TAG_KEY, viewBinding.trackDetailActivityViewPager.getCurrentItem());
    }

    @Override
    protected void onStop() {
        super.onStop();
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        trackRecordingServiceConnection.unbind(this);
        trackDataHub.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;
        trackRecordingServiceConnection = null;
    }

    @NonNull
    @Override
    protected View createRootView() {
        viewBinding = TrackRecordingBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.track_record, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.track_detail_menu_show_on_map) {
            IntentDashboardUtils.showTrackOnMap(this, true, trackId);
            return true;
        }

        if (item.getItemId() == R.id.track_detail_insert_marker) {
            TrackPoint trackPoint = trackRecordingServiceConnection.getTrackRecordingService().getLastStoredTrackPointWithLocation();
            if (trackPoint == null) {
                return true;
            }
            Intent intent = IntentUtils
                    .newIntent(this, MarkerEditActivity.class)
                    .putExtra(MarkerEditActivity.EXTRA_TRACK_ID, trackId)
                    .putExtra(MarkerEditActivity.EXTRA_LOCATION, trackPoint.getLocation());
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.track_detail_menu_select_layout) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            List<String> layoutNames = PreferencesUtils.getAllCustomLayoutNames();
            builder.setTitle(getString(R.string.custom_layout_select_layout)).setItems(layoutNames.toArray(new String[0]), (dialog, which) -> PreferencesUtils.setDefaultLayout(layoutNames.get(which)));
            builder.create().show();
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

    @Override
    public void onChooseActivityTypeDone(ActivityType activityType) {
        Track track = contentProviderUtils.getTrack(trackId);
        String activityTypeLocalized = getString(activityType.getLocalizedStringId());
        track.setActivityTypeLocalizedAndUpdateActivityType(this, activityTypeLocalized);

        contentProviderUtils.updateTrack(track);
    }

    private class CustomFragmentPagerAdapter extends FragmentStateAdapter {

        public CustomFragmentPagerAdapter(@NonNull FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 0 -> StatisticsRecordingFragment.newInstance();
                case 1 -> IntervalsFragment.newInstance(trackId, false);
                case 2 -> ChartFragment.newInstance(false);
                case 3 -> ChartFragment.newInstance(true);
                default ->
                        throw new RuntimeException("There isn't Fragment associated with the position: " + position);
            };
        }

        @Override
        public int getItemCount() {
            return 4;
        }

        public CharSequence getPageTitle(int position) {
            return switch (position) {
                case 0 -> getString(R.string.track_detail_stats_tab);
                case 1 -> getString(R.string.track_detail_intervals_tab);
                case 2 -> getString(R.string.settings_chart_by_time);
                case 3 -> getString(R.string.settings_chart_by_distance);
                default ->
                        throw new RuntimeException("There isn't Fragment associated with the position: " + position);
            };
        }
    }

    private void onRecordingStatusChanged(RecordingStatus status) {
        if (!status.isRecording()) {
            finish();
        }
        recordingStatus = status;

        trackDataHub.setRecordingStatus(recordingStatus);

        setLockscreenPolicy();
        setScreenOnPolicy();
    }

    private void onGpsStatusChanged(GpsStatusValue gpsStatusValue) {
        boolean snackbarShowing = snackbar != null && snackbar.isShown();
        if (gpsStatusValue.isGpsStarted()) {
            if (snackbarShowing) {
                snackbar.dismiss();
            }
            return;
        }

        if (snackbarShowing) {
            return;
        }

        if (!PreferencesUtils.shouldShowGpsDisabledWarning()) {
            return;
        }

        snackbar = Snackbar
                .make(viewBinding.trackRecordingCoordinatorLayout,
                        getString(R.string.gps_recording_status, getString(gpsStatusValue.message), getString(R.string.gps_recording_without_signal)),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.generic_dismiss), v -> {
                });
        snackbar.show();
    }
}