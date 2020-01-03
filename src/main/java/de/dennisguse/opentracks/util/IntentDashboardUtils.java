package de.dennisguse.opentracks.util;

import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;

import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;

/**
 * Create an {@link Intent} to request showing a Dashboard.
 * The receiving {@link android.app.Activity} gets temporary access to the {@link TracksColumns} and the {@link TrackPointsColumns} (incl. update).
 */
public class IntentDashboardUtils {

    public static final String ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard";

    public static final String ACTION_DASHBOARD_PAYLOAD = ACTION_DASHBOARD + ".Payload";

    private IntentDashboardUtils() {
    }

    public static boolean startDashboard(Context context, long trackId) {
        ArrayList<Uri> uris = new ArrayList<>();

        uris.add(0, ContentUris.withAppendedId(TracksColumns.CONTENT_URI, trackId));
        uris.add(1, ContentUris.withAppendedId(TrackPointsColumns.CONTENT_URI, trackId));

        Intent intent = new Intent(ACTION_DASHBOARD);
        intent.putParcelableArrayListExtra(ACTION_DASHBOARD_PAYLOAD, uris);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ClipData clipData = ClipData.newRawUri(null, uris.get(0));
        clipData.addItem(new ClipData.Item(uris.get(1)));
        intent.setClipData(clipData);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(Intent.createChooser(intent, null));
            return true;
        }

        return false;
    }
}
