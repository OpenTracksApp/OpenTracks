package de.dennisguse.opentracks.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;
import de.dennisguse.opentracks.viewmodels.IntervalStatisticsModel;
import de.dennisguse.opentracks.views.IntervalListView;

/**
 * A fragment to display the intervals from recording track.
 */
public class IntervalsRecordingFragment extends Fragment implements IntervalListView.IntervalListListener {

    private static final String TAG = IntervalsRecordingFragment.class.getSimpleName();

    // Refreshing intervals stats it's not so demanding so 5 seconds is enough to balance performance and user experience.
    private static final long UI_UPDATE_INTERVAL = 5 * UnitConversions.ONE_SECOND_MS;

    private IntervalStatisticsModel viewModel;
    private IntervalListView.IntervalReverseListView intervalListView;

    public static Fragment newInstance() {
        return new IntervalsRecordingFragment();
    }

    private final Runnable intervalRunner = new Runnable() {
        @Override
        public void run() {
            if (isResumed()) {
                updateIntervals();
                intervalHandler.postDelayed(intervalRunner, UI_UPDATE_INTERVAL);
            }
        }
    };

    private Handler intervalHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intervals_recording, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        intervalHandler = new Handler();

        intervalListView = new IntervalListView.IntervalReverseListView(getActivity(), this);
        intervalListView.setId(View.generateViewId());
        intervalListView.findViewById(R.id.interval_title_label).setVisibility(View.GONE);
        LinearLayout linearLayout = view.findViewById(R.id.root_view);
        linearLayout.removeAllViews();
        linearLayout.addView(intervalListView);

        viewModel = new ViewModelProvider(this).get(IntervalStatisticsModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        intervalHandler.post(intervalRunner);
    }

    @Override
    public void onPause() {
        super.onPause();
        intervalHandler.removeCallbacks(intervalRunner);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        intervalListView.destroy();
        intervalListView = null;
        viewModel = null;
    }

    /**
     * Update intervals through {@link IntervalStatisticsModel} view model.
     *
     * @param interval intervals will split in this interval if not null. If it's null then view model will use the default one.
     */
    private void updateIntervals(@Nullable IntervalStatisticsModel.IntervalOption interval) {
        if (viewModel == null || intervalListView == null) {
            return;
        }

        Track.Id trackId = PreferencesUtils.getRecordingTrackId(getContext());
        viewModel.invalidate();
        LiveData<IntervalStatistics> liveData;
        if (interval == null) {
            liveData = viewModel.getIntervalStats(trackId);
        } else {
            liveData = viewModel.getIntervalStats(trackId, interval);
        }
        liveData.observe(getActivity(), intervalStatistics -> {
            if (intervalStatistics != null) {
                intervalListView.display(intervalStatistics.getIntervalList());
            }
        });
    }

    private void updateIntervals() {
        updateIntervals(null);
    }

    @Override
    public void intervalChanged(IntervalStatisticsModel.IntervalOption interval) {
        updateIntervals(interval);
    }
}