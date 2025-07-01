package de.dennisguse.opentracks.ui.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.google.android.material.R;

public class ThemeUtils {

    private ThemeUtils() {
    }

    /**
     * Get the material design default background color.
     */
    public static int getBackgroundColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorSurface, typedValue, true);

        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static int getTextColorPrimary(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true);

        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static int getTextColorSecondary(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorSecondary, typedValue, true);

        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static int getFontSizeSmallInPx(Context context) {
        TypedArray typedArray = context.obtainStyledAttributes(R.style.TextAppearance_MaterialComponents_Body2, new int[]{android.R.attr.textSize});
        int fontSize = typedArray.getDimensionPixelSize(0, 12);
        typedArray.recycle();
        return fontSize;
    }

    public static int getFontSizeMediumInPx(Context context) {
        TypedArray typedArray = context.obtainStyledAttributes(R.style.TextAppearance_MaterialComponents_Body1, new int[]{android.R.attr.textSize});
        int fontSize = typedArray.getDimensionPixelSize(0, 15);
        typedArray.recycle();
        return fontSize;
    }

    /**
     * Gets the photo height.
     */
    public static int getPhotoHeight(Context context) {
        int[] attrs = new int[]{android.R.attr.listPreferredItemHeight};
        TypedArray typeArray = context.obtainStyledAttributes(attrs);
        int height = typeArray.getDimensionPixelSize(0, 128);
        typeArray.recycle();
        return 2 * height;
    }
}
