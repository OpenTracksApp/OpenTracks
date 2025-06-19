package de.dennisguse.opentracks.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.ActivitySettingsCustomLayoutListBinding;
import de.dennisguse.opentracks.ui.customRecordingLayout.RecordingLayout;
import de.dennisguse.opentracks.ui.customRecordingLayout.SettingsCustomLayoutListAdapter;
import de.dennisguse.opentracks.ui.util.RecyclerViewSwipeDeleteCallback;

public class SettingsCustomLayoutListActivity extends AbstractActivity implements SettingsCustomLayoutListAdapter.SettingsCustomLayoutProfileClickListener {

    private ActivitySettingsCustomLayoutListBinding viewBinding;
    private SettingsCustomLayoutListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new SettingsCustomLayoutListAdapter(this, this);
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.recyclerView.setAdapter(adapter);

        viewBinding.customLayoutListCancelButton.setOnClickListener(view -> clearAndHideEditLayout());

        viewBinding.customLayoutListOkButton.setEnabled(false);
        viewBinding.customLayoutListOkButton.setOnClickListener(view -> {
            PreferencesUtils.addCustomLayout(viewBinding.customLayoutListEditName.getText().toString());
            clearAndHideEditLayout();
            adapter.reloadLayouts();
        });

        viewBinding.customLayoutListEditName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s == null || s.toString().isEmpty()) {
                    viewBinding.customLayoutListOkButton.setEnabled(false);
                    return;
                }

                if (adapter.getLayouts().stream().anyMatch(layout -> layout.sameName(s.toString()))) {
                    viewBinding.customLayoutListOkButton.setEnabled(false);
                    viewBinding.customLayoutListInputLayout.setError(getString(R.string.custom_layout_list_edit_already_exists));
                } else {
                    viewBinding.customLayoutListOkButton.setEnabled(true);
                    viewBinding.customLayoutListInputLayout.setError("");
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        RecyclerViewSwipeDeleteCallback recyclerViewSwipeDeleteCallback = new RecyclerViewSwipeDeleteCallback(this) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // When there's only one profile it cannot be deleted (so, "disable" movements: drag flags and swipe flags).
                return adapter.getItemCount() > 1 ? makeMovementFlags(0, ItemTouchHelper.LEFT) : makeMovementFlags(0, 0);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                final int position = viewHolder.getAdapterPosition();
                final RecordingLayout item = adapter.getLayouts().get(position);

                adapter.removeLayout(position);

                Snackbar snackbar = Snackbar.make(viewBinding.recyclerView, getString(R.string.custom_layout_list_layout_removed), Snackbar.LENGTH_LONG);
                snackbar.setAction(getString(R.string.generic_undo).toUpperCase(), view -> {
                    adapter.restoreItem(item, position);
                    viewBinding.recyclerView.scrollToPosition(position);
                });

                snackbar.show();
            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(recyclerViewSwipeDeleteCallback);
        itemTouchhelper.attachToRecyclerView(viewBinding.recyclerView);

        setSupportActionBar(viewBinding.bottomAppBarLayout.bottomAppBar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.reloadLayouts();
    }

    @NonNull
    @Override
    protected View createRootView() {
        PreferencesUtils.getCustomLayout();
        viewBinding = ActivitySettingsCustomLayoutListBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.custom_layout_edit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.custom_layout_edit_add_profile) {
            viewBinding.customLayoutListAddLinearLayout.setVisibility(View.VISIBLE);
            viewBinding.customLayoutListEditName.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(viewBinding.customLayoutListEditName, InputMethodManager.SHOW_IMPLICIT);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSettingsCustomLayoutProfileClicked(@NonNull RecordingLayout recordingLayout) {
        Intent intent = new Intent(this, SettingsCustomLayoutEditActivity.class);
        intent.putExtra(SettingsCustomLayoutEditActivity.EXTRA_LAYOUT, recordingLayout);
        startActivity(intent);
    }

    private void clearAndHideEditLayout() {
        viewBinding.customLayoutListEditName.setText("");
        viewBinding.customLayoutListInputLayout.setError("");
        viewBinding.customLayoutListAddLinearLayout.setVisibility(View.GONE);
    }
}
