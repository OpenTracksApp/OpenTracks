package de.dennisguse.opentracks.publicapi;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.ui.markers.MarkerEditActivity;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * INTERNAL: only meant for clients of OSMDashboard API.
 */
public class CreateMarkerActivity extends AppCompatActivity {

    public static final String EXTRA_TRACK_ID = "track_id";
    public static final String EXTRA_LOCATION = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Track.Id trackId = new Track.Id(getIntent().getLongExtra(EXTRA_TRACK_ID, -1L));
        Location location = getIntent().getParcelableExtra(EXTRA_LOCATION);
        if (!getIntent().hasExtra(EXTRA_TRACK_ID) || location == null) {
            throw new IllegalStateException("Parameter 'track_id' and/or 'location' missing or invalid.");
        }

        TrackRecordingServiceConnection.execute(this, (service, self) -> {
            Intent intent = IntentUtils
                    .newIntent(this, MarkerEditActivity.class)
                    .putExtra(MarkerEditActivity.EXTRA_TRACK_ID, trackId)
                    .putExtra(MarkerEditActivity.EXTRA_LOCATION, location);
            startActivity(intent);
            finish();
        });
    }

}
