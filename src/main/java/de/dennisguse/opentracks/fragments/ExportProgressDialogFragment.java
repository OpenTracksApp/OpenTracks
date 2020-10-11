/*
 * Copyright 2012 Google Inc.
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

package de.dennisguse.opentracks.fragments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.DialogUtils;
import de.dennisguse.opentracks.util.ExportUtils;
import de.dennisguse.opentracks.util.FileUtils;

public class ExportProgressDialogFragment extends DialogFragment {

    private static final String EXTRA_DIRECTORY_URI_KEY = "directory_uri";
    private static final String EXTRA_TRACKFILEFORMAT_KEY = "trackfileformat";

    private static final String TAG = ExportProgressDialogFragment.class.getSimpleName();

    public static final String EXPORT_DIALOG_TAG = "export_dialog_tag";

    private DismissCallback caller;

    private ExportThread exportThread;

    private String directoryDisplayName;

    private int trackExportSuccessCount;
    private int trackCount;

    /**
     * Create a new instance.
     */
    public static void showDialog(FragmentManager fragmentManager, TrackFileFormat trackFileFormat, Uri documentFileUri) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_DIRECTORY_URI_KEY, documentFileUri);
        bundle.putSerializable(EXTRA_TRACKFILEFORMAT_KEY, trackFileFormat);

        ExportProgressDialogFragment dialogFragment = new ExportProgressDialogFragment();
        dialogFragment.setArguments(bundle);
        dialogFragment.setRetainInstance(true);
        dialogFragment.show(fragmentManager, EXPORT_DIALOG_TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This fragment uses a background thread, so it should not be recreated during activity re-creation.
        setRetainInstance(true);

        Uri directoryUri = getArguments().getParcelable(EXTRA_DIRECTORY_URI_KEY);
        TrackFileFormat trackFileFormat = (TrackFileFormat) getArguments().getSerializable(EXTRA_TRACKFILEFORMAT_KEY);
        DocumentFile documentFile = DocumentFile.fromTreeUri(getContext(), directoryUri);

        directoryDisplayName = FileUtils.getPath(documentFile);

        exportThread = new ExportThread(trackFileFormat, documentFile);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            caller = (DismissCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement " + DismissCallback.class.getSimpleName());
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog progressDialog = DialogUtils.createHorizontalProgressDialog(getContext(),
                R.string.export_progress_message,
                dialog -> {
                    exportThread.interrupt();
                    dialog.dismiss();
                    caller.onExportCanceled(directoryDisplayName, trackExportSuccessCount, trackCount);
                }, directoryDisplayName);

        progressDialog.setIndeterminate(false);
        progressDialog.setMax(trackCount);
        progressDialog.setProgress(Math.min(trackExportSuccessCount, trackCount));

        return progressDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        setProgress();
        if (!exportThread.isAlive()) {
            exportThread.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!exportThread.isAlive()) {
            exportThread.interrupt();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        caller = null;
    }

    private void onProgress(int trackImportSuccessCount, int fileCount) {
        this.trackExportSuccessCount = trackImportSuccessCount;
        this.trackCount = fileCount;
        setProgress();
    }

    private void setProgress() {
        ProgressDialog dialog = (ProgressDialog) getDialog();
        if (dialog != null) {
            dialog.setIndeterminate(false);
            dialog.setMax(trackCount);
            dialog.setProgress(Math.min(trackExportSuccessCount, trackCount));
        }
    }

    private void onExportCompleted(int successCount, int fileCount) {
        if (caller != null && !isDetached()) {
            caller.onExportFinished(directoryDisplayName, successCount, fileCount);
        }
        dismissAllowingStateLoss();
    }

    private class ExportThread extends Thread {

        private final TrackFileFormat trackFileFormat;

        private final DocumentFile directory;

        ExportThread(TrackFileFormat trackFileFormat, DocumentFile directory) {
            this.directory = directory;
            this.trackFileFormat = trackFileFormat;
        }

        @Override
        public void run() {
            Context context = ExportProgressDialogFragment.this.getContext();

            ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

            try (Cursor cursor = contentProviderUtils.getTrackCursor(null, null, TracksColumns._ID)) {
                if (cursor == null) {
                    onExportCompleted(0, 0);
                    return;
                }

                trackCount = cursor.getCount();
                for (int i = 0; i < trackCount; i++) {
                    onProgress(i, trackCount);
                    if (Thread.interrupted()) {
                        return;
                    }

                    cursor.moveToPosition(i);
                    Track track = contentProviderUtils.createTrack(cursor);
                    if (track != null && ExportUtils.exportTrack(context, trackFileFormat, directory, track)) {
                        trackExportSuccessCount++;
                    }
                }

                onExportCompleted(trackExportSuccessCount, trackCount);
            }
        }
    }

    public interface DismissCallback {
        void onExportCanceled(String directoryDisplayName, int successCount, int fileCount);

        void onExportFinished(String directoryDisplayName, int successCount, int fileCount);
    }
}