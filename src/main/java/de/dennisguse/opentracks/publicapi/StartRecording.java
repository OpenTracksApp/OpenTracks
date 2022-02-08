package de.dennisguse.opentracks.publicapi;

import android.os.Bundle;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.IntentDashboardUtils;

public class StartRecording extends AbstractAPIActivity {
    private static final String TAG = StartRecording.class.getSimpleName();

    protected void execute(TrackRecordingService service) {
        Track.Id trackId = service.startNewTrack();
        if ((trackId != null) && (PreferencesUtils.isPublicAPIDashboardEnabled())) {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                String targetPackage = bundle.getString("STATS_TARGET_PACKAGE", null);
                String targetClass = bundle.getString("STATS_TARGET_CLASS", null);
                IntentDashboardUtils.startDashboard(this, true, targetPackage, targetClass, trackId);
            }
        }
    }

    @Override
    protected boolean isPostExecuteStopService() {
        return false;
    }
}
