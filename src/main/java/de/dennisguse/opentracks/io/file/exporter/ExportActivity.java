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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.tables.TracksColumns;
import de.dennisguse.opentracks.databinding.ExportActivityBinding;
import de.dennisguse.opentracks.databinding.ToolbarBinding;
import de.dennisguse.opentracks.io.file.ErrorListDialog;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.ExportUtils;
import de.dennisguse.opentracks.util.FileUtils;

/**
 * An activity for saving tracks to the external storage.
 *
 * @author Rodrigo Damazio
 */
public class ExportActivity extends FragmentActivity implements ExportServiceResultReceiver.Receiver {

    private static final String TAG = ExportActivity.class.getSimpleName();

    public static final String EXTRA_DIRECTORY_URI_KEY = "directory_uri";
    public static final String EXTRA_TRACKFILEFORMAT_KEY = "trackfileformat";

    private static final String BUNDLE_AUTO_CONFLICT = "auto_conflict";
    private static final String BUNDLE_SUCCESS_COUNT = "track_export_success_count";
    private static final String BUNDLE_ERROR_COUNT = "track_export_error_ount";
    private static final String BUNDLE_OVERWRITTEN_COUNT = "track_export_overwritten_count";
    private static final String BUNDLE_SKIPPED_COUNT = "track_export_skipped_count";
    private static final String BUNDLE_TOTAL_COUNT = "track_export_total_count";
    private static final String BUNDLE_DIRECTORY_FILES = "track_directory_files";
    private static final String BUNDLE_TRACK_ERRORS = "track_errors";

    private static final int CONFLICT_NONE = 0;
    private static final int CONFLICT_OVERWRITE = 1;
    private static final int CONFLICT_SKIP = 2;

    private TrackFileFormat trackFileFormat;
    private Uri directoryUri;

    private ExportServiceResultReceiver resultReceiver;

    private List<String> directoryFiles;

    private int trackExportSuccessCount;
    private int trackExportErrorCount;
    private int trackExportOverwrittenCount;
    private int trackExportSkippedCount;
    private int trackExportTotalCount;

    boolean doubleBackToCancel = false;

    private ExportActivityBinding viewBinding;

    private ArrayList<String> trackErrors = new ArrayList<>();

    private int autoConflict;

    private ContentProviderUtils contentProviderUtils;

    // List of tracks to be exported.
    private final ArrayList<Track> tracks = new ArrayList<>();

    private final LinkedBlockingQueue<PendingConflict> conflictsQueue = new LinkedBlockingQueue<>();
    private final Handler conflictsHandler = new Handler();

    private final Runnable conflictsRunnable = new Runnable() {
        @Override
        public void run() {
            if (conflictsQueue.size() > 0) {
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
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ExportActivityBinding.inflate(getLayoutInflater());
        ToolbarBinding mergedBinding = ToolbarBinding.bind(viewBinding.getRoot());
        setContentView(viewBinding.getRoot());

        directoryUri = getIntent().getParcelableExtra(EXTRA_DIRECTORY_URI_KEY);
        trackFileFormat = (TrackFileFormat) getIntent().getSerializableExtra(EXTRA_TRACKFILEFORMAT_KEY);

        contentProviderUtils = new ContentProviderUtils(this);

        DocumentFile documentFile = DocumentFile.fromTreeUri(this, directoryUri);
        String directoryDisplayName = FileUtils.getPath(documentFile);

        mergedBinding.toolbar.setTitle(getString(R.string.export_progress_message, directoryDisplayName));

        resultReceiver = new ExportServiceResultReceiver(new Handler(), this);

        if (savedInstanceState == null) {
            autoConflict = CONFLICT_NONE;
            setProgress();
            new Thread(() -> {
                directoryFiles = ExportUtils.getAllFiles(ExportActivity.this, documentFile.getUri());
                runOnUiThread(() -> initExport(0));
            }).start();
        } else {
            autoConflict = savedInstanceState.getInt(BUNDLE_AUTO_CONFLICT);
            trackExportSuccessCount = savedInstanceState.getInt(BUNDLE_SUCCESS_COUNT);
            trackExportErrorCount = savedInstanceState.getInt(BUNDLE_ERROR_COUNT);
            trackExportOverwrittenCount = savedInstanceState.getInt(BUNDLE_OVERWRITTEN_COUNT);
            trackExportSkippedCount = savedInstanceState.getInt(BUNDLE_SKIPPED_COUNT);
            trackExportTotalCount = savedInstanceState.getInt(BUNDLE_TOTAL_COUNT);
            directoryFiles = savedInstanceState.getStringArrayList(BUNDLE_DIRECTORY_FILES);
            trackErrors = savedInstanceState.getStringArrayList(BUNDLE_TRACK_ERRORS);

            setProgress();
            initExport(getTotalDone());
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_AUTO_CONFLICT, autoConflict);
        outState.putInt(BUNDLE_SUCCESS_COUNT, trackExportSuccessCount);
        outState.putInt(BUNDLE_ERROR_COUNT, trackExportErrorCount);
        outState.putInt(BUNDLE_OVERWRITTEN_COUNT, trackExportOverwrittenCount);
        outState.putInt(BUNDLE_SKIPPED_COUNT, trackExportSkippedCount);
        outState.putInt(BUNDLE_TOTAL_COUNT, trackExportTotalCount);
        outState.putStringArrayList(BUNDLE_DIRECTORY_FILES, (ArrayList<String>) directoryFiles);
        outState.putStringArrayList(BUNDLE_TRACK_ERRORS, trackErrors);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        conflictsQueue.clear();
        tracks.clear();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToCancel || getTotalDone() == trackExportTotalCount) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToCancel = true;
        Toast.makeText(this, getString(R.string.generic_click_twice_cancel), Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToCancel=false, 2000);
    }

    private void initExport(int from) {
        try (Cursor cursor = contentProviderUtils.getTrackCursor(null, null, TracksColumns._ID)) {
            if (cursor == null) {
                onExportEnded();
                return;
            }

            trackExportTotalCount = cursor.getCount();
            viewBinding.exportProgressTotal.setText("" + trackExportTotalCount);
            for (int i = from; i < trackExportTotalCount; i++) {
                cursor.moveToPosition(i);
                Track track = ContentProviderUtils.createTrack(cursor);
                tracks.add(track);
            }

            if (!tracks.isEmpty()) {
                export(tracks.get(0));
            } else {
                onExportEnded();
            }
        }
    }

    /**
     * Enqueue track identified by UUID to be exported if not exported already or there is a conflict resolution.
     *
     * @param track              Track object.
     * @param conflictResolution conflict resolution to be applied if needed.
     */
    private void export(Track track, int conflictResolution) {
        boolean fileExists = ExportUtils.isExportFileExists(track.getUuid(), trackFileFormat.getExtension(), directoryFiles);

        if (fileExists && conflictResolution == CONFLICT_NONE) {
            conflict(track);
        } else if (fileExists && conflictResolution == CONFLICT_SKIP) {
            trackExportSkippedCount++;
            onExportCompleted(track);
        } else {
            ExportService.enqueue(this, resultReceiver, track.getId(), trackFileFormat, directoryUri);
        }
    }

    private void export(Track track) {
        export(track, autoConflict);
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
        int done = getTotalDone();

        viewBinding.exportProgressDone.setText("" + done);
        viewBinding.exportProgressTotal.setText("" + trackExportTotalCount);

        viewBinding.exportProgressBar.setProgress((int) ((float) done / (float) trackExportTotalCount * 100f));
        viewBinding.exportProgressSummaryNew.setText(String.valueOf(trackExportSuccessCount));
        viewBinding.exportProgressSummaryOverwrite.setText(String.valueOf(trackExportOverwrittenCount));
        viewBinding.exportProgressSummarySkip.setText(String.valueOf(trackExportSkippedCount));
        viewBinding.exportProgressSummaryErrors.setText(String.valueOf(trackExportErrorCount));
        viewBinding.exportProgressSummaryNewGroup.setVisibility(trackExportSuccessCount > 0 ? View.VISIBLE : View.GONE);
        viewBinding.exportProgressSummaryOverwriteGroup.setVisibility(trackExportOverwrittenCount > 0 ? View.VISIBLE : View.GONE);
        viewBinding.exportProgressSummarySkipGroup.setVisibility(trackExportSkippedCount > 0 ? View.VISIBLE : View.GONE);
        viewBinding.exportProgressSummaryErrorsGroup.setVisibility(trackExportErrorCount > 0 ? View.VISIBLE : View.GONE);
    }

    private void onExportCompleted(Track track) {
        tracks.remove(track);

        setProgress();
        if (tracks.isEmpty()) {
            onExportEnded();
            return;
        }
        export(tracks.get(0));
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
    public void onExportSuccess(Track.Id trackId) {
        Track track = contentProviderUtils.getTrack(trackId);

        if (ExportUtils.isExportFileExists(track.getUuid(), trackFileFormat.getExtension(), directoryFiles)) {
            trackExportOverwrittenCount++;
        } else {
            trackExportSuccessCount++;
        }

        onExportCompleted(track);
    }

    @Override
    public void onExportError(Track.Id trackId) {
        Track track = contentProviderUtils.getTrack(trackId);

        trackExportErrorCount++;
        trackErrors.add(track.getName());

        onExportCompleted(track);
    }

    private void conflict(Track track) {
        PendingConflict newConflict = new PendingConflict(track);
        conflictsQueue.add(newConflict);

        if (conflictsQueue.size() == 1) {
            conflictsHandler.post(conflictsRunnable);
        }
    }

    /**
     * Handle conflicts (exporting file already exists).
     */
    private class PendingConflict {
        private final Track track;

        public PendingConflict(Track track) {
            this.track = track;
        }

        /**
         * Try to resolve the conflict if user gave the info needed.
         * Otherwise shows the buttons and views needed to gives the user the possibility of take a decision.
         *
         * @return true if it could resolve the conflict or false otherwise.
         */
        public boolean resolve() {
            if (autoConflict == CONFLICT_NONE) {
                viewBinding.exportProgressAlertIcon.setImageDrawable(ContextCompat.getDrawable(ExportActivity.this, R.drawable.ic_report_problem_24));
                viewBinding.exportProgressAlertMsg.setText(getString(R.string.export_track_already_exists_msg, track.getName()));
                setConflictVisibility(View.VISIBLE);
                return false;
            }

            export(track);
            return true;
        }

        /**
         * Overwrite the export file and set the autoConflict if user set the "do it for all" switch button.
         */
        public void overwrite() {
            export(track, CONFLICT_OVERWRITE);

            if (viewBinding.exportProgressApplyToAll.isChecked()) {
                autoConflict = CONFLICT_OVERWRITE;
            }
        }

        /**
         * Skip the export file and set the autoConflict if user set the "do it for all" switch button.
         */
        public void skip() {
            export(track, CONFLICT_SKIP);

            if (viewBinding.exportProgressApplyToAll.isChecked()) {
                autoConflict = CONFLICT_SKIP;
            }
        }
    }
}
