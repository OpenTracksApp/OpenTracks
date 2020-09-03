package de.dennisguse.opentracks.fragments;

import android.os.Bundle;
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
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;
import de.dennisguse.opentracks.viewmodels.IntervalStatisticsModel;
import de.dennisguse.opentracks.views.IntervalListView;

/**
 * A fragment to display the intervals from recorded track.
 */
public class IntervalsRecordedFragment extends Fragment implements IntervalListView.IntervalListListener {

    private static final String TAG = IntervalsRecordedFragment.class.getSimpleName();

    private static final String TRACK_ID_KEY = "trackId";

    private IntervalStatisticsModel viewModel;
    private IntervalListView intervalListView;

    private Track.Id trackId;

    public static Fragment newInstance(Track.Id trackId) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(TRACK_ID_KEY, trackId);

        Fragment fragment = new IntervalsRecordedFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.intervals_recording, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        trackId = getArguments().getParcelable(TRACK_ID_KEY);

        intervalListView = new IntervalListView(getActivity(), this);
        intervalListView.setId(View.generateViewId());
        LinearLayout linearLayout = view.findViewById(R.id.root_view);
        linearLayout.removeAllViews();
        linearLayout.addView(intervalListView);

        viewModel = new ViewModelProvider(this).get(IntervalStatisticsModel.class);

        intervalChanged(null);
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
    @Override
    public void intervalChanged(@Nullable IntervalStatisticsModel.IntervalOption interval) {
        if (viewModel == null || intervalListView == null) {
            return;
        }

        LiveData<IntervalStatistics> liveData = viewModel.getIntervalStats(trackId, interval);
        liveData.observe(getActivity(), intervalStatistics -> {
            if (intervalStatistics != null) {
                intervalListView.display(intervalStatistics.getIntervalList());
            }
        });
    }

    @Override
    public void unitChanged() {
        if (viewModel != null) {
            LiveData<IntervalStatistics> liveData = viewModel.getIntervalStats(trackId, null);
            liveData.observe(getActivity(), intervalStatistics -> {
            if (intervalStatistics != null) {
                intervalListView.display(intervalStatistics.getIntervalList());
            }
        });
        }
    }
}