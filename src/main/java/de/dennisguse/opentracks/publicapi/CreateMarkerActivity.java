package de.dennisguse.opentracks.publicapi;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.time.Instant;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.ui.markers.MarkerEditActivity;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * Public API to creates a Marker for a given track with a given location
 */
public class CreateMarkerActivity extends AppCompatActivity {

    public static final String EXTRA_TRACK_ID = "track_id";
    public static final String EXTRA_LOCATION = "location";

    private static final String TAG = CreateMarkerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: do we want to check if public API is enabled?

        Track.Id trackId;
        if (getIntent().hasExtra(EXTRA_TRACK_ID)) {
            trackId = new Track.Id(getIntent().getLongExtra(EXTRA_TRACK_ID, 0L));
        } else {
            trackId = null;
        }
        TrackPoint trackPoint;
        if (getIntent().hasExtra(EXTRA_LOCATION)) {
            Location location = getIntent().getParcelableExtra(EXTRA_LOCATION);
            trackPoint = new TrackPoint(location, Instant.now());
        } else {
            trackPoint = null;
        }

        TrackRecordingServiceConnection.execute(this, (service, self) -> {
            Marker.Id marker = createNewMarker(trackId, trackPoint, service);
            if (marker == null) {
                Toast.makeText(this, R.string.create_marker_error, Toast.LENGTH_LONG).show();
            } else {
                Intent intent = IntentUtils
                        .newIntent(this, MarkerEditActivity.class)
                        .putExtra(MarkerEditActivity.EXTRA_TRACK_ID, trackId)
                        .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, marker);
                startActivity(intent);
            }
            finish();
        });
    }

    private Marker.Id createNewMarker(Track.Id trackId, TrackPoint trackPoint, TrackRecordingService trackRecordingService) {
        try {
            if (trackPoint == null || trackId == null) {
                throw new IllegalStateException("TrackPoint or Track.Id is null");
            }
            return trackRecordingService.insertMarker("", "", "", null, trackId, trackPoint);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Unable to add marker.", e);
        }
        return null;
    }

}
