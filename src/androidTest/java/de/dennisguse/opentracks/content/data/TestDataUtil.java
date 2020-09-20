package de.dennisguse.opentracks.content.data;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.util.Pair;

import java.io.File;
import java.io.IOException;

import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.util.FileUtils;

public class TestDataUtil {

    public static final double INITIAL_LATITUDE = 37.0;
    public static final double INITIAL_LONGITUDE = -57.0;
    public static final double ALTITUDE_INTERVAL = 2.5;
    public static final float ELEVATION_GAIN = 3;

    /**
     * Create a track without any trackPoints.
     */
    public static Track createTrack(Track.Id trackId) {
        Track track = new Track();
        track.setId(trackId);
        track.setName("Test: " + trackId.getId());

        return track;
    }

    /**
     * Simulates a track which is used for testing.
     *
     * @param trackId   the trackId of the track
     * @param numPoints the trackPoints number in the track
     */
    public static Pair<Track, TrackPoint[]> createTrack(Track.Id trackId, int numPoints) {
        Track track = createTrack(trackId);

        TrackPoint[] trackPoints = new TrackPoint[numPoints];
        for (int i = 0; i < numPoints; i++) {
            trackPoints[i] = createTrackPoint(i);
        }

        return new Pair<>(track, trackPoints);
    }

    public static Track createTrackAndInsert(ContentProviderUtils contentProviderUtils, Track.Id trackId, int numPoints) {
        Pair<Track, TrackPoint[]> pair = createTrack(trackId, numPoints);

        insertTrackWithLocations(contentProviderUtils, pair.first, pair.second);

        return pair.first;
    }

    /**
     * Creates a location.
     *
     * @param i the index for the TrackPoint.
     */
    public static TrackPoint createTrackPoint(int i) {
        TrackPoint trackPoint = new TrackPoint();
        trackPoint.setLatitude(INITIAL_LATITUDE + (double) i / 10000.0);
        trackPoint.setLongitude(INITIAL_LONGITUDE - (double) i / 10000.0);
        trackPoint.setAccuracy((float) i / 100.0f);
        trackPoint.setAltitude(i * ALTITUDE_INTERVAL);
        trackPoint.setTime(i + 1);
        trackPoint.setSpeed(5f + (i / 10));

        trackPoint.setHeartRate_bpm(100f + i);
        trackPoint.setCyclingCadence_rpm(300f + i);
        trackPoint.setPower(400f + i);
        trackPoint.setElevationGain(ELEVATION_GAIN);
        return trackPoint;
    }

    /**
     * Inserts a track with locations into the database.
     *
     * @param track       track to be inserted
     * @param trackPoints trackPoints to be inserted
     */
    public static void insertTrackWithLocations(ContentProviderUtils contentProviderUtils, Track track, TrackPoint[] trackPoints) {
        contentProviderUtils.insertTrack(track);
        contentProviderUtils.bulkInsertTrackPoint(trackPoints, track.getId());
    }

    /**
     * Creates a Waypoint with a photo.
     *
     * @param context  The context.
     * @param trackId  The track id.
     * @param location The location.
     * @return the Waypoint created.
     */
    public static Marker createWaypointWithPhoto(Context context, Track.Id trackId, Location location) throws IOException {
        File dstFile = new File(FileUtils.getImageUrl(context, trackId));
        dstFile.createNewFile();
        Uri photoUri = FileUtils.getUriForFile(context, dstFile);
        String photoUrl = photoUri.toString();

        return new Marker("Waypoint name", "Waypoint description", "Waypoint category", "", trackId, 0.0, 0, location, photoUrl);
    }
}
