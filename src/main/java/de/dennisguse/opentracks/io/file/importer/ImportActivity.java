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
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import java.io.File;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.fragments.FileTypeDialogFragment;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.DialogUtils;
import de.dennisguse.opentracks.util.FileUtils;

/**
 * An activity to import files from the external storage. Optionally to import
 * one specific file.
 *
 * @author Rodrigo Damazio
 */
public class ImportActivity extends FragmentActivity implements FileTypeDialogFragment.FileTypeCaller {

    private static final String TAG = ImportActivity.class.getSimpleName();
    private static final int DIALOG_PROGRESS_ID = 0;
    private static final int DIALOG_RESULT_ID = 1;

    private ImportAsyncTask importAsyncTask;
    private ProgressDialog progressDialog;

    // the path on the external storage to import
    private String directoryDisplayName;

    // the number of files successfully imported
    private int successCount;

    // the number of files to import
    private int totalCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FileTypeDialogFragment
                .newInstance(R.string.import_selection_title, R.string.import_selection_option)
                .show(getSupportFragmentManager(), FileTypeDialogFragment.FILE_TYPE_DIALOG_TAG);
    }


    @Override
    public void onFileTypeDone(TrackFileFormat trackFileFormat) {
        if (!FileUtils.isExternalStorageAvailable()) {
            Toast.makeText(this, R.string.external_storage_not_available, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        directoryDisplayName = FileUtils.getPathDisplayName(trackFileFormat.getExtension());
        String directoryPath = FileUtils.getPath(trackFileFormat.getExtension());
        if (!FileUtils.isDirectory(new File(directoryPath))) {
            Toast.makeText(this, getString(R.string.import_no_directory, directoryDisplayName), Toast.LENGTH_LONG)
                    .show();
            finish();
            return;
        }

        //TODO (still needed?): getLastNonConfiguration instance returned SaveAsyncTask before
//        Object retained = getLastNonConfigurationInstance();
//        if (retained instanceof ImportAsyncTask) {
//            importAsyncTask = (ImportAsyncTask) retained;
//            importAsyncTask.setActivity(this);
//        } else {
        importAsyncTask = new ImportAsyncTask(this, trackFileFormat, directoryPath);
        importAsyncTask.execute();
//        }
    }

    @Override
    public void onDismissed() {
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PROGRESS_ID:
                progressDialog = DialogUtils.createHorizontalProgressDialog(
                        this, R.string.import_progress_message, new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                importAsyncTask.cancel(true);
                                dialog.dismiss();
                                onDismissed();
                            }
                        }, directoryDisplayName);
                return progressDialog;
            case DIALOG_RESULT_ID:
                int iconId;
                int titleId;
                String message;
                String totalFiles = getResources()
                        .getQuantityString(R.plurals.files, totalCount, totalCount);
                if (successCount == totalCount) {
                    if (totalCount == 0) {
                        iconId = android.R.drawable.ic_dialog_info;
                        titleId = R.string.import_no_file_title;
                        message = getString(R.string.import_no_file, directoryDisplayName);
                    } else {
                        iconId = R.drawable.ic_dialog_success;
                        titleId = R.string.generic_success_title;
                        message = getString(R.string.import_success, totalFiles, directoryDisplayName);
                    }
                } else {
                    iconId = android.R.drawable.ic_dialog_alert;
                    titleId = R.string.generic_error_title;
                    message = getString(
                            R.string.import_error, successCount, totalFiles, directoryDisplayName);
                }
                final Dialog dialog = new AlertDialog.Builder(this).setCancelable(true).setIcon(iconId)
                        .setMessage(message).setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                dialogInterface.dismiss();
                                onDismissed();
                            }
                        }).setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                dialogInterface.dismiss();
                                onDismissed();
                            }
                        }).setTitle(titleId).create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        DialogUtils.setDialogTitleDivider(ImportActivity.this, dialog);
                    }
                });
                return dialog;
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
        successCount = aSuccessCount;
        totalCount = aTotalCount;
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
}
