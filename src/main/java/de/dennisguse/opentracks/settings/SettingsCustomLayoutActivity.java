package de.dennisguse.opentracks.settings;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.adapters.SettingsCustomLayoutAdapter;
import de.dennisguse.opentracks.content.data.Layout;
import de.dennisguse.opentracks.databinding.ActivitySettingsCustomLayoutBinding;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StatsUtils;

public class SettingsCustomLayoutActivity extends AbstractActivity implements SettingsCustomLayoutAdapter.SettingsCustomLayoutItemClickListener {

    private ActivitySettingsCustomLayoutBinding viewBinding;
    private GridLayoutManager gridLayoutManager;
    private SettingsCustomLayoutAdapter adapterFieldsVisible;
    private SettingsCustomLayoutAdapter adapterFieldsHidden;
    private Layout layoutFieldsVisible;
    private Layout layoutFieldsHidden;
    private SharedPreferences sharedPreferences;
    private int numColumns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferencesUtils.getSharedPreferences(this);

        // Recycler view with visible stats.
        layoutFieldsVisible = StatsUtils.filterVisible(PreferencesUtils.getCustomLayout(sharedPreferences, this), true);
        adapterFieldsVisible = new SettingsCustomLayoutAdapter(this, this, layoutFieldsVisible);

        numColumns = PreferencesUtils.getLayoutColumns(sharedPreferences, this);
        RecyclerView recyclerViewVisible = viewBinding.recyclerViewVisible;
        gridLayoutManager = new GridLayoutManager(this, numColumns);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapterFieldsVisible.getItemViewType(position) == SettingsCustomLayoutAdapter.VIEW_TYPE_LONG) {
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
        ArrayAdapter<Integer> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new Integer[]{1, 2, 3});
        viewBinding.spinnerOptions.setAdapter(spinnerAdapter);
        viewBinding.spinnerOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                numColumns = position + 1;
                gridLayoutManager.setSpanCount(numColumns);
                PreferencesUtils.setLayoutColumns(sharedPreferences, SettingsCustomLayoutActivity.this, position + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        viewBinding.spinnerOptions.setSelection(PreferencesUtils.getLayoutColumns(sharedPreferences, SettingsCustomLayoutActivity.this) - 1);

        // Recycler view with not visible stats.
        layoutFieldsHidden = StatsUtils.filterVisible(PreferencesUtils.getCustomLayout(sharedPreferences, this), false);
        adapterFieldsHidden = new SettingsCustomLayoutAdapter(this, this, layoutFieldsHidden);
        RecyclerView recyclerViewNotVisible = viewBinding.recyclerViewNotVisible;
        recyclerViewNotVisible.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotVisible.setAdapter(adapterFieldsHidden);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!layoutFieldsVisible.getFields().isEmpty() || !layoutFieldsHidden.getFields().isEmpty()) {
            Layout newLayout = new Layout(layoutFieldsVisible.getProfile());
            newLayout.getFields().addAll(layoutFieldsVisible.getFields());
            newLayout.getFields().addAll(layoutFieldsHidden.getFields());
            PreferencesUtils.setCustomLayout(sharedPreferences, this, newLayout);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences = null;
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
    public void onSettingsCustomLayoutItemClicked(@NonNull Layout.Field field) {
        if (field.isVisible()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.generic_choose_an_option)
                    .setItems(new String[]{getString(field.isPrimary() ? R.string.field_set_secondary : R.string.field_set_primary), getString(R.string.field_remove_from_layout)}, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                field.togglePrimary();
                            } else {
                                layoutFieldsVisible.removeField(field);
                                field.toggleVisibility();
                                layoutFieldsHidden.addField(field);
                            }

                            adapterFieldsVisible.swapValues(layoutFieldsVisible);
                            adapterFieldsHidden.swapValues(layoutFieldsHidden);
                        }
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
