package de.dennisguse.opentracks.util;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import de.dennisguse.opentracks.content.data.MarkerColumns;
import de.dennisguse.opentracks.content.data.Track;
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

    /**
     * Assume "v1" if not present.
     */
    private static final String EXTRAS_PROTOCOL_VERSION = "PROTOCOL_VERSION";

    /**
     * version 1: the initial version.
     * version 2: replaced pause/resume trackpoints for track segmentation (lat=100 / lat=200) by TrackPoint.Type.
     */
    private static final int CURRENT_VERSION = 2;

    private static final String EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK = "EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK";
    private static final String EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_FULLSCREEN = "EXTRAS_SHOULD_FULLSCREEN";

    private static final int TRACK_URI_INDEX = 0;
    private static final int TRACKPOINTS_URI_INDEX = 1;
    private static final int MARKERS_URI_INDEX = 2;

    private IntentDashboardUtils() {
    }

    /**
     * Send intent to show tracks on a map (needs an another app) as resource URIs.
     *
     * @param context  the context
     * @param trackIds the track ids
     */
    public static void startDashboard(Context context, boolean isRecording, Track.Id... trackIds) {
        if (trackIds.length == 0) {
            return;
        }

        String trackIdList = ContentProviderUtils.formatIdListForUri(trackIds);

        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(TRACK_URI_INDEX, Uri.withAppendedPath(TracksColumns.CONTENT_URI, trackIdList));
        uris.add(TRACKPOINTS_URI_INDEX, Uri.withAppendedPath(TrackPointsColumns.CONTENT_URI_BY_TRACKID, trackIdList));
        uris.add(MARKERS_URI_INDEX, Uri.withAppendedPath(MarkerColumns.CONTENT_URI_BY_TRACKID, trackIdList));

        Intent intent = new Intent(ACTION_DASHBOARD);
        intent.putExtra(EXTRAS_PROTOCOL_VERSION, CURRENT_VERSION);

        intent.putParcelableArrayListExtra(ACTION_DASHBOARD_PAYLOAD, uris);

        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);

        intent.putExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, PreferencesUtils.shouldKeepScreenOn(sharedPreferences, context));
        intent.putExtra(EXTRAS_SHOW_WHEN_LOCKED, PreferencesUtils.shouldShowStatsOnLockscreen(sharedPreferences, context));
        intent.putExtra(EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, isRecording);
        if (isRecording) {
            intent.putExtra(EXTRAS_SHOW_FULLSCREEN, PreferencesUtils.shouldUseFullscreen(sharedPreferences, context));
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ClipData clipData = ClipData.newRawUri(null, uris.get(TRACK_URI_INDEX));
        clipData.addItem(new ClipData.Item(uris.get(TRACKPOINTS_URI_INDEX)));
        clipData.addItem(new ClipData.Item(uris.get(MARKERS_URI_INDEX)));
        intent.setClipData(clipData);

        context.startActivity(intent);
    }

    public static Set<Track.Id> extractTrackIdsFromIntent(@NonNull Intent intent) {
        final ArrayList<Uri> uris = intent.getParcelableArrayListExtra(ACTION_DASHBOARD_PAYLOAD);
        final Uri tracksUri = uris.get(TRACK_URI_INDEX);

        String[] trackIdsString = ContentProviderUtils.parseTrackIdsFromUri(tracksUri);
        Set<Track.Id> trackIds = new HashSet<>(trackIdsString.length);
        for (String s : trackIdsString) {
            trackIds.add(new Track.Id(Long.parseLong(s)));
        }

        return trackIds;
    }
}
