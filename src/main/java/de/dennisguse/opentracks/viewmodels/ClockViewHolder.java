package de.dennisguse.opentracks.viewmodels;

import android.view.LayoutInflater;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.StatsClockItemBinding;
import de.dennisguse.opentracks.services.RecordingData;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.ui.customRecordingLayout.DataField;

public class ClockViewHolder extends StatisticViewHolder<StatsClockItemBinding> {

    @Override
    protected StatsClockItemBinding createViewBinding(LayoutInflater inflater) {
        return StatsClockItemBinding.inflate(inflater);
    }

    @Override
    public void configureUI(DataField dataField) {
        //TODO Unify with GenericStatisticsViewHolder?
        getBinding().statsClock.setTextAppearance(getContext(), dataField.isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryValue : R.style.TextAppearance_OpenTracks_SecondaryValue);
    }

    @Override
    public int getTitleId() {
        return R.string.stats_clock;
    }

    @Override
    public void onChanged(UnitSystem unitSystem, RecordingData data) {
    }
}
