package de.dennisguse.opentracks.util;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;

/**
 * Create an {@link Intent} to request showing a Dashboard.
 * The receiving {@link android.app.Activity} gets temporary access to the {@link TracksColumns} and the {@link TrackPointsColumns} (incl. update).
 */
public class IntentDashboardUtils {

    private static final String ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard";

    private static final String ACTION_DASHBOARD_PAYLOAD = ACTION_DASHBOARD + ".Payload";

    private static final String EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOULD_KEEP_SCREEN_ON";

    private static int TRACK_URI_INDEX = 0;
    private static int TRACKPOINTS_URI_INDEX = 1;

    private IntentDashboardUtils() {
    }

    public static void startDashboard(Context context, long[] trackIds) {
        ArrayList<Uri> uris = new ArrayList<>();
        String trackIdList = ContentProviderUtils.formatIdListForUri(trackIds);

        uris.add(0, Uri.withAppendedPath(TracksColumns.CONTENT_URI, trackIdList));
        uris.add(1, Uri.withAppendedPath(TrackPointsColumns.CONTENT_URI_BY_TRACKID, trackIdList));

        Intent intent = new Intent(ACTION_DASHBOARD);
        intent.putParcelableArrayListExtra(ACTION_DASHBOARD_PAYLOAD, uris);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, PreferencesUtils.shouldKeepScreenOn(context));
        intent.putExtra(EXTRAS_SHOW_WHEN_LOCKED, PreferencesUtils.shouldShowStatsOnLockscreen(context));

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ClipData clipData = ClipData.newRawUri(null, uris.get(TRACK_URI_INDEX));
        clipData.addItem(new ClipData.Item(uris.get(TRACKPOINTS_URI_INDEX)));
        intent.setClipData(clipData);

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_track_using_dashboard)));
    }

    public static long[] extractTrackIdsFromIntent(@NonNull Intent intent) {
        final ArrayList<Uri> uris = intent.getParcelableArrayListExtra(ACTION_DASHBOARD_PAYLOAD);
        final Uri tracksUri = uris.get(TRACK_URI_INDEX);

        String[] trackIdsString = ContentProviderUtils.parseTrackIdsFromUri(tracksUri);
        long[] trackIds = new long[trackIdsString.length];
        for (int i = 0; i < trackIdsString.length; i++) {
            trackIds[i] = Long.parseLong(trackIdsString[i]);
        }

        return trackIds;
    }
}
