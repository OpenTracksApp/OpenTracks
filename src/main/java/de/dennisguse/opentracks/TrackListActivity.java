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

import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.ResourceCursorAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.button.MaterialButton;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;

import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.tables.TracksColumns;
import de.dennisguse.opentracks.databinding.TrackListBinding;
import de.dennisguse.opentracks.services.RecordingStatus;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.services.handlers.GpsStatusValue;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.SettingsActivity;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.share.ShareUtils;
import de.dennisguse.opentracks.ui.aggregatedStatistics.AggregatedStatisticsActivity;
import de.dennisguse.opentracks.ui.aggregatedStatistics.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.ui.markers.MarkerListActivity;
import de.dennisguse.opentracks.ui.util.ActivityUtils;
import de.dennisguse.opentracks.ui.util.ListItemUtils;
import de.dennisguse.opentracks.util.IntentDashboardUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PermissionRequester;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * An activity displaying a list of tracks.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackListActivity extends AbstractTrackDeleteActivity implements ConfirmDeleteDialogFragment.ConfirmDeleteCaller {

    private static final String TAG = TrackListActivity.class.getSimpleName();

    // The following are set in onCreate
    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private ResourceCursorAdapter resourceCursorAdapter;

    private TrackListBinding viewBinding;

    private final TrackLoaderCallBack loaderCallbacks = new TrackLoaderCallBack();

    // Preferences
    private UnitSystem unitSystem = UnitSystem.defaultUnitSystem();

    private GpsStatusValue gpsStatusValue = TrackRecordingService.STATUS_GPS_DEFAULT;
    private RecordingStatus recordingStatus = TrackRecordingService.STATUS_DEFAULT;

    // Callback when an item is selected in the contextual action mode
    private final ActivityUtils.ContextualActionModeCallback contextualActionModeCallback = new ActivityUtils.ContextualActionModeCallback() {

        @Override
        public void onPrepare(Menu menu, int[] positions, long[] trackIds, boolean showSelectAll) {
            boolean isSingleSelection = trackIds.length == 1;

            viewBinding.bottomAppBar.performHide(true);
            viewBinding.trackListFabAction.setVisibility(View.INVISIBLE);

            menu.findItem(R.id.list_context_menu_edit).setVisible(isSingleSelection);
            menu.findItem(R.id.list_context_menu_select_all).setVisible(showSelectAll);
        }

        @Override
        public boolean onClick(int itemId, int[] positions, long[] trackIds) {
            return handleContextItem(itemId, trackIds);
        }

        @Override
        public void onDestroy() {
            viewBinding.trackListFabAction.setVisibility(View.VISIBLE);
            viewBinding.bottomAppBar.performShow(true);
        }
    };

    private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.stats_units_key, key)) {
            unitSystem = PreferencesUtils.getUnitSystem();
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

    private final TrackRecordingServiceConnection.Callback bindChangedCallback = (service, unused) -> {
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
        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        requestRequiredPermissions();

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindChangedCallback);

        viewBinding.aggregatedStatsButton.setOnClickListener((view) -> startActivity(IntentUtils.newIntent(this, AggregatedStatisticsActivity.class)));
        viewBinding.sensorStartButton.setOnClickListener((view) -> {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else {
                if (gpsStatusValue.isGpsStarted()) {
                    trackRecordingServiceConnection.unbindAndStop(this);
                } else {
                    trackRecordingServiceConnection.startAndBindWithCallback(this);
                }
            }
        });

        viewBinding.trackList.setEmptyView(viewBinding.trackListEmptyView);
        viewBinding.trackList.setOnItemClickListener((parent, view, position, trackIdId) -> {
            Track.Id trackId = new Track.Id(trackIdId);
            if (recordingStatus.isRecording() && trackId.equals(recordingStatus.getTrackId())) {
                // Is recording -> open record activity.
                Intent newIntent = IntentUtils.newIntent(TrackListActivity.this, TrackRecordingActivity.class)
                        .putExtra(TrackRecordedActivity.EXTRA_TRACK_ID, trackId);
                startActivity(newIntent);
            } else {
                // Not recording -> open detail activity.
                Intent newIntent = IntentUtils.newIntent(TrackListActivity.this, TrackRecordedActivity.class)
                        .putExtra(TrackRecordedActivity.EXTRA_TRACK_ID, trackId);
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
                int activityTypeIndex = cursor.getColumnIndexOrThrow(TracksColumns.ACTIVITY_TYPE_LOCALIZED);
                int descriptionIndex = cursor.getColumnIndexOrThrow(TracksColumns.DESCRIPTION);
                int markerCountIndex = cursor.getColumnIndexOrThrow(TracksColumns.MARKER_COUNT);

                Track.Id trackId = new Track.Id(cursor.getLong(idIndex));
                boolean isRecording = trackId.equals(recordingStatus.getTrackId());
                String icon = cursor.getString(iconIndex);
                int iconId = ActivityType.findBy(icon)
                        .getIconDrawableId();
                String name = cursor.getString(nameIndex);
                String totalTime = StringUtils.formatElapsedTime(Duration.ofMillis(cursor.getLong(totalTimeIndex)));
                String totalDistance = DistanceFormatter.Builder()
                        .setUnit(unitSystem)
                        .build(TrackListActivity.this).formatDistance(Distance.of(cursor.getDouble(totalDistanceIndex)));
                int markerCount = cursor.getInt(markerCountIndex);
                long startTime = cursor.getLong(startTimeIndex);
                int startTimeOffset = cursor.getInt(startTimeOffsetIndex);
                String activityType = icon != null && !icon.equals("") ? null : cursor.getString(activityTypeIndex);
                String description = cursor.getString(descriptionIndex);

                ListItemUtils.setListItem(TrackListActivity.this, view, isRecording,
                        iconId, R.string.image_track, name, totalTime, totalDistance, markerCount,
                        OffsetDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneOffset.ofTotalSeconds(startTimeOffset)),
                        activityType, description, false);
            }
        };
        viewBinding.trackList.setAdapter(resourceCursorAdapter);
        ActivityUtils.configureListViewContextualMenu(viewBinding.trackList, contextualActionModeCallback);

        viewBinding.trackListFabAction.setOnClickListener((view) -> {
            if (recordingStatus.isRecording()) {
                Toast.makeText(TrackListActivity.this, getString(R.string.hold_to_stop), Toast.LENGTH_LONG).show();
                return;
            }

            // Not Recording -> Recording
            updateGpsMenuItem(false, true);
            new TrackRecordingServiceConnection((service, connection) -> {
                Track.Id trackId = service.startNewTrack();

                Intent newIntent = IntentUtils.newIntent(TrackListActivity.this, TrackRecordingActivity.class);
                newIntent.putExtra(TrackRecordingActivity.EXTRA_TRACK_ID, trackId);
                startActivity(newIntent);

                connection.unbind(this);
            }).startAndBind(this, true);
        });
        viewBinding.trackListFabAction.setOnLongClickListener((view) -> {
            if (!recordingStatus.isRecording()) {
                return false;
            }

            // Recording -> Stop
            ActivityUtils.vibrate(this, 1000);
            updateGpsMenuItem(false, false);
            trackRecordingServiceConnection.stopRecording(TrackListActivity.this);
            viewBinding.trackListFabAction.setImageResource(R.drawable.ic_baseline_record_24);
            viewBinding.trackListFabAction.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.red_dark));
            return true;
        });

        setSupportActionBar(viewBinding.trackListToolbar);

        loadData(getIntent());
    }

    private void requestRequiredPermissions() {
        PermissionRequester.ALL.requestPermissionsIfNeeded(this, this, null, (requester) -> Toast.makeText(this, R.string.permission_gps_failed, Toast.LENGTH_LONG).show());
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

        // Float button
        setFloatButton();
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
        ActivityUtils.configureSearchWidget(this, searchMenuItem);

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
            return true;
        }

        if (item.getItemId() == R.id.track_list_help) {
            startActivity(IntentUtils.newIntent(this, HelpActivity.class));
            return true;
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

    @Override
    public void onDeleteFinished() {
        // Do nothing
    }

    @Nullable
    @Override
    protected Track.Id getRecordingTrackId() {
        return recordingStatus.getTrackId();
    }

    /**
     * Updates the menu items with the icon specified.
     *
     * @param isGpsStarted true if gps is started
     * @param isRecording  true if recording
     */
    //TODO Check if if can be avoided to call this outside of onGpsStatusChanged()
    private void updateGpsMenuItem(boolean isGpsStarted, boolean isRecording) {
        MaterialButton startGpsMenuItem = viewBinding.sensorStartButton;
        startGpsMenuItem.setVisibility(!isRecording ? View.VISIBLE : View.INVISIBLE);
        if (!isRecording) {
            startGpsMenuItem.setIcon(AppCompatResources.getDrawable(this, isGpsStarted ? gpsStatusValue.icon : R.drawable.ic_gps_off_24dp));
            if (startGpsMenuItem.getIcon() instanceof AnimatedVectorDrawable) {
                ((AnimatedVectorDrawable) startGpsMenuItem.getIcon()).start();
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
            IntentDashboardUtils.showTrackOnMap(this, false, trackIds);
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
            viewBinding.trackListToolbar.setTitle(searchQuery == null ? getString(R.string.app_name) : searchQuery);
        }

        public void restart() {
            LoaderManager.getInstance(TrackListActivity.this).restartLoader(0, null, loaderCallbacks);
        }

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
            final String[] PROJECTION = new String[]{TracksColumns._ID, TracksColumns.NAME,
                    TracksColumns.DESCRIPTION, TracksColumns.ACTIVITY_TYPE_LOCALIZED, TracksColumns.STARTTIME, TracksColumns.STARTTIME_OFFSET,
                    TracksColumns.TOTALDISTANCE, TracksColumns.TOTALTIME, TracksColumns.ICON, TracksColumns.MARKER_COUNT};

            final String sortOrder = TracksColumns.STARTTIME + " DESC";

            if (searchQuery == null) {
                return new CursorLoader(TrackListActivity.this, TracksColumns.CONTENT_URI, PROJECTION, null, null, sortOrder);
            } else {
                final String SEARCH_QUERY = TracksColumns.NAME + " LIKE ? OR " +
                        TracksColumns.DESCRIPTION + " LIKE ? OR " +
                        TracksColumns.ACTIVITY_TYPE_LOCALIZED + " LIKE ?";
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

    public void onGpsStatusChanged(GpsStatusValue newStatus) {
        gpsStatusValue = newStatus;
        updateGpsMenuItem(true, recordingStatus.isRecording());
    }

    private void setFloatButton() {
        viewBinding.trackListFabAction.setImageResource(recordingStatus.isRecording() ? R.drawable.ic_baseline_stop_24 : R.drawable.ic_baseline_record_24);
        viewBinding.trackListFabAction.setBackgroundTintList(ContextCompat.getColorStateList(this, recordingStatus.isRecording() ? R.color.opentracks : R.color.red_dark));
    }

    private void onRecordingStatusChanged(RecordingStatus status) {
        recordingStatus = status;
        setFloatButton();
    }
}
