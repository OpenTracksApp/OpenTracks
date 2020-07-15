package de.dennisguse.opentracks;

import android.os.Bundle;
import android.widget.ListView;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import de.dennisguse.opentracks.adapters.AggregatedStatisticsAdapter;
import de.dennisguse.opentracks.viewmodels.AggregatedStatistics;
import de.dennisguse.opentracks.viewmodels.AggregatedStatisticsModel;

public class AggregatedStatisticsActivity extends AbstractActivity {

    private ListView listView;
    private AggregatedStatisticsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listView = findViewById(R.id.aggregated_stats_list);
        listView.setEmptyView(findViewById(R.id.aggregated_stats_empty_view));

        final AggregatedStatisticsModel viewModel = new ViewModelProvider(this).get(AggregatedStatisticsModel.class);
        viewModel.getAggregatedStats().observe(this, aggregatedStatistics -> {
            if (aggregatedStatistics != null) {
                adapter = new AggregatedStatisticsAdapter(getApplicationContext(), aggregatedStatistics);
                listView.setAdapter(adapter);
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.aggregated_stats;
    }
}
