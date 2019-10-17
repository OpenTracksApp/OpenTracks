package de.dennisguse.opentracks.util;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;

import de.dennisguse.opentracks.ContextualActionModeCallback;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.SearchListActivity;
import de.dennisguse.opentracks.TrackController;

public class ActivityUtils {

    private static final String TAG = ActivityUtils.class.getSimpleName();

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
        final SearchView searchView = (SearchView) menuItem.getActionView();
        SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            //NOTE: for some reason activity.getComponentName() did not trigger the SearchListActivity
            searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(activity, SearchListActivity.class)));
        } else {
            Log.w(TAG, "Could not retrieve SearchManager.");
        }
        searchView.setQueryRefinementEnabled(true);
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // Hide and show trackController when searchable widget has focus/no focus
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
}
