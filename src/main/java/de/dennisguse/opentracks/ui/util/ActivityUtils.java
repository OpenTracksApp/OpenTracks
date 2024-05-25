package de.dennisguse.opentracks.ui.util;

import static android.content.Context.VIBRATOR_SERVICE;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Menu;

import androidx.annotation.NonNull;

import java.time.Duration;

public class ActivityUtils {

    private static final String TAG = ActivityUtils.class.getSimpleName();

    public static void vibrate(@NonNull Context context, Duration duration) {
        final Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration.toMillis(), VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(duration.toMillis());
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
