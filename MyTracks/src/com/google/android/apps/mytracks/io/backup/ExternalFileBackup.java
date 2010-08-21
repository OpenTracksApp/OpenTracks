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

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.MyTracksSettings;
import com.google.android.apps.mytracks.content.TrackPointsColumns;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

/**
 * Handler for writing or reading single-file backups.
 *
 * @author Rodrigo Damazio
 */
public class ExternalFileBackup {
  // Filename format - in UTC
  private static final SimpleDateFormat BACKUP_FILENAME_FORMAT =
      new SimpleDateFormat("'backup-'yyyy-MM-dd_HH-mm-ss");
  static {
    BACKUP_FILENAME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  // Since the user sees this format, we use the local timezone
  private static final SimpleDateFormat DISPLAY_BACKUP_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final String BACKUPS_SUBDIR = "backups";
  private static final int BACKUP_FILE_VERSION = 1;
  
  private static final Comparator<String> REVERSE_STRING_COMPARATOR =
      new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
          return s2.compareTo(s1);
        }
      };

  private final Context context;
  private final FileUtils fileUtils;
  private final Handler uiThreadHandler = new Handler();

  public ExternalFileBackup(Context context) {
    this.context = context;
    this.fileUtils = new FileUtils();
  }

  /**
   * Writes a full backup to the default file.
   * This shows the results to the user.
   */
  public void writeToDefaultFile() {
    if (!fileUtils.isSdCardAvailable()) {
      showToast(R.string.io_no_external_storage_found);
      return;
    }

    String dirName = fileUtils.buildExternalDirectoryPath(BACKUPS_SUBDIR);
    final File dir = new File(dirName);
    if (!fileUtils.ensureDirectoryExists(dir)) {
      showToast(R.string.io_create_dir_failed);
      return;
    }

    final ProgressDialog progressDialog = ProgressDialog.show(
        context,
        context.getString(R.string.progress_title),
        context.getString(R.string.backup_write_progress_message),
        true);

    // Do the writing in another thread
    new Thread() {
      @Override
      public void run() {
        try {
          final String filename = BACKUP_FILENAME_FORMAT.format(new Date());
          final File outputFile = new File(dir, filename);

          Log.d(MyTracksConstants.TAG, "Writing backup to file " + filename);
          writeToFile(outputFile);
          showToast(R.string.io_write_finished);
        } catch (IOException e) {
          showToast(R.string.io_write_failed);
          return;
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
  public void restoreFromFileList() {
    // Get the list of existing backups
    if (!fileUtils.isSdCardAvailable()) {
      showToast(R.string.io_no_external_storage_found);
      return;
    }

    String dirName = fileUtils.buildExternalDirectoryPath(BACKUPS_SUBDIR);
    final File backupDir = new File(dirName);
    if (!backupDir.isDirectory()) {
      showToast(R.string.no_backups);
      return;
    }

    final String[] backupFiles = getAvailableBackups(backupDir);

    if (backupFiles == null || backupFiles.length == 0) {
      showToast(R.string.no_backups);
      return;
    }

    // Show a confirmation dialog
    Builder confirmationDialogBuilder = new AlertDialog.Builder(context);
    confirmationDialogBuilder.setMessage(R.string.restore_overwrites_warning);
    confirmationDialogBuilder.setCancelable(false);
    confirmationDialogBuilder.setNegativeButton(android.R.string.no, null);
    confirmationDialogBuilder.setPositiveButton(android.R.string.yes, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        restoreFromFileListConfirmed(backupDir, backupFiles);
      }
    });
    confirmationDialogBuilder.create().show();
  }

  /**
   * Shows a backup list for the user to pick, then restores it.
   *
   * @param backupDir the backup directory
   * @param backupFiles the list of available backup files
   */
  private void restoreFromFileListConfirmed(final File backupDir, final String[] backupFiles) {
    if (backupFiles.length == 1) {
      // Only one choice, don't bother showing the list
      File inputFile = new File(backupDir, backupFiles[0]);
      restoreFromFileAsync(inputFile);
      return;
    }

    // Make a user-visible version of the backup filenames
    final String backupFileDates[] = new String[backupFiles.length];
    for (int i = 0; i < backupFiles.length; i++) {
      try {
        Date backupDate = BACKUP_FILENAME_FORMAT.parse(backupFiles[i]);
        backupFileDates[i] = DISPLAY_BACKUP_FORMAT.format(backupDate);
      } catch (ParseException e) {
        throw new IllegalStateException("All filenames should be good here");
      }
    }
    Arrays.sort(backupFileDates, REVERSE_STRING_COMPARATOR);

    // Show a dialog for the user to pick which backup to restore
    Builder dialogBuilder = new AlertDialog.Builder(context);
    dialogBuilder.setCancelable(true);
    dialogBuilder.setTitle(R.string.select_backup_to_restore);
    dialogBuilder.setItems(backupFileDates, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        // User picked to restore this one
        final String fileName = backupFiles[which];
        final File inputFile = new File(backupDir, fileName);

        restoreFromFileAsync(inputFile);
      }
    });
    dialogBuilder.create().show();
  }

  /**
   * Shows a progress dialog, then starts restoring the backup osynchronously.
   *
   * @param inputFile the file to restore from
   */
  private void restoreFromFileAsync(final File inputFile) {
    // Show a progress dialog
    final ProgressDialog progressDialog = ProgressDialog.show(
        context,
        context.getString(R.string.progress_title),
        context.getString(R.string.backup_import_progress_message),
        true);

    // Do the actual importing in another thread (don't block the UI)
    new Thread() {
      @Override
      public void run() {
        try {
          Log.d(MyTracksConstants.TAG, "Restoring from file " + inputFile.getAbsolutePath());
          restoreFromFile(inputFile);
          showToast(R.string.io_read_finished);
        } catch (IOException e) {
          showToast(R.string.io_read_failed);
        } finally {
          dismissDialog(progressDialog);
        }
      }
    }.start();
  }

  /**
   * Returns a list of available backups to be restored.
   */
  public String[] getAvailableBackups(File dir) {
    return dir.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String filename) {
        try {
          BACKUP_FILENAME_FORMAT.parse(filename);
          return true;
        } catch (ParseException e) {
          return false;
        }
      }
    });
  }

  /**
   * Shows a toast with the given contents.
   */
  private void showToast(final int resId) {
    uiThreadHandler.post(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show();
      }
    });
  }

  /**
   * Safely dismisses the given dialog.
   */
  private void dismissDialog(final Dialog dialog) {
    uiThreadHandler.post(new Runnable() {
      @Override
      public void run() {
        dialog.dismiss();
      }
    });
  }

  /**
   * Synchronously writes a backup to the given file.
   */
  private void writeToFile(File outputFile) throws IOException {
    // Create all the auxiliary classes that will do the writing
    PreferenceBackupHelper preferencesHelper = new PreferenceBackupHelper();
    DatabaseDumper trackDumper = new DatabaseDumper(
        TracksColumns.BACKUP_COLUMNS,
        TracksColumns.BACKUP_COLUMN_TYPES,
        false);
    DatabaseDumper waypointDumper = new DatabaseDumper(
        WaypointsColumns.BACKUP_COLUMNS,
        WaypointsColumns.BACKUP_COLUMN_TYPES,
        false);
    DatabaseDumper pointDumper = new DatabaseDumper(
        TrackPointsColumns.BACKUP_COLUMNS,
        TrackPointsColumns.BACKUP_COLUMN_TYPES,
        false);

    // Open the target for writing
    FileOutputStream outputStream = new FileOutputStream(outputFile);
    DataOutputStream outWriter = new DataOutputStream(outputStream);

    try {
      // Output a version header
      outWriter.writeInt(BACKUP_FILE_VERSION);

      // Dump preferences
      SharedPreferences preferences =
          context.getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
      preferencesHelper.exportPreferences(preferences, outWriter);

      // Dump the entire contents of each table
      ContentResolver contentResolver = context.getContentResolver();
      Cursor tracksCursor = contentResolver.query(
          TracksColumns.CONTENT_URI, null, null, null, null);
      try {
        trackDumper.writeAllRows(tracksCursor, outWriter);
      } finally {
        tracksCursor.close();
      }

      Cursor waypointsCursor = contentResolver.query(
          WaypointsColumns.CONTENT_URI, null, null, null, null);
      try {
        waypointDumper.writeAllRows(waypointsCursor, outWriter);
      } finally {
        waypointsCursor.close();
      }

      Cursor pointsCursor = contentResolver.query(
          TrackPointsColumns.CONTENT_URI, null, null, null, null);
      try {
        pointDumper.writeAllRows(pointsCursor, outWriter);
      } finally {
        pointsCursor.close();
      }
    } finally {
      outputStream.flush();
      outputStream.close();
    }
  }

  /**
   * Synchronously restores the backup from the given file.
   */
  private void restoreFromFile(File inputFile) throws IOException {
    // TODO: At this point we should stop recording if we were

    PreferenceBackupHelper preferencesHelper = new PreferenceBackupHelper();
    ContentResolver resolver = context.getContentResolver();
    DatabaseImporter trackImporter = new DatabaseImporter(TracksColumns.CONTENT_URI, resolver, false);
    DatabaseImporter waypointImporter = new DatabaseImporter(WaypointsColumns.CONTENT_URI, resolver, false);
    DatabaseImporter pointImporter = new DatabaseImporter(TrackPointsColumns.CONTENT_URI, resolver, false);

    FileInputStream inputStream = new FileInputStream(inputFile);
    DataInputStream reader = new DataInputStream(inputStream);

    try {
      int backupVersion = reader.readInt();
      if (backupVersion != BACKUP_FILE_VERSION) {
        throw new IOException("Unknown backup file version " + backupVersion);
      }

      // Restore preferences
      SharedPreferences preferences =
          context.getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
      preferencesHelper.importPreferences(reader, preferences);

      // Delete all previous contents of the tables.
      resolver.delete(TracksColumns.CONTENT_URI, null, null);
      resolver.delete(TrackPointsColumns.CONTENT_URI, null, null);
      resolver.delete(WaypointsColumns.CONTENT_URI, null, null);

      // Import the new contents of each table
      trackImporter.importAllRows(reader);
      waypointImporter.importAllRows(reader);
      pointImporter.importAllRows(reader);
    } finally {
      inputStream.close();
    }
  }
}
