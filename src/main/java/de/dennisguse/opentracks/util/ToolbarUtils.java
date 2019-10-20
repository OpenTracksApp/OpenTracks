package de.dennisguse.opentracks.util;

import android.content.Context;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.core.internal.view.SupportMenuItem;
import androidx.core.view.MenuItemCompat;

public class ToolbarUtils {

    private ToolbarUtils() {
    }

    //TODO Compat is not working as the AbsListView.MultiChoiceModeListener; instantiating it manually using the non-compat is a workaround.
    @Deprecated
    public static void setupShareActionProvider(@NonNull Context context, @NonNull MenuItem shareMenuItem, @NonNull long[] trackIds) {
        android.widget.ShareActionProvider shareActionProvider = new android.widget.ShareActionProvider(context);
        shareActionProvider.setShareIntent(trackIds.length == 0 ? null : IntentUtils.newShareFileIntent(context, trackIds));
        shareMenuItem.setActionProvider(shareActionProvider);
    }

    /**
     * @param shareMenuItem Should be a {@link SupportMenuItem}.
     */
    public static void setupShareActionProviderCompat(@NonNull Context context, @NonNull MenuItem shareMenuItem, @NonNull long[] trackIds) {
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);
        shareActionProvider.setShareIntent(trackIds.length == 0 ? null : IntentUtils.newShareFileIntent(context, trackIds));
        MenuItemCompat.setActionProvider(shareMenuItem, shareActionProvider);
    }
}
