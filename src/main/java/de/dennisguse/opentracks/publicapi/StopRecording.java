package de.dennisguse.opentracks.publicapi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordedActivity;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.TrackStoppedActivity;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.services.RecordingData;
import de.dennisguse.opentracks.services.RecordingStatus;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.util.ExportUtils;
import de.dennisguse.opentracks.util.IntentUtils;

public class StopRecording extends AbstractAPIActivity {
    public static final String EXTRA_OPEN_CURRENT_TRACK_SCREEN = "OPEN_CURRENT_TRACK_SCREEN";

    protected void execute(TrackRecordingService service) {
        RecordingData recordingData = service.getRecordingDataObservable().getValue();
        Track.Id trackId = null;
        if (recordingData != null && recordingData.getTrack() != null) {
            trackId = recordingData.getTrack().getId();
        } else {
            Intent newIntent = IntentUtils.newIntent(StopRecording.this, StartRecording.class)
                    .putExtra(TrackRecordedActivity.EXTRA_TRACK_ID, trackId);
            startActivity(newIntent);
            return;
        }

        service.endCurrentTrack();

        if (trackId != null) {
            ExportUtils.postWorkoutExport(this, trackId);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle.getBoolean(EXTRA_OPEN_CURRENT_TRACK_SCREEN, false)) {
            Intent newIntent = IntentUtils.newIntent(StopRecording.this, TrackStoppedActivity.class)
                    .putExtra(TrackRecordedActivity.EXTRA_TRACK_ID, trackId);
            startActivity(newIntent);
        }
    }

    @Override
    protected boolean isPostExecuteStopService() {
        return true;
    }
}
