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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;

import de.dennisguse.opentracks.chart.ChartFragment;
import de.dennisguse.opentracks.chart.TrackDataHubInterface;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.TrackDataHub;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.databinding.TrackRecordedBinding;
import de.dennisguse.opentracks.fragments.StatisticsRecordedFragment;
import de.dennisguse.opentracks.services.RecordingStatus;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.SettingsActivity;
import de.dennisguse.opentracks.share.ShareUtils;
import de.dennisguse.opentracks.ui.aggregatedStatistics.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.ui.intervals.IntervalsFragment;
import de.dennisguse.opentracks.ui.markers.MarkerListActivity;
import de.dennisguse.opentracks.util.IntentDashboardUtils;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * An activity to show the track detail, record a new track or resumes an existing one.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
//TODO Should not use TrackRecordingServiceConnection; only used to determine if there is NO current recording, to enable resume functionality.
public class TrackRecordedActivity extends AbstractTrackDeleteActivity implements ConfirmDeleteDialogFragment.ConfirmDeleteCaller, TrackDataHubInterface {

    private static final String TAG = TrackRecordedActivity.class.getSimpleName();

    public static final String VIEW_TRACK_ICON = "track_icon";

    public static final String EXTRA_TRACK_ID = "track_id";

    private static final String CURRENT_TAB_TAG_KEY = "current_tab_tag_key";

    // The following are setFrequency in onCreate.
    private ContentProviderUtils contentProviderUtils;
    private TrackDataHub trackDataHub;

    private TrackRecordedBinding viewBinding;

    private Track.Id trackId;
    private RecordingStatus recordingStatus = TrackRecordingService.STATUS_DEFAULT;

    private TrackRecordingServiceConnection trackRecordingServiceConnection;

    private final TrackRecordingServiceConnection.Callback bindCallback = (service, unused) -> service.getRecordingStatusObservable()
            .observe(TrackRecordedActivity.this, this::onRecordingStatusChanged);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contentProviderUtils = new ContentProviderUtils(this);

        handleIntent(getIntent());

        trackDataHub = new TrackDataHub(this);

        CustomFragmentPagerAdapter pagerAdapter = new CustomFragmentPagerAdapter(this);
        viewBinding.trackDetailActivityViewPager.setAdapter(pagerAdapter);
        new TabLayoutMediator(viewBinding.trackDetailActivityTablayout, viewBinding.trackDetailActivityViewPager,
                (tab, position) -> tab.setText(pagerAdapter.getPageTitle(position))).attach();
        if (savedInstanceState != null) {
            viewBinding.trackDetailActivityViewPager.setCurrentItem(savedInstanceState.getInt(CURRENT_TAB_TAG_KEY));
        }

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindCallback);

        Track track = contentProviderUtils.getTrack(trackId);
        viewBinding.bottomAppBarLayout.bottomAppBar.replaceMenu(R.menu.track_detail);
        setSupportActionBar(viewBinding.bottomAppBarLayout.bottomAppBar);

        postponeEnterTransition();
    }

    @Override
    protected void onStart() {
        super.onStart();

        trackDataHub.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update UI
        this.invalidateOptionsMenu();

        if (trackId != null) {
            trackDataHub.loadTrack(trackId);
        }

        trackRecordingServiceConnection.bind(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        trackRecordingServiceConnection.unbind(this);
        trackDataHub.stop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_TAB_TAG_KEY, viewBinding.trackDetailActivityViewPager.getCurrentItem());
    }

    @Override
    protected View getRootView() {
        viewBinding = TrackRecordedBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.track_detail_markers).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.findItem(R.id.track_detail_resume_track).setVisible(!recordingStatus.isRecording());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.track_detail_share) {
            Intent intent = Intent.createChooser(ShareUtils.newShareFileIntent(this, trackId), null);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.track_detail_menu_show_on_map) {
            IntentDashboardUtils.showTrackOnMap(this, false, trackId);
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

        if (item.getItemId() == R.id.track_detail_delete) {
            deleteTracks(trackId);
            return true;
        }

        if (item.getItemId() == R.id.track_detail_resume_track) {
            TrackRecordingServiceConnection.execute(this, (service, connection) -> {
                service.resumeTrack(trackId);

                Intent newIntent = IntentUtils.newIntent(TrackRecordedActivity.this, TrackRecordingActivity.class)
                        .putExtra(TrackRecordingActivity.EXTRA_TRACK_ID, trackId);
                startActivity(newIntent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                finish();
            });
            return true;
        }

        if (item.getItemId() == R.id.track_detail_settings) {
            startActivity(IntentUtils.newIntent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    protected Track.Id getRecordingTrackId() {
        return recordingStatus.trackId();
    }

    @Override
    protected void onDeleteConfirmed() {
        runOnUiThread(this::finish);
    }

    public void onDeleteFinished() {
        runOnUiThread(this::finish);
    }

    /**
     * Gets the {@link TrackDataHub}.
     */
    @Override
    public TrackDataHub getTrackDataHub() {
        return trackDataHub;
    }

    private void handleIntent(Intent intent) {
        trackId = intent.getParcelableExtra(EXTRA_TRACK_ID);
        if (trackId == null) {
            Log.e(TAG, TrackRecordedActivity.class.getSimpleName() + " needs EXTRA_TRACK_ID.");
            finish();
        }
    }

    private class CustomFragmentPagerAdapter extends FragmentStateAdapter {

        public CustomFragmentPagerAdapter(@NonNull FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 0 -> StatisticsRecordedFragment.newInstance(trackId);
                case 1 -> IntervalsFragment.newInstance(trackId, true);
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

    public void startPostponedEnterTransitionWith(View viewIcon) {
        ViewCompat.setTransitionName(viewIcon, TrackRecordedActivity.VIEW_TRACK_ICON);
        startPostponedEnterTransition();
    }

    private void onRecordingStatusChanged(RecordingStatus status) {
        recordingStatus = status;
    }
}