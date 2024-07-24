package de.dennisguse.opentracks.publicapi;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.time.Instant;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.PreferencesUtils;
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

        if (!PreferencesUtils.isPublicAPIenabled()) {
            Toast.makeText(this, R.string.publicapi_disabled, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Track.Id trackId = new Track.Id(getIntent().getLongExtra(EXTRA_TRACK_ID, 0L));
        TrackPoint trackPoint = new TrackPoint(getIntent().<Location>getParcelableExtra(EXTRA_LOCATION), Instant.now());

        TrackRecordingServiceConnection.execute(this, (service, self) -> {
            Marker.Id marker = createNewMarker(trackId, trackPoint, service);
            if (marker == null) {
                Toast.makeText(this, R.string.create_marker_error, Toast.LENGTH_LONG).show();
            } else {
                Intent intent = IntentUtils
                        .newIntent(this, MarkerEditActivity.class)
                        .putExtra(MarkerEditActivity.EXTRA_TRACK_ID, trackId)
                        .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, marker)
                        .putExtra(MarkerEditActivity.EXTRA_IS_NEW_MARKER, true);
                startActivity(intent);
            }
            finish();
        });
    }

    private Marker.Id createNewMarker(@NonNull Track.Id trackId, @NonNull TrackPoint trackPoint, TrackRecordingService trackRecordingService) {
        return trackRecordingService.insertMarker("", "", "", null, trackId, trackPoint);
    }

}
