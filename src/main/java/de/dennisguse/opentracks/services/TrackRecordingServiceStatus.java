package de.dennisguse.opentracks.services;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.services.handlers.GpsStatusValue;

/**
 * This class handles the status of the recording service.
 * Also offer an interface through which to know the recording Track.Id, pause status and GPS status.
 */
public class TrackRecordingServiceStatus {

    private Track.Id recordingTrackId;
    private boolean recordingTrackPaused;

    private final List<Listener> listeners = new ArrayList<>();

    public void addListener(@NonNull Listener listener) {
        if (this.listeners.contains(listener)) {
            return;
        }
        this.listeners.add(listener);
        listener.onTrackRecordingId(recordingTrackId);
        listener.onTrackRecordingPaused(recordingTrackPaused);
    }

    public boolean getRecordingTrackPaused() {
        return this.recordingTrackPaused;
    }

    public Track.Id getRecordingTrackId() {
        return this.recordingTrackId;
    }

    public void onStop() {
        for (Listener listener : listeners) {
            listener.onGpsStatus(GpsStatusValue.GPS_NONE);
            listener.onTrackRecordingId(null);
            listener.onTrackRecordingPaused(true);
        }
        listeners.clear();
    }

    public boolean isRecording() {
        return recordingTrackId != null;
    }

    void onChange(Track.Id trackId, boolean paused) {
        recordingTrackId = trackId;
        recordingTrackPaused = paused;
        for (Listener listener : listeners) {
            listener.onTrackRecordingId(recordingTrackId);
            listener.onTrackRecordingPaused(recordingTrackPaused);
        }
    }

    void onChange(boolean paused) {
        recordingTrackPaused = paused;
        for (Listener listener : listeners) {
            listener.onTrackRecordingPaused(recordingTrackPaused);
        }
    }

    void onChange(GpsStatusValue statusValue) {
        for (Listener listener : listeners) {
            listener.onGpsStatus(statusValue);
        }
    }

    public interface Listener {
        default void onGpsStatus(GpsStatusValue newValue) {}
        default void onTrackRecordingPaused(boolean paused) {}
        default void onTrackRecordingId(Track.Id trackId) {}
    }
}
