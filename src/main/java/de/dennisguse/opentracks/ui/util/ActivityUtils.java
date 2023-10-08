package de.dennisguse.opentracks.ui.util;

import static android.content.Context.VIBRATOR_SERVICE;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;

public class ActivityUtils {

    private static final String TAG = ActivityUtils.class.getSimpleName();

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
        final Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(milliseconds);
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
