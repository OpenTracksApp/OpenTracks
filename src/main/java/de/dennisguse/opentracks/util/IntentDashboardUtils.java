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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    // Supported track file formats for showing tracks on a map
    public static final TrackFileFormat[] SHOW_ON_MAP_TRACK_FILE_FORMATS = new TrackFileFormat[]{
            TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES,
            TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA,
            TrackFileFormat.GPX};
    public static final String PREFERENCE_ID_ASK = "ASK";
    private static final String TAG = IntentDashboardUtils.class.getSimpleName();
    // Intent actions
    private static final String ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard";
    // Preference IDs for showing tracks on a map or dashboard
    public static final String PREFERENCE_ID_DASHBOARD = ACTION_DASHBOARD;
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

    // Extra keys for passing data to the dashboard
    private static final String EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK = "EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK";
    private static final String EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_FULLSCREEN = "EXTRAS_SHOULD_FULLSCREEN";

    // Indices for the different URIs in the ACTION_DASHBOARD_PAYLOAD
    private static final int TRACK_URI_INDEX = 0;
    private static final int TRACKPOINTS_URI_INDEX = 1;
    private static final int MARKERS_URI_INDEX = 2;

    // Constants for checking if a format is selected in the AlertDialog
    private static final int NONE_SELECTED = -1;


    private IntentDashboardUtils() {
    }

    /**
     * Sends an intent to show tracks on a map as resource URIs. Shows an AlertDialog with different format options if none is defined as preference.
     *
     * @param context     the context
     * @param isRecording indicates if we are currently recording
     * @param trackIds    the track ids
     */
    public static void showTrackOnMap(Context context, boolean isRecording, Track.Id... trackIds) {

        // Get the different format options as a map
        Map<String, String> options = TrackFileFormat.toPreferenceIdLabelMap(context.getResources(), IntentDashboardUtils.SHOW_ON_MAP_TRACK_FILE_FORMATS);
        options.put(IntentDashboardUtils.PREFERENCE_ID_DASHBOARD, context.getString(R.string.show_on_dashboard));

        // Convert the map values and keys to arrays
        final String[] optionLabels = options.values().toArray(new String[0]);
        final String[] optionValues = options.keySet().toArray(new String[0]);

        // Set the currently selected option as checked
        final AtomicInteger checkedItem = new AtomicInteger(NONE_SELECTED);
        String preferenceValue = PreferencesUtils.getShowOnMapFormat();
        for (int i = 0; i < optionValues.length; i++) {
            if (optionValues[i].equals(preferenceValue)) {
                checkedItem.set(i);
            }
        }

        // If no option is selected, show an AlertDialog with the options
        if (checkedItem.get() == NONE_SELECTED) {
            checkedItem.set(0); // set first option as default

            // Build the AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.select_show_on_map_behavior);
            builder.setSingleChoiceItems(optionLabels, checkedItem.get(), (dialog, which) -> checkedItem.set(which));
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> onFormatSelected(context, isRecording, optionValues[checkedItem.get()], trackIds, false));
            builder.setNeutralButton(R.string.always, ((dialog, which) -> onFormatSelected(context, isRecording, optionValues[checkedItem.get()], trackIds, true)));
            builder.setNegativeButton(android.R.string.cancel, null);

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            // If an option is selected, call onFormatSelected with the selected option
            onFormatSelected(context, isRecording, preferenceValue, trackIds, true);
        }
    }

    /**
     * Sends an intent to show multiple tracks on a map as resource URIs.
     * Creates a URI for each track in the provided list and concatenates them as a single string.
     * Then, it sends an intent to show all tracks on the map simultaneously.
     *
     * @param context         the context
     * @param trackFileFormat the track file format to be used for showing tracks on the map
     * @param trackIds        the list of track ids to be displayed on the map
     */
    public static void showMultipleTracksOnMap(Context context, TrackFileFormat trackFileFormat, List<Track.Id> trackIds) {
        // If there are no track ids, return
        if (trackIds.isEmpty()) {
            return;
        }

        // Create a Uri for each track and join them as a single string
        StringBuilder trackUriList = new StringBuilder();
        for (Track.Id trackId : trackIds) {
            if (trackUriList.length() > 0) {
                trackUriList.append(",");
            }
            trackUriList.append(trackId.getId());
        }

        // Create the intent to show the tracks on a map
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Create a Uri and MIME type for the tracks based on the track file format
        Pair<Uri, String> uriAndMime = ShareContentProvider.createURI(new HashSet<>(trackIds), "SharingTracks", trackFileFormat);
        intent.setDataAndType(uriAndMime.first, uriAndMime.second);

        // Add the read URI permission flag to the intent
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Show a chooser dialog to open the tracks in the chosen app
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_track_as_trackfileformat, trackFileFormat.getExtension())));
    }


    /**
     * Called when a format is selected. Sets the selected format as the default preference if <code>always</code> is true and starts the necessary action.
     *
     * @param context       the context
     * @param isRecording   indicates if we are currently recording
     * @param selectedValue the chosen format
     * @param trackIds      the track ids
     * @param always        indicates if the selectedValue should be set as the default preference
     */
    private static void onFormatSelected(final Context context, final boolean isRecording, final String selectedValue, final Track.Id[] trackIds, final boolean always) {

        // If always is true, set the selectedValue as the default preference
        if (always) {
            PreferencesUtils.setShowOnMapFormat(selectedValue);
        }

        // Get the TrackFileFormat corresponding to the selectedValue
        TrackFileFormat trackFileFormat = TrackFileFormat.valueOfPreferenceId(selectedValue);

        // If a valid TrackFileFormat is found, show the tracks on the map with the selected format
        if (trackFileFormat != null) {
            showTrackOnMapWithFileFormat(context, trackFileFormat, Set.of(trackIds));
        }
        // If no valid TrackFileFormat is found, start the dashboard
        else {
            startDashboard(context, isRecording, null, null, trackIds);
        }
    }


    /**
     * Sends an intent to show tracks on a dashboard app as resource URIs.
     * If targetPackage and targetClass are provided, an explicit intent is sent, bypassing the need for the user to select an app.
     *
     * @param context       the context
     * @param isRecording   indicates if we are currently recording
     * @param targetPackage the target package (can be null)
     * @param targetClass   the target class (can be null)
     * @param trackIds      the track ids
     */
    public static void startDashboard(Context context, boolean isRecording, @Nullable String targetPackage, @Nullable String targetClass, Track.Id... trackIds) {

        // If there are no track ids, return
        if (trackIds.length == 0) {
            return;
        }

        // Format the track ids as a string list for the Uri
        String trackIdList = ContentProviderUtils.formatIdListForUri(trackIds);

        // Create a list of Uris for the tracks, trackpoints, and markers
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(TRACK_URI_INDEX, Uri.withAppendedPath(TracksColumns.CONTENT_URI, trackIdList));
        uris.add(TRACKPOINTS_URI_INDEX, Uri.withAppendedPath(TrackPointsColumns.CONTENT_URI_BY_TRACKID, trackIdList));
        uris.add(MARKERS_URI_INDEX, Uri.withAppendedPath(MarkerColumns.CONTENT_URI_BY_TRACKID, trackIdList));

        // Create the intent to start the dashboard
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

        // Create a ClipData object for the Uris and set it on the intent
        ClipData clipData = ClipData.newRawUri(null, uris.get(TRACK_URI_INDEX));
        clipData.addItem(new ClipData.Item(uris.get(TRACKPOINTS_URI_INDEX)));
        clipData.addItem(new ClipData.Item(uris.get(MARKERS_URI_INDEX)));
        intent.setClipData(clipData);

        // If targetPackage and targetClass are provided, set the explicit intent and log it
        if (targetPackage != null && targetClass != null) {
            Log.i(TAG, "Starting dashboard activity with explicit intent (package=" + targetPackage + ", class=" + targetClass + ")");
            intent.setClassName(targetPackage, targetClass);
        }
        // Otherwise, log the use of a generic intent
        else {
            Log.i(TAG, "Starting dashboard activity with generic intent (package=" + targetPackage + ", class=" + targetClass + ")");
        }

        // Try to start the dashboard activity with the intent; show an error message if the dashboard app is not installed
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Dashboard not installed; cannot start it.");
            Toast.makeText(context, R.string.show_on_dashboard_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Filters the given list of tracks by distance.
     *
     * @param inputTracks the list of input tracks to be filtered
     * @param minDistance the minimum distance (in meters) a track must have to be included in the output list
     * @param maxDistance the maximum distance (in meters) a track can have to be included in the output list
     * @return a list of tracks filtered by distance
     */
    public static List<Track> filterTracksByDistance(List<Track> inputTracks, double minDistance, double maxDistance) {
        List<Track> filteredTracks = new ArrayList<>();

        for (Track track : inputTracks) {
            double trackDistance = track.getTrackStatistics().getTotalDistance().toM();
            if (trackDistance >= minDistance && trackDistance <= maxDistance) {
                filteredTracks.add(track);
            }
        }

        return filteredTracks;
    }


    /**
     * Filters the given list of tracks by date.
     *
     * @param inputTracks the list of input tracks to be filtered
     * @param startDate   the start date (inclusive) a track must have to be included in the output list
     * @param endDate     the end date (inclusive) a track can have to be included in the output list
     * @return a list of tracks filtered by date
     */
    public static List<Track> filterTracksByDate(List<Track> inputTracks, Instant startDate, Instant endDate) {
        List<Track> filteredTracks = new ArrayList<>();

        for (Track track : inputTracks) {
            Instant trackStartTime = Instant.from(track.getStartTime());
            if (trackStartTime.compareTo(startDate) >= 0 && trackStartTime.compareTo(endDate) <= 0) {
                filteredTracks.add(track);
            }
        }

        return filteredTracks;
    }


    /**
     * Creates a custom intent to start a dashboard with additional configuration options.
     *
     * @param context        the context
     * @param isRecording    indicates if we are currently recording
     * @param targetPackage  the target package (can be null)
     * @param targetClass    the target class (can be null)
     * @param trackIds       the track ids
     * @param keepScreenOn   indicates if the screen should be kept on while the dashboard is active
     * @param showWhenLocked indicates if the dashboard should be shown when the screen is locked
     * @param showFullScreen indicates if the dashboard should be shown in full-screen mode
     * @return a custom intent to start the dashboard activity
     */
    public static Intent createCustomDashboardIntent(Context context, boolean isRecording, @Nullable String targetPackage, @Nullable String targetClass, Track.Id[] trackIds, boolean keepScreenOn, boolean showWhenLocked, boolean showFullScreen) {
        // If there are no track ids, return null
        if (trackIds.length == 0) {
            return null;
        }

        // Format the track ids as a string list for the Uri
        String trackIdList = ContentProviderUtils.formatIdListForUri(trackIds);

        // Create a list of Uris for the tracks, trackpoints, and markers
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(TRACK_URI_INDEX, Uri.withAppendedPath(TracksColumns.CONTENT_URI, trackIdList));
        uris.add(TRACKPOINTS_URI_INDEX, Uri.withAppendedPath(TrackPointsColumns.CONTENT_URI_BY_TRACKID, trackIdList));
        uris.add(MARKERS_URI_INDEX, Uri.withAppendedPath(MarkerColumns.CONTENT_URI_BY_TRACKID, trackIdList));

        // Create the intent to start the dashboard
        Intent intent = new Intent(ACTION_DASHBOARD);
        intent.putExtra(EXTRAS_PROTOCOL_VERSION, CURRENT_VERSION);
        intent.putParcelableArrayListExtra(ACTION_DASHBOARD_PAYLOAD, uris);
        intent.putExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, keepScreenOn);
        intent.putExtra(EXTRAS_SHOW_WHEN_LOCKED, showWhenLocked);
        intent.putExtra(EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, isRecording);
        if (isRecording) {
            intent.putExtra(EXTRAS_SHOW_FULLSCREEN, showFullScreen);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Create a ClipData object for the Uris and set it on the intent
        ClipData clipData = ClipData.newRawUri(null, uris.get(TRACK_URI_INDEX));
        clipData.addItem(new ClipData.Item(uris.get(TRACKPOINTS_URI_INDEX)));
        clipData.addItem(new ClipData.Item(uris.get(MARKERS_URI_INDEX)));
        intent.setClipData(clipData);

        // If targetPackage and targetClass are provided, set the explicit intent and log it
        if (targetPackage != null && targetClass != null) {
            Log.i(TAG, "Starting dashboard activity with explicit intent (package=" + targetPackage + ", class=" + targetClass + ")");
            intent.setClassName(targetPackage, targetClass);
        }
        // Otherwise, log the use of a generic intent
        else {
            Log.i(TAG, "Creating custom dashboard intent with generic intent (package=" + targetPackage + ", class=" + targetClass + ")");
        }

// Return the custom dashboard intent
        return intent;
    }

    /**
     * Sends an intent to show tracks on a map app that supports a specific track file format.
     *
     * @param context         the context
     * @param trackFileFormat the track file format
     * @param trackIds        the track ids
     */
    private static void showTrackOnMapWithFileFormat(Context context, TrackFileFormat trackFileFormat, Set<Track.Id> trackIds) {

        // If there are no track ids, return
        if (trackIds.isEmpty()) {
            return;
        }

        // Create the intent to show the tracks on a map
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Create a Uri and MIME type for the tracks based on the track file format
        Pair<Uri, String> uriAndMime = ShareContentProvider.createURI(trackIds, "SharingTrack", trackFileFormat);
        intent.setDataAndType(uriAndMime.first, uriAndMime.second);

        // Add the read URI permission flag to the intent
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Show a chooser dialog to open the tracks in the chosen app
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_track_as_trackfileformat, trackFileFormat.getExtension())));
    }


}
