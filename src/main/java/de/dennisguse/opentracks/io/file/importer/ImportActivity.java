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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.DialogUtils;
import de.dennisguse.opentracks.util.FileUtils;

/**
 * An activity to import files from the external storage.
 *
 * @author Rodrigo Damazio
 */
public class ImportActivity extends FragmentActivity {

    private static final String TAG = ImportActivity.class.getSimpleName();

    public static final String EXTRA_DIRECTORY_URI_KEY = "directory_uri";

    private static final int DIALOG_PROGRESS_ID = 0;
    private static final int DIALOG_RESULT_ID = 1;

    private DocumentFile pickedDirectory;
    private String directoryDisplayName;


    private Thread importTask = new ImportThread();
    private ProgressDialog progressDialog;

    private int importedTrackCount;
    private int totalTrackCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri directoryUri = getIntent().getParcelableExtra(EXTRA_DIRECTORY_URI_KEY);
        pickedDirectory = DocumentFile.fromTreeUri(this, directoryUri);
        directoryDisplayName = FileUtils.getPath(pickedDirectory);
    }

    @Override
    protected void onStart() {
        super.onStart();
        showDialog(DIALOG_PROGRESS_ID);
        importTask.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        importTask.interrupt();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PROGRESS_ID:
                progressDialog = DialogUtils.createHorizontalProgressDialog(
                        this, R.string.import_progress_message, dialog -> {
                            importTask.interrupt();
                            dialog.dismiss();
                            finish();
                        }, directoryDisplayName);
                return progressDialog;
            case DIALOG_RESULT_ID:
                int iconId;
                int titleId;
                String message;
                String totalFiles = getResources()
                        .getQuantityString(R.plurals.files, totalTrackCount, totalTrackCount);
                if (importedTrackCount == totalTrackCount) {
                    if (totalTrackCount == 0) {
                        iconId = R.drawable.ic_dialog_info_24dp;
                        titleId = R.string.import_no_file_title;
                        message = getString(R.string.import_no_file, directoryDisplayName);
                    } else {
                        iconId = R.drawable.ic_dialog_success_24dp;
                        titleId = R.string.generic_success_title;
                        message = getString(R.string.import_success, totalFiles, directoryDisplayName);
                    }
                } else {
                    iconId = R.drawable.ic_dialog_error_24dp;
                    titleId = R.string.generic_error_title;
                    message = getString(R.string.import_error, importedTrackCount, totalFiles, directoryDisplayName);
                }
                return new AlertDialog.Builder(this).setCancelable(true).setIcon(iconId)
                        .setMessage(message).setOnCancelListener(dialogInterface -> {
                            dialogInterface.dismiss();
                            finish();
                        }).setPositiveButton(R.string.generic_ok, (dialogInterface, which) -> {
                            dialogInterface.dismiss();
                            finish();
                        }).setTitle(titleId).create();
            default:
                return null;
        }
    }

    /**
     * Invokes when the associated AsyncTask completes.
     *
     * @param aSuccessCount the number of files successfully imported
     * @param aTotalCount   the number of files to import
     */
    public void onAsyncTaskCompleted(int aSuccessCount, int aTotalCount) {
        runOnUiThread(() -> {
            importedTrackCount = aSuccessCount;
            totalTrackCount = aTotalCount;
            removeDialog(DIALOG_PROGRESS_ID);
            showDialog(DIALOG_RESULT_ID);
        });
    }

    /**
     * Sets the progress dialog value.
     *
     * @param number the number of files imported
     * @param max    the maximum number of files
     */
    public void setProgressDialogValue(int number, int max) {
        if (progressDialog != null) {
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(max);
            progressDialog.setProgress(Math.min(number, max));
        }
    }

    public class ImportThread extends Thread {

        @Override
        public void run() {
            List<DocumentFile> files = FileUtils.getFiles(pickedDirectory);
            int totalTrackCount = files.size();
            int importedTrackCount = 0;

            for (int i = 0; i < totalTrackCount; i++) {
                if (Thread.interrupted()) {
                    return;
                }
                if (importFile(files.get(i))) {
                    importedTrackCount++;
                }
                setProgressDialogValue(i + 1, totalTrackCount);
            }

            onAsyncTaskCompleted(importedTrackCount, totalTrackCount);
        }

        /**
         * Imports a file.
         *
         * @param file the file
         */
        private boolean importFile(final DocumentFile file) {
            TrackImporter trackImporter;
            String fileExtension = FileUtils.getExtension(file);

            if (TrackFileFormat.GPX.getExtension().equals(fileExtension)) {
                trackImporter = new GpxFileTrackImporter(ImportActivity.this);
            } else if (TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.getExtension().equals(fileExtension)) {
                trackImporter = new KmlFileTrackImporter(ImportActivity.this, -1L);
            } else if (TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES.getExtension().equals(fileExtension)) {
                trackImporter = new KmzTrackImporter(ImportActivity.this, -1L, file.getUri());
            } else {
                Log.i(TAG, "Unsupported file format.");
                return false;
            }

            try (InputStream inputStream = ImportActivity.this.getContentResolver().openInputStream(file.getUri())) {
                return trackImporter.importFile(inputStream) != -1L;
            } catch (IOException e) {
                Log.e(TAG, "Unable to import file", e);
                return false;
            }
        }
    }
}
