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

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.Objects;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.databinding.MarkerListBinding;
import de.dennisguse.opentracks.services.RecordingStatus;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.share.ShareUtils;
import de.dennisguse.opentracks.ui.util.ActivityUtils;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * Activity to show a list of markers in a track.
 *
 * @author Leif Hendrik Wilden
 */
public class MarkerListActivity extends AbstractActivity implements DeleteMarkerDialogFragment.DeleteMarkerCaller {

    public static final String EXTRA_TRACK_ID = "track_id";

    private ContentProviderUtils contentProviderUtils;

    private RecordingStatus recordingStatus = TrackRecordingService.STATUS_DEFAULT;

    private Track.Id trackId;

    private MarkerListAdapter adapter;

    private MarkerListBinding viewBinding;

    private TrackRecordingServiceConnection trackRecordingServiceConnection;

    private final TrackRecordingServiceConnection.Callback bindCallback = (service, unused) -> service.getRecordingStatusObservable()
            .observe(MarkerListActivity.this, this::onRecordingStatusChanged);

    // Callback when an item is selected in the contextual action mode
    private final ActivityUtils.ContextualActionModeCallback contextualActionModeCallback = new ActivityUtils.ContextualActionModeCallback() {
        @Override
        public void onPrepare(Menu menu, int[] positions, long[] ids, boolean showSelectAll) {
            boolean isSingleSelection = ids.length == 1;

            viewBinding.bottomAppBarLayout.bottomAppBar.performHide(true);

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

        @Override
        public void onDestroy() {
            viewBinding.bottomAppBarLayout.bottomAppBar.performShow(true);
        }
    };
    private MenuItem insertMarkerMenuItem;

    private String searchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentProviderUtils = new ContentProviderUtils(this);

        trackId = getIntent().getParcelableExtra(EXTRA_TRACK_ID);

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindCallback);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        adapter = new MarkerListAdapter(this, viewBinding.markerList, null);
        viewBinding.markerList.setLayoutManager(layoutManager);
        viewBinding.markerList.setAdapter(adapter);

        adapter.setActionModeCallback(contextualActionModeCallback);

        setSupportActionBar(viewBinding.bottomAppBarLayout.bottomAppBar);
        setSupportActionBar(viewBinding.markerListToolbar);
        viewBinding.bottomAppBarLayout.bottomAppBar.setNavigationOnClickListener(item -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        trackRecordingServiceConnection.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        trackRecordingServiceConnection.bind(this);
        this.invalidateOptionsMenu();
        loadData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        trackRecordingServiceConnection.unbind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;
        adapter = null;
        contentProviderUtils = null;
    }

    @NonNull
    @Override
    protected View createRootView() {
        viewBinding = MarkerListBinding.inflate(getLayoutInflater());

        viewBinding.markerListSearchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            searchQuery = viewBinding.markerListSearchView.getEditText().getText().toString();
            viewBinding.markerListSearchView.hide();
            loadData();
            return true;
        });

        return viewBinding.getRoot();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.marker_list, menu);

        insertMarkerMenuItem = menu.findItem(R.id.marker_list_insert_marker);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        insertMarkerMenuItem.setVisible(trackId != null && trackId.equals(recordingStatus.trackId()));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (trackId != null && item.getItemId() == R.id.marker_list_insert_marker) {
            Intent intent = IntentUtils.newIntent(this, MarkerEditActivity.class)
                    .putExtra(MarkerEditActivity.EXTRA_TRACK_ID, trackId);
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
            adapter.setAllSelected(true);
            return false;
        }

        return false;
    }

    @UiThread
    private void loadData() {
        viewBinding.markerListToolbar.setText(searchQuery);

        viewBinding.markerListToolbar.setTitle(Objects.requireNonNullElseGet(searchQuery, () -> getString(R.string.menu_markers)));

        List<Marker> markers = contentProviderUtils.searchMarkers(trackId, searchQuery);

        adapter.swapData(markers);

        if (markers.isEmpty()) {
            viewBinding.markerListEmpty.setVisibility(View.VISIBLE);
            viewBinding.markerList.setVisibility(View.GONE);
        } else {
            viewBinding.markerListEmpty.setVisibility(View.GONE);
            viewBinding.markerList.setVisibility(View.VISIBLE);
        }
    }

    private void onRecordingStatusChanged(RecordingStatus status) {
        recordingStatus = status;
    }

    @Override
    public void onMarkerDeleted() {
        runOnUiThread(this::loadData);
    }
}
