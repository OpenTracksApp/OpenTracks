package de.dennisguse.opentracks.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import de.dennisguse.opentracks.R;

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

    public static int getTextColorPrimary(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);

        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static int getTextColorSecondary(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);

        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static int getFontSizeSmallInPx(Context context) {
        TypedArray typedArray = context.obtainStyledAttributes(R.style.TextSmall, new int[]{android.R.attr.textSize});
        int fontSize = typedArray.getDimensionPixelSize(0, 12);
        typedArray.recycle();
        return fontSize;
    }

    public static int getFontSizeMediumInPx(Context context) {
        TypedArray typedArray = context.obtainStyledAttributes(R.style.TextMedium, new int[]{android.R.attr.textSize});
        int fontSize = typedArray.getDimensionPixelSize(0, 15);
        typedArray.recycle();
        return fontSize;
    }
}
