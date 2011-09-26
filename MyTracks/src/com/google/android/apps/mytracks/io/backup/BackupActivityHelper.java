/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks.io.backup;

import com.google.android.apps.mytracks.MyTracks;
import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * Helper which shows a UI for writing or restoring a backup,
 * and calls the appropriate handler for actually executing those
 * operations.
 *
 * @author Rodrigo Damazio
 */
public class BackupActivityHelper {

  // Since the user sees this format, we use the local timezone
  private static final SimpleDateFormat DISPLAY_BACKUP_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final Comparator<Date> REVERSE_DATE_ORDER =
      new Comparator<Date>() {
        @Override
        public int compare(Date s1, Date s2) {
          return s2.compareTo(s1);
        }
      };

  private final FileUtils fileUtils;
  private final ExternalFileBackup backup;
  private final Activity activity;

  public BackupActivityHelper(Activity activity) {
    this.activity = activity;
    this.fileUtils = new FileUtils();
    this.backup = new ExternalFileBackup(activity, fileUtils);
  }

  /**
   * Writes a full backup to the default file.
   * This shows the results to the user.
   */
  public void writeBackup() {
    if (!fileUtils.isSdCardAvailable()) {
      showToast(R.string.io_no_external_storage_found);
      return;
    }

    if (!backup.isBackupsDirectoryAvailable(true)) {
      showToast(R.string.io_create_dir_failed);
      return;
    }

    final ProgressDialog progressDialog = ProgressDialog.show(
        activity,
        activity.getString(R.string.progress_title),
        activity.getString(R.string.backup_write_progress_message),
        true);

    // Do the writing in another thread
    new Thread() {
      @Override
      public void run() {
        try {
          backup.writeToDefaultFile();
          showToast(R.string.io_write_finished);
        } catch (IOException e) {
          Log.e(Constants.TAG, "Failed to write backup", e);
          showToast(R.string.io_write_failed);
        } finally {
          dismissDialog(progressDialog);
        }
      }
    }.start();
  }

  /**
   * Restores a full backup from the SD card.
   * The user will be given a choice of which backup to restore as well as a
   * confirmation dialog.
   */
  public void restoreBackup() {
    // Get the list of existing backups
    if (!fileUtils.isSdCardAvailable()) {
      showToast(R.string.io_no_external_storage_found);
      return;
    }

    if (!backup.isBackupsDirectoryAvailable(false)) {
      showToast(R.string.no_backups);
      return;
    }

    final Date[] backupDates = backup.getAvailableBackups();
    if (backupDates == null || backupDates.length == 0) {
      showToast(R.string.no_backups);
      return;
    }
    Arrays.sort(backupDates, REVERSE_DATE_ORDER);

    // Show a confirmation dialog
    Builder confirmationDialogBuilder = new AlertDialog.Builder(activity);
    confirmationDialogBuilder.setMessage(R.string.restore_overwrites_warning);
    confirmationDialogBuilder.setCancelable(true);
    confirmationDialogBuilder.setPositiveButton(android.R.string.yes,
        new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            pickBackupForRestore(backupDates);
          }
        });
    confirmationDialogBuilder.setNegativeButton(android.R.string.no, null);
    confirmationDialogBuilder.create().show();
  }

  /**
   * Shows a backup list for the user to pick, then restores it.
   *
   * @param backupDir the backup directory
   * @param backupDates the list of available backup files
   */
  private void pickBackupForRestore(final Date[] backupDates) {
    if (backupDates.length == 1) {
      // Only one choice, don't bother showing the list
      restoreFromDateAsync(backupDates[0]);
      return;
    }

    // Make a user-visible version of the backup filenames
    final String backupDateStrs[] = new String[backupDates.length];
    for (int i = 0; i < backupDates.length; i++) {
      backupDateStrs[i] = DISPLAY_BACKUP_FORMAT.format(backupDates[i]);
    }

    // Show a dialog for the user to pick which backup to restore
    Builder dialogBuilder = new AlertDialog.Builder(activity);
    dialogBuilder.setCancelable(true);
    dialogBuilder.setTitle(R.string.select_backup_to_restore);
    dialogBuilder.setItems(backupDateStrs, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        // User picked to restore this one
        restoreFromDateAsync(backupDates[which]);
      }
    });
    dialogBuilder.create().show();
  }

  /**
   * Shows a progress dialog, then starts restoring the backup osynchronously.
   *
   * @param inputFile the file to restore from
   */
  private void restoreFromDateAsync(final Date date) {
    // Show a progress dialog
    ProgressDialog.show(
        activity,
        activity.getString(R.string.progress_title),
        activity.getString(R.string.backup_import_progress_message),
        true);

    // Do the actual importing in another thread (don't block the UI)
    new Thread() {
      @Override
      public void run() {
        try {
          backup.restoreFromDate(date);
          showToast(R.string.io_read_finished);
        } catch (IOException e) {
          Log.e(Constants.TAG, "Failed to restore backup", e);
          showToast(R.string.io_read_failed);
        } finally {
          // Data may have been restored, "reboot" the app to catch it
          restartApplication();
        }
      }
    }.start();
  }

  /**
   * Restarts My Tracks completely.
   * This forces any modified data to be re-read.
   */
  private void restartApplication() {
    Intent intent = new Intent(activity, MyTracks.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    activity.startActivity(intent);
  }

  /**
   * Shows a toast with the given contents.
   */
  private void showToast(final int resId) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(activity, resId, Toast.LENGTH_LONG).show();
      }
    });
  }

  /**
   * Safely dismisses the given dialog.
   */
  private void dismissDialog(final Dialog dialog) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        dialog.dismiss();
      }
    });
  }
}
