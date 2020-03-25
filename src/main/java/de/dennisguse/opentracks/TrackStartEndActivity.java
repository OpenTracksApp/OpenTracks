package de.dennisguse.opentracks;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.services.TrackRecordingServiceInterface;
import de.dennisguse.opentracks.stats.TripStatistics;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * An activity to handle the track's start, resume and save.
 */
public class TrackStartEndActivity extends AbstractListActivity {

    private static final String TAG = TrackStartEndActivity.class.getSimpleName();

    public static final String EXTRA_COMMAND = "command";
    public static final int COMMAND_NOTHING = -1;
    public static final int COMMAND_START = 0;
    public static final int COMMAND_FINISH = 1;

    // Views.
    private ImageButton leftButton;
    private ImageButton rightButton;
    private View trackSumUpContainer;
    private View textArrowContainer;
    private TextView leftText;
    private TextView rightText;
    private TextView distanceValue;
    private TextView distanceUnit;
    private TextView movingTimeValue;

    private int command;
    private TrackRecordingServiceConnection trackRecordingServiceConnection;

    private final Runnable bindChangedCallback = new Runnable() {
        @Override
        public void run() {
            if (command == COMMAND_START) {
                TrackRecordingServiceInterface service = trackRecordingServiceConnection.getServiceIfBound();
                if (service == null) {
                    Log.d(TAG, "service not available to start gps or a new recording");
                    return;
                }

                long trackId = service.startNewTrack();
                Intent intent = IntentUtils.newIntent(TrackStartEndActivity.this, TrackDetailActivity.class)
                        .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, trackId);
                startActivity(intent);
                Toast.makeText(TrackStartEndActivity.this, R.string.track_list_record_success, Toast.LENGTH_SHORT)
                        .show();
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        command = getIntent().getIntExtra(EXTRA_COMMAND, COMMAND_NOTHING);
        if (command == COMMAND_NOTHING) {
            finish();
        }

        leftButton = findViewById(R.id.start_end_left_button);
        rightButton = findViewById(R.id.start_end_right_button);

        leftText = findViewById(R.id.start_end_left_text);
        rightText = findViewById(R.id.start_end_right_text);

        trackSumUpContainer = findViewById(R.id.start_end_track_sum_up);
        textArrowContainer = findViewById(R.id.start_end_text_arrow);

        distanceValue = findViewById(R.id.start_end_distance_value);
        distanceUnit = findViewById(R.id.start_end_distance_unit);

        movingTimeValue = findViewById(R.id.start_end_moving_time_value);

        if (command == COMMAND_START)  {
            updateStartUi();

        } else {
            updateResumeFinishUi();
        }

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, bindChangedCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();
        trackRecordingServiceConnection.startConnection(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        trackRecordingServiceConnection.unbind(this);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.track_start_end;
    }

    private void startRecording() {
        trackRecordingServiceConnection.startAndBind(this);

        /*
         * If the binding has happened, then invoke the callback to start a new recording.
         * If the binding hasn't happened, then invoking the callback will have no effect.
         * But when the binding occurs, the callback will get invoked.
         */
        bindChangedCallback.run();
    }

    private void updateStartUi() {
        setTitle(getString(R.string.generic_start));

        trackSumUpContainer.setVisibility(View.GONE);
        textArrowContainer.setVisibility(View.VISIBLE);

        leftText.setText(getString(R.string.generic_start));
        leftButton.setVisibility(View.VISIBLE);
        leftText.setVisibility(View.VISIBLE);

        rightButton.setVisibility(View.INVISIBLE);
        rightText.setVisibility(View.INVISIBLE);

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leftButton.setClickable(false);
                startRecording();
            }
        });
    }

    private void updateResumeFinishUi() {
        long recordingTrackId = PreferencesUtils.getRecordingTrackId(TrackStartEndActivity.this);
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(this);
        Track track = contentProviderUtils.getTrack(recordingTrackId);
        TripStatistics tripStatistics = track != null ? track.getTripStatistics() : null;

        // Set title
        setTitle(track != null ? track.getName() : getString(R.string.generic_finish));

        // Set time
        if (tripStatistics != null) {
            movingTimeValue.setText(StringUtils.formatElapsedTime(tripStatistics.getMovingTime()));
        }

        // Set total distance
        {
            boolean metricUnits = PreferencesUtils.isMetricUnits(this);
            double totalDistance = tripStatistics == null ? Double.NaN : tripStatistics.getTotalDistance();
            Pair<String, String> parts = StringUtils.getDistanceParts(this, totalDistance, metricUnits);

            distanceValue.setText(parts.first);
            distanceUnit.setText(parts.second);
        }

        trackSumUpContainer.setVisibility(View.VISIBLE);
        textArrowContainer.setVisibility(View.INVISIBLE);

        leftText.setText(getString(R.string.generic_resume));
        leftButton.setVisibility(View.VISIBLE);
        leftText.setVisibility(View.VISIBLE);

        rightText.setText(getString(R.string.generic_finish));
        rightButton.setVisibility(View.VISIBLE);
        rightText.setVisibility(View.VISIBLE);

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leftButton.setClickable(false);
                trackRecordingServiceConnection.resumeTrack();
                finish();
            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rightButton.setClickable(false);
                trackRecordingServiceConnection.stopRecording(TrackStartEndActivity.this, true);
                finish();
            }
        });
    }

    @Override
    protected TrackRecordingServiceConnection getTrackRecordingServiceConnection() {
        return trackRecordingServiceConnection;
    }

    @Override
    protected void onDeleted() {

    }
}