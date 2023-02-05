package de.dennisguse.opentracks.ui.aggregatedStatistics;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.TrackSelection;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.databinding.AggregatedStatsBinding;

public class AggregatedStatisticsActivity extends AbstractActivity implements FilterDialogFragment.FilterDialogListener {

    public static final String EXTRA_TRACK_IDS = "track_ids";

    static final String STATE_ARE_FILTERS_APPLIED = "areFiltersApplied";

    private AggregatedStatsBinding viewBinding;

    private AggregatedStatisticsAdapter adapter;

    private AggregatedStatisticsModel viewModel;
    private final TrackSelection selection = new TrackSelection();

    private boolean areFiltersApplied;
    private MenuItem filterItem;
    private MenuItem clearFilterItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewBinding.aggregatedStatsList.setEmptyView(viewBinding.aggregatedStatsEmptyView);

        areFiltersApplied = savedInstanceState != null && savedInstanceState.getBoolean(STATE_ARE_FILTERS_APPLIED);

        List<Track.Id> trackIds = getIntent().getParcelableArrayListExtra(EXTRA_TRACK_IDS, Track.Id.class);

        if (trackIds != null && !trackIds.isEmpty()) {
            trackIds.stream().forEach(selection::addTrackId);
        }

        viewModel = new ViewModelProvider(this).get(AggregatedStatisticsModel.class);
        viewModel.getAggregatedStats(selection).observe(this, aggregatedStatistics -> {
            if ((aggregatedStatistics == null || aggregatedStatistics.getCount() == 0) && !selection.isEmpty()) {
                viewBinding.aggregatedStatsEmptyView.setText(getString(R.string.aggregated_stats_filter_no_results));
            }
            if (aggregatedStatistics != null) {
                adapter = new AggregatedStatisticsAdapter(this, aggregatedStatistics);
                viewBinding.aggregatedStatsList.setAdapter(adapter);
            }
            adapter.notifyDataSetChanged();
        });

        setSupportActionBar(viewBinding.bottomAppBarLayout.bottomAppBar);
        viewBinding.bottomAppBarLayout.bottomAppBarTitle.setText(getString(R.string.menu_aggregated_statistics));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_ARE_FILTERS_APPLIED, areFiltersApplied);
    }

    @Override
    protected View getRootView() {
        viewBinding = AggregatedStatsBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.aggregated_statistics, menu);
        clearFilterItem = menu.findItem(R.id.aggregated_statistics_clear_filter);
        filterItem = menu.findItem(R.id.aggregated_statistics_filter);
        setMenuVisibility(areFiltersApplied);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.aggregated_statistics_filter) {
            ArrayList<FilterDialogFragment.FilterItem> filterItems = new ArrayList<>();
            adapter.getCategories().stream().forEach(category -> filterItems.add(new FilterDialogFragment.FilterItem(category, category, true)));
            FilterDialogFragment.showDialog(getSupportFragmentManager(), filterItems);
            return true;
        }

        if (item.getItemId() == R.id.aggregated_statistics_clear_filter) {
            setMenuVisibility(false);
            viewModel.clearSelection();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setMenuVisibility(boolean areFiltersApplied) {
        this.areFiltersApplied = areFiltersApplied;
        if (clearFilterItem != null && filterItem != null) {
            clearFilterItem.setVisible(this.areFiltersApplied);
            filterItem.setVisible(!this.areFiltersApplied);
        }
    }

    @Override
    public void onFilterDone(ArrayList<FilterDialogFragment.FilterItem> filterItems, LocalDateTime from, LocalDateTime to) {
        setMenuVisibility(true);
        selection.addDateRange(from.atZone(ZoneId.systemDefault()).toInstant(), to.atZone(ZoneId.systemDefault()).toInstant());
        filterItems.stream().filter(fi -> fi.isChecked).forEach(fi -> selection.addCategory(fi.value));
        viewModel.updateSelection(selection);
    }
}
