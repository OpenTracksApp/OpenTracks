package de.dennisguse.opentracks.util;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.ShareContentProvider;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.tables.MarkerColumns;
import de.dennisguse.opentracks.data.tables.TrackPointsColumns;
import de.dennisguse.opentracks.data.tables.TracksColumns;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * Create an {@link Intent} to request showing tracks on a Map or a Dashboard.
 * The receiving {@link android.app.Activity} gets temporary access to the {@link TracksColumns} and the {@link TrackPointsColumns} (incl. update).
 */
public class IntentDashboardUtils {

    private static final String TAG = IntentDashboardUtils.class.getSimpleName();

    private static final String ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard";

    private static final String ACTION_DASHBOARD_PAYLOAD = ACTION_DASHBOARD + ".Payload";

    public static final TrackFileFormat[] SHOW_ON_MAP_TRACK_FILE_FORMATS = new TrackFileFormat[] {
            TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES,
            TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA,
            TrackFileFormat.GPX};

    public static final String PREFERENCE_ID_DASHBOARD = ACTION_DASHBOARD;
    public static final String PREFERENCE_ID_ASK = "ASK";

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
    private static final int NONE_SELECTED = -1;

    private IntentDashboardUtils() {
    }

    /**
     * Send intent to show tracks on a map (needs an another app) as resource URIs.
     * Shows an AlertDialog with different format option if none is defined as preference.
     *
     * @param context  the context
     * @param isRecording are we currently recording?
     * @param trackIds the track ids
     */
    public static void showTrackOnMap(Context context, boolean isRecording, Track.Id... trackIds) {
        Map<String, String> options = TrackFileFormat.toPreferenceIdLabelMap(context.getResources(), IntentDashboardUtils.SHOW_ON_MAP_TRACK_FILE_FORMATS);
        options.put(IntentDashboardUtils.PREFERENCE_ID_DASHBOARD, context.getString(R.string.show_on_dashboard));
        final String[] optionLabels = options.values().toArray(new String[0]);
        final String[] optionValues = options.keySet().toArray(new String[0]);
        final AtomicInteger checkedItem = new AtomicInteger(NONE_SELECTED);
        String preferenceValue = PreferencesUtils.getShowOnMapFormat();
        for (int i = 0; i < optionValues.length; i++) {
            if (optionValues[i].equals(preferenceValue)) {
                checkedItem.set(i);
            }
        }
        if (checkedItem.get() == NONE_SELECTED) {
            checkedItem.set(0); // set first option as default
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.select_show_on_map_behavior);
            builder.setSingleChoiceItems(optionLabels, checkedItem.get(), (dialog, which) -> checkedItem.set(which));
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> onFormatSelected(context, isRecording, optionValues[checkedItem.get()], trackIds, false));
            builder.setNeutralButton(R.string.always, ((dialog, which) -> onFormatSelected(context, isRecording, optionValues[checkedItem.get()], trackIds, true)));
            builder.setNegativeButton(android.R.string.cancel, null);

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            onFormatSelected(context, isRecording, preferenceValue, trackIds, true);
        }
    }

    /**
     * A format was selected, remember if <code>always</code> is true and start the necessary action
     *
     * @param context the context
     * @param isRecording are we currently recording?
     * @param selectedValue the chosen format
     * @param trackIds the track ids
     * @param always set the selectedValue as default preference
     */
    private static void onFormatSelected(final Context context, final boolean isRecording, final String selectedValue, final Track.Id[] trackIds, final boolean always) {
        if (always) {
            PreferencesUtils.setShowOnMapFormat(selectedValue);
        }
        TrackFileFormat trackFileFormat = TrackFileFormat.valueOfPreferenceId(selectedValue);
        if (trackFileFormat != null) {
            showTrackOnMapWithFileFormat(context, trackFileFormat, Set.of(trackIds));
        } else {
            startDashboard(context, isRecording, null, null, trackIds);
        }
    }

    /**
     * Send intent to show tracks on a map (needs an another app) as resource URIs.
     * By providing a targetPackage and targetClass an explicit intent can be sent,
     * thus bypassing the need for the user to select an app.
     *
     * @param context the context
     * @param isRecording are we currently recording?
     * @param targetPackage the target package
     * @param targetClass the target class
     * @param trackIds the track ids
     */
    public static void startDashboard(Context context, boolean isRecording, @Nullable String targetPackage, @Nullable String targetClass, Track.Id... trackIds) {
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

        intent.putExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, PreferencesUtils.shouldKeepScreenOn());
        intent.putExtra(EXTRAS_SHOW_WHEN_LOCKED, PreferencesUtils.shouldShowStatsOnLockscreen());
        intent.putExtra(EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, isRecording);
        if (isRecording) {
            intent.putExtra(EXTRAS_SHOW_FULLSCREEN, PreferencesUtils.shouldUseFullscreen());
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ClipData clipData = ClipData.newRawUri(null, uris.get(TRACK_URI_INDEX));
        clipData.addItem(new ClipData.Item(uris.get(TRACKPOINTS_URI_INDEX)));
        clipData.addItem(new ClipData.Item(uris.get(MARKERS_URI_INDEX)));
        intent.setClipData(clipData);

        if (targetPackage != null && targetClass != null) {
            Log.i(TAG, "Starting dashboard activity with explicit intent (package=" + targetPackage + ", class=" + targetClass + ")");
            intent.setClassName(targetPackage, targetClass);
        } else {
            Log.i(TAG, "Starting dashboard activity with generic intent (package=" + targetPackage + ", class=" + targetClass + ")");
        }

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Dashboard not installed; cannot start it.");
            Toast.makeText(context, R.string.show_on_dashboard_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Send intent to show tracks on a map (needs an another app) to support specific trackFileFormat.
     *
     * @param context  the context
     * @param trackFileFormat the track file format
     * @param trackIds the track ids
     */
    private static void showTrackOnMapWithFileFormat(Context context, TrackFileFormat trackFileFormat, Set<Track.Id> trackIds) {
        if (trackIds.isEmpty()) {
            return;
        }

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Pair<Uri, String> uriAndMime = ShareContentProvider.createURI(trackIds, "SharingTrack", trackFileFormat);
        intent.setDataAndType(uriAndMime.first, uriAndMime.second);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_track_as_trackfileformat, trackFileFormat.getExtension())));
    }

}
