package de.dennisguse.opentracks;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ShareContentProvider;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.IntentDashboardUtils;

/**
 * Used to convert IntentDashboardUtils.startDashboard-requests into {@link TrackFileFormat}.
 */
public abstract class ShowOnMapProxyActivity extends AppCompatActivity {

    private final TrackFileFormat trackFileFormat;

    protected ShowOnMapProxyActivity(TrackFileFormat trackFileFormat) {
        this.trackFileFormat = trackFileFormat;
    }

    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Set<Track.Id> trackIds = IntentDashboardUtils.extractTrackIdsFromIntent(getIntent());

        showTrackfileFormat(this, trackFileFormat, trackIds);

        finish();
    }

    /**
     * Send intent to show tracks on a map (needs an another app) as KMZ.
     *
     * @param context  the context
     * @param trackIds the track ids
     */
    private static void showTrackfileFormat(Context context, TrackFileFormat trackFileFormat, Set<Track.Id> trackIds) {
        if (trackIds.isEmpty()) {
            return;
        }

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Pair<Uri, String> uriAndMime = ShareContentProvider.createURI(trackIds, "SharingTrack", trackFileFormat);
        intent.setDataAndType(uriAndMime.first, uriAndMime.second);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_track_as_trackfileformat, trackFileFormat.getExtension())));
    }

    public static class KML extends ShowOnMapProxyActivity {
        public KML() {
            super(TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA);
        }
    }

    public static class GPX extends ShowOnMapProxyActivity {
        public GPX() {
            super(TrackFileFormat.GPX);
        }
    }
}