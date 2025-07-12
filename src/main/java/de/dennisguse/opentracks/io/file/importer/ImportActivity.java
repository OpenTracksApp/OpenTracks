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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordedActivity;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.databinding.ImportActivityBinding;
import de.dennisguse.opentracks.io.file.ErrorListDialog;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * An activity to import files from the external storage.
 *
 * @author Rodrigo Damazio
 */
public class ImportActivity extends AbstractActivity<ImportActivityBinding> {

    private static final String TAG = ImportActivity.class.getSimpleName();

    public static final String EXTRA_DIRECTORY_URI_KEY = "directory_uri";

    private static final String BUNDLE_DOCUMENT_URIS = "document_uris";
    private static final String BUNDLE_IS_DIRECTORY = "is_directory";

    private ArrayList<Uri> documentUris = new ArrayList<>();
    private ArrayList<DocumentFile> filesToImport = new ArrayList<>();
    private boolean isDirectory;

    private Summary summary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final ClipData intentClipData = intent.getClipData();

        summary = new Summary();

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

        //TODO init UI
        loadData(documentFiles);
        importNextFile();

        //Works for a directory, but we might have received multiple files via SEND_MULTIPLE.
        viewBinding.importActivityToolbar.setTitle(getString(R.string.import_progress_message, documentFiles.get(0).getName()));
    }

    @NonNull
    @Override
    protected ImportActivityBinding createRootView() {
        return ImportActivityBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(BUNDLE_DOCUMENT_URIS, documentUris);
        outState.putBoolean(BUNDLE_IS_DIRECTORY, isDirectory);
    }

    private void loadData(List<DocumentFile> documentFiles) {
        filesToImport.addAll(FileUtils.getFiles(documentFiles));
        summary.totalCount = filesToImport.size();
    }

    private void importNextFile() {
        if (filesToImport.isEmpty()) {
            onImportEnded();
            return;
        }

        final DocumentFile documentFile = filesToImport.get(0);

        WorkManager workManager = WorkManager.getInstance(getApplication());
        WorkRequest importRequest = new OneTimeWorkRequest.Builder(ImportWorker.class)
                .setInputData(new Data.Builder()
                        .putString(ImportWorker.URI_KEY, documentFile.getUri().toString())
                        .build())
                .build();

        workManager
                .getWorkInfoByIdLiveData(importRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null) {
                        WorkInfo.State state = workInfo.getState();
                        if (state.isFinished()) {
                            switch (state) {
                                case SUCCEEDED -> {
                                    summary.importedTrackIds.addAll(
                                            Arrays.stream(workInfo.getOutputData().getLongArray(ImportWorker.RESULT_SUCCESS_LIST_TRACKIDS_KEY))
                                                    .mapToObj(Track.Id::new)
                                                    .toList());

                                    summary.successCount++;
                                }
                                case FAILED -> {
                                    if (workInfo.getOutputData().getBoolean(ImportWorker.RESULT_FAILURE_IS_DUPLICATE, false)) {
                                        summary.existsCount++;
                                    } else {
                                        // Some error happened
                                        String errorMessage = workInfo.getOutputData().getString(ImportWorker.RESULT_MESSAGE_KEY);
                                        summary.fileErrors.add(getApplication().getString(R.string.import_error_info, documentFile.getName(), errorMessage));
                                    }
                                }
                            }

                            setProgress();
                            importNextFile();
                        }
                    }
                });

        workManager.enqueue(importRequest);
        filesToImport.remove(0);
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

    static class Summary {
        private int totalCount;
        private int successCount;
        private int existsCount;
        private final ArrayList<Track.Id> importedTrackIds = new ArrayList<>();
        private final ArrayList<String> fileErrors = new ArrayList<>();

        public int getTotalCount() {
            return totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getExistsCount() {
            return existsCount;
        }

        public int getErrorCount() {
            return fileErrors.size();
        }

        public ArrayList<Track.Id> getImportedTrackIds() {
            return importedTrackIds;
        }

        public ArrayList<String> getFileErrors() {
            return fileErrors;
        }
    }
}