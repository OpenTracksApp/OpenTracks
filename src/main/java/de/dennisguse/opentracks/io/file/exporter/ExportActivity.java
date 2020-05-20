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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.DialogUtils;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * An activity for saving tracks to the external storage.
 *
 * @author Rodrigo Damazio
 */
public class ExportActivity extends FragmentActivity {

    private static final int DIRECTORY_PICKER_REQUEST_CODE = 6;

    private static final int DIALOG_PROGRESS_ID = 0;
    private static final int DIALOG_RESULT_ID = 1;

    private String directoryDisplayName;

    private ExportAsyncTask exportAsyncTask;
    private ProgressDialog progressDialog;

    private int processedTrackCount;
    private int totalTrackCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        startActivityForResult(intent, DIRECTORY_PICKER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == DIRECTORY_PICKER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri directoryUri = resultData.getData();
                DocumentFile pickedDirectory = DocumentFile.fromTreeUri(this, directoryUri);
                DocumentFile exportDirectory = pickedDirectory.findFile(FileUtils.EXPORT_DIR);
                if (exportDirectory == null) {
                    exportDirectory = pickedDirectory.createDirectory(FileUtils.EXPORT_DIR);
                }

                TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat(this);

                directoryDisplayName = FileUtils.getPath(exportDirectory);
                exportAsyncTask = new ExportAsyncTask(this, trackFileFormat, exportDirectory);
                exportAsyncTask.execute();
            } else {
                finish();
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PROGRESS_ID:
                progressDialog = DialogUtils.createHorizontalProgressDialog(this,
                        R.string.export_progress_message,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                exportAsyncTask.cancel(true);
                                dialog.dismiss();
                                finish();
                            }
                        }, directoryDisplayName);
                return progressDialog;
            case DIALOG_RESULT_ID:
                int iconId;
                int titleId;
                String message;
                String totalTracks = getResources()
                        .getQuantityString(R.plurals.tracks, totalTrackCount, totalTrackCount);
                if (processedTrackCount == totalTrackCount) {
                    iconId = R.drawable.ic_dialog_success_24dp;
                    titleId = R.string.generic_success_title;
                    message = getString(
                            R.string.export_success, totalTracks, directoryDisplayName);
                } else {
                    iconId = R.drawable.ic_dialog_error_24dp;
                    titleId = R.string.generic_error_title;
                    message = getString(R.string.export_error, processedTrackCount, totalTracks, directoryDisplayName);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setCancelable(true)
                        .setIcon(iconId).setMessage(message)
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                dialog.dismiss();
                                finish();
                            }
                        }).setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int arg1) {
                                dialog.dismiss();
                                finish();
                            }
                        }).setTitle(titleId);
                return builder.create();
            default:
                return null;
        }
    }

    /**
     * Invokes when the associated AsyncTask completes.
     *
     * @param aSuccessCount the number of tracks successfully saved
     * @param aTotalCount   the number of tracks to save
     */
    public void onAsyncTaskCompleted(int aSuccessCount, int aTotalCount) {
        processedTrackCount = aSuccessCount;
        totalTrackCount = aTotalCount;
        removeDialog(DIALOG_PROGRESS_ID);
        showDialog(DIALOG_RESULT_ID);
    }

    /**
     * Shows the progress dialog.
     */
    public void showProgressDialog() {
        showDialog(DIALOG_PROGRESS_ID);
    }

    /**
     * Sets the progress dialog value.
     *
     * @param number the number of points saved
     * @param max    the maximum number of points
     */
    public void setProgressDialogValue(int number, int max) {
        if (progressDialog != null) {
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(max);
            progressDialog.setProgress(Math.min(number, max));
        }
    }
}
