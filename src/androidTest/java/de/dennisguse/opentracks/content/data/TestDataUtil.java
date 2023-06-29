package de.dennisguse.opentracks.content.data;

import android.content.Context;
import android.net.Uri;
import android.util.Pair;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.TrackPointIterator;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.ui.markers.MarkerUtils;
import de.dennisguse.opentracks.util.FileUtils;

public class TestDataUtil {

    public static final double INITIAL_LATITUDE = 37.0;
    public static final double INITIAL_LONGITUDE = -57.0;
    public static final double ALTITUDE_INTERVAL = 2.5;
    public static final float ALTITUDE_GAIN = 3;
    public static final float ALTITUDE_LOSS = 3;

    /**
     * Create a track without any trackPoints.
     */
    public static Track createTrack(Track.Id trackId) {
        Track track = new Track();
        track.setId(trackId);
        track.setName("Test: " + trackId.id());

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
                TrackPoint.createSegmentStartManualWithTime(Instant.ofEpochSecond(i++)),
                createTrackPoint(i++),
                createTrackPoint(i++),
                createTrackPoint(i++),
                createTrackPoint(i++, TrackPoint.Type.SEGMENT_START_AUTOMATIC),
                createTrackPoint(i++),
                createTrackPoint(i++),
                createTrackPoint(i++),
                createTrackPoint(i++, TrackPoint.Type.SEGMENT_END_MANUAL),

                TrackPoint.createSegmentStartManualWithTime(Instant.ofEpochSecond(i++)),
                createTrackPoint(i++),
                createTrackPoint(i++),
                createTrackPoint(i++),
                createTrackPoint(i, TrackPoint.Type.SEGMENT_END_MANUAL)
        );

        //TODO Use TrackStatisticsUpdater
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(0));
        stats.setTotalTime(Duration.ofMillis(0));
        List<Marker> markers = List.of(
                new Marker("Marker 1", "Marker description 1", "Marker category 3", "", trackId, stats, trackPoints.get(1), null),
                new Marker("Marker 2", "Marker description 2", "Marker category 3", "", trackId, stats, trackPoints.get(4), null),
                new Marker("Marker 3", "Marker description 3", "Marker category 3", "", trackId, stats, trackPoints.get(5), null)
        );

        return new TrackData(track, trackPoints, markers);
    }

    public record TrackData(Track track, List<TrackPoint> trackPoints, List<Marker> markers) {
    }


    public static Track createTrackAndInsert(ContentProviderUtils contentProviderUtils, Track.Id trackId, int numPoints) {
        Pair<Track, List<TrackPoint>> pair = createTrack(trackId, numPoints);

        insertTrackWithLocations(contentProviderUtils, pair.first, pair.second);

        return pair.first;
    }

    public static TrackPoint createTrackPoint(int i) {
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochSecond(i));
        trackPoint.setLatitude(INITIAL_LATITUDE + (double) i / 10000.0);
        trackPoint.setLongitude(INITIAL_LONGITUDE - (double) i / 10000.0);
        trackPoint.setHorizontalAccuracy(Distance.of(i / 100.0f));
        trackPoint.setAltitude(i * ALTITUDE_INTERVAL);
        trackPoint.setSpeed(Speed.of(5f + (i / 10f)));

        trackPoint.setHeartRate(100f + i % 80);
        trackPoint.setCadence(300f + i);
        trackPoint.setPower(400f + i);
        trackPoint.setAltitudeGain(ALTITUDE_GAIN);
        trackPoint.setAltitudeLoss(ALTITUDE_LOSS);
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
        File dstFile = new File(MarkerUtils.getImageUrl(context, trackId));
        dstFile.createNewFile();
        Uri photoUri = FileUtils.getUriForFile(context, dstFile);
        String photoUrl = photoUri.toString();

        //TODO Use TrackStatisticsUpdater
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(0));
        stats.setTotalTime(Duration.ofMillis(0));

        return new Marker("Marker name", "Marker description", "Marker category", "", trackId, stats, trackPoint, photoUrl);
    }

    public static List<TrackPoint> getTrackPoints(ContentProviderUtils contentProviderUtils, Track.Id trackId) {
        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, null)) {
            ArrayList<TrackPoint> trackPoints = new ArrayList<>();
            while (trackPointIterator.hasNext()) {
                trackPoints.add(trackPointIterator.next());
            }
            return trackPoints;
        }
    }

    public static Pair<Track.Id, TrackStatistics> buildTrackWithTrackPoints(ContentProviderUtils contentProviderUtils, int numberOfPoints) {
        Track dummyTrack = new Track();
        dummyTrack.setId(new Track.Id(System.currentTimeMillis()));
        dummyTrack.setName("Dummy Track");
        contentProviderUtils.insertTrack(dummyTrack);
        TrackStatisticsUpdater trackStatisticsUpdater = new TrackStatisticsUpdater();
        for (int i = 0; i < numberOfPoints; i++) {
            TrackPoint tp = TestDataUtil.createTrackPoint(i);
            contentProviderUtils.insertTrackPoint(tp, dummyTrack.getId());
            trackStatisticsUpdater.addTrackPoint(tp);
        }
        dummyTrack.setTrackStatistics(trackStatisticsUpdater.getTrackStatistics());
        contentProviderUtils.updateTrack(dummyTrack);
        return new Pair<>(dummyTrack.getId(), trackStatisticsUpdater.getTrackStatistics());
    }
}
