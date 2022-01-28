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
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.cursoradapter.widget.ResourceCursorAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.tables.TracksColumns;
import de.dennisguse.opentracks.databinding.TrackListBinding;
import de.dennisguse.opentracks.services.RecordingStatus;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.services.handlers.GpsStatusValue;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.SettingsActivity;
import de.dennisguse.opentracks.share.ShareUtils;
import de.dennisguse.opentracks.ui.aggregatedStatistics.AggregatedStatisticsActivity;
import de.dennisguse.opentracks.ui.aggregatedStatistics.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.ui.markers.MarkerListActivity;
import de.dennisguse.opentracks.ui.util.ActivityUtils;
import de.dennisguse.opentracks.ui.util.ListItemUtils;
import de.dennisguse.opentracks.util.IntentDashboardUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;

/**
 * An activity displaying a list of tracks.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackListActivity extends AbstractTrackDeleteActivity implements ConfirmDeleteDialogFragment.ConfirmDeleteCaller, ControllerFragment.Callback {

    private static final String TAG = TrackListActivity.class.getSimpleName();

    // The following are setFrequency in onCreate
    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private ResourceCursorAdapter resourceCursorAdapter;

    private TrackListBinding viewBinding;

    private final TrackLoaderCallBack loaderCallbacks = new TrackLoaderCallBack();

    // Preferences
    private boolean metricUnits = true;

    private GpsStatusValue gpsStatusValue = TrackRecordingService.STATUS_GPS_DEFAULT;
    private RecordingStatus recordingStatus = TrackRecordingService.STATUS_DEFAULT;

    // Callback when an item is selected in the contextual action mode
    private final ActivityUtils.ContextualActionModeCallback contextualActionModeCallback = new ActivityUtils.ContextualActionModeCallback() {

        @Override
        public void onPrepare(Menu menu, int[] positions, long[] trackIds, boolean showSelectAll) {
            boolean isSingleSelection = trackIds.length == 1;

            menu.findItem(R.id.list_context_menu_edit).setVisible(isSingleSelection);
            menu.findItem(R.id.list_context_menu_select_all).setVisible(showSelectAll);
        }

        @Override
        public boolean onClick(int itemId, int[] positions, long[] trackIds) {
            return handleContextItem(itemId, trackIds);
        }
    };

    private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.stats_units_key, key)) {
            metricUnits = PreferencesUtils.isMetricUnits();
        }
        if (key != null) {
            runOnUiThread(() -> {
                TrackListActivity.this.invalidateOptionsMenu();
                loaderCallbacks.restart();
            });
        }
    };

    // Menu items
    private MenuItem searchMenuItem;
    private MenuItem startGpsMenuItem;

    private final TrackRecordingServiceConnection.Callback bindChangedCallback = service -> {
        service.getRecordingStatusObservable()
                .observe(TrackListActivity.this, this::onRecordingStatusChanged);

        service.getGpsStatusObservable()
                .observe(TrackListActivity.this, this::onGpsStatusChanged);

        updateGpsMenuItem(true, recordingStatus.isRecording());

        if (service.getGpsStatusObservable().getValue().isGpsStarted()) {
            return;
        }

        //TODO Not cool to do this in a callback that might be called more than once!
        service.tryStartSensors();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Reset theme after splash
        setTheme(R.style.ThemeCustom);

        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindChangedCallback);

        viewBinding.trackList.setEmptyView(viewBinding.trackListEmptyView);
        viewBinding.trackList.setOnItemClickListener((parent, view, position, trackId) -> {
            if (recordingStatus.isRecording() && trackId == recordingStatus.getTrackId().getId()) {
                // Is recording -> open record activity.
                Intent newIntent = IntentUtils.newIntent(TrackListActivity.this, TrackRecordingActivity.class)
                        .putExtra(TrackRecordedActivity.EXTRA_TRACK_ID, new Track.Id(trackId));
                startActivity(newIntent);
            } else {
                // Not recording -> open detail activity.
                Intent newIntent = IntentUtils.newIntent(TrackListActivity.this, TrackRecordedActivity.class)
                        .putExtra(TrackRecordedActivity.EXTRA_TRACK_ID, new Track.Id(trackId));
                ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(
                        this,
                        new Pair<>(view.findViewById(R.id.list_item_icon), TrackRecordedActivity.VIEW_TRACK_ICON));
                startActivity(newIntent, activityOptions.toBundle());
            }
        });

        resourceCursorAdapter = new ResourceCursorAdapter(this, R.layout.list_item, null, 0) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                int idIndex = cursor.getColumnIndexOrThrow(TracksColumns._ID);
                int iconIndex = cursor.getColumnIndexOrThrow(TracksColumns.ICON);
                int nameIndex = cursor.getColumnIndexOrThrow(TracksColumns.NAME);
                int totalTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
                int totalDistanceIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
                int startTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
                int startTimeOffsetIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME_OFFSET);
                int categoryIndex = cursor.getColumnIndexOrThrow(TracksColumns.CATEGORY);
                int descriptionIndex = cursor.getColumnIndexOrThrow(TracksColumns.DESCRIPTION);
                int markerCountIndex = cursor.getColumnIndexOrThrow(TracksColumns.MARKER_COUNT);

                Track.Id trackId = new Track.Id(cursor.getLong(idIndex));
                boolean isRecording = trackId.equals(recordingStatus.getTrackId());
                String icon = cursor.getString(iconIndex);
                int iconId = TrackIconUtils.getIconDrawable(icon);
                String name = cursor.getString(nameIndex);
                String totalTime = StringUtils.formatElapsedTime(Duration.ofMillis(cursor.getLong(totalTimeIndex)));
                String totalDistance = StringUtils.formatDistance(TrackListActivity.this, Distance.of(cursor.getDouble(totalDistanceIndex)), metricUnits);
                int markerCount = cursor.getInt(markerCountIndex);
                long startTime = cursor.getLong(startTimeIndex);
                int startTimeOffset = cursor.getInt(startTimeOffsetIndex);
                String category = icon != null && !icon.equals("") ? null : cursor.getString(categoryIndex);
                String description = cursor.getString(descriptionIndex);

                ListItemUtils.setListItem(TrackListActivity.this, view, isRecording, recordingStatus.isPaused(),
                        iconId, R.string.image_track, name, totalTime, totalDistance, markerCount,
                        OffsetDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneOffset.ofTotalSeconds(startTimeOffset)),
                        category, description, false);
            }
        };
        viewBinding.trackList.setAdapter(resourceCursorAdapter);
        ActivityUtils.configureListViewContextualMenu(viewBinding.trackList, contextualActionModeCallback);

        loadData(getIntent());

        requestGPSPermissions();
    }

    @Override
    protected void setupActionBarBack(Toolbar toolbar) {
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_logo_color_24dp);
    }

    @Override
    protected void onStart() {
        super.onStart();

        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        trackRecordingServiceConnection.startConnection(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update UI
        this.invalidateOptionsMenu();
        LoaderManager.getInstance(this).restartLoader(0, null, loaderCallbacks);
    }

    @Override
    protected void onStop() {
        super.onStop();

        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        trackRecordingServiceConnection.unbind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;
        trackRecordingServiceConnection = null;
    }

    @Override
    protected View getRootView() {
        viewBinding = TrackListBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.track_list, menu);

        searchMenuItem = menu.findItem(R.id.track_list_search);
        SearchView searchView = ActivityUtils.configureSearchWidget(this, searchMenuItem);
        searchView.setOnCloseListener(() -> {
            searchView.clearFocus();
            searchMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            return true;
        });

        startGpsMenuItem = menu.findItem(R.id.track_list_start_gps);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateGpsMenuItem(gpsStatusValue.isGpsStarted(), recordingStatus.isRecording());

        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setQuery("", false);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.track_list_start_gps) {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else {
                // Invoke trackRecordingService
                if (!gpsStatusValue.isGpsStarted()) {
                    trackRecordingServiceConnection.startAndBindWithCallback(this);
                } else {
                    TrackRecordingService trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
                    if (trackRecordingService != null) {
                        trackRecordingService.stopSensorsAndShutdown(); //TODO Handle this in TrackRecordingServiceConnection
                    }
                    trackRecordingServiceConnection.unbindAndStop(this);
                }

                // Update menu after starting or stopping gps
                this.invalidateOptionsMenu();
            }

            return true;
        }

        if (item.getItemId() == R.id.track_list_aggregated_stats) {
            startActivity(IntentUtils.newIntent(this, AggregatedStatisticsActivity.class));
            return true;
        }

        if (item.getItemId() == R.id.track_list_markers) {
            startActivity(IntentUtils.newIntent(this, MarkerListActivity.class));
            return true;
        }

        if (item.getItemId() == R.id.track_list_settings) {
            startActivity(IntentUtils.newIntent(this, SettingsActivity.class));
            return true;
        }

        if (item.getItemId() == R.id.track_list_search) {
            SearchView searchView = (SearchView) searchMenuItem.getActionView();
            searchView.setIconified(false);
            searchMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH && searchMenuItem != null) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void overridePendingTransition(int enterAnim, int exitAnim) {
        //Disable animations as it is weird going into searchMode; looks okay for SplashScreen.
    }

    @Override
    public void onBackPressed() {
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        }

        if (loaderCallbacks.getSearchQuery() != null) {
            loaderCallbacks.setSearch(null);
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadData(intent);
    }

    private void loadData(Intent intent) {
        String searchQuery = null;
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchQuery = intent.getStringExtra(SearchManager.QUERY);
        }

        loaderCallbacks.setSearch(searchQuery);
    }

    @Override
    protected void onDeleteConfirmed() {
        // Do nothing
    }

    @Nullable
    @Override
    protected Track.Id getRecordingTrackId() {
        return recordingStatus.getTrackId();
    }

    private void requestGPSPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (fineLocationGranted == null || !fineLocationGranted
                            || coarseLocationGranted == null || !coarseLocationGranted) {
                        Toast.makeText(this, R.string.permission_gps_failed, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
        );
        String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        locationPermissionRequest.launch(permissions);
    }

    /**
     * Updates the menu items with the icon specified.
     *
     * @param isGpsStarted true if gps is started
     * @param isRecording  true if recording
     */
    //TODO Check if if can be avoided to call this outside of onGpsStatusChanged()
    private void updateGpsMenuItem(boolean isGpsStarted, boolean isRecording) {
        if (startGpsMenuItem != null) {
            startGpsMenuItem.setVisible(!isRecording);
            if (!isRecording) {
                startGpsMenuItem.setTitle(isGpsStarted ? R.string.menu_stop_gps : R.string.menu_start_gps);
                startGpsMenuItem.setIcon(isGpsStarted ? gpsStatusValue.icon : R.drawable.ic_gps_off_24dp);
                if (startGpsMenuItem.getIcon() instanceof AnimatedVectorDrawable) {
                    ((AnimatedVectorDrawable) startGpsMenuItem.getIcon()).start();
                }
            }
        }
    }

    /**
     * Handles a context item selection.
     *
     * @param itemId       the menu item id
     * @param longTrackIds the track ids
     * @return true if handled.
     */
    private boolean handleContextItem(int itemId, long... longTrackIds) {
        Track.Id[] trackIds = new Track.Id[longTrackIds.length];
        for (int i = 0; i < longTrackIds.length; i++) {
            trackIds[i] = new Track.Id(longTrackIds[i]);
        }

        if (itemId == R.id.list_context_menu_show_on_map) {
            IntentDashboardUtils.startDashboard(this, false, trackIds);
            return true;
        }

        if (itemId == R.id.list_context_menu_share) {
            Intent intent = ShareUtils.newShareFileIntent(this, trackIds);
            intent = Intent.createChooser(intent, null);
            startActivity(intent);
            return true;
        }

        if (itemId == R.id.list_context_menu_edit) {
            Intent intent = IntentUtils.newIntent(this, TrackEditActivity.class)
                    .putExtra(TrackEditActivity.EXTRA_TRACK_ID, trackIds[0]);
            startActivity(intent);
            return true;
        }

        if (itemId == R.id.list_context_menu_delete) {
            deleteTracks(trackIds);
            return true;
        }

        if (itemId == R.id.list_context_menu_aggregated_stats) {
            Intent intent = IntentUtils.newIntent(this, AggregatedStatisticsActivity.class)
                    .putParcelableArrayListExtra(AggregatedStatisticsActivity.EXTRA_TRACK_IDS, new ArrayList<>(Arrays.asList(trackIds)));
            startActivity(intent);
            return true;
        }

        if (itemId == R.id.list_context_menu_select_all) {
            for (int i = 0; i < viewBinding.trackList.getCount(); i++) {
                viewBinding.trackList.setItemChecked(i, true);
            }
            return false;
        }

        return false;
    }

    private class TrackLoaderCallBack implements LoaderManager.LoaderCallbacks<Cursor> {

        private String searchQuery = null;

        public String getSearchQuery() {
            return searchQuery;
        }

        public void setSearch(String searchQuery) {
            this.searchQuery = searchQuery;
            restart();
            if (searchQuery != null) {
                setTitle(searchQuery);
            } else {
                setTitle(R.string.app_name);
            }
        }

        public void restart() {
            LoaderManager.getInstance(TrackListActivity.this).restartLoader(0, null, loaderCallbacks);
        }

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
            final String[] PROJECTION = new String[]{TracksColumns._ID, TracksColumns.NAME,
                    TracksColumns.DESCRIPTION, TracksColumns.CATEGORY, TracksColumns.STARTTIME, TracksColumns.STARTTIME_OFFSET,
                    TracksColumns.TOTALDISTANCE, TracksColumns.TOTALTIME, TracksColumns.ICON, TracksColumns.MARKER_COUNT};

            final String sortOrder = TracksColumns.STARTTIME + " DESC";

            if (searchQuery == null) {
                return new CursorLoader(TrackListActivity.this, TracksColumns.CONTENT_URI, PROJECTION, null, null, sortOrder);
            } else {
                final String SEARCH_QUERY = TracksColumns.NAME + " LIKE ? OR " +
                        TracksColumns.DESCRIPTION + " LIKE ? OR " +
                        TracksColumns.CATEGORY + " LIKE ?";
                final String[] selectionArgs = new String[]{"%" + searchQuery + "%", "%" + searchQuery + "%", "%" + searchQuery + "%"};
                return new CursorLoader(TrackListActivity.this, TracksColumns.CONTENT_URI, PROJECTION, SEARCH_QUERY, selectionArgs, sortOrder);
            }
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
            resourceCursorAdapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            resourceCursorAdapter.swapCursor(null);
        }
    }

    @Override
    public void recordStart() {
        if (recordingStatus.getTrackId() == null) {
            // Not recording -> Recording
            updateGpsMenuItem(false, true);
            Intent newIntent = IntentUtils.newIntent(TrackListActivity.this, TrackRecordingActivity.class);
            startActivity(newIntent);
        } else if (recordingStatus.isPaused()) {
            // Paused -> Resume
            updateGpsMenuItem(false, true);
            trackRecordingServiceConnection.resumeTrack();
        }
    }

    @Override
    public void recordPause() {
        updateGpsMenuItem(false, true);
        trackRecordingServiceConnection.pauseTrack();
    }

    @Override
    public void recordStop() {
        updateGpsMenuItem(false, false);
        trackRecordingServiceConnection.stopRecording(TrackListActivity.this);
    }

    public void onGpsStatusChanged(GpsStatusValue newStatus) {
        gpsStatusValue = newStatus;
        updateGpsMenuItem(true, recordingStatus.isRecording());
    }

    private void onRecordingStatusChanged(RecordingStatus status) {
        recordingStatus = status;
    }
}
