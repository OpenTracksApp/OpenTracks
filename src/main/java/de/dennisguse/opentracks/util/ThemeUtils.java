package de.dennisguse.opentracks.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

public class ThemeUtils {

    private ThemeUtils() {
    }

    /**
     * Get the material design default background color.
     */
    public static int getBackgroundColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true);

        return ContextCompat.getColor(context, typedValue.resourceId);
    }
}
