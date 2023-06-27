package de.dennisguse.opentracks.services;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * {@link Track} and {@link TrackPoint} must be immutable (i.e., their content does not change).
 */
public record RecordingData(Track track, TrackPoint latestTrackPoint, SensorDataSet sensorDataSet) {

    public String getTrackCategory() {
        if (track == null) {
            return "";
        }
        return track.getActivityTypeLocalized();
    }

    @NonNull
    public TrackStatistics getTrackStatistics() {
        if (track == null) {
            return new TrackStatistics();
        }

        return track.getTrackStatistics();
    }
}
