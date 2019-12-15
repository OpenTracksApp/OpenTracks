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

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import java.io.File;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.fragments.FileTypeDialogFragment;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.DialogUtils;
import de.dennisguse.opentracks.util.FileUtils;

/**
 * An activity for saving tracks to the external storage.
 * If saving a specific track, option to save it to a temp directory and play the track afterward.
 *
 * @author Rodrigo Damazio
 */
public class ExportActivity extends FragmentActivity implements FileTypeDialogFragment.FileTypeCaller {

    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 6;

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

        if (!FileUtils.isExternalStorageWriteable() || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            fileTypeDialogStart();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.external_storage_not_writable, Toast.LENGTH_LONG).show();
            } else {
                fileTypeDialogStart();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void fileTypeDialogStart() {
        FileTypeDialogFragment.showDialog(getSupportFragmentManager(), R.string.export_all_title, R.string.export_all_option);
    }

    @Override
    public void onFileTypeDone(TrackFileFormat trackFileFormat) {
        if (!FileUtils.isExternalStorageWriteable()) {
            Toast.makeText(this, R.string.external_storage_not_writable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        File directory = new File(FileUtils.getPath(trackFileFormat.getExtension()));
        if (!FileUtils.ensureDirectoryExists(directory)) {
            Toast.makeText(this, R.string.external_storage_not_writable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        directoryDisplayName = FileUtils.getPathDisplayName(trackFileFormat.getExtension());
        exportAsyncTask = new ExportAsyncTask(this, trackFileFormat, directory);
        exportAsyncTask.execute();
    }

    @Override
    public void onDismissed() {
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PROGRESS_ID:
                progressDialog = DialogUtils.createHorizontalProgressDialog(this,
                        R.string.export_external_storage_progress_message,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                exportAsyncTask.cancel(true);
                                dialog.dismiss();
                                onDismissed();
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
                            R.string.export_external_storage_success, totalTracks, directoryDisplayName);
                } else {
                    iconId = R.drawable.ic_dialog_error_24dp;
                    titleId = R.string.generic_error_title;
                    message = getString(R.string.export_external_storage_error, processedTrackCount, totalTracks,
                            directoryDisplayName);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setCancelable(true)
                        .setIcon(iconId).setMessage(message)
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                dialog.dismiss();
                                onDismissed();
                            }
                        }).setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int arg1) {
                                dialog.dismiss();
                                onDismissed();
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
