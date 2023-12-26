package de.dennisguse.opentracks.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.databinding.StatisticsRecordingBinding;
import de.dennisguse.opentracks.services.RecordingData;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.ui.customRecordingLayout.DataField;
import de.dennisguse.opentracks.ui.customRecordingLayout.RecordingLayout;
import de.dennisguse.opentracks.viewmodels.Mapping;
import de.dennisguse.opentracks.viewmodels.StatisticViewHolder;

/**
 * A fragment to display track statistics to the user for a currently recording {@link Track}.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatisticsRecordingFragment extends Fragment {

    private static final String TAG = StatisticsRecordingFragment.class.getSimpleName();

    public static Fragment newInstance() {
        return new StatisticsRecordingFragment();
    }

    private TrackRecordingServiceConnection trackRecordingServiceConnection;

    private RecordingData recordingData = TrackRecordingService.NOT_RECORDING;

    private final List<StatisticViewHolder<?>> viewHolders = new LinkedList<>();

    private RecordingLayout recordingLayout;

    private StatisticsRecordingBinding viewBinding;

    private UnitSystem unitSystem = UnitSystem.defaultUnitSystem();

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.stats_units_key, key)) {
            unitSystem = PreferencesUtils.getUnitSystem();
            updateDataOnUI();
        }

        if (PreferencesUtils.isKey(R.string.stats_custom_layouts_key, key) || PreferencesUtils.isKey(R.string.stats_custom_layout_selected_layout_key, key)) {
            onLayoutChanged(PreferencesUtils.getCustomLayout());
        }
    };

    private final TrackRecordingServiceConnection.Callback bindChangedCallback = (service, unused) -> service.getRecordingDataObservable()
            .observe(StatisticsRecordingFragment.this, this::onRecordingDataChanged);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindChangedCallback);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = StatisticsRecordingBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        trackRecordingServiceConnection.bind(getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        trackRecordingServiceConnection.unbind(getContext());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewHolders.clear();
        viewBinding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        trackRecordingServiceConnection = null;
    }

    private void onLayoutChanged(@NonNull RecordingLayout newRecordingLayout) {
        if (newRecordingLayout.equals(recordingLayout)) {
            return;
        }
        recordingLayout = newRecordingLayout;

        viewBinding.statsLayout.removeAllViews(); //Let's start from scratch
        viewBinding.statsLayout.setColumnCount(recordingLayout.getColumnsPerRow());
        viewHolders.clear();

        Map<String, Callable<StatisticViewHolder<?>>> m = Mapping.create(getContext());

        int rowIndex = 0;
        int columnIndex = 0;
        for (DataField dataField : recordingLayout.toRecordingLayout(true).getFields()) {
            GridLayout.LayoutParams param = new GridLayout.LayoutParams();
            param.setGravity(Gravity.FILL_HORIZONTAL);
            param.width = 0;

            if (dataField.isWide()) {
                rowIndex++;
                param.columnSpec = GridLayout.spec(0, recordingLayout.getColumnsPerRow(), 1);
                param.rowSpec = GridLayout.spec(rowIndex, 1, 1);
                columnIndex = 0;
                rowIndex++;
            } else {
                if (columnIndex >= recordingLayout.getColumnsPerRow()) {
                    columnIndex = 0;
                    rowIndex++;
                }
                param.columnSpec = GridLayout.spec(columnIndex, 1, 1);
                param.rowSpec = GridLayout.spec(rowIndex, 1, 1);

                columnIndex++;
            }

            try {
                StatisticViewHolder<?> viewHolder = m.get(dataField.getKey()).call();
                viewHolder.initialize(getContext(), getLayoutInflater());
                viewHolder.configureUI(dataField);
                viewHolders.add(viewHolder);

                viewBinding.statsLayout.addView(viewHolder.getView(), param);
            } catch (Exception e) {
                throw new RuntimeException("Could not add " + dataField.getKey(), e);
            }
        }
    }

    private void onRecordingDataChanged(RecordingData recordingData) {
        String oldCategory = this.recordingData.getTrackCategory();
        String newCategory = recordingData.getTrackCategory();
        this.recordingData = recordingData;

        if (!oldCategory.equals(newCategory)) {
            sharedPreferenceChangeListener.onSharedPreferenceChanged(null, getString(R.string.stats_rate_key));
        }

        updateDataOnUI();
    }

    private void updateDataOnUI() {
        if (isResumed()) {
            viewHolders.forEach(i -> i.onChanged(unitSystem, recordingData));
        }
    }
}
