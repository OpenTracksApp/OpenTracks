/*
 * Copyright 2009 Google Inc.
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.cursoradapter.widget.ResourceCursorAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.WaypointsColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.fragments.DeleteMarkerDialogFragment;
import de.dennisguse.opentracks.fragments.DeleteMarkerDialogFragment.DeleteMarkerCaller;
import de.dennisguse.opentracks.util.ActivityUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.ListItemUtils;
import de.dennisguse.opentracks.util.MarkerUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * Activity to show a list of markers in a track.
 *
 * @author Leif Hendrik Wilden
 */
public class MarkerListActivity extends AbstractActivity implements DeleteMarkerCaller {

    public static final String EXTRA_TRACK_ID = "track_id";

    private static final String TAG = MarkerListActivity.class.getSimpleName();

    private static final String[] PROJECTION = new String[]{WaypointsColumns._ID,
            WaypointsColumns.NAME, WaypointsColumns.DESCRIPTION, WaypointsColumns.CATEGORY,
            WaypointsColumns.TIME, WaypointsColumns.PHOTOURL,
            WaypointsColumns.LATITUDE, WaypointsColumns.LONGITUDE};

    private ContentProviderUtils contentProviderUtils;

    private SharedPreferences sharedPreferences;

    private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    private boolean recordingTrackPaused;

    private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            // Note that the key can be null
            if (PreferencesUtils.isKey(MarkerListActivity.this, R.string.recording_track_id_key, key)) {
                recordingTrackId = PreferencesUtils.getRecordingTrackId(MarkerListActivity.this);
            }
            if (PreferencesUtils.isKey(MarkerListActivity.this, R.string.recording_track_paused_key, key)) {
                recordingTrackPaused = PreferencesUtils.isRecordingTrackPaused(MarkerListActivity.this);
            }
            if (key != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MarkerListActivity.this.invalidateOptionsMenu();
                    }
                });
            }
        }
    };
    private Track track;
    private ResourceCursorAdapter resourceCursorAdapter;
    // UI elements
    private ListView listView;
    // Callback when an item is selected in the contextual action mode
    private final ContextualActionModeCallback contextualActionModeCallback = new ContextualActionModeCallback() {
        @Override
        public void onPrepare(Menu menu, int[] positions, long[] ids, boolean showSelectAll) {
            boolean isSingleSelection = ids.length == 1;

            menu.findItem(R.id.list_context_menu_share).setVisible(false);
            menu.findItem(R.id.list_context_menu_show_on_map).setVisible(isSingleSelection);
            menu.findItem(R.id.list_context_menu_edit).setVisible(isSingleSelection);
            menu.findItem(R.id.list_context_menu_delete).setVisible(true);
            /*
             * Set select all to the same visibility as delete since delete is the
             * only action that can be applied to multiple markers.
             */
            menu.findItem(R.id.list_context_menu_select_all).setVisible(showSelectAll);
        }

        @Override
        public boolean onClick(int itemId, int[] positions, long[] ids) {
            return handleContextItem(itemId, ids);
        }
    };
    private MenuItem insertMarkerMenuItem;
    private MenuItem searchMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        long trackId = getIntent().getLongExtra(EXTRA_TRACK_ID, -1L);
        recordingTrackPaused = PreferencesUtils.isRecordingTrackPausedDefault(this);

        contentProviderUtils = new ContentProviderUtils(this);
        sharedPreferences = PreferencesUtils.getSharedPreferences(this);

        track = trackId != -1L ? contentProviderUtils.getTrack(trackId) : null;

        listView = findViewById(R.id.marker_list);
        listView.setEmptyView(findViewById(R.id.marker_list_empty));
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = IntentUtils.newIntent(MarkerListActivity.this, MarkerDetailActivity.class)
                        .putExtra(MarkerDetailActivity.EXTRA_MARKER_ID, id);
                startActivity(intent);
            }
        });
        resourceCursorAdapter = new ResourceCursorAdapter(this, R.layout.list_item, null, 0) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                int nameIndex = cursor.getColumnIndex(WaypointsColumns.NAME);
                int timeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.TIME);
                int categoryIndex = cursor.getColumnIndex(WaypointsColumns.CATEGORY);
                int descriptionIndex = cursor.getColumnIndex(WaypointsColumns.DESCRIPTION);
                int photoUrlIndex = cursor.getColumnIndex(WaypointsColumns.PHOTOURL);
                int latitudeIndex = cursor.getColumnIndex(WaypointsColumns.LATITUDE);
                int longitudeIndex = cursor.getColumnIndex(WaypointsColumns.LONGITUDE);

                int iconId = MarkerUtils.ICON_ID;
                String name = cursor.getString(nameIndex);
                long time = cursor.getLong(timeIndex);
                String category = cursor.getString(categoryIndex);
                String description = cursor.getString(descriptionIndex);
                String photoUrl = cursor.getString(photoUrlIndex);
                //TODO also show latitude and longitude in list
                double latitude = cursor.getDouble(latitudeIndex);
                double longitude = cursor.getDouble(longitudeIndex);

                ListItemUtils.setListItem(MarkerListActivity.this, view, false, true, iconId, R.string.image_marker, name, null, null, 0, time, false, category, description, photoUrl);
            }
        };
        listView.setAdapter(resourceCursorAdapter);
        ActivityUtils.configureListViewContextualMenu(listView, contextualActionModeCallback);

        LoaderManager.getInstance(this).initLoader(0, null, new LoaderCallbacks<Cursor>() {
            @NonNull
            @Override
            public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
                if (track != null) {
                    return new CursorLoader(MarkerListActivity.this, WaypointsColumns.CONTENT_URI, PROJECTION,
                            WaypointsColumns.TRACKID + "=?",
                            new String[]{String.valueOf(track.getId())}, null);
                } else {
                    return new CursorLoader(MarkerListActivity.this, WaypointsColumns.CONTENT_URI, PROJECTION,
                            null, null, null);
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
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.invalidateOptionsMenu();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.marker_list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.marker_list, menu);

        insertMarkerMenuItem = menu.findItem(R.id.marker_list_insert_marker);

        searchMenuItem = menu.findItem(R.id.marker_list_search);
        ActivityUtils.configureSearchWidget(this, searchMenuItem, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        insertMarkerMenuItem.setVisible(track != null && track.getId() == recordingTrackId && !recordingTrackPaused);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (track != null && item.getItemId() == R.id.marker_list_insert_marker) {
            Intent intent = IntentUtils.newIntent(this, MarkerEditActivity.class)
                    .putExtra(MarkerEditActivity.EXTRA_TRACK_ID, track.getId());
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles a context item selection.
     *
     * @param itemId    the menu item id
     * @param markerIds the marker ids
     * @return true if handled.
     */
    private boolean handleContextItem(int itemId, long[] markerIds) {
        Intent intent;
        switch (itemId) {
            case R.id.list_context_menu_show_on_map:
                if (markerIds.length == 1) {
                    IntentUtils.showCoordinateOnMap(this, contentProviderUtils.getWaypoint(markerIds[0]));
                }
                return true;
            case R.id.list_context_menu_edit:
                if (markerIds.length == 1) {
                    intent = IntentUtils.newIntent(this, MarkerEditActivity.class)
                            .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, markerIds[0]);
                    startActivity(intent);
                }
                return true;
            case R.id.list_context_menu_delete:
                if (markerIds.length > 1 && markerIds.length == listView.getCount()) {
                    markerIds = new long[]{-1L};
                }
                DeleteMarkerDialogFragment.showDialog(getSupportFragmentManager(), markerIds);
                return true;
            case R.id.list_context_menu_select_all:
                int size = listView.getCount();
                for (int i = 0; i < size; i++) {
                    listView.setItemChecked(i, true);
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH && searchMenuItem != null) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onDeleteMarkerDone() {
        // Do nothing
    }
}
