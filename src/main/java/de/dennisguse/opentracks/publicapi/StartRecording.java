package de.dennisguse.opentracks.publicapi;

import android.os.Bundle;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.IntentDashboardUtils;
import de.dennisguse.opentracks.util.TrackUtils;

public class StartRecording extends AbstractAPIActivity {

    public static final String EXTRA_TRACK_NAME = "TRACK_NAME";
    public static final String EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED = "TRACK_CATEGORY"; //TODO Update constant
    public static final String EXTRA_TRACK_ACTIVITY_TYPE_ID = "TRACK_ICON"; //TODO Update constant
    public static final String EXTRA_TRACK_DESCRIPTION = "TRACK_DESCRIPTION";

    public static final String EXTRA_STATS_TARGET_PACKAGE = "STATS_TARGET_PACKAGE";
    public static final String EXTRA_STATS_TARGET_CLASS = "STATS_TARGET_CLASS";

    private static final String TAG = StartRecording.class.getSimpleName();

    protected void execute(TrackRecordingService service) {
        Track.Id trackId = service.startNewTrack();
        if (trackId != null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                updateTrackMetadata(trackId, bundle);

                if (PreferencesUtils.isPublicAPIDashboardEnabled()) {
                    startDashboardAPI(trackId, bundle);
                }
            }
        }
    }

    private void updateTrackMetadata(@NonNull Track.Id trackId, @NonNull Bundle bundle) {
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(this);
        Track track = contentProviderUtils.getTrack(trackId);

        TrackUtils.updateTrack(this, track,
                bundle.getString(EXTRA_TRACK_NAME, null),
                bundle.getString(EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED, null),
                ActivityType.findBy(bundle.getString(EXTRA_TRACK_ACTIVITY_TYPE_ID, null)),
                bundle.getString(EXTRA_TRACK_DESCRIPTION, null),
                contentProviderUtils);
    }

    private void startDashboardAPI(@NonNull Track.Id trackId, @NonNull Bundle bundle) {
        String targetPackage = bundle.getString(EXTRA_STATS_TARGET_PACKAGE, null);
        String targetClass = bundle.getString(EXTRA_STATS_TARGET_CLASS, null);
        if (targetClass != null && targetPackage != null) {
            IntentDashboardUtils.startDashboard(this, true, targetPackage, targetClass, trackId);
        }
    }

    @Override
    protected boolean isPostExecuteStopService() {
        return false;
    }

    @Override
    protected boolean isStartServiceForeground() {
        return true;
    }
}
