package de.dennisguse.opentracks;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.SpeedFormatter;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.databinding.TrackStoppedBinding;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.ExportUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackUtils;

public class TrackStoppedActivity extends AbstractActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller {

    private static final String TAG = TrackStoppedActivity.class.getSimpleName();

    public static final String EXTRA_TRACK_ID = "track_id";

    private TrackStoppedBinding viewBinding;

    private Track.Id trackId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trackId = getIntent().getParcelableExtra(EXTRA_TRACK_ID);
        if (trackId == null) {
            Log.e(TAG, "TrackStoppedActivity needs EXTRA_TRACK_ID.");
            finish();
        }

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(this);
        Track track = contentProviderUtils.getTrack(trackId);

        viewBinding.fields.trackEditName.setText(track.getName());

        viewBinding.fields.trackEditActivityType.setText(track.getCategory());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.activity_types, android.R.layout.simple_dropdown_item_1line);
        viewBinding.fields.trackEditActivityType.setAdapter(adapter);
        viewBinding.fields.trackEditActivityType.setOnItemClickListener((parent, view, position, id) -> setActivityTypeIcon(TrackIconUtils.getIconValue(this, (String) viewBinding.fields.trackEditActivityType.getAdapter().getItem(position))));
        viewBinding.fields.trackEditActivityType.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                setActivityTypeIcon(TrackIconUtils.getIconValue(
                        TrackStoppedActivity.this, viewBinding.fields.trackEditActivityType.getText().toString()));
            }
        });

        String iconValue = track.getIcon();

        setActivityTypeIcon(iconValue);
        viewBinding.fields.trackEditActivityTypeIcon.setOnClickListener(v -> ChooseActivityTypeDialogFragment.showDialog(getSupportFragmentManager(), viewBinding.fields.trackEditActivityType.getText().toString()));

        viewBinding.fields.trackEditDescription.setText(track.getDescription());

        viewBinding.time.setText(StringUtils.formatElapsedTime(track.getTrackStatistics().getMovingTime()));

        {
            Pair<String, String> parts = SpeedFormatter.Builder()
                    .setUnit(PreferencesUtils.getUnitSystem())
                    .setReportSpeedOrPace(PreferencesUtils.isReportSpeed(track.getCategory()))
                    .build(this)
                    .getSpeedParts(track.getTrackStatistics().getAverageMovingSpeed());
            viewBinding.speed.setText(parts.first);
            viewBinding.speedUnit.setText(parts.second);
        }

        {
            Pair<String, String> parts = DistanceFormatter.Builder()
                    .setUnit(PreferencesUtils.getUnitSystem())
                    .build(this)
                    .getDistanceParts(track.getTrackStatistics().getTotalDistance());
            viewBinding.distance.setText(parts.first);
            viewBinding.distanceUnit.setText(parts.second);
        }

        viewBinding.finishButton.setOnClickListener(v -> {
            TrackUtils.updateTrack(TrackStoppedActivity.this, track, viewBinding.fields.trackEditName.getText().toString(),
                    viewBinding.fields.trackEditActivityType.getText().toString(), viewBinding.fields.trackEditDescription.getText().toString(),
                    contentProviderUtils);
            ExportUtils.postWorkoutExport(this, trackId);
            finish();
        });

        viewBinding.resumeButton.setOnClickListener(v -> {
            resumeTrackAndFinish();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        resumeTrackAndFinish();
    }

    @Override
    protected View getRootView() {
        viewBinding = TrackStoppedBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    private void setActivityTypeIcon(String iconValue) {
        viewBinding.fields.trackEditActivityTypeIcon.setImageResource(TrackIconUtils.getIconDrawable(iconValue));
    }

    @Override
    public void onChooseActivityTypeDone(String iconValue) {
        setActivityTypeIcon(iconValue);
        viewBinding.fields.trackEditActivityType.setText(getString(TrackIconUtils.getIconActivityType(iconValue)));
    }

    private void resumeTrackAndFinish() {
        new TrackRecordingServiceConnection((service, connection) -> {
            service.resumeTrack(trackId);

            Intent newIntent = IntentUtils.newIntent(TrackStoppedActivity.this, TrackRecordingActivity.class)
                    .putExtra(TrackRecordingActivity.EXTRA_TRACK_ID, trackId);
            startActivity(newIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            connection.unbind(this);
            finish();
        }).startAndBind(this);
    }
}