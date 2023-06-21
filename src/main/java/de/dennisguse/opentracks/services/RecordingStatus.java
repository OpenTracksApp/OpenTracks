package de.dennisguse.opentracks.services;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Track;

public record RecordingStatus(Track.Id trackId) {

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
}
