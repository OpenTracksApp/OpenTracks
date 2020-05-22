package de.dennisguse.opentracks.util;

import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ViewUtils {

    private ViewUtils() {
    }

    /**
     * Traverses all childs of {@link View} recursively and makes links within {@link TextView}s clickable.
     */
    public static void makeClickableLinks(ViewGroup view) {
        if (view == null) {
            return;
        }

        for (int i = 0; i < view.getChildCount(); i++) {
            final View child = view.getChildAt(i);
            if (child instanceof ViewGroup) {
                makeClickableLinks((ViewGroup) child);
            } else if (child instanceof TextView) {
                ((TextView) child).setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }
}
