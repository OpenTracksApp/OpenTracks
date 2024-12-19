package de.dennisguse.opentracks.publicapi;

import de.dennisguse.opentracks.services.TrackRecordingService;

public class CreateMarker extends AbstractAPIActivity {

    @Override
    protected void execute(TrackRecordingService service) {
        service.createMarker();
    }

    @Override
    protected boolean isPostExecuteStopService() {
        return false;
    }
}
