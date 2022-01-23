package de.dennisguse.opentracks.publicapi;

import de.dennisguse.opentracks.services.TrackRecordingService;

public class StartRecording extends AbstractAPIActivity {

    protected void execute(TrackRecordingService service) {
        service.startNewTrack();
    }

    @Override
    protected boolean isPostExecuteStopService() {
        return false;
    }
}
