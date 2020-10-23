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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
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

    private String directoryDisplayName;
    private List<String> directoryFiles;

    private int trackExportSuccessCount;
    private int trackExportErrorCount;
    private int trackExportOverwrittenCount;
    private int trackExportSkippedCount;
    private int trackExportTotalCount;

    boolean doubleBackToCancel = false;

    private TextView viewTotal;
    private TextView viewDone;
    private TextView viewSummary;
    private ProgressBar viewProgressBar;
    private ImageView viewAlertIcon;
    private TextView viewAlertMsg;
    private CheckBox viewDoItForAllSwitch;
    private Button viewLeftButton;
    private Button viewRightButton;

    private ArrayList<String> trackErrors = new ArrayList<>();

    private int autoConflict;

    private ContentProviderUtils contentProviderUtils;

    // List of tracks to be exported.
    private ArrayList<Track.Id> trackIds = new ArrayList<>();

    private LinkedBlockingQueue<PendingConflict> conflictsQueue = new LinkedBlockingQueue<>();
    private Handler conflictsHandler = new Handler();

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

                viewLeftButton.setOnClickListener((view) -> {
                    setConflictVisibility(View.GONE);
                    conflict.skip();
                    conflictsQueue.remove(conflict);
                    if (!conflictsQueue.isEmpty()) {
                        conflictsHandler.post(conflictsRunnable);
                    }
                });

                viewRightButton.setOnClickListener((view) -> {
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
        setContentView(R.layout.export_progress_activity);

        directoryUri = getIntent().getParcelableExtra(EXTRA_DIRECTORY_URI_KEY);
        trackFileFormat = (TrackFileFormat) getIntent().getSerializableExtra(EXTRA_TRACKFILEFORMAT_KEY);

        contentProviderUtils = new ContentProviderUtils(this);

        DocumentFile documentFile = DocumentFile.fromTreeUri(this, directoryUri);
        directoryDisplayName = FileUtils.getPath(documentFile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.export_progress_message, directoryDisplayName));

        resultReceiver = new ExportServiceResultReceiver(new Handler(), this);

        viewTotal = findViewById(R.id.export_progress_total);
        viewDone = findViewById(R.id.export_progress_done);
        viewSummary = findViewById(R.id.export_progress_summary);
        viewProgressBar = findViewById(R.id.export_progress_bar);
        viewAlertIcon = findViewById(R.id.export_progress_alert_icon);
        viewAlertMsg = findViewById(R.id.export_progress_alert_msg);
        viewDoItForAllSwitch = findViewById(R.id.export_progress_toggle);
        viewLeftButton = findViewById(R.id.export_progress_left_button);
        viewRightButton = findViewById(R.id.export_progress_right_button);

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
        trackIds.clear();
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
            viewTotal.setText("" + trackExportTotalCount);
            for (int i = from; i < trackExportTotalCount; i++) {
                cursor.moveToPosition(i);
                Track track = contentProviderUtils.createTrack(cursor);
                trackIds.add(track.getId());
            }

            if (!trackIds.isEmpty()) {
                export(trackIds.get(0));
            } else {
                onExportEnded();
            }
        }
    }

    /**
     * Enqueue track identified by trackId to be exported if not exported already or there is a conflict resolution.
     *
     * @param trackId            the track's id.
     * @param conflictResolution conflict resolution to be applied if needed.
     */
    private void export(Track.Id trackId, int conflictResolution) {
        boolean fileExists = ExportUtils.isExportFileExists(trackId, trackFileFormat.getExtension(), directoryFiles);

        if (fileExists && conflictResolution == CONFLICT_NONE) {
            conflict(trackId);
        } else if (fileExists && conflictResolution == CONFLICT_SKIP) {
            trackExportSkippedCount++;
            onExportCompleted(trackId);
        } else {
            ExportService.enqueue(this, resultReceiver, trackId, trackFileFormat, directoryUri);
        }
    }

    private void export(Track.Id trackId) {
        export(trackId, autoConflict);
    }

    private void setConflictVisibility(int visibility) {
        viewAlertIcon.setVisibility(visibility);
        viewAlertMsg.setVisibility(visibility);
        viewDoItForAllSwitch.setVisibility(visibility);
        viewLeftButton.setVisibility(visibility);
        viewRightButton.setVisibility(visibility);
    }

    private int getTotalDone() {
        return trackExportSuccessCount + trackExportOverwrittenCount + trackExportSkippedCount + trackExportErrorCount;
    }

    private void setProgress() {
        int done = getTotalDone();

        viewDone.setText("" + done);
        viewTotal.setText("" + trackExportTotalCount);

        viewProgressBar.setProgress((int) ((float) done / (float) trackExportTotalCount * 100f));
        viewSummary.setText(getString(R.string.export_progress_review, getTotalDone(), trackExportSuccessCount, trackExportOverwrittenCount, trackExportSkippedCount, trackExportErrorCount));
    }

    private void onExportCompleted(Track.Id trackId) {
        trackIds.remove(trackId);
        setProgress();
        if (trackIds.isEmpty()) {
            onExportEnded();
            return;
        }
        export(trackIds.get(0));
    }

    private void onExportEnded() {
        viewAlertIcon.setVisibility(View.VISIBLE);
        viewAlertMsg.setVisibility(View.VISIBLE);
        viewRightButton.setVisibility(View.VISIBLE);
        viewRightButton.setText(getString(R.string.generic_ok));

        if (trackExportErrorCount > 0) {
            viewLeftButton.setVisibility(View.VISIBLE);
            viewLeftButton.setText(getString(R.string.generic_show_errors));
            viewLeftButton.setOnClickListener((view) -> ErrorListDialog.showDialog(getSupportFragmentManager(), getString(R.string.export_track_errors), trackErrors));
            viewAlertIcon.setImageDrawable(getDrawable(R.drawable.ic_report_problem_24));
            String msg = getResources().getQuantityString(R.plurals.generic_completed_with_errors, trackExportErrorCount, trackExportErrorCount);
            viewAlertMsg.setText(msg);
        } else {
            viewLeftButton.setVisibility(View.GONE);
            viewAlertIcon.setImageDrawable(getDrawable(R.drawable.ic_dialog_success_24dp));
            viewAlertMsg.setText(getString(R.string.generic_completed));
        }

        viewRightButton.setOnClickListener((view) -> finish());
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultData == null) {
            throw new RuntimeException(TAG + ": onReceiveResult resultData NULL");
        }

        Track.Id trackId = resultData.getParcelable(ExportServiceResultReceiver.RESULT_EXTRA_TRACK_ID);

        switch (resultCode) {
            case ExportServiceResultReceiver.RESULT_CODE_ERROR:
                trackExportErrorCount++;
                Track track = contentProviderUtils.getTrack(trackId);
                trackErrors.add(track.getName());
                break;
            case ExportServiceResultReceiver.RESULT_CODE_SUCCESS:
                if (ExportUtils.isExportFileExists(trackId, trackFileFormat.getExtension(), directoryFiles)) {
                    trackExportOverwrittenCount++;
                } else {
                    trackExportSuccessCount++;
                }
                break;
            default:
                throw new RuntimeException(TAG + ": export service result code invalid: " + resultCode);
        }

        onExportCompleted(trackId);
    }

    private void conflict(Track.Id trackId) {
        PendingConflict newConflict = new PendingConflict(trackId);
        conflictsQueue.add(newConflict);

        if (conflictsQueue.size() == 1) {
            conflictsHandler.post(conflictsRunnable);
        }
    }

    /**
     * Handle conflicts (exporting file already exists).
     */
    private class PendingConflict {
        private Track.Id trackId;

        public PendingConflict(Track.Id trackId) {
            this.trackId = trackId;
        }

        /**
         * Try to resolve the conflict if user gave the info needed.
         * Otherwise shows the buttons and views needed to gives the user the possibility of take a decision.
         *
         * @return true if it could resolve the conflict or false otherwise.
         */
        public boolean resolve() {
            if (autoConflict == CONFLICT_NONE) {
                Track track = contentProviderUtils.getTrack(trackId);
                viewAlertIcon.setImageDrawable(getDrawable(R.drawable.ic_report_problem_24));
                viewAlertMsg.setText(getString(R.string.export_track_already_exists_msg, track.getName()));
                setConflictVisibility(View.VISIBLE);
                return false;
            }

            export(trackId);
            return true;
        }

        /**
         * Overwrite the export file and set the autoConflict if user set the "do it for all" switch button.
         */
        public void overwrite() {
            export(trackId, CONFLICT_OVERWRITE);

            if (viewDoItForAllSwitch.isChecked()) {
                autoConflict = CONFLICT_OVERWRITE;
            }
        }

        /**
         * Skip the export file and set the autoConflict if user set the "do it for all" switch button.
         */
        public void skip() {
            export(trackId, CONFLICT_SKIP);

            if (viewDoItForAllSwitch.isChecked()) {
                autoConflict = CONFLICT_SKIP;
            }
        }
    }
}
