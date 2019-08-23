/*
 * Copyright 2012 Google Inc.
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

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SearchView;

import java.util.ArrayList;

import de.dennisguse.opentracks.fragments.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.fragments.ConfirmDeleteDialogFragment.ConfirmDeleteCaller;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.TrackRecordingServiceConnectionUtils;

/**
 * An abstract class for the following common tasks across
 * {@link TrackListActivity}, {@link TrackDetailActivity}, and
 * {@link SearchListActivity}:
 * <p>
 * - share track <br>
 * - delete tracks <br>
 *
 * @author Jimmy Shih
 */
public abstract class AbstractListActivity extends AbstractActivity implements ConfirmDeleteCaller {

    protected static final int GPS_REQUEST_CODE = 6;
    private static final int DELETE_REQUEST_CODE = 3;

    public static void configureListViewContextualMenu(final ListView listView, final ContextualActionModeCallback contextualActionModeCallback) {
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.list_context_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                contextualActionModeCallback.onPrepare(menu, getCheckedPositions(listView), listView.getCheckedItemIds(), true);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // Do nothing
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                mode.invalidate();
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (contextualActionModeCallback.onClick(item.getItemId(), getCheckedPositions(listView), listView.getCheckedItemIds())) {
                    mode.finish();
                }
                return true;
            }

            /**
             * Gets the checked positions in a list view.
             *
             * @param list the list view
             */
            private int[] getCheckedPositions(ListView list) {
                SparseBooleanArray positions = list.getCheckedItemPositions();
                ArrayList<Integer> arrayList = new ArrayList<>();
                for (int i = 0; i < positions.size(); i++) {
                    int key = positions.keyAt(i);
                    if (positions.valueAt(i)) {
                        arrayList.add(key);
                    }
                }
                int[] result = new int[arrayList.size()];
                for (int i = 0; i < arrayList.size(); i++) {
                    result[i] = arrayList.get(i);
                }
                return result;
            }
        });
    }

    public static void configureSearchWidget(Activity activity, final MenuItem menuItem, final TrackController trackController) {
        SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
        searchView.setQueryRefinementEnabled(true);
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // Hide and show trackController when search widget has focus/no focus
                if (trackController != null) {
                    if (hasFocus) {
                        trackController.hide();
                    } else {
                        trackController.show();
                    }
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                menuItem.collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                menuItem.collapseActionView();
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DELETE_REQUEST_CODE) {
            onDeleted();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Delete tracks.
     *
     * @param trackIds the track ids
     */
    protected void deleteTracks(long[] trackIds) {
        ConfirmDeleteDialogFragment.newInstance(trackIds)
                .show(getSupportFragmentManager(), ConfirmDeleteDialogFragment.CONFIRM_DELETE_DIALOG_TAG);
    }

    @Override
    public void onConfirmDeleteDone(long[] trackIds) {
        boolean stopRecording = false;
        if (trackIds.length == 1 && trackIds[0] == -1L) {
            stopRecording = true;
        } else {
            long recordingTrackId = PreferencesUtils.getLong(this, R.string.recording_track_id_key);
            for (long trackId : trackIds) {
                if (trackId == recordingTrackId) {
                    stopRecording = true;
                    break;
                }
            }
        }
        if (stopRecording) {
            TrackRecordingServiceConnectionUtils.stopRecording(this, getTrackRecordingServiceConnection(), false);
        }
        Intent intent = IntentUtils.newIntent(this, DeleteActivity.class);
        intent.putExtra(DeleteActivity.EXTRA_TRACK_IDS, trackIds);
        startActivityForResult(intent, DELETE_REQUEST_CODE);
    }

    /**
     * Gets the track recording service connection. For stopping the current
     * recording if need to delete the current recording track.
     */
    abstract protected TrackRecordingServiceConnection getTrackRecordingServiceConnection();

    /**
     * Called after {@link DeleteActivity} returns its result.
     */
    abstract protected void onDeleted();
}
