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

package de.dennisguse.opentracks.io.file.exporter;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.databinding.ExportActivityBinding;
import de.dennisguse.opentracks.io.file.ErrorListDialog;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.TrackFilenameGenerator;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.ExportUtils;
import de.dennisguse.opentracks.util.FileUtils;

/**
 * An activity for saving tracks to the external storage.
 *
 * @author Rodrigo Damazio
 * TODO: This class needs some refactoring.
 * * It pushes each export job (usually one Track) for export; although export could be run in parallel.
 *   Also this requires that the ExportActivity stays in foreground, so the user has to activily wait.
 *   It would be better to let the ExportService handle this and let it report progress / conflicts to ExportActivity
 * * File name conflicts are checked in this class instead of the ExportService.
 *    So, for this check actually a different file name might be used than in the ExportService.
 * * Saved state as an object instead of individual values.
 */
public class ExportActivity extends AppCompatActivity implements ExportService.ExportServiceResultReceiver.Receiver {

    private static final String TAG = ExportActivity.class.getSimpleName();

    public static final String EXTRA_DIRECTORY_URI_KEY = "directory_uri";
    public static final String EXTRA_ONE_FILE_KEY = "one_file";
    public static final String EXTRA_TRACKFILEFORMAT_KEY = "trackfileformat";

    private static final String BUNDLE_AUTO_CONFLICT = "auto_conflict";
    private static final String BUNDLE_SUCCESS_COUNT = "track_export_success_count";
    private static final String BUNDLE_ERROR_COUNT = "track_export_error_ount";
    private static final String BUNDLE_OVERWRITTEN_COUNT = "track_export_overwritten_count";
    private static final String BUNDLE_SKIPPED_COUNT = "track_export_skipped_count";
    private static final String BUNDLE_TOTAL_COUNT = "track_export_total_count";
    private static final String BUNDLE_DIRECTORY_FILES = "track_directory_files";
    private static final String BUNDLE_TRACK_ERRORS = "track_errors";

    private static final String BUNDLE_EXPORT_TASKS = "export_tasks";

    private enum ConflictResolutionStrategy {
        CONFLICT_NONE,
        CONFLICT_OVERWRITE,
        CONFLICT_SKIP
    }

    private TrackFileFormat trackFileFormat;
    private Uri directoryUri;

    private ExportService.ExportServiceResultReceiver resultReceiver;

    private List<String> directoryFiles;

    private int trackExportSuccessCount;
    private int trackExportErrorCount;
    private int trackExportOverwrittenCount;
    private int trackExportSkippedCount;
    private int trackExportTotalCount;

    boolean doubleBackToCancel = false;

    private ExportActivityBinding viewBinding;

    private ArrayList<String> trackErrors = new ArrayList<>();

    private ConflictResolutionStrategy autoConflict;

    private ContentProviderUtils contentProviderUtils;

    // List of tracks to be exported.
    private ArrayList<ExportTask> exportTasks;

    private final LinkedBlockingQueue<PendingConflict> conflictsQueue = new LinkedBlockingQueue<>();
    private final Handler conflictsHandler = new Handler();

    private final Runnable conflictsRunnable = new Runnable() {
        @Override
        public void run() {
            if (conflictsQueue.isEmpty()) {
                return;
            }

            PendingConflict conflict = conflictsQueue.peek();
            if (conflict.resolve()) {
                conflictsQueue.remove(conflict);
                if (!conflictsQueue.isEmpty()) {
                    conflictsHandler.post(conflictsRunnable);
                }
                return;
            }

            viewBinding.exportProgressLeftButton.setOnClickListener((view) -> {
                setConflictVisibility(View.GONE);
                conflict.skip();
                conflictsQueue.remove(conflict);
                if (!conflictsQueue.isEmpty()) {
                    conflictsHandler.post(conflictsRunnable);
                }
            });

            viewBinding.exportProgressRightButton.setOnClickListener((view) -> {
                setConflictVisibility(View.GONE);
                conflict.overwrite();
                conflictsQueue.remove(conflict);
                if (!conflictsQueue.isEmpty()) {
                    conflictsHandler.post(conflictsRunnable);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ExportActivityBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        setSupportActionBar(viewBinding.bottomAppBarLayout.bottomAppBar);

        directoryUri = getIntent().getParcelableExtra(EXTRA_DIRECTORY_URI_KEY);
        trackFileFormat = (TrackFileFormat) getIntent().getSerializableExtra(EXTRA_TRACKFILEFORMAT_KEY);
        boolean allInOneFile = getIntent().getBooleanExtra(EXTRA_ONE_FILE_KEY, false);

        contentProviderUtils = new ContentProviderUtils(this);

        DocumentFile documentFile = DocumentFile.fromTreeUri(this, directoryUri);
        String directoryDisplayName = FileUtils.getPath(documentFile);

        resultReceiver = new ExportService.ExportServiceResultReceiver(new Handler(), this);

        if (savedInstanceState == null) {
            autoConflict = ConflictResolutionStrategy.CONFLICT_NONE;
            setProgress();
            new Thread(() -> {
                directoryFiles = ExportUtils.getAllFiles(ExportActivity.this, documentFile.getUri());
                runOnUiThread(() -> {
                    createExportTasks(allInOneFile);
                    nextExport(null);
                });
            }).start();
        } else {
            autoConflict = ConflictResolutionStrategy.valueOf(savedInstanceState.getString(BUNDLE_AUTO_CONFLICT));
            trackExportSuccessCount = savedInstanceState.getInt(BUNDLE_SUCCESS_COUNT);
            trackExportErrorCount = savedInstanceState.getInt(BUNDLE_ERROR_COUNT);
            trackExportOverwrittenCount = savedInstanceState.getInt(BUNDLE_OVERWRITTEN_COUNT);
            trackExportSkippedCount = savedInstanceState.getInt(BUNDLE_SKIPPED_COUNT);
            trackExportTotalCount = savedInstanceState.getInt(BUNDLE_TOTAL_COUNT);
            directoryFiles = savedInstanceState.getStringArrayList(BUNDLE_DIRECTORY_FILES);
            trackErrors = savedInstanceState.getStringArrayList(BUNDLE_TRACK_ERRORS);
            exportTasks = new ArrayList<>(savedInstanceState.getParcelableArrayList(BUNDLE_EXPORT_TASKS));
            setProgress();
            nextExport(null);
        }

        viewBinding.exportActivityToolbar.setTitle(getString(R.string.export_progress_message, directoryDisplayName));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_AUTO_CONFLICT, autoConflict.name());
        outState.putInt(BUNDLE_SUCCESS_COUNT, trackExportSuccessCount);
        outState.putInt(BUNDLE_ERROR_COUNT, trackExportErrorCount);
        outState.putInt(BUNDLE_OVERWRITTEN_COUNT, trackExportOverwrittenCount);
        outState.putInt(BUNDLE_SKIPPED_COUNT, trackExportSkippedCount);
        outState.putInt(BUNDLE_TOTAL_COUNT, trackExportTotalCount);
        outState.putStringArrayList(BUNDLE_DIRECTORY_FILES, (ArrayList<String>) directoryFiles);
        outState.putStringArrayList(BUNDLE_TRACK_ERRORS, trackErrors);
        outState.putParcelableArrayList(BUNDLE_EXPORT_TASKS, exportTasks);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        conflictsQueue.clear();
        exportTasks.clear();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToCancel || getTotalDone() == trackExportTotalCount) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToCancel = true;
        Toast.makeText(this, getString(R.string.generic_click_twice_cancel), Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToCancel = false, 2000);
    }

    private void createExportTasks(boolean allInOneFile) {
        List<Track> tracks = contentProviderUtils.getTracks();
        exportTasks = new ArrayList<>();
        if (allInOneFile) {
            String filename = "OpenTracks-Backup";
            exportTasks.add(new ExportTask(filename, trackFileFormat, tracks.stream().map(Track::getId).toList()));
        } else {
            exportTasks.addAll(tracks.stream().map(it -> new ExportTask(null, trackFileFormat, List.of(it.getId()))).toList());
        }
        trackExportTotalCount = exportTasks.size();
    }

    /**
     * Enqueue track identified by UUID to be exported if not exported already or there is a conflict resolution.
     */
    private void export(ExportTask exportTask, ConflictResolutionStrategy conflictResolution) {
        boolean fileExists = exportFileExists(exportTask);

        if (fileExists && conflictResolution == ConflictResolutionStrategy.CONFLICT_NONE) {
            conflict(exportTask);
        } else if (fileExists && conflictResolution == ConflictResolutionStrategy.CONFLICT_SKIP) {
            trackExportSkippedCount++;
            nextExport(exportTask);
        } else {
            ExportService.enqueue(this, resultReceiver, exportTask, directoryUri);
        }
    }

    private void export(ExportTask exportTask) {
        export(exportTask, autoConflict);
    }

    @Deprecated //TODO Check should be done in ExportService
    private boolean exportFileExists(ExportTask exportTask) {
        String filename;
        if (exportTask.isMultiExport()) {
            filename = TrackFilenameGenerator.format(exportTask.getFilename(), exportTask.getTrackFileFormat());
        } else {
            Track track = contentProviderUtils.getTrack(exportTask.getTrackIds().get(0));
            filename = PreferencesUtils.getTrackFileformatGenerator().format(track, trackFileFormat);
        }
        return directoryFiles.stream().anyMatch(filename::equals);
    }

    private void setConflictVisibility(int visibility) {
        viewBinding.exportProgressAlertIcon.setVisibility(visibility);
        viewBinding.exportProgressAlertMsg.setVisibility(visibility);
        viewBinding.exportProgressApplyToAll.setVisibility(visibility);
        viewBinding.exportProgressLeftButton.setVisibility(visibility);
        viewBinding.exportProgressRightButton.setVisibility(visibility);
    }

    private int getTotalDone() {
        return trackExportSuccessCount + trackExportOverwrittenCount + trackExportSkippedCount + trackExportErrorCount;
    }

    private void setProgress() {
        viewBinding.exportProgressDone.setText("" + getTotalDone());
        viewBinding.exportProgressTotal.setText("" + trackExportTotalCount);

        viewBinding.exportProgressBar.setProgress((int) ((float) getTotalDone() / (float) trackExportTotalCount * 100f));
        viewBinding.exportProgressSummaryNew.setText("" + trackExportSuccessCount);
        viewBinding.exportProgressSummaryOverwrite.setText("" + trackExportOverwrittenCount);
        viewBinding.exportProgressSummarySkip.setText("" + trackExportSkippedCount);
        viewBinding.exportProgressSummaryErrors.setText("" + trackExportErrorCount);
        viewBinding.exportProgressSummaryNewGroup.setVisibility(trackExportSuccessCount > 0 ? View.VISIBLE : View.GONE);
        viewBinding.exportProgressSummaryOverwriteGroup.setVisibility(trackExportOverwrittenCount > 0 ? View.VISIBLE : View.GONE);
        viewBinding.exportProgressSummarySkipGroup.setVisibility(trackExportSkippedCount > 0 ? View.VISIBLE : View.GONE);
        viewBinding.exportProgressSummaryErrorsGroup.setVisibility(trackExportErrorCount > 0 ? View.VISIBLE : View.GONE);
    }

    private void nextExport(@Nullable ExportTask exportTask) {
        exportTasks.remove(exportTask);

        setProgress();
        if (exportTasks.isEmpty()) {
            onExportEnded();
            return;
        }
        export(exportTasks.get(0));
    }

    private void onExportEnded() {
        viewBinding.exportProgressRightButton.setVisibility(View.VISIBLE);
        viewBinding.exportProgressRightButton.setText(getString(android.R.string.ok));
        viewBinding.exportProgressRightButton.setOnClickListener((view) -> finish());

        viewBinding.exportProgressAlertIcon.setVisibility(View.VISIBLE);
        viewBinding.exportProgressAlertMsg.setVisibility(View.VISIBLE);
        if (trackExportErrorCount > 0) {
            viewBinding.exportProgressLeftButton.setVisibility(View.VISIBLE);
            viewBinding.exportProgressLeftButton.setText(getString(R.string.generic_show_errors));
            viewBinding.exportProgressLeftButton.setOnClickListener((view) -> ErrorListDialog.showDialog(getSupportFragmentManager(), getString(R.string.export_track_errors), trackErrors));
            viewBinding.exportProgressAlertIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_report_problem_24));
            String msg = getResources().getQuantityString(R.plurals.generic_completed_with_errors, trackExportErrorCount, trackExportErrorCount);
            viewBinding.exportProgressAlertMsg.setText(msg);
        } else {
            viewBinding.exportProgressLeftButton.setVisibility(View.GONE);
            viewBinding.exportProgressAlertIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_dialog_success_24dp));
            viewBinding.exportProgressAlertMsg.setText(getString(R.string.generic_completed));
        }
    }

    @Override
    public void onExportSuccess(ExportTask exportTask) {
        if (exportFileExists(exportTask)) {
            trackExportOverwrittenCount++;
        } else {
            trackExportSuccessCount++;
        }

        nextExport(exportTask);
    }

    @Override
    public void onExportError(ExportTask exportTask, String errorMessage) {
        trackExportErrorCount++;
        String name;
        if (exportTask.isMultiExport()) {
            name = exportTask.getFilename();
        } else {
            name = contentProviderUtils.getTrack(exportTask.getTrackIds().get(0)).getName();
        }
        Log.e(TAG, "Error exporting " + name + ": " + errorMessage);
        trackErrors.add(name);

        nextExport(exportTask);
    }

    private void conflict(ExportTask exportTask) {
        PendingConflict newConflict = new PendingConflict(exportTask);
        conflictsQueue.add(newConflict);

        if (conflictsQueue.size() == 1) {
            conflictsHandler.post(conflictsRunnable);
        }
    }

    /**
     * Handle conflicts (exporting file already exists).
     */
    private class PendingConflict {
        private final ExportTask exportTask;

        public PendingConflict(ExportTask exportTask) {
            this.exportTask = exportTask;
        }

        /**
         * Try to resolve the conflict if user gave the info needed.
         * Otherwise shows the buttons and views needed to gives the user the possibility of take a decision.
         *
         * @return true if it could resolve the conflict or false otherwise.
         */
        public boolean resolve() {
            if (autoConflict == ConflictResolutionStrategy.CONFLICT_NONE) {
                viewBinding.exportProgressAlertIcon.setImageDrawable(ContextCompat.getDrawable(ExportActivity.this, R.drawable.ic_report_problem_24));
                String name;
                if (exportTask.isMultiExport()) {
                    name = exportTask.getFilename();
                } else {
                    name = contentProviderUtils.getTrack(exportTask.getTrackIds().get(0)).getName();
                }
                viewBinding.exportProgressAlertMsg.setText(getString(R.string.export_track_already_exists_msg, name));
                setConflictVisibility(View.VISIBLE);
                return false;
            }

            export(exportTask);
            return true;
        }

        /**
         * Overwrite the export file and set the autoConflict if user set the "do it for all" switch button.
         */
        public void overwrite() {
            export(exportTask, ConflictResolutionStrategy.CONFLICT_OVERWRITE);

            if (viewBinding.exportProgressApplyToAll.isChecked()) {
                autoConflict = ConflictResolutionStrategy.CONFLICT_OVERWRITE;
            }
        }

        /**
         * Skip the export file and set the autoConflict if user set the "do it for all" switch button.
         */
        public void skip() {
            export(exportTask, ConflictResolutionStrategy.CONFLICT_SKIP);

            if (viewBinding.exportProgressApplyToAll.isChecked()) {
                autoConflict = ConflictResolutionStrategy.CONFLICT_SKIP;
            }
        }
    }
}
