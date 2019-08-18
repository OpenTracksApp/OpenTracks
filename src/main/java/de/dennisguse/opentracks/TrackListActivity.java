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
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.cursoradapter.widget.ResourceCursorAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.content.TracksColumns;
import de.dennisguse.opentracks.fragments.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.FileTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.services.ITrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.SettingsActivity;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.ListItemUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackRecordingServiceConnectionUtils;
import de.dennisguse.opentracks.util.TrackUtils;

/**
 * An activity displaying a list of tracks.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackListActivity extends AbstractTrackActivity implements ConfirmDeleteDialogFragment.ConfirmDeleteCaller {

    private static final String TAG = TrackListActivity.class.getSimpleName();
    private static final String[] PROJECTION = new String[]{TracksColumns._ID, TracksColumns.NAME,
            TracksColumns.DESCRIPTION, TracksColumns.CATEGORY, TracksColumns.STARTTIME,
            TracksColumns.TOTALDISTANCE, TracksColumns.TOTALTIME, TracksColumns.ICON};
    // The following are set in onCreate
    private ContentProviderUtils contentProviderUtils;
    private SharedPreferences sharedPreferences;
    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private TrackController trackController;
    private ListView listView;
    private ResourceCursorAdapter resourceCursorAdapter;
    private final LoaderCallbacks<Cursor> loaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
            return new CursorLoader(TrackListActivity.this, TracksColumns.CONTENT_URI, PROJECTION, null,
                    null, TrackUtils.TRACK_SORT_ORDER);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
            resourceCursorAdapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            resourceCursorAdapter.swapCursor(null);
        }
    };
    // Preferences
    private boolean metricUnits = true;
    private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    // Callback when an item is selected in the contextual action mode
    private final ContextualActionModeCallback contextualActionModeCallback = new ContextualActionModeCallback() {

        @Override
        public void onPrepare(Menu menu, int[] positions, long[] ids, boolean showSelectAll) {
            boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
            boolean isSingleSelection = ids.length == 1;

            menu.findItem(R.id.list_context_menu_share).setVisible(!isRecording && isSingleSelection);
            menu.findItem(R.id.list_context_menu_edit).setVisible(isSingleSelection);
            menu.findItem(R.id.list_context_menu_select_all).setVisible(showSelectAll);
        }

        @Override
        public boolean onClick(int itemId, int[] positions, long[] ids) {
            return handleContextItem(itemId, ids);
        }
    };
    private boolean recordingTrackPaused = PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT;
    /*
     * Note that sharedPreferenceChangeListener cannot be an anonymous inner class.
     * Anonymous inner class will get garbage collected.
     */
    private final OnSharedPreferenceChangeListener
            sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (key == null || key.equals(PreferencesUtils.getKey(TrackListActivity.this, R.string.stats_units_key))) {
                metricUnits = PreferencesUtils.isMetricUnits(TrackListActivity.this);
            }
            if (key == null || key.equals(
                    PreferencesUtils.getKey(TrackListActivity.this, R.string.recording_track_id_key))) {
                recordingTrackId = PreferencesUtils.getLong(
                        TrackListActivity.this, R.string.recording_track_id_key);
                if (key != null && recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
                    trackRecordingServiceConnection.startAndBind();
                }
            }
            if (key == null || key.equals(PreferencesUtils.getKey(
                    TrackListActivity.this, R.string.recording_track_paused_key))) {
                recordingTrackPaused = PreferencesUtils.getBoolean(TrackListActivity.this,
                        R.string.recording_track_paused_key,
                        PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
            }
            if (key != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TrackListActivity.this.invalidateOptionsMenu();
                        LoaderManager.getInstance(TrackListActivity.this).restartLoader(0, null, loaderCallbacks);
                        boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
                        trackController.update(isRecording, recordingTrackPaused);
                    }
                });
            }
        }
    };
    // Menu items
    private MenuItem searchMenuItem;
    private MenuItem startGpsMenuItem;
    private MenuItem aggregatedStatisticsMenuItem;
    private MenuItem deleteAllMenuItem;
    private final OnClickListener stopListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            updateMenuItems(false, false);
            TrackRecordingServiceConnectionUtils.stopRecording(
                    TrackListActivity.this, trackRecordingServiceConnection, true);
        }
    };
    private boolean startGps = false; // true to start gps
    private boolean startNewRecording = false; // true to start a new recording
    // Callback when the trackRecordingServiceConnection binding changes.
    private final Runnable bindChangedCallback = new Runnable() {
        @Override
        public void run() {
            /*
             * After binding changes (e.g., becomes available), update the total time
             * in trackController.
             */
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    trackController.update(recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, recordingTrackPaused);
                }
            });

            if (!startGps && !startNewRecording) {
                return;
            }

            ITrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
            if (service == null) {
                Log.d(TAG, "service not available to start gps or a new recording");
                return;
            }
            if (startNewRecording) {
                startGps = false;

                long trackId = service.startNewTrack();
                startNewRecording = false;
                Intent intent = IntentUtils.newIntent(TrackListActivity.this, TrackDetailActivity.class)
                        .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, trackId);
                startActivity(intent);
                Toast.makeText(
                        TrackListActivity.this, R.string.track_list_record_success, Toast.LENGTH_SHORT)
                        .show();
            }
            if (startGps) {
                service.startGps();
                startGps = false;
            }
        }
    };
    private final OnClickListener recordListener = new OnClickListener() {
        public void onClick(View v) {
            if (recordingTrackId == PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
                // Not recording -> Recording
                updateMenuItems(false, true);
                startRecording();
            } else {
                if (recordingTrackPaused) {
                    // Paused -> Resume
                    updateMenuItems(false, true);
                    TrackRecordingServiceConnectionUtils.resumeTrack(trackRecordingServiceConnection);
                    trackController.update(true, false);
                } else {
                    // Recording -> Paused
                    updateMenuItems(false, true);
                    TrackRecordingServiceConnectionUtils.pauseTrack(trackRecordingServiceConnection);
                    trackController.update(true, true);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Enabling strict mode");
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        contentProviderUtils = ContentProviderUtils.Factory.get(this);
        sharedPreferences = PreferencesUtils.getSharedPreferences(this);

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, bindChangedCallback);
        trackController = new TrackController(this, trackRecordingServiceConnection, true, recordListener, stopListener);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        // Show trackController when search dialog is dismissed
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchManager.setOnDismissListener(new SearchManager.OnDismissListener() {
            @Override
            public void onDismiss() {
                trackController.show();
            }
        });

        listView = findViewById(R.id.track_list);
        listView.setEmptyView(findViewById(R.id.track_list_empty_view));
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent newIntent = IntentUtils.newIntent(TrackListActivity.this, TrackDetailActivity.class)
                        .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, id);
                startActivity(newIntent);
            }
        });
        resourceCursorAdapter = new ResourceCursorAdapter(this, R.layout.list_item, null, 0) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                int idIndex = cursor.getColumnIndex(TracksColumns._ID);
                int iconIndex = cursor.getColumnIndex(TracksColumns.ICON);
                int nameIndex = cursor.getColumnIndex(TracksColumns.NAME);
                int totalTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
                int totalDistanceIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
                int startTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
                int categoryIndex = cursor.getColumnIndex(TracksColumns.CATEGORY);
                int descriptionIndex = cursor.getColumnIndex(TracksColumns.DESCRIPTION);

                long trackId = cursor.getLong(idIndex);
                boolean isRecording = trackId == recordingTrackId;
                String icon = cursor.getString(iconIndex);
                int iconId = TrackIconUtils.getIconDrawable(icon);
                String name = cursor.getString(nameIndex);
                String totalTime = StringUtils.formatElapsedTime(cursor.getLong(totalTimeIndex));
                String totalDistance = StringUtils.formatDistance(TrackListActivity.this, cursor.getDouble(totalDistanceIndex), metricUnits);
                int markerCount = contentProviderUtils.getWaypointCount(trackId);
                long startTime = cursor.getLong(startTimeIndex);
                String category = icon != null && !icon.equals("") ? null : cursor.getString(categoryIndex);
                String description = cursor.getString(descriptionIndex);

                ListItemUtils.setListItem(TrackListActivity.this, view, isRecording, recordingTrackPaused,
                        iconId, R.string.image_track, name, totalTime, totalDistance, markerCount,
                        startTime, true, category, description, null);
            }
        };
        listView.setAdapter(resourceCursorAdapter);
        AbstractTrackActivity.configureListViewContextualMenu(listView, contextualActionModeCallback);

        LoaderManager.getInstance(this).initLoader(0, null, loaderCallbacks);
        showStartupDialogs();
    }

    @Override
    protected void onStart() {
        super.onStart();

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
        TrackRecordingServiceConnectionUtils.startConnection(this, trackRecordingServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update UI
        this.invalidateOptionsMenu();
        LoaderManager.getInstance(this).restartLoader(0, null, loaderCallbacks);
        boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
        trackController.onResume(isRecording, recordingTrackPaused);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Update UI
        trackController.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        trackRecordingServiceConnection.unbind();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == GPS_REQUEST_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_gps_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.track_list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.track_list, menu);

        searchMenuItem = menu.findItem(R.id.track_list_search);
        AbstractTrackActivity.configureSearchWidget(this, searchMenuItem, trackController);

        startGpsMenuItem = menu.findItem(R.id.track_list_start_gps);
        aggregatedStatisticsMenuItem = menu.findItem(R.id.track_list_aggregated_statistics);
        deleteAllMenuItem = menu.findItem(R.id.track_list_delete_all);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isGpsStarted = TrackRecordingServiceConnectionUtils.isRecordingServiceRunning(this);
        boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
        updateMenuItems(isGpsStarted, isRecording);

        View searchView = searchMenuItem.getActionView();
        if (searchView instanceof SearchView) {
            ((SearchView) searchView).setQuery("", false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.track_list_start_gps:
                LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                } else {
                    startGps = !TrackRecordingServiceConnectionUtils.isRecordingServiceRunning(this);

                    // Show toast
                    Toast toast = Toast.makeText(this, startGps ? R.string.gps_starting : R.string.gps_stopping, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    // Invoke trackRecordingService
                    if (startGps) {
                        trackRecordingServiceConnection.startAndBind();
                        bindChangedCallback.run();
                    } else {
                        ITrackRecordingService trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
                        if (trackRecordingService != null) {
                            trackRecordingService.stopGps();
                        }
                        trackRecordingServiceConnection.unbindAndStop();
                    }

                    // Update menu after starting or stopping gps
                    this.invalidateOptionsMenu();
                }
                return true;
            case R.id.track_list_markers:
                intent = IntentUtils.newIntent(this, MarkerListActivity.class);
                startActivity(intent);
                return true;
            case R.id.track_list_aggregated_statistics:
                intent = IntentUtils.newIntent(this, AggregatedStatsActivity.class);
                startActivity(intent);
                return true;
            case R.id.track_list_delete_all:
                deleteTracks(new long[]{-1L});
                return true;
            case R.id.track_list_settings:
                intent = IntentUtils.newIntent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.list_context_menu, menu);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        contextualActionModeCallback.onPrepare(
                menu, new int[]{info.position}, new long[]{info.id}, false);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        if (handleContextItem(item.getItemId(), new long[]{info.id})) {
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH && searchMenuItem != null) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onSearchRequested() {
        // Hide trackController when search dialog is shown
        trackController.hide();
        return super.onSearchRequested();
    }

    @Override
    protected TrackRecordingServiceConnection getTrackRecordingServiceConnection() {
        return trackRecordingServiceConnection;
    }

    @Override
    protected void onDeleted() {
        // Do nothing
    }

    /**
     * Shows start up dialogs.
     */
    public void showStartupDialogs() {
        // If stats_units_key is undefined, set it
        if (PreferencesUtils.getString(this, R.string.stats_units_key, "").equals("")) {
            String statsUnits = getString(Locale.US.equals(Locale.getDefault()) ? R.string.stats_units_imperial : R.string.stats_units_metric);
            PreferencesUtils.setString(this, R.string.stats_units_key, statsUnits);
        }

        requestGPSPermissions();
    }

    private void requestGPSPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_REQUEST_CODE);
        }
    }

    /**
     * Updates the menu items.
     *
     * @param isGpsStarted true if gps is started
     * @param isRecording  true if recording
     */
    private void updateMenuItems(boolean isGpsStarted, boolean isRecording) {
        boolean hasTrack = listView != null && listView.getCount() != 0;
        if (startGpsMenuItem != null) {
            startGpsMenuItem.setVisible(!isRecording);
            if (!isRecording) {
                startGpsMenuItem.setTitle(isGpsStarted ? R.string.menu_stop_gps : R.string.menu_start_gps);
                startGpsMenuItem.setIcon(isGpsStarted ? R.drawable.ic_menu_stop_gps : R.drawable.ic_menu_start_gps);
                TrackIconUtils.setMenuIconColor(startGpsMenuItem);
            }
        }
        if (aggregatedStatisticsMenuItem != null) {
            aggregatedStatisticsMenuItem.setVisible(hasTrack);
        }
        if (deleteAllMenuItem != null) {
            deleteAllMenuItem.setVisible(hasTrack && !isRecording);
        }
    }

    /**
     * Starts a new recording.
     */
    private void startRecording() {
        startNewRecording = true;
        trackRecordingServiceConnection.startAndBind();

        /*
         * If the binding has happened, then invoke the callback to start a new
         * recording. If the binding hasn't happened, then invoking the callback
         * will have no effect. But when the binding occurs, the callback will get
         * invoked.
         */
        bindChangedCallback.run();
    }

    /**
     * Save tracks in one file and send an intent to show it with a map application.
     * TODO: Exported file is not deleted automatically.
     *
     * @param trackIds
     */
    private void showOnExternalMap(long[] trackIds) {
        if (trackIds.length == 0) {
            return;
        }

        Track[] tracks = new Track[trackIds.length];
        for (int i = 0; i < trackIds.length; i++) {
            tracks[i] = contentProviderUtils.getTrack(trackIds[i]);
        }

        TrackFileFormat trackFileFormat = TrackFileFormat.KML;
        TrackExporter trackExporter = new FileTrackExporter(contentProviderUtils, tracks,
                trackFileFormat.newTrackWriter(this, tracks.length > 1), null);

        File directory = new File(FileUtils.getPath(trackFileFormat.getExtension()));
        if (!FileUtils.ensureDirectoryExists(directory)) {
            Toast.makeText(this, R.string.external_storage_not_writable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String fileName = FileUtils.buildUniqueFileName(directory, tracks[0].getName(), trackFileFormat.getExtension());
        File file = new File(directory, fileName);

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            if (trackExporter.writeTrack(fileOutputStream)) {
                Uri fileUri = FileProvider.getUriForFile(this, FileUtils.FILEPROVIDER, file);

                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, "application/vnd.google-earth.kml+xml");

                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } else {
                if (!file.delete()) {
                    Log.d(TAG, "Unable to delete file");
                }
                Log.e(TAG, "Unable to export track");
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to open file " + file.getName(), e);
        } catch (IOException e) {
            Log.e(TAG, "Unable to close file output stream", e);
        }
    }

    /**
     * Handles a context item selection.
     *
     * @param itemId   the menu item id
     * @param trackIds the track ids
     * @return true if handled.
     */
    private boolean handleContextItem(int itemId, long[] trackIds) {
        switch (itemId) {
            case R.id.list_context_menu_show_on_map:
                showOnExternalMap(trackIds);
                return true;
            case R.id.list_context_menu_share:
                //TODO
                Log.e(TAG, "Not implemented");
                return true;
            case R.id.list_context_menu_edit:
                Intent intent = IntentUtils.newIntent(this, TrackEditActivity.class)
                        .putExtra(TrackEditActivity.EXTRA_TRACK_ID, trackIds[0]);
                startActivity(intent);
                return true;
            case R.id.list_context_menu_delete:
                if (trackIds.length > 1 && trackIds.length == listView.getCount()) {
                    trackIds = new long[]{-1L};
                }
                deleteTracks(trackIds);
                return true;
            case R.id.list_context_menu_select_all:
                int size = listView.getCount();
                for (int i = 0; i < size; i++) {
                    listView.setItemChecked(i, true);
                }
                return false;
        }
        return false;
    }
}
