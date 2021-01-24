package de.dennisguse.opentracks.content.data;

import android.content.Context;
import android.net.Uri;
import android.util.Pair;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.util.FileUtils;

public class TestDataUtil {

    public static final double INITIAL_LATITUDE = 37.0;
    public static final double INITIAL_LONGITUDE = -57.0;
    public static final double ALTITUDE_INTERVAL = 2.5;
    public static final float ELEVATION_GAIN = 3;
    public static final float ELEVATION_LOSS = 3;

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
    @Deprecated //TODO Should start with SEGMENT_START_MANUAL and end with SEGMENT_END_MANUAL.
    public static Pair<Track, List<TrackPoint>> createTrack(Track.Id trackId, int numPoints) {
        Track track = createTrack(trackId);

        List<TrackPoint> trackPoints = new ArrayList<>(numPoints);
        for (int i = 0; i < numPoints; i++) {
            trackPoints.add(createTrackPoint(i));
        }

        return new Pair<>(track, trackPoints);
    }


    public static TrackData createTestingTrack(Track.Id trackId) {
        Track track = createTrack(trackId);

        int i = 0;
        List<TrackPoint> trackPoints = List.of(
                TrackPoint.createSegmentStartManualWithTime(Instant.ofEpochMilli(i++ + 1)),
                createTrackPoint(i++),
                createTrackPoint(i++),
                createTrackPoint(i++),
                createTrackPoint(i++, TrackPoint.Type.SEGMENT_START_AUTOMATIC),
                createTrackPoint(i++),
                createTrackPoint(i++),
                createTrackPoint(i++),
                TrackPoint.createSegmentEndWithTime(Instant.ofEpochSecond(i++ + 1)),

                TrackPoint.createSegmentStartManualWithTime(Instant.ofEpochSecond(i++)),
                createTrackPoint(i++),
                createTrackPoint(i++),
                createTrackPoint(i++),
                TrackPoint.createSegmentEndWithTime(Instant.ofEpochSecond(i++ + 1))
        );

        List<Marker> markers = List.of(
                new Marker("Marker 1", "Marker description 1", "Marker category 3", "", trackId, 0.0, 0, trackPoints.get(1), null),
                new Marker("Marker 2", "Marker description 2", "Marker category 3", "", trackId, 0.0, 0, trackPoints.get(4), null),
                new Marker("Marker 3", "Marker description 3", "Marker category 3", "", trackId, 0.0, 0, trackPoints.get(5), null)
        );

        return new TrackData(track, trackPoints, markers);
    }

    public static class TrackData {
        public final Track track;
        public final List<TrackPoint> trackPoints;
        public final List<Marker> markers;

        public TrackData(Track track, List<TrackPoint> trackPoints, List<Marker> markers) {
            this.track = track;
            this.trackPoints = trackPoints;
            this.markers = markers;
        }
    }


    public static Track createTrackAndInsert(ContentProviderUtils contentProviderUtils, Track.Id trackId, int numPoints) {
        Pair<Track, List<TrackPoint>> pair = createTrack(trackId, numPoints);

        insertTrackWithLocations(contentProviderUtils, pair.first, pair.second);

        return pair.first;
    }

    /**
     * Creates a location.
     *
     * @param i the index for the TrackPoint.
     */
    public static TrackPoint createTrackPoint(int i) {
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT);
        trackPoint.setLatitude(INITIAL_LATITUDE + (double) i / 10000.0);
        trackPoint.setLongitude(INITIAL_LONGITUDE - (double) i / 10000.0);
        trackPoint.setAccuracy((float) i / 100.0f);
        trackPoint.setAltitude(i * ALTITUDE_INTERVAL);
        trackPoint.setTime(Instant.ofEpochSecond(i + 1));
        trackPoint.setSpeed(5f + (i / 10f));

        trackPoint.setHeartRate_bpm(100f + i);
        trackPoint.setCyclingCadence_rpm(300f + i);
        trackPoint.setPower(400f + i);
        trackPoint.setElevationGain(ELEVATION_GAIN);
        trackPoint.setElevationLoss(ELEVATION_LOSS);
        return trackPoint;
    }

    public static TrackPoint createTrackPoint(int i, TrackPoint.Type type) {
        TrackPoint trackPoint = createTrackPoint(i);
        trackPoint.setType(type);
        return trackPoint;
    }

    /**
     * Inserts a track with locations into the database.
     *
     * @param track       track to be inserted
     * @param trackPoints trackPoints to be inserted
     */
    public static void insertTrackWithLocations(ContentProviderUtils contentProviderUtils, Track track, List<TrackPoint> trackPoints) {
        contentProviderUtils.insertTrack(track);
        contentProviderUtils.bulkInsertTrackPoint(trackPoints, track.getId());
    }

    public static Marker createMarkerWithPhoto(Context context, Track.Id trackId, TrackPoint trackPoint) throws IOException {
        File dstFile = new File(FileUtils.getImageUrl(context, trackId));
        dstFile.createNewFile();
        Uri photoUri = FileUtils.getUriForFile(context, dstFile);
        String photoUrl = photoUri.toString();

        return new Marker("Marker name", "Marker description", "Marker category", "", trackId, 0.0, 0, trackPoint, photoUrl);
    }
}
