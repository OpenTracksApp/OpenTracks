package de.dennisguse.opentracks.content.data;

public class TestDataUtil {

    public static final double INITIAL_LATITUDE = 37.0;
    public static final double INITIAL_LONGITUDE = -57.0;
    public static final double ALTITUDE_INTERVAL = 2.5;

    /**
     * Simulates a track which is used for testing.
     *
     * @param id        the id of the track
     * @param numPoints the location number in the track
     * @return the simulated track
     */
    public static Track getTrack(long id, int numPoints) {
        Track track = new Track();
        track.setId(id);
        track.setName("Test: " + id);
        for (int i = 0; i < numPoints; i++) {
            track.addTrackPoint(createTrackPoint(i));
        }
        return track;
    }

    /**
     * Creates a location.
     *
     * @param i the index to set the value of location.
     * @return created location
     */
    public static TrackPoint createTrackPoint(int i) {
        TrackPoint trackpoint = new TrackPoint("test");
        trackpoint.setLatitude(INITIAL_LATITUDE + (double) i / 10000.0);
        trackpoint.setLongitude(INITIAL_LONGITUDE - (double) i / 10000.0);
        trackpoint.setAccuracy((float) i / 100.0f);
        trackpoint.setAltitude(i * ALTITUDE_INTERVAL);
        trackpoint.setTime(i + 1);
        return trackpoint;
    }
}
