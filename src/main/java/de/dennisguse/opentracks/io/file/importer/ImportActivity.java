/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.io.file.importer;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordedActivity;
import de.dennisguse.opentracks.databinding.ImportActivityBinding;
import de.dennisguse.opentracks.io.file.ErrorListDialog;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * An activity to import files from the external storage.
 *
 * @author Rodrigo Damazio
 */
public class ImportActivity extends FragmentActivity {

    private static final String TAG = ImportActivity.class.getSimpleName();

    public static final String EXTRA_DIRECTORY_URI_KEY = "directory_uri";

    private static final String BUNDLE_DOCUMENT_URIS = "document_uris";
    private static final String BUNDLE_IS_DIRECTORY = "is_directory";

    private ImportActivityBinding viewBinding;

    boolean doubleBackToCancel = false;

    private ArrayList<Uri> documentUris = new ArrayList<>();
    private boolean isDirectory;

    private ImportViewModel viewModel;
    private ImportViewModel.Summary summary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ImportActivityBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        final Intent intent = getIntent();
        final ClipData intentClipData = intent.getClipData();

        if (savedInstanceState == null) {
            if (intent.getData() != null) {
                documentUris.add(intent.getData());
                isDirectory = false;
            } else {
                if (intentClipData != null && intentClipData.getItemCount() > 0) {
                    for (int i = 0; i < intentClipData.getItemCount(); i++) {
                        documentUris.add(intentClipData.getItemAt(i).getUri());
                    }
                    isDirectory = false;
                } else {
                    // Started from DirectoryChooserActivity
                    documentUris.add(intent.getParcelableExtra(EXTRA_DIRECTORY_URI_KEY));
                    isDirectory = true;
                }
            }

        } else {
            documentUris = savedInstanceState.getParcelable(BUNDLE_DOCUMENT_URIS);
            isDirectory = savedInstanceState.getBoolean(BUNDLE_IS_DIRECTORY);
        }

        final List<DocumentFile> documentFiles;
        if (isDirectory) {
            documentFiles = new ArrayList<>();
            documentFiles.add(DocumentFile.fromTreeUri(this, documentUris.get(0)));
        } else {
            documentFiles = documentUris.stream().map(it -> DocumentFile.fromSingleUri(this, it)).collect(Collectors.toList());
        }

        initViews();

        viewModel = new ViewModelProvider(this).get(ImportViewModel.class);
        viewModel.getImportData(documentFiles).observe(this, data -> {
            summary = data;
            setProgress();
        });

        //Works for a directory, but we might have received multiple files via SEND_MULTIPLE.
        viewBinding.importActivityToolbar.setTitle(getString(R.string.import_progress_message, documentFiles.get(0).getName()));
        viewBinding.bottomAppBarLayout.bottomAppBar.setNavigationIcon(R.drawable.ic_logo_color_24dp);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(BUNDLE_DOCUMENT_URIS, documentUris);
        outState.putBoolean(BUNDLE_IS_DIRECTORY, isDirectory);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToCancel || (summary != null && getTotalDone() == summary.getTotalCount())) {
            super.onBackPressed();
            viewModel.cancel();
            getViewModelStore().clear();
            return;
        }

        this.doubleBackToCancel = true;
        Toast.makeText(this, getString(R.string.generic_click_twice_cancel), Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToCancel=false, 2000);
    }

    private void initViews() {
        viewBinding.importProgressDone.setText("0");
        viewBinding.importProgressTotal.setText("0");
        viewBinding.importProgressSummaryOk.setText("0");
        viewBinding.importProgressSummaryExists.setText("0");
        viewBinding.importProgressSummaryErrors.setText("0");
    }

    private int getTotalDone() {
        return summary != null ? summary.getSuccessCount() + summary.getExistsCount() + summary.getErrorCount() : 0;
    }

    private void setProgress() {
        int done = getTotalDone();

        viewBinding.importProgressDone.setText("" + done);
        viewBinding.importProgressTotal.setText("" + summary.getTotalCount());

        viewBinding.importProgressBar.setProgress((int) ((float) done / (float) summary.getTotalCount() * 100f));
        viewBinding.importProgressSummaryOk.setText(String.valueOf(summary.getSuccessCount()));
        viewBinding.importProgressSummaryExists.setText(String.valueOf(summary.getExistsCount()));
        viewBinding.importProgressSummaryErrors.setText(String.valueOf(summary.getErrorCount()));
        viewBinding.importProgressSummaryOkGroup.setVisibility(summary.getSuccessCount() > 0 ? View.VISIBLE : View.GONE);
        viewBinding.importProgressSummaryExistsGroup.setVisibility(summary.getExistsCount() > 0 ? View.VISIBLE : View.GONE);
        viewBinding.importProgressSummaryErrorsGroup.setVisibility(summary.getErrorCount() > 0 ? View.VISIBLE : View.GONE);

        if (done == summary.getTotalCount()) {
            onImportEnded();
        }
    }

    private void onImportEnded() {
        viewBinding.importProgressAlertMsg.setVisibility(View.VISIBLE);
        viewBinding.importProgressAlertIcon.setVisibility(View.VISIBLE);

        viewBinding.importProgressRightButton.setVisibility(View.VISIBLE);
        viewBinding.importProgressRightButton.setText(getString(android.R.string.ok));
        viewBinding.importProgressRightButton.setOnClickListener((view) -> {
            getViewModelStore().clear();
            finish();
        });

        if (summary.getErrorCount() > 0) {
            toggleUIEndWithErrors();
        } else {
            toggleUIEndOk();
        }
    }

    private void toggleUIEndWithErrors() {
        viewBinding.importProgressLeftButton.setVisibility(View.VISIBLE);
        viewBinding.importProgressLeftButton.setText(getString(R.string.generic_show_errors));
        viewBinding.importProgressLeftButton.setOnClickListener((view) -> ErrorListDialog.showDialog(getSupportFragmentManager(), getString(R.string.import_error_list_dialog_title), summary.getFileErrors()));
        viewBinding.importProgressAlertIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_report_problem_24));
        viewBinding.importProgressAlertMsg.setText(getResources().getQuantityString(R.plurals.generic_completed_with_errors, summary.getErrorCount(), summary.getErrorCount()));
    }

    private void toggleUIEndOk() {
        viewBinding.importProgressAlertIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_dialog_success_24dp));
        viewBinding.importProgressAlertMsg.setText(getString(R.string.generic_completed));

        if (summary.getTotalCount() == 1 && summary.getImportedTrackIds().size() == 1) {
            viewBinding.importProgressLeftButton.setVisibility(View.VISIBLE);
            viewBinding.importProgressLeftButton.setText(getString(R.string.generic_open_track));
            viewBinding.importProgressLeftButton.setOnClickListener((view) -> {
                Intent newIntent = IntentUtils.newIntent(ImportActivity.this, TrackRecordedActivity.class)
                        .putExtra(TrackRecordedActivity.EXTRA_TRACK_ID, summary.getImportedTrackIds().get(0));
                startActivity(newIntent);
                getViewModelStore().clear();
                finish();
            });
        } else {
            viewBinding.importProgressLeftButton.setVisibility(View.GONE);
        }
    }
}