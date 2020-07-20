package de.dennisguse.opentracks;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;

import androidx.appcompat.app.AppCompatActivity;

import de.dennisguse.opentracks.content.data.TrackPointsColumns;
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
        long[] trackIds = IntentDashboardUtils.extractTrackIdsFromIntent(getIntent());

        showTrackTrackfileFormat(this, trackFileFormat, trackIds);

        finish();
    }

    /**
     * Send intent to show tracks on a map (needs an another app) as KMZ.
     *
     * @param context  the context
     * @param trackIds the track ids
     */
    private static void showTrackTrackfileFormat(Context context, TrackFileFormat trackFileFormat, long[] trackIds) {
        if (trackIds.length == 0) {
            return;
        }

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(TrackPointsColumns.TRACKID, trackIds[0]);
        Pair<Uri, String> uriAndMime = ShareContentProvider.createURI(trackIds, "SharingTrack", trackFileFormat);
        intent.setDataAndType(uriAndMime.first, uriAndMime.second);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_track_as_trackfileformat, trackFileFormat.getExtension())));
    }

    public static class KMZ extends ShowOnMapProxyActivity {
        public KMZ() {
            super(TrackFileFormat.KMZ_WITH_TRACKDETAIL);
        }
    }

    public static class GPX extends ShowOnMapProxyActivity {
        public GPX() {
            super(TrackFileFormat.GPX);
        }
    }
}