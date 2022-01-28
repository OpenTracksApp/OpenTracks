package de.dennisguse.opentracks.services;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Objects;

import de.dennisguse.opentracks.data.models.Track;

public class RecordingStatus {
    private final Track.Id trackId;
    private final boolean paused;

    @VisibleForTesting
    RecordingStatus(Track.Id trackId, boolean paused) {
        this.trackId = trackId;
        this.paused = paused;
    }

    public Track.Id getTrackId() {
        return trackId;
    }

    public boolean isRecording() {
        return trackId != null;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isRecordingAndNotPaused() {
        return isRecording() && !isPaused();
    }

    static RecordingStatus notRecording() {
        return new RecordingStatus(null, false);
    }

    static RecordingStatus record(@NonNull Track.Id trackId) {
        return new RecordingStatus(trackId, false);
    }

    RecordingStatus pause() {
        return new RecordingStatus(getTrackId(), true);
    }

    public RecordingStatus stop() {
        return TrackRecordingService.STATUS_DEFAULT;
    }

    @NonNull
    @Override
    public String toString() {
        return "RecordingStatus{" +
                "trackId=" + trackId +
                ", paused=" + paused +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordingStatus that = (RecordingStatus) o;
        return paused == that.paused && Objects.equals(trackId, that.trackId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackId, paused);
    }
}
