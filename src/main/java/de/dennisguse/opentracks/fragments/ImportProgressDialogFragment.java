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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.importer.GpxFileTrackImporter;
import de.dennisguse.opentracks.io.file.importer.KmlFileTrackImporter;
import de.dennisguse.opentracks.io.file.importer.KmzTrackImporter;
import de.dennisguse.opentracks.io.file.importer.TrackImporter;
import de.dennisguse.opentracks.util.DialogUtils;
import de.dennisguse.opentracks.util.FileUtils;

public class ImportProgressDialogFragment extends DialogFragment {

    private static final String EXTRA_IS_TREE_KEY = "tree_uri";

    private static final String EXTRA_DATA_URI_KEY = "data_uri";

    private static final String TAG = ImportProgressDialogFragment.class.getSimpleName();

    public static final String IMPORT_DIALOG_TAG = "import_dialog_tag";

    private DismissCallback caller;

    private ImportThread importThread;

    private String directoryDisplayName;

    private int trackImportSuccessCount;
    private int fileCount;

    public static void showDialog(FragmentManager fragmentManager, Uri documentFileUri, boolean isDirectory) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_DATA_URI_KEY, documentFileUri);
        bundle.putBoolean(EXTRA_IS_TREE_KEY, isDirectory);

        ImportProgressDialogFragment dialogFragment = new ImportProgressDialogFragment();
        dialogFragment.setArguments(bundle);
        dialogFragment.setRetainInstance(true);
        dialogFragment.show(fragmentManager, IMPORT_DIALOG_TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This fragment uses a background thread, so it should not be recreated during activity re-creation.
        setRetainInstance(true);

        Uri directoryUri = getArguments().getParcelable(EXTRA_DATA_URI_KEY);
        DocumentFile documentFile;
        if (getArguments().getBoolean(EXTRA_IS_TREE_KEY)) {
            documentFile = DocumentFile.fromTreeUri(getContext(), directoryUri);
        } else {
            documentFile = DocumentFile.fromSingleUri(getContext(), directoryUri);
        }
        directoryDisplayName = FileUtils.getPath(documentFile);

        importThread = new ImportThread(documentFile);
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
        ProgressDialog progressDialog = DialogUtils.createHorizontalProgressDialog(
                getContext(), R.string.import_progress_message,
                dialogInterface -> {
                    importThread.interrupt();
                    dialogInterface.dismiss();
                    caller.onImportCanceled(directoryDisplayName, trackImportSuccessCount, fileCount);
                }, directoryDisplayName);

        progressDialog.setIndeterminate(false);
        progressDialog.setMax(fileCount);
        progressDialog.setProgress(Math.min(trackImportSuccessCount, fileCount));

        return progressDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        setProgress();
        if (!importThread.isAlive()) {
            importThread.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!importThread.isAlive()) {
            importThread.interrupt();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        caller = null;
    }

    private void onProgress(int trackImportSuccessCount, int fileCount) {
        this.trackImportSuccessCount = trackImportSuccessCount;
        this.fileCount = fileCount;
        setProgress();
    }

    private void setProgress() {
        ProgressDialog dialog = (ProgressDialog) getDialog();
        if (dialog != null) {
            dialog.setIndeterminate(false);
            dialog.setMax(fileCount);
            dialog.setProgress(Math.min(trackImportSuccessCount, fileCount));
        }
    }

    private void onImportCompleted(int successCount, int fileCount) {
        if (caller != null && !isDetached()) {
            caller.onImportFinished(directoryDisplayName, successCount, fileCount);
        }
        dismissAllowingStateLoss();
    }

    private class ImportThread extends Thread {

        private final DocumentFile file;

        ImportThread(DocumentFile file) {
            this.file = file;
        }

        @Override
        public void run() {
            Context context = ImportProgressDialogFragment.this.getContext();

            List<DocumentFile> files = FileUtils.getFiles(file);

            int importedTrackCount = 0;
            for (int i = 0; i < files.size(); i++) {
                onProgress(i, files.size());

                if (Thread.interrupted()) {
                    return;
                }
                if (importFile(context, files.get(i))) {
                    importedTrackCount++;
                }
            }

            onImportCompleted(importedTrackCount, files.size());
        }

        /**
         * Imports a file.
         *
         * @param file the file
         */
        private boolean importFile(final Context context, final DocumentFile file) {
            TrackImporter trackImporter;
            String fileExtension = FileUtils.getExtension(file);

            if (TrackFileFormat.GPX.getExtension().equals(fileExtension)) {
                trackImporter = new GpxFileTrackImporter(context);
            } else if (TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.getExtension().equals(fileExtension)) {
                trackImporter = new KmlFileTrackImporter(ImportProgressDialogFragment.this.getContext());
            } else if (TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES.getExtension().equals(fileExtension)) {
                trackImporter = new KmzTrackImporter(context, file.getUri());
            } else {
                Log.i(TAG, "Unsupported file format.");
                return false;
            }

            try (InputStream inputStream = context.getContentResolver().openInputStream(file.getUri())) {
                return trackImporter.importFile(inputStream) != -1L;
            } catch (IOException e) {
                Log.e(TAG, "Unable to import file", e);
                return false;
            }
        }
    }

    public interface DismissCallback {
        void onImportCanceled(String directoryDisplayName, int successCount, int fileCount);

        void onImportFinished(String directoryDisplayName, int successCount, int fileCount);
    }
}