package de.dennisguse.opentracks.ui.util;

import static android.content.Context.VIBRATOR_SERVICE;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;

import de.dennisguse.opentracks.R;

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
                contextualActionModeCallback.onDestroy();
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
                return arrayList.stream().mapToInt(i -> i).toArray();
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

    public static void vibrate(@NonNull Context context, int milliseconds) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            // backward compatibility for Android API < 31,
            // VibratorManager was only added on API level 31 release.
            // noinspection deprecation
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        //final Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        final int DELAY = 0, VIBRATE = 1000, SLEEP = 1000, START = 0;
        long[] vibratePattern = {DELAY, VIBRATE, SLEEP};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(vibratePattern, START));
           // vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            // backward compatibility for Android API < 26
            // noinspection deprecation
            vibrator.vibrate(vibratePattern, START);
            //vibrator.vibrate(milliseconds);
        }
    }

    /**
     * Callback when items in the contextual action mode are selected.
     *
     * @author Jimmy Shih
     */
    public interface ContextualActionModeCallback {

        /**
         * Invoked to prepare the menu for the selected items.
         *
         * @param menu          the menu
         * @param positions     the selected items' positions
         * @param ids           the selected items' ids, if available
         * @param showSelectAll true to show select all
         */
        void onPrepare(Menu menu, int[] positions, long[] ids, boolean showSelectAll);

        /**
         * Invoked when items are selected.
         *
         * @param itemId    the context menu item id
         * @param positions the selected items' positions
         * @param ids       the selected items' ids, if available
         */
        boolean onClick(int itemId, int[] positions, long[] ids);

        /**
         * Invoked when contextual action mode is destroyed.
         */
        void onDestroy();
    }
}
