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
import de.dennisguse.opentracks.fragments.StatisticsRecordedFragment;
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
public class TrackRecordedActivity extends AbstractListActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller, ConfirmDeleteDialogFragment.ConfirmDeleteCaller, TrackActivityDataHubInterface {

    public static final String EXTRA_TRACK_ID = "track_id";
    public static final String EXTRA_MARKER_ID = "marker_id";

    private static final String TAG = TrackRecordedActivity.class.getSimpleName();

    private static final String CURRENT_TAB_TAG_KEY = "current_tab_tag_key";

    // The following are set in onCreate.
    private ContentProviderUtils contentProviderUtils;
    private TrackDataHub trackDataHub;
    private ViewPager pager;

    // From intent.
    private long trackId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contentProviderUtils = new ContentProviderUtils(this);
        handleIntent(getIntent());

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
                        return new StatisticsRecordedFragment();
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

        if (trackId != -1L) {
            trackDataHub.loadTrack(trackId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.track_detail_markers).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.findItem(R.id.track_detail_resume_track).setVisible(!PreferencesUtils.isRecording(this));
        Track track = contentProviderUtils.getTrack(trackId);
        setTitle(track != null ? track.getName() : "");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.track_detail_share:
                intent = IntentUtils.newShareFileIntent(this, new long[]{trackId});
                intent = Intent.createChooser(intent, null);
                startActivity(intent);
                return true;
            case R.id.track_detail_menu_show_on_map:
                IntentDashboardUtils.startDashboard(this, new long[]{trackId}, false);
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
            case R.id.track_detail_resume_track:
                Intent newIntent = IntentUtils.newIntent(TrackRecordedActivity.this, TrackRecordingActivity.class)
                        .putExtra(TrackRecordingActivity.EXTRA_TRACK_ID, trackId);
                startActivity(newIntent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
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
        // Not needed.
        return null;
    }

    @Override
    protected void onDeleted() {
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
            Log.e(TAG, "TrackDetailActivity needs EXTRA_TRACK_ID.");
            finish();
        }
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