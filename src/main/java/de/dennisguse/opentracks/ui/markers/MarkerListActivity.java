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

package de.dennisguse.opentracks.ui.markers;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.tables.MarkerColumns;
import de.dennisguse.opentracks.databinding.MarkerListBinding;
import de.dennisguse.opentracks.services.RecordingStatus;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.share.ShareUtils;
import de.dennisguse.opentracks.ui.markers.DeleteMarkerDialogFragment.DeleteMarkerCaller;
import de.dennisguse.opentracks.ui.util.ActivityUtils;
import de.dennisguse.opentracks.ui.util.ScrollVisibleViews;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * Activity to show a list of markers in a track.
 *
 * @author Leif Hendrik Wilden
 */
public class MarkerListActivity extends AbstractActivity implements DeleteMarkerCaller {

    public static final String EXTRA_TRACK_ID = "track_id";

    private static final String TAG = MarkerListActivity.class.getSimpleName();

    private ContentProviderUtils contentProviderUtils;

    private RecordingStatus recordingStatus = TrackRecordingService.STATUS_DEFAULT;

    private Track track;
    private MarkerResourceCursorAdapter resourceCursorAdapter;

    private MarkerListBinding viewBinding;

    private final MarkerLoaderCallback loaderCallbacks = new MarkerLoaderCallback();

    private TrackRecordingServiceConnection trackRecordingServiceConnection;

    private final TrackRecordingServiceConnection.Callback bindCallback = service -> service.getRecordingStatusObservable()
            .observe(MarkerListActivity.this, this::onRecordingStatusChanged);

    // Callback when an item is selected in the contextual action mode
    private final ActivityUtils.ContextualActionModeCallback contextualActionModeCallback = new ActivityUtils.ContextualActionModeCallback() {
        @Override
        public void onPrepare(Menu menu, int[] positions, long[] ids, boolean showSelectAll) {
            boolean isSingleSelection = ids.length == 1;

            menu.findItem(R.id.list_context_menu_show_on_map).setVisible(isSingleSelection);
            menu.findItem(R.id.list_context_menu_edit).setVisible(isSingleSelection);
            menu.findItem(R.id.list_context_menu_delete).setVisible(true);

            // Set select all to the same visibility as delete since delete is the only action that can be applied to multiple markers.
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
        Track.Id trackId = getIntent().getParcelableExtra(EXTRA_TRACK_ID);

        contentProviderUtils = new ContentProviderUtils(this);

        track = trackId != null ? contentProviderUtils.getTrack(trackId) : null;

        viewBinding.markerList.setEmptyView(viewBinding.markerListEmpty);
        viewBinding.markerList.setOnItemClickListener((parent, view, position, id) -> {
            resourceCursorAdapter.markerInvalid(id);
            Intent intent = IntentUtils.newIntent(MarkerListActivity.this, MarkerDetailActivity.class)
                    .putExtra(MarkerDetailActivity.EXTRA_MARKER_ID, new Marker.Id(id));
            startActivity(intent);
        });

        resourceCursorAdapter = new MarkerResourceCursorAdapter(this, R.layout.list_item);
        ScrollVisibleViews scrollVisibleViews = new ScrollVisibleViews(resourceCursorAdapter);
        viewBinding.markerList.setOnScrollListener(scrollVisibleViews);
        viewBinding.markerList.setAdapter(resourceCursorAdapter);
        ActivityUtils.configureListViewContextualMenu(viewBinding.markerList, contextualActionModeCallback);

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();
        trackRecordingServiceConnection.startConnection(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        trackRecordingServiceConnection.bind(this);
        this.invalidateOptionsMenu();
        loadData(getIntent());
    }

    @Override
    protected void onStop() {
        super.onStop();
        trackRecordingServiceConnection.unbind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resourceCursorAdapter.clear();
        viewBinding = null;
        resourceCursorAdapter = null;
        contentProviderUtils = null;
    }

    @Override
    protected View getRootView() {
        viewBinding = MarkerListBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.marker_list, menu);

        insertMarkerMenuItem = menu.findItem(R.id.marker_list_insert_marker);

        searchMenuItem = menu.findItem(R.id.marker_list_search);
        ActivityUtils.configureSearchWidget(this, searchMenuItem);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        insertMarkerMenuItem.setVisible(track != null && track.getId().equals(recordingStatus.getTrackId()) && !recordingStatus.isPaused());
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
     * @param itemId        the menu item id
     * @param longMarkerIds the marker ids
     * @return true if handled.
     */
    private boolean handleContextItem(int itemId, long... longMarkerIds) {
        Marker.Id[] markerIds = new Marker.Id[longMarkerIds.length];
        for (int i = 0; i < longMarkerIds.length; i++) {
            markerIds[i] = new Marker.Id(longMarkerIds[i]);
        }

        if (itemId == R.id.list_context_menu_show_on_map) {
            if (markerIds.length == 1) {
                IntentUtils.showCoordinateOnMap(this, contentProviderUtils.getMarker(markerIds[0]));
            }
            return true;
        }

        if (itemId == R.id.list_context_menu_share) {
            Intent intent = ShareUtils.newShareFileIntent(this, markerIds);
            if (intent != null) {
                intent = Intent.createChooser(intent, null);
                startActivity(intent);
            }
            return true;
        }

        if (itemId == R.id.list_context_menu_edit) {
            if (markerIds.length == 1) {
                resourceCursorAdapter.markerInvalid(markerIds[0].getId());
                Intent intent = IntentUtils.newIntent(this, MarkerEditActivity.class)
                        .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, markerIds[0]);
                startActivity(intent);
            }
            return true;
        }

        if (itemId == R.id.list_context_menu_delete) {
            DeleteMarkerDialogFragment.showDialog(getSupportFragmentManager(), markerIds);
            return true;
        }

        if (itemId == R.id.list_context_menu_select_all) {
            for (int i = 0; i < viewBinding.markerList.getCount(); i++) {
                viewBinding.markerList.setItemChecked(i, true);
            }
            return false;
        }

        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH && searchMenuItem != null) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
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
    public void onMarkerDeleted() {
        // Do nothing
    }

    private void onRecordingStatusChanged(RecordingStatus status) {
        recordingStatus = status;
    }

    private class MarkerLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {

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
                setTitle(R.string.menu_markers);
            }
        }

        public void restart() {
            LoaderManager.getInstance(MarkerListActivity.this).restartLoader(0, null, loaderCallbacks);
        }

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
            final String[] PROJECTION = new String[]{MarkerColumns._ID,
                    MarkerColumns.NAME, MarkerColumns.DESCRIPTION, MarkerColumns.CATEGORY,
                    MarkerColumns.TIME, MarkerColumns.PHOTOURL, MarkerColumns.TRACKID};

            if (searchQuery == null) {
                if (track != null) {
                    return new CursorLoader(MarkerListActivity.this, MarkerColumns.CONTENT_URI, PROJECTION, MarkerColumns.TRACKID + "=?", new String[]{String.valueOf(track.getId().getId())}, null);
                } else {
                    return new CursorLoader(MarkerListActivity.this, MarkerColumns.CONTENT_URI, PROJECTION, null, null, null);
                }
            } else {
                final String SEARCH_QUERY = MarkerColumns.NAME + " LIKE ? OR " +
                        MarkerColumns.DESCRIPTION + " LIKE ? OR " +
                        MarkerColumns.CATEGORY + " LIKE ?";
                final String[] selectionArgs = new String[]{"%" + searchQuery + "%", "%" + searchQuery + "%", "%" + searchQuery + "%"};
                return new CursorLoader(MarkerListActivity.this, MarkerColumns.CONTENT_URI, PROJECTION, SEARCH_QUERY, selectionArgs, MarkerColumns.DEFAULT_SORT_ORDER + " DESC");
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
}
