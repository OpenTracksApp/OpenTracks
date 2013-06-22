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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.TrackPointsColumns;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.util.FileUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Handler for writing or reading single-file backups.
 *
 * @author Rodrigo Damazio
 */
class ExternalFileBackup {
  // Filename format - in UTC
  private static final SimpleDateFormat BACKUP_FILENAME_FORMAT = new SimpleDateFormat(
      "'backup-'yyyy-MM-dd_HH-mm-ss'.zip'", Locale.US);
  static {
    BACKUP_FILENAME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private static final int BACKUP_FORMAT_VERSION = 1;
  private static final String ZIP_ENTRY_NAME =
      "backup.mytracks.v" + BACKUP_FORMAT_VERSION;
  private static final int COMPRESSION_LEVEL = 8;

  private final Context context;

  public ExternalFileBackup(Context context) {
    this.context = context;
  }

  /**
   * Returns whether the backups directory is (or can be made) available.
   *
   * @param create whether to try creating the directory if it doesn't exist
   */
  public boolean isBackupsDirectoryAvailable(boolean create) {
    return getBackupsDirectory(create) != null;
  }

  /**
   * Returns the backup directory, or null if not available.
   *
   * @param create whether to try creating the directory if it doesn't exist
   */
  private File getBackupsDirectory(boolean create) {
    String directoryPath = FileUtils.getDirectoryPath(FileUtils.BACKUPS_DIR);
    final File dir = new File(directoryPath);
    Log.d(Constants.TAG, "Dir: " + dir.getAbsolutePath());
    if (create) {
      // Try to create - if that fails, return null
      return FileUtils.ensureDirectoryExists(dir) ? dir : null;
    } else {
      // Return it if it already exists, otherwise return null
      return FileUtils.isDirectory(dir) ? dir : null;
    }
  }

  /**
   * Returns a list of available backups to be restored.
   */
  public Date[] getAvailableBackups() {
    File dir = getBackupsDirectory(false);
    if (dir == null) { return null; }
    String[] fileNames = dir.list();

    List<Date> backupDates = new ArrayList<Date>(fileNames.length);
    for (int i = 0; i < fileNames.length; i++) {
      String fileName = fileNames[i];
      try {
        backupDates.add(BACKUP_FILENAME_FORMAT.parse(fileName));
      } catch (ParseException e) {
        // Not a backup file, ignore
      }
    }

    return backupDates.toArray(new Date[backupDates.size()]);
  }

  /**
   * Writes the backup to the default file.
   */
  public void writeToDefaultFile() throws IOException {
    writeToFile(getFileForDate(new Date()));
  }

  /**
   * Restores the backup from the given date.
   */
  public void restoreFromDate(Date when) throws IOException {
    restoreFromFile(getFileForDate(when));
  }

  /**
   * Produces the proper file descriptor for the given backup date.
   */
  private File getFileForDate(Date when) {
    File dir = getBackupsDirectory(false);
    String fileName = BACKUP_FILENAME_FORMAT.format(when);
    File file = new File(dir, fileName);
    return file;
  }

  /**
   * Synchronously writes a backup to the given file.
   */
  private void writeToFile(File outputFile) throws IOException {
    Log.d(Constants.TAG,
        "Writing backup to file " + outputFile.getAbsolutePath());

    // Create all the auxiliary classes that will do the writing
    PreferenceBackupHelper preferencesHelper = new PreferenceBackupHelper(context);
    DatabaseDumper trackDumper = new DatabaseDumper(
        TracksColumns.COLUMNS,
        TracksColumns.COLUMN_TYPES,
        false);
    DatabaseDumper waypointDumper = new DatabaseDumper(
        WaypointsColumns.COLUMNS,
        WaypointsColumns.COLUMN_TYPES,
        false);
    DatabaseDumper pointDumper = new DatabaseDumper(
        TrackPointsColumns.COLUMNS,
        TrackPointsColumns.COLUMN_TYPES,
        false);

    // Open the target for writing
    FileOutputStream outputStream = new FileOutputStream(outputFile);
    ZipOutputStream compressedStream = new ZipOutputStream(outputStream);
    compressedStream.setLevel(COMPRESSION_LEVEL);
    compressedStream.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
    DataOutputStream outWriter = new DataOutputStream(compressedStream);

    try {
      // Dump the entire contents of each table
      ContentResolver contentResolver = context.getContentResolver();
      Cursor tracksCursor = null;
      try {
        tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, null);
        if (tracksCursor != null) {
          trackDumper.writeAllRows(tracksCursor, outWriter);
        }
      } finally {
        if (tracksCursor != null) {
          tracksCursor.close();
        }
      }

      Cursor waypointsCursor = null;
      try {
        waypointsCursor = contentResolver.query(
            WaypointsColumns.CONTENT_URI, null, null, null, null);
        if (waypointsCursor != null) {
          waypointDumper.writeAllRows(waypointsCursor, outWriter);
        }
      } finally {
        if (waypointsCursor != null) {
          waypointsCursor.close();
        }
      }

      Cursor pointsCursor = null;
      try {
        pointsCursor = contentResolver.query(
            TrackPointsColumns.CONTENT_URI, null, null, null, null);
        if (pointsCursor != null) {
          pointDumper.writeAllRows(pointsCursor, outWriter);
        }
      } finally {
        if (pointsCursor != null) {
          pointsCursor.close();
        }
      }

      // Dump preferences
      SharedPreferences preferences = context.getSharedPreferences(
          Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
      preferencesHelper.exportPreferences(preferences, outWriter);
    } catch (IOException e) {
      // We tried to delete the partially created file, but do nothing
      // if that also fails.
      if (!outputFile.delete()) {
        Log.w(TAG, "Failed to delete file " + outputFile.getAbsolutePath());
      }

      throw e;
    } finally {
      compressedStream.closeEntry();
      compressedStream.close();
    }
  }

  /**
   * Synchronously restores the backup from the given file.
   */
  private void restoreFromFile(File inputFile) throws IOException {
    Log.d(Constants.TAG,
        "Restoring from file " + inputFile.getAbsolutePath());

    PreferenceBackupHelper preferencesHelper = new PreferenceBackupHelper(context);
    ContentResolver resolver = context.getContentResolver();
    DatabaseImporter trackImporter =
        new DatabaseImporter(TracksColumns.CONTENT_URI, resolver, false);
    DatabaseImporter waypointImporter =
        new DatabaseImporter(WaypointsColumns.CONTENT_URI, resolver, false);
    DatabaseImporter pointImporter =
        new DatabaseImporter(TrackPointsColumns.CONTENT_URI, resolver, false);

    ZipFile zipFile = new ZipFile(inputFile, ZipFile.OPEN_READ);
    ZipEntry zipEntry = zipFile.getEntry(ZIP_ENTRY_NAME);
    if (zipEntry == null) {
      throw new IOException("Invalid backup ZIP file");
    }

    InputStream compressedStream = zipFile.getInputStream(zipEntry);
    DataInputStream reader = new DataInputStream(compressedStream);

    try {
      // Delete all previous contents of the tables and preferences.
      resolver.delete(TracksColumns.CONTENT_URI, null, null);
      resolver.delete(TrackPointsColumns.CONTENT_URI, null, null);
      resolver.delete(WaypointsColumns.CONTENT_URI, null, null);

      // Import the new contents of each table
      trackImporter.importAllRows(reader);
      waypointImporter.importAllRows(reader);
      pointImporter.importAllRows(reader);

      // Restore preferences
      SharedPreferences preferences = context.getSharedPreferences(
          Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
      preferencesHelper.importPreferences(reader, preferences);
    } finally {
      compressedStream.close();
      zipFile.close();
    }
  }
}
