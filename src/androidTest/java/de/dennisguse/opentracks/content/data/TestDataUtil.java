package de.dennisguse.opentracks.content.data;

import android.location.Location;
import android.util.Pair;

import de.dennisguse.opentracks.content.provider.ContentProviderUtils;

public class TestDataUtil {

    public static final double INITIAL_LATITUDE = 37.0;
    public static final double INITIAL_LONGITUDE = -57.0;
    public static final double ALTITUDE_INTERVAL = 2.5;

    /**
     * Create a track without any trackPoints.
     */
    public static Track createTrack(long trackId) {
        Track track = new Track();
        track.setId(trackId);
        track.setName("Test: " + trackId);

        return track;
    }

    /**
     * Simulates a track which is used for testing.
     *
     * @param trackId   the trackId of the track
     * @param numPoints the trackPoints number in the track
     */
    public static Pair<Track, TrackPoint[]> createTrack(long trackId, int numPoints) {
        Track track = createTrack(trackId);

        TrackPoint[] trackPoints = new TrackPoint[numPoints];
        for (int i = 0; i < numPoints; i++) {
            trackPoints[i] = (createTrackPoint(i));
        }

        return new Pair<>(track, trackPoints);
    }

    public static Track createTrackAndInsert(ContentProviderUtils contentProviderUtils, long trackId, int numPoints) {
        Pair<Track, TrackPoint[]> pair = createTrack(trackId, numPoints);

        insertTrackWithLocations(contentProviderUtils, pair.first, pair.second);

        return pair.first;
    }

    /**
     * Creates a location.
     *
     * @param i the index to set the value of location.
     * @return created location
     */
    public static TrackPoint createTrackPoint(int i) {
        Location location = new Location("test");
        location.setLatitude(INITIAL_LATITUDE + (double) i / 10000.0);
        location.setLongitude(INITIAL_LONGITUDE - (double) i / 10000.0);
        location.setAccuracy((float) i / 100.0f);
        location.setAltitude(i * ALTITUDE_INTERVAL);
        location.setTime(i + 1);
        return new TrackPoint(location);
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
}
