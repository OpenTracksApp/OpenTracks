package de.dennisguse.opentracks.content.data;

import android.location.Location;

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
        Location location = new Location("test");
        location.setLatitude(INITIAL_LATITUDE + (double) i / 10000.0);
        location.setLongitude(INITIAL_LONGITUDE - (double) i / 10000.0);
        location.setAccuracy((float) i / 100.0f);
        location.setAltitude(i * ALTITUDE_INTERVAL);
        location.setTime(i + 1);
        return new TrackPoint(location);
    }
}
