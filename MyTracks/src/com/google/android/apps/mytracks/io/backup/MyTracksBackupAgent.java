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
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackPointsColumns;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.io.backup.BackupStateManager.TrackState;
import com.google.android.maps.mytracks.R;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Backup agent used to backup and restore all preferences and tracks.
 *
 * @author Rodrigo Damazio
 */
public class MyTracksBackupAgent extends BackupAgent {
  private static final String PREFERENCES_ENTITY = "prefs";

  private static final String TRACK_ENTITY_PREFIX = "track_";

  /**
   * Initial size of the buffer used to back up a track.
   */
  private static final int INITIAL_BUFFER_SIZE = 4096;

  /**
   * Size of the buffer after which it's flushed into the backup.
   */
  private static final int MAX_BUFFER_SIZE = 32768;

  /**
   * Utilities for accessing the content provider.
   */
  private MyTracksProviderUtils providerUtils;

  // Database dumpers
  protected DatabaseDumper trackDumper;
  protected DatabaseDumper waypointDumper;
  protected DatabaseDumper pointDumper;

  // Database importers
  protected DatabaseImporter trackImporter;
  protected DatabaseImporter waypointImporter;
  protected DatabaseImporter pointImporter;

  @Override
  public void onCreate() {
    providerUtils = MyTracksProviderUtils.Factory.get(this);
  }

  @Override
  public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
      ParcelFileDescriptor newState) throws IOException {
    SharedPreferences preferences = this.getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (!preferences.getBoolean(getString(R.string.backup_to_cloud_key), true)) {
      Log.i(MyTracksConstants.TAG, "Cloud backup disabled - not doing it.");
      return;
    }

    // TODO: Trigger backup on: track stop, preference change, track edit
    // TODO: How to handle backup while recording? (track may still change)
    BackupStateManager backupStateManager = createStateManager(oldState, newState);
    BackupStateManager stateManager = backupStateManager;

    backupPreferences(data, preferences);
    backupTracks(data, stateManager);

    stateManager.flushNewState();
  }

  protected BackupStateManager createStateManager(ParcelFileDescriptor oldState,
      ParcelFileDescriptor newState) {
    return new BackupStateManager(oldState, newState);
  }

  private void backupPreferences(BackupDataOutput data, SharedPreferences preferences) throws IOException {
    PreferenceBackupHelper preferenceDumper = createPreferenceBackupHelper();
    byte[] dumpedContents = preferenceDumper.exportPreferences(preferences);
    data.writeEntityHeader(PREFERENCES_ENTITY, dumpedContents.length);
    data.writeEntityData(dumpedContents, dumpedContents.length);
  }

  protected PreferenceBackupHelper createPreferenceBackupHelper() {
    return new PreferenceBackupHelper();
  }

  /**
   * Backs up all the tracks.
   *
   * @param data the backup data to read from
   * @param stateManager the state manager to read from/write to
   * @throws IOException if there are any errors while reading or writing
   */
  private void backupTracks(BackupDataOutput data, BackupStateManager stateManager) throws IOException {
    Cursor tracksCursor = providerUtils.getTracksCursor(null);
    if (!tracksCursor.moveToFirst()) {
      Log.w(MyTracksConstants.TAG, "Nothing to back up");
      return;
    }

    ensureDumpers();

    // For each existing track
    Map<Long, TrackState> oldTrackState = stateManager.getOldTrackState();
    Set<Long> currentTrackIds = new HashSet<Long>(tracksCursor.getCount());
    do {
      // Check if not already backed up
      Track track = providerUtils.createTrack(tracksCursor);
      TrackState oldTrack = oldTrackState.get(track.getId());
      if (oldTrack != null && oldTrack.represents(track)) {
        // Track is already backed up and hasn't been changed
        continue;
      }

      // Back it up
      stateManager.addTrackState(track);
      writeTrack(data, track, tracksCursor);
    } while (tracksCursor.moveToNext());
    tracksCursor.close();

    // For each previously-backed-up track, minus those that currently exist
    // (=only those that no longer exist)
    Set<Long> deletedTrackIds = new HashSet<Long>(oldTrackState.keySet());
    deletedTrackIds.removeAll(currentTrackIds);
    for (Long trackId : deletedTrackIds) {
      // Delete it from the backup
      data.writeEntityHeader(TRACK_ENTITY_PREFIX + trackId, -1);
    }
  }

  protected void ensureDumpers() {
    if (trackDumper == null) {
      trackDumper =
          new DatabaseDumper(
              TracksColumns.BACKUP_COLUMNS,
              TracksColumns.BACKUP_COLUMN_TYPES,
              false);
      waypointDumper =
          new DatabaseDumper(
              WaypointsColumns.BACKUP_COLUMNS,
              WaypointsColumns.BACKUP_COLUMN_TYPES,
              false);
      pointDumper =
          new DatabaseDumper(
              TrackPointsColumns.BACKUP_COLUMNS,
              TrackPointsColumns.BACKUP_COLUMN_TYPES,
              true);  // Keep the size of each entry constant
    }
  }

  /**
   * Writes a single track to the backup.
   *
   * @param data the backup data to write to
   * @param track the track to write
   * @param tracksCursor the tracks database cursor pointing to the given track
   * @throws IOException if there are any problems while writing
   */
  private void writeTrack(BackupDataOutput data, Track track, Cursor tracksCursor) throws IOException {
    // Write the header
    ByteArrayOutputStream bufStream = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
    DataOutputStream outWriter = new DataOutputStream(bufStream);

    // Write track metadata
    trackDumper.writeHeaders(tracksCursor, 1, outWriter);
    trackDumper.writeOneRow(tracksCursor, outWriter);

    // Write waypoints
    Cursor waypointsCursor = providerUtils.getWaypointsCursor(track.getId(), -1, -1);
    waypointDumper.writeAllRows(waypointsCursor, outWriter);
    waypointsCursor.close();

    // Read points from the database
    Cursor pointsCursor = providerUtils.getLocationsCursor(track.getId(), 0, -1, false);

    // Write the header for the points
    int numPoints = pointsCursor.getCount();
    pointDumper.writeHeaders(pointsCursor, numPoints, outWriter);

    // Write the first point to the output and calculate its size
    // Since all points have the same size in the output, we can finally predict
    // the total size of the track, and write the backup header.
    int oldSize = bufStream.size();
    pointDumper.writeOneRow(pointsCursor, outWriter);
    int newSize = bufStream.size();
    int trackPointsSize = numPoints * (newSize - oldSize);
    int totalSize = oldSize + trackPointsSize;

    // Write out the backup header
    data.writeEntityHeader(TRACK_ENTITY_PREFIX + track.getId(), totalSize);

    // Write the rest of the points, in chunks
    while (pointsCursor.moveToNext()) {
      if (bufStream.size() >= MAX_BUFFER_SIZE) {
        outWriter.flush();
        data.writeEntityData(bufStream.toByteArray(), bufStream.size());
        bufStream.reset();
      }

      pointDumper.writeOneRow(pointsCursor, outWriter);
    }
    pointsCursor.close();

    // Do the last flushing of the buffer
    outWriter.flush();
    data.writeEntityData(bufStream.toByteArray(), bufStream.size());
  }

  @Override
  public void onRestore(BackupDataInput data, int appVersionCode,
      ParcelFileDescriptor newState) throws IOException {
    BackupStateManager stateManager = new BackupStateManager(null, newState);
    providerUtils.deleteAllTracks();

    ensureImporters();

    while (data.readNextHeader()) {
      String key = data.getKey();
      if (key.startsWith(TRACK_ENTITY_PREFIX)) {
        Track restoredTrack = restoreTrack(data);
        stateManager.addTrackState(restoredTrack);
      } else if (key.startsWith(PREFERENCES_ENTITY)) {
        restorePreferences(data);
      } else {
        Log.e(MyTracksConstants.TAG, "Found unknown backup entity: " + key);
        data.skipEntityData();
      }
    }

    stateManager.flushNewState();
  }

  protected void ensureImporters() {
    if (trackImporter == null) {
      ContentResolver resolver = this.getContentResolver();
      trackImporter =
          new DatabaseImporter(TracksColumns.CONTENT_URI, resolver, false);
      waypointImporter =
          new DatabaseImporter(WaypointsColumns.CONTENT_URI, resolver, false);
      pointImporter =
          new DatabaseImporter(TrackPointsColumns.CONTENT_URI, resolver, true);
    }
  }

  /**
   * Restores all preferences from the backup.
   *
   * @param data the backup data to read from
   * @throws IOException if there are any errors while reading
   */
  private void restorePreferences(BackupDataInput data) throws IOException {
    int dataSize = data.getDataSize();
    byte[] dataBuffer = new byte[dataSize];
    int read = data.readEntityData(dataBuffer, 0, dataSize);
    if (read != dataSize) {
      throw new IOException("Failed to read all the preferences data");
    }

    SharedPreferences preferences = this.getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    PreferenceBackupHelper importer = createPreferenceBackupHelper();
    importer.importPreferences(dataBuffer, preferences);
  }

  /**
   * Restores a single track from the backup.
   *
   * @param data the data to restore from
   * @return the restored track
   * @throws IOException
   */
  private Track restoreTrack(BackupDataInput data) throws IOException {
    BackupDataInputStream inputStream = new BackupDataInputStream(data);
    DataInputStream reader = new DataInputStream(inputStream);

    trackImporter.importAllRows(reader);
    waypointImporter.importAllRows(reader);
    pointImporter.importAllRows(reader);

    // Get the expected track ID from the entity key
    String key = data.getKey();
    int splitPos = key.lastIndexOf('_');
    String trackIdStr = key.substring(splitPos + 1);
    long trackId = Long.parseLong(trackIdStr);

    return providerUtils.getTrack(trackId);
  }
}
