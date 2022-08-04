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
import de.dennisguse.opentracks.ui.aggregatedStatistics.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.util.ExportUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackUtils;

public class TrackStoppedActivity extends AbstractTrackDeleteActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller {

    private static final String TAG = TrackStoppedActivity.class.getSimpleName();

    public static final String EXTRA_TRACK_ID = "track_id";

    private TrackStoppedBinding viewBinding;

    private Track.Id trackId;

    private boolean isDiscarding = false;

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

        viewBinding.trackEditName.setText(track.getName());

        viewBinding.trackEditActivityType.setText(track.getCategory());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.activity_types, android.R.layout.simple_dropdown_item_1line);
        viewBinding.trackEditActivityType.setAdapter(adapter);
        viewBinding.trackEditActivityType.setOnItemClickListener((parent, view, position, id) -> setActivityTypeIcon(TrackIconUtils.getIconValue(this, (String) viewBinding.trackEditActivityType.getAdapter().getItem(position))));
        viewBinding.trackEditActivityType.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                setActivityTypeIcon(TrackIconUtils.getIconValue(
                        TrackStoppedActivity.this, viewBinding.trackEditActivityType.getText().toString()));
            }
        });

        String iconValue = track.getIcon();

        setActivityTypeIcon(iconValue);
        viewBinding.trackEditActivityTypeIcon.setOnClickListener(v -> ChooseActivityTypeDialogFragment.showDialog(getSupportFragmentManager(), viewBinding.trackEditActivityType.getText().toString()));

        viewBinding.trackEditDescription.setText(track.getDescription());

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
            storeTrackMetaData(contentProviderUtils, track);
            ExportUtils.postWorkoutExport(this, trackId);
            finish();
        });

        viewBinding.resumeButton.setOnClickListener(v -> {
            storeTrackMetaData(contentProviderUtils, track);
            resumeTrackAndFinish();
        });

        viewBinding.discardButton.setOnClickListener(v -> ConfirmDeleteDialogFragment.showDialog(getSupportFragmentManager(), trackId));
    }

    private void storeTrackMetaData(ContentProviderUtils contentProviderUtils, Track track) {
        TrackUtils.updateTrack(TrackStoppedActivity.this, track, viewBinding.trackEditName.getText().toString(),
                viewBinding.trackEditActivityType.getText().toString(), viewBinding.trackEditDescription.getText().toString(),
                contentProviderUtils);
    }

    @Override
    public void onBackPressed() {
        if (isDiscarding) {
            return;
        }
        super.onBackPressed();
        resumeTrackAndFinish();
    }

    @Override
    protected View getRootView() {
        viewBinding = TrackStoppedBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    private void setActivityTypeIcon(String iconValue) {
        viewBinding.trackEditActivityTypeIcon.setImageResource(TrackIconUtils.getIconDrawable(iconValue));
    }

    @Override
    public void onChooseActivityTypeDone(String iconValue) {
        setActivityTypeIcon(iconValue);
        viewBinding.trackEditActivityType.setText(getString(TrackIconUtils.getIconActivityType(iconValue)));
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
        }).startAndBind(this, true);
    }

    @Override
    protected void onDeleteConfirmed() {
        isDiscarding = true;
        viewBinding.loadingLayout.loadingText.setText(getString(R.string.track_discarding));
        viewBinding.contentLinearLayout.setVisibility(View.GONE);
        viewBinding.loadingLayout.loadingIndeterminate.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDeleteFinished() {
        finish();
    }

    @Override
    protected Track.Id getRecordingTrackId() {
        return null;
    }
}
