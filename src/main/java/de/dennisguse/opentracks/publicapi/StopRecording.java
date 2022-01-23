package de.dennisguse.opentracks.publicapi;

import de.dennisguse.opentracks.services.TrackRecordingService;

public class StopRecording extends AbstractAPIActivity {

    protected void execute(TrackRecordingService service) {
        service.endCurrentTrack();
    }

    @Override
    protected boolean isPostExecuteStopService() {
        return true;
    }
}
