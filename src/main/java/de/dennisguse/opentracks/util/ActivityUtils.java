package de.dennisguse.opentracks.util;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;

import de.dennisguse.opentracks.ContextualActionModeCallback;
import de.dennisguse.opentracks.R;

import static android.content.Context.VIBRATOR_SERVICE;

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

    public static SearchView configureSearchWidget(Activity activity, final MenuItem menuItem) {
        final SearchView searchView = (SearchView) menuItem.getActionView();
        SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            //NOTE: Use searchManager.getSearchableInfo(new ComponentName(activity, SearchActivity.class)) if another activity should handle the search
            searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
        } else {
            Log.w(TAG, "Could not retrieve SearchManager.");
        }
        searchView.setSubmitButtonEnabled(true);
        return searchView;
    }

    public static void vibrate(Context context, int milliseconds) {
        final Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(milliseconds);
        }
    }

    public static void applyNightMode(SharedPreferences sharedPreferences, Context context) {
        AppCompatDelegate.setDefaultNightMode(PreferencesUtils.getDefaultNightMode(sharedPreferences, context));
    }
}
