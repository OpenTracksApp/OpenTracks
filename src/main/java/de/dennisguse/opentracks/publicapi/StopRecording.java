package de.dennisguse.opentracks.publicapi;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.services.RecordingData;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.util.ExportUtils;

public class StopRecording extends AbstractAPIActivity {

    protected void execute(TrackRecordingService service) {
        RecordingData recordingData = service.getRecordingDataObservable().getValue();
        Track.Id trackId = null;
        if (recordingData != null && recordingData.track() != null) {
            trackId = recordingData.track().getId();
        }

        service.endCurrentTrack();

        if (trackId != null) {
            ExportUtils.postWorkoutExport(this, trackId);
        }
    }

    @Override
    protected boolean isPostExecuteStopService() {
        return true;
    }
}
