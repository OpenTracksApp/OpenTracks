package de.dennisguse.opentracks.ui.util;

import android.content.Context;

/**
 * Utils related to handling Android resources.
 */
public class ResourceUtils {

    /**
     * Convert display density to physical pixel.
     */
    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
