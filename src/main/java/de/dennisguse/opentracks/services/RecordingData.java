package de.dennisguse.opentracks.services;

import androidx.annotation.NonNull;

import java.util.Objects;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.stats.TrackStatistics;

public class RecordingData {

    private final Track track;

    private final TrackPoint latestTrackPoint;

    private final SensorDataSet sensorDataSet;

    /**
     * {@link Track} and {@link TrackPoint} must be immutable (i.e., their content does not change).
     */
    public RecordingData(Track track, TrackPoint lastTrackPoint, SensorDataSet sensorDataSet) {
        this.track = track;
        this.latestTrackPoint = lastTrackPoint;
        this.sensorDataSet = sensorDataSet;
    }

    public Track getTrack() {
        return track;
    }

    public String getTrackCategory() {
        if (track == null) {
            return "";
        }
        return track.getCategory();
    }

    @NonNull
    public TrackStatistics getTrackStatistics() {
        if (track == null) {
            return new TrackStatistics();
        }

        return track.getTrackStatistics();
    }

    public TrackPoint getLatestTrackPoint() {
        return latestTrackPoint;
    }

    public SensorDataSet getSensorDataSet() {
        return sensorDataSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordingData that = (RecordingData) o;
        return Objects.equals(track, that.track) && Objects.equals(latestTrackPoint, that.latestTrackPoint) && Objects.equals(sensorDataSet, that.sensorDataSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(track, latestTrackPoint, sensorDataSet);
    }

    @NonNull
    @Override
    public String toString() {
        return "RecordingData{" +
                "track=" + track +
                ", latestTrackPoint=" + latestTrackPoint +
                ", sensorDataSet=" + sensorDataSet +
                '}';
    }
}
