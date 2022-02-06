package de.dennisguse.opentracks.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.stream.IntStream;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.ui.util.ArrayAdapterFilterDisabled;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.ActivitySettingsCustomLayoutBinding;
import de.dennisguse.opentracks.ui.customRecordingLayout.DataField;
import de.dennisguse.opentracks.ui.customRecordingLayout.Layout;
import de.dennisguse.opentracks.ui.customRecordingLayout.SettingsCustomLayoutEditAdapter;
import de.dennisguse.opentracks.util.StatisticsUtils;

public class SettingsCustomLayoutEditActivity extends AbstractActivity implements SettingsCustomLayoutEditAdapter.SettingsCustomLayoutItemClickListener {

    public static final String EXTRA_LAYOUT = "extraLayout";
    private ActivitySettingsCustomLayoutBinding viewBinding;
    private GridLayoutManager gridLayoutManager;
    private SettingsCustomLayoutEditAdapter adapterFieldsVisible;
    private SettingsCustomLayoutEditAdapter adapterFieldsHidden;
    private String profile;
    private Layout layoutFieldsVisible;
    private Layout layoutFieldsHidden;
    private int numColumns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Recycler view with visible stats.
        Layout layout = getIntent().getParcelableExtra(EXTRA_LAYOUT);
        profile = layout.getName();
        layoutFieldsVisible = StatisticsUtils.filterVisible(layout, true);
        adapterFieldsVisible = new SettingsCustomLayoutEditAdapter(this, this, layoutFieldsVisible);

        numColumns = layout.getColumnsPerRow();
        RecyclerView recyclerViewVisible = viewBinding.recyclerViewVisible;
        gridLayoutManager = new GridLayoutManager(this, numColumns);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapterFieldsVisible.isItemWide(position)) {
                    return numColumns;
                }
                return 1;
            }
        });
        recyclerViewVisible.setLayoutManager(gridLayoutManager);
        recyclerViewVisible.setAdapter(adapterFieldsVisible);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                layoutFieldsVisible = adapterFieldsVisible.move(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewVisible);

        // Spinner with items per row.
        ArrayAdapterFilterDisabled<Integer> rowsOptionAdapter = new ArrayAdapterFilterDisabled<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                IntStream.of(getResources().getIntArray(R.array.stats_custom_layout_fields_columns_per_row)).boxed().toArray(Integer[]::new));
        viewBinding.rowsOptions.setAdapter(rowsOptionAdapter);
        viewBinding.rowsOptions.setOnItemClickListener((parent, view, position, id) -> {
            numColumns = position + 1;
            gridLayoutManager.setSpanCount(numColumns);
        });

        viewBinding.rowsOptions.setText(rowsOptionAdapter.getItem(numColumns - 1).toString(), false);

        // Recycler view with not visible stats.
        layoutFieldsHidden = StatisticsUtils.filterVisible(layout, false);
        adapterFieldsHidden = new SettingsCustomLayoutEditAdapter(this, this, layoutFieldsHidden);
        RecyclerView recyclerViewNotVisible = viewBinding.recyclerViewNotVisible;
        recyclerViewNotVisible.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotVisible.setAdapter(adapterFieldsHidden);

        setTitle(profile);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!layoutFieldsVisible.getFields().isEmpty() || !layoutFieldsHidden.getFields().isEmpty()) {
            Layout layout = new Layout(profile, numColumns);
            layout.addFields(layoutFieldsVisible.getFields());
            layout.addFields(layoutFieldsHidden.getFields());
            PreferencesUtils.updateCustomLayout(layout);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        layoutFieldsVisible = null;
    }

    @Override
    protected void setupActionBarBack(Toolbar toolbar) {
        super.setupActionBarBack(toolbar);
        toolbar.setTitle(R.string.menu_settings);
    }

    @Override
    protected View getRootView() {
        viewBinding = ActivitySettingsCustomLayoutBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    @Override
    public void onSettingsCustomLayoutItemClicked(@NonNull DataField field) {
        if (field.isVisible()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.generic_choose_an_option)
                    .setItems(new String[]{getString(field.isPrimary() ? R.string.field_set_secondary : R.string.field_set_primary), getString(R.string.field_remove_from_layout)}, (dialog, which) -> {
                        if (which == 0) {
                            field.togglePrimary();
                        } else {
                            layoutFieldsVisible.removeField(field);
                            field.toggleVisibility();
                            layoutFieldsHidden.addField(field);
                        }

                        adapterFieldsVisible.swapValues(layoutFieldsVisible);
                        adapterFieldsHidden.swapValues(layoutFieldsHidden);
                    })
                    .create()
                    .show();
        } else {
            layoutFieldsHidden.removeField(field);
            field.toggleVisibility();
            layoutFieldsVisible.addField(field);
            viewBinding.scrollView.fullScroll(ScrollView.FOCUS_UP);

            adapterFieldsVisible.swapValues(layoutFieldsVisible);
            adapterFieldsHidden.swapValues(layoutFieldsHidden);
        }
    }
}
