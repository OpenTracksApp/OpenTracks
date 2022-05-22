package de.dennisguse.opentracks.services;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Objects;

import de.dennisguse.opentracks.data.models.Track;

public class RecordingStatus {
    private final Track.Id trackId;

    @VisibleForTesting
    RecordingStatus(Track.Id trackId) {
        this.trackId = trackId;
    }

    public Track.Id getTrackId() {
        return trackId;
    }

    public boolean isRecording() {
        return trackId != null;
    }

    static RecordingStatus notRecording() {
        return new RecordingStatus(null);
    }

    static RecordingStatus record(@NonNull Track.Id trackId) {
        return new RecordingStatus(trackId);
    }

    public RecordingStatus stop() {
        return TrackRecordingService.STATUS_DEFAULT;
    }

    @NonNull
    @Override
    public String toString() {
        return "RecordingStatus{" +
                "trackId=" + trackId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordingStatus that = (RecordingStatus) o;
        return Objects.equals(trackId, that.trackId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackId);
    }
}
