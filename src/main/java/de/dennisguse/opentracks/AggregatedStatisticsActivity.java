package de.dennisguse.opentracks;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import de.dennisguse.opentracks.adapters.AggregatedStatisticsAdapter;
import de.dennisguse.opentracks.databinding.AggregatedStatsBinding;
import de.dennisguse.opentracks.viewmodels.AggregatedStatisticsModel;

public class AggregatedStatisticsActivity extends AbstractActivity {

    private AggregatedStatsBinding viewBinding;

    private AggregatedStatisticsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewBinding.aggregatedStatsList.setEmptyView(viewBinding.aggregatedStatsEmptyView);

        final AggregatedStatisticsModel viewModel = new ViewModelProvider(this).get(AggregatedStatisticsModel.class);
        viewModel.getAggregatedStats().observe(this, aggregatedStatistics -> {
            if (aggregatedStatistics != null) {
                adapter = new AggregatedStatisticsAdapter(this, aggregatedStatistics);
                viewBinding.aggregatedStatsList.setAdapter(adapter);
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected View getRootView() {
        viewBinding = AggregatedStatsBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }
}
