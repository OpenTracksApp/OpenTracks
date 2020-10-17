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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
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

    private static final String BUNDLE_ACTIVITY_RECREATED = "activity_recreated";

    private TrackFileFormat trackFileFormat;
    private Uri directoryUri;

    private ExportServiceResultReceiver resultReceiver;

    private String directoryDisplayName;

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
    private SwitchCompat viewDoItForAllSwitch;
    private Button viewLeftButton;
    private Button viewRightButton;

    private int autoConflict;

    private ContentProviderUtils contentProviderUtils;

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

        resultReceiver = new ExportServiceResultReceiver(new Handler());
        resultReceiver.setReceiver(this);

        viewTotal = findViewById(R.id.export_progress_total);
        viewDone = findViewById(R.id.export_progress_done);
        viewSummary = findViewById(R.id.export_progress_summary);
        viewProgressBar = findViewById(R.id.export_progress_bar);
        viewAlertIcon = findViewById(R.id.export_progress_alert_icon);
        viewAlertMsg = findViewById(R.id.export_progress_alert_msg);
        viewDoItForAllSwitch = findViewById(R.id.export_progress_toggle);
        viewLeftButton = findViewById(R.id.export_progress_left_button);
        viewRightButton = findViewById(R.id.export_progress_right_button);

        viewTotal.setText("0");
        viewDone.setText("0");
        viewProgressBar.setProgress(0);
        viewSummary.setText(getString(R.string.export_progress_review, getTotalDone(), trackExportSuccessCount, trackExportOverwrittenCount, trackExportSkippedCount, trackExportErrorCount));

        autoConflict = ExportService.CONFLICT_UNKNOWN;

        if (savedInstanceState == null || !savedInstanceState.getBoolean(BUNDLE_ACTIVITY_RECREATED, false)) {
            export();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_ACTIVITY_RECREATED, true);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToCancel || getTotalDone() == trackExportTotalCount) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToCancel = true;
        Toast.makeText(this, getString(R.string.export_click_twice_cancel), Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToCancel=false, 2000);
    }

    private void export() {
        try (Cursor cursor = contentProviderUtils.getTrackCursor(null, null, TracksColumns._ID)) {
            if (cursor == null) {
                onExportCompleted();
                return;
            }

            trackExportTotalCount = cursor.getCount();
            viewTotal.setText("" + trackExportTotalCount);
            for (int i = 0; i < trackExportTotalCount; i++) {
                cursor.moveToPosition(i);
                Track track = contentProviderUtils.createTrack(cursor);
                ExportService.enqueue(this, resultReceiver, track.getId(), trackFileFormat, directoryUri, autoConflict);
            }
        }
    }

    private void setConflictVisibility(int visibility) {
        viewAlertIcon.setVisibility(visibility);
        viewAlertMsg.setVisibility(visibility);
        viewDoItForAllSwitch.setVisibility(visibility);
        viewLeftButton.setVisibility(visibility == View.GONE ? View.INVISIBLE : visibility);
        viewRightButton.setVisibility(visibility == View.GONE ? View.INVISIBLE : visibility);
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

    private void onExportCompleted() {
        setProgress();

        if (getTotalDone() == trackExportTotalCount) {
            onExportEnded();
        }
    }

    private void onExportEnded() {
        viewAlertIcon.setVisibility(View.VISIBLE);
        viewAlertMsg.setVisibility(View.VISIBLE);
        viewRightButton.setVisibility(View.VISIBLE);
        viewRightButton.setText(getString(R.string.generic_ok));

        if (trackExportErrorCount > 0) {
            viewLeftButton.setVisibility(View.VISIBLE);
            viewLeftButton.setText(getString(R.string.export_show_errors));
            viewLeftButton.setOnClickListener((view) -> new ErrorsListDialog().show(getSupportFragmentManager(), ErrorsListDialog.TAG));
            viewAlertIcon.setImageDrawable(getDrawable(R.drawable.ic_report_problem_24));
            viewAlertIcon.setColorFilter(ContextCompat.getColor(ExportActivity.this, android.R.color.holo_red_dark));
            String msg = trackExportErrorCount > 1 ? getString(R.string.export_all_end_with_errors, trackExportErrorCount) : getString(R.string.export_all_end_with_error);
            viewAlertMsg.setText(msg);
        } else {
            viewLeftButton.setVisibility(View.GONE);
            viewAlertIcon.setImageDrawable(getDrawable(R.drawable.ic_dialog_success_24dp));
            viewAlertIcon.setColorFilter(ContextCompat.getColor(ExportActivity.this, android.R.color.holo_green_dark));
            viewAlertMsg.setText(getString(R.string.export_all_end));
        }

        viewRightButton.setOnClickListener((view) -> finish());
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultData == null) {
            throw new RuntimeException(TAG + ": onReceiveResult resultData NULL");
        }

        Track.Id trackId = resultData.getParcelable(ExportServiceResultReceiver.RESULT_EXTRA_TRACK_ID);
        boolean success = resultData.getBoolean(ExportServiceResultReceiver.RESULT_EXTRA_SUCCESS);

        switch (resultCode) {
            case ExportServiceResultReceiver.RESULT_EXPORTED:
                if (success) {
                    trackExportSuccessCount++;
                } else {
                    Track track = contentProviderUtils.getTrack(trackId);
                    ErrorsListDialog.trackErrors.add(track.getName());
                    trackExportErrorCount++;
                }
                onExportCompleted();
                break;
            case ExportServiceResultReceiver.RESULT_OVERWRITTEN:
                if (success) {
                    trackExportOverwrittenCount++;
                } else {
                    Track track = contentProviderUtils.getTrack(trackId);
                    ErrorsListDialog.trackErrors.add(track.getName());
                    trackExportErrorCount++;
                }
                onExportCompleted();
                break;
            case ExportServiceResultReceiver.RESULT_SKIPPED:
                trackExportSkippedCount++;
                onExportCompleted();
                break;
            case ExportServiceResultReceiver.RESULT_UNKNOWN:
                conflict(trackId);
                break;
            default:
                throw new RuntimeException(TAG + ": export service result code invalid: " + resultCode);
        }
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
         * Otherwise shows the buttons and views needed to gives the user the possibility of take to a decision.
         *
         * @return true if it could resolve the conflict or false otherwise.
         */
        public boolean resolve() {
            if (autoConflict == ExportService.CONFLICT_UNKNOWN) {
                Track track = contentProviderUtils.getTrack(trackId);
                viewAlertIcon.setImageDrawable(getDrawable(R.drawable.ic_report_problem_24));
                viewAlertIcon.setColorFilter(ContextCompat.getColor(ExportActivity.this, android.R.color.holo_red_dark));
                viewAlertMsg.setText(getString(R.string.export_track_already_exists_msg, track.getName()));
                setConflictVisibility(View.VISIBLE);
                return false;
            }

            ExportService.enqueue(ExportActivity.this, resultReceiver, trackId, trackFileFormat, directoryUri, autoConflict);
            return true;
        }

        /**
         * Overwrite the export file and set the autoConflict if user set the "do it for all" switch button.
         */
        public void overwrite() {
            ExportService.enqueue(ExportActivity.this, resultReceiver, trackId, trackFileFormat, directoryUri, ExportService.CONFLICT_OVERWRITE);

            if (viewDoItForAllSwitch.isChecked()) {
                autoConflict = ExportService.CONFLICT_OVERWRITE;
            }
        }

        /**
         * Skip the export file and set the autoConflict if user set the "do it for all" switch button.
         */
        public void skip() {
            ExportService.enqueue(ExportActivity.this, resultReceiver, trackId, trackFileFormat, directoryUri, ExportService.CONFLICT_SKIP);

            if (viewDoItForAllSwitch.isChecked()) {
                autoConflict = ExportService.CONFLICT_SKIP;
            }
        }
    }

    public static class ErrorsListDialog extends DialogFragment {

        public static final String TAG = ErrorsListDialog.class.getSimpleName();

        public static List<String> trackErrors = new ArrayList<>();

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String[] tracks = trackErrors.stream().toArray(String[]::new);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.export_track_errors))
                    .setItems(tracks, null)
                    .setNegativeButton(R.string.generic_ok, (dialog, which) -> dismiss());
            AlertDialog dialog = alertDialogBuilder.create();
            return dialog;
        }
    }
}
