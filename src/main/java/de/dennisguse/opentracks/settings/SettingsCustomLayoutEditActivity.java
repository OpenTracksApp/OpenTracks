package de.dennisguse.opentracks.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.stream.IntStream;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.ActivitySettingsCustomLayoutBinding;
import de.dennisguse.opentracks.ui.customRecordingLayout.DataField;
import de.dennisguse.opentracks.ui.customRecordingLayout.RecordingLayout;
import de.dennisguse.opentracks.ui.customRecordingLayout.SettingsCustomLayoutEditAdapter;
import de.dennisguse.opentracks.ui.util.ArrayAdapterFilterDisabled;

public class SettingsCustomLayoutEditActivity extends AbstractActivity implements SettingsCustomLayoutEditAdapter.SettingsCustomLayoutItemClickListener {

    public static final String EXTRA_LAYOUT = "extraLayout";
    private ActivitySettingsCustomLayoutBinding viewBinding;
    private GridLayoutManager gridLayoutManager;
    private SettingsCustomLayoutEditAdapter adapterFieldsVisible;
    private SettingsCustomLayoutEditAdapter adapterFieldsHidden;
    private String profile;
    private RecordingLayout recordingLayoutFieldsVisible;
    private RecordingLayout recordingLayoutFieldsHidden;
    private int numColumns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Recycler view with visible stats.
        RecordingLayout recordingLayout = getIntent().getParcelableExtra(EXTRA_LAYOUT);
        profile = recordingLayout.getName();
        recordingLayoutFieldsVisible = recordingLayout.toRecordingLayout(true);
        adapterFieldsVisible = new SettingsCustomLayoutEditAdapter(this, this, recordingLayoutFieldsVisible);

        numColumns = recordingLayout.getColumnsPerRow();
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
                recordingLayoutFieldsVisible = adapterFieldsVisible.move(fromPosition, toPosition);
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
        recordingLayoutFieldsHidden = recordingLayout.toRecordingLayout(false);
        adapterFieldsHidden = new SettingsCustomLayoutEditAdapter(this, this, recordingLayoutFieldsHidden);
        RecyclerView recyclerViewNotVisible = viewBinding.recyclerViewNotVisible;
        recyclerViewNotVisible.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotVisible.setAdapter(adapterFieldsHidden);

//        viewBinding.bottomAppBarLayout.bottomAppBarTitle.setText(profile); TODO
        setSupportActionBar(viewBinding.bottomAppBarLayout.bottomAppBar);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!recordingLayoutFieldsVisible.getFields().isEmpty() || !recordingLayoutFieldsHidden.getFields().isEmpty()) {
            RecordingLayout recordingLayout = new RecordingLayout(profile, numColumns);
            recordingLayout.addFields(recordingLayoutFieldsVisible.getFields());
            recordingLayout.addFields(recordingLayoutFieldsHidden.getFields());
            PreferencesUtils.updateCustomLayout(recordingLayout);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recordingLayoutFieldsVisible = null;
    }

    @NonNull
    @Override
    protected View createRootView() {
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
                            recordingLayoutFieldsVisible.removeField(field);
                            field.toggleVisibility();
                            recordingLayoutFieldsHidden.addField(field);
                        }

                        adapterFieldsVisible.swapValues(recordingLayoutFieldsVisible);
                        adapterFieldsHidden.swapValues(recordingLayoutFieldsHidden);
                    })
                    .create()
                    .show();
        } else {
            recordingLayoutFieldsHidden.removeField(field);
            field.toggleVisibility();
            recordingLayoutFieldsVisible.addField(field);
            viewBinding.scrollView.fullScroll(ScrollView.FOCUS_UP);

            adapterFieldsVisible.swapValues(recordingLayoutFieldsVisible);
            adapterFieldsHidden.swapValues(recordingLayoutFieldsHidden);
        }
    }
}
