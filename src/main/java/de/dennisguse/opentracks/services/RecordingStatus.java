package de.dennisguse.opentracks.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.dennisguse.opentracks.data.models.Track;

public record RecordingStatus(@Nullable Track.Id trackId) {

    static RecordingStatus record(@NonNull Track.Id trackId) {
        return new RecordingStatus(trackId);
    }

    public boolean isRecording() {
        return trackId != null;
    }

    static RecordingStatus notRecording() {
        return new RecordingStatus(null);
    }

    public RecordingStatus stop() {
        return TrackRecordingService.STATUS_DEFAULT;
    }
}
