/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.mytracks.io.sync;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.file.exporter.KmzTrackExporter;
import com.google.android.apps.mytracks.io.file.importer.KmlFileTrackImporter;
import com.google.android.apps.mytracks.io.file.importer.KmzTrackImporter;
import com.google.android.apps.mytracks.io.file.importer.TrackImporter;
import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.maps.mytracks.R;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Changes;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * SyncAdapter to sync tracks with Google Drive.
 * 
 * @author Jimmy Shih
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

  private static final String TAG = SyncAdapter.class.getSimpleName();

  // drive.about.get fields. Contains one field, largestChangeId
  private static final String ABOUT_GET_FIELDS = "largestChangeId";

  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;
  private Drive drive;
  private String driveAccountName; // the account name associated with the drive
  private String folderId;

  public SyncAdapter(Context context) {
    super(context, true);
    this.context = context;
    this.myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
      ContentProviderClient provider, SyncResult syncResult) {
    if (!PreferencesUtils.getBoolean(
        context, R.string.drive_sync_key, PreferencesUtils.DRIVE_SYNC_DEFAULT)) {
      return;
    }

    if (account == null) {
      return;
    }

    String googleAccount = PreferencesUtils.getString(
        context, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    if (googleAccount == null || googleAccount.equals(PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT)) {
      return;
    }

    if (!googleAccount.equals(account.name)) {
      return;
    }

    try {
      GoogleAccountCredential credential = SendToGoogleUtils.getGoogleAccountCredential(
          context, account.name, SendToGoogleUtils.DRIVE_SCOPE);
      if (credential == null) {
        return;
      }

      if (drive == null || !driveAccountName.equals(account.name)) {
        drive = SyncUtils.getDriveService(credential);
        driveAccountName = account.name;
      }
      folderId = getFolderId();

      long largestChangeId = PreferencesUtils.getLong(
          context, R.string.drive_largest_change_id_key);
      if (largestChangeId == PreferencesUtils.DRIVE_LARGEST_CHANGE_ID_DEFAULT) {
        performInitialSync();
      } else {
        performIncrementalSync(largestChangeId);
      }
      insertNewDriveFiles();
    } catch (UserRecoverableAuthException e) {
      SendToGoogleUtils.sendNotification(
          context, account.name, e.getIntent(), SendToGoogleUtils.DRIVE_NOTIFICATION_ID);
    } catch (GoogleAuthException e) {
      Log.e(TAG, "GoogleAuthException", e);
    } catch (UserRecoverableAuthIOException e) {
      SendToGoogleUtils.sendNotification(
          context, account.name, e.getIntent(), SendToGoogleUtils.DRIVE_NOTIFICATION_ID);
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
    }
  }

  /**
   * Gets the folder id..
   */
  private String getFolderId() throws IOException {
    File folder = SyncUtils.getMyTracksFolder(context, drive);
    if (folder == null) {
      throw new IOException("folder is null");
    }
    String id = folder.getId();
    if (id == null) {
      throw new IOException("folder id is null");
    }
    return id;
  }

  /**
   * Performs initial sync.
   */
  private void performInitialSync() throws IOException {

    // Get the largest change id first to avoid race conditions
    About about = drive.about().get().setFields(ABOUT_GET_FIELDS).execute();
    long largestChangeId = about.getLargestChangeId();

    // Get all the KML/KMZ files in the "My Drive:/My Tracks" folder
    Files.List myTracksFolderRequest = drive.files()
        .list().setQ(String.format(Locale.US, SyncUtils.MY_TRACKS_FOLDER_FILES_QUERY, folderId));
    Map<String, File> myTracksFolderMap = getFiles(myTracksFolderRequest, true);

    // Handle tracks that are already uploaded to Google Drive
    Set<String> syncedDriveIds = updateSyncedTracks();
    for (String driveId : syncedDriveIds) {
      myTracksFolderMap.remove(driveId);
    }

    // Get all the KML/KMZ files in the "Shared with me:/" folder
    Files.List sharedWithMeRequest = drive.files()
        .list().setQ(SyncUtils.SHARED_WITH_ME_FILES_QUERY);
    Map<String, File> sharedWithMeMap = getFiles(sharedWithMeRequest, false);

    try {
      insertNewTracks(myTracksFolderMap.values());
      insertNewTracks(sharedWithMeMap.values());
      PreferencesUtils.setLong(context, R.string.drive_largest_change_id_key, largestChangeId);
    } catch (IOException e) {

      // Remove all imported tracks
      Cursor cursor = null;
      try {
        cursor = myTracksProviderUtils.getTrackCursor(SyncUtils.DRIVE_ID_TRACKS_QUERY, null, null);
        if (cursor != null && cursor.moveToFirst()) {
          do {
            Track track = myTracksProviderUtils.createTrack(cursor);
            if (!syncedDriveIds.contains(track.getDriveId())) {
              myTracksProviderUtils.deleteTrack(context, track.getId());
            }
          } while (cursor.moveToNext());
        }
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
      throw e;
    }
  }

  /**
   * Updates synced tracks.
   * 
   * @return drive ids of the synced tracks
   */
  private Set<String> updateSyncedTracks() throws IOException {
    Set<String> result = new HashSet<String>();
    Cursor cursor = null;
    try {
      cursor = myTracksProviderUtils.getTrackCursor(SyncUtils.DRIVE_ID_TRACKS_QUERY, null, null);
      if (cursor != null && cursor.moveToFirst()) {
        do {
          Track track = myTracksProviderUtils.createTrack(cursor);
          String driveId = track.getDriveId();
          if (driveId != null && !driveId.equals("")) {
            if (!track.isSharedWithMe()) {
              File driveFile = drive.files().get(driveId).execute();
              if (SyncUtils.isInMyTracksAndValid(driveFile, folderId)) {
                merge(track, driveFile);
                result.add(driveId);
              } else {
                /*
                 * Track has a drive id, but the drive id is no longer valid.
                 * E.g., the file is moved to another folder. Clear the drive
                 * id.
                 */
                SyncUtils.updateTrack(myTracksProviderUtils, track, null);
              }
            }
          }
        } while (cursor.moveToNext());
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return result;
  }

  /**
   * Performs incremental sync.
   * 
   * @param largestChangeId the largest change id
   */
  private void performIncrementalSync(long largestChangeId) throws IOException {

    // Handle deleted tracks
    String driveDeletedList = PreferencesUtils.getString(
        context, R.string.drive_deleted_list_key, PreferencesUtils.DRIVE_DELETED_LIST_DEFAULT);
    if (!PreferencesUtils.DRIVE_DELETED_LIST_DEFAULT.equals(driveDeletedList)) {
      String deletedIds[] = TextUtils.split(driveDeletedList, ";");
      for (String driveId : deletedIds) {
        deleteDriveFile(driveId, true);
      }
      PreferencesUtils.setString(
          context, R.string.drive_deleted_list_key, PreferencesUtils.DRIVE_DELETED_LIST_DEFAULT);
    }

    // Handle edited tracks
    String driveEditedList = PreferencesUtils.getString(
        context, R.string.drive_edited_list_key, PreferencesUtils.DRIVE_EDITED_LIST_DEFAULT);
    if (!PreferencesUtils.DRIVE_EDITED_LIST_DEFAULT.equals(driveEditedList)) {
      String editedIds[] = TextUtils.split(driveEditedList, ";");
      for (String id : editedIds) {
        Track track = myTracksProviderUtils.getTrack(Long.valueOf(id));
        if (track == null) {
          continue;
        }
        if (track.isSharedWithMe()) {
          continue;
        }
        String driveId = track.getDriveId();
        if (driveId == null || driveId.equals("")) {
          continue;
        }
        File driveFile = drive.files().get(driveId).execute();
        if (SyncUtils.isInMyTracksAndValid(driveFile, folderId)) {
          merge(track, driveFile);
        }
      }
      PreferencesUtils.setString(
          context, R.string.drive_edited_list_key, PreferencesUtils.DRIVE_EDITED_LIST_DEFAULT);
    }

    // Handle changes from Google Drive
    Map<String, File> changes = new HashMap<String, File>();
    long newLargestChangeId = getDriveChangesInfo(largestChangeId, changes);
    if (newLargestChangeId != largestChangeId) {
      Cursor cursor = null;
      try {
        // Get all the local tracks with drive file id
        cursor = myTracksProviderUtils.getTrackCursor(SyncUtils.DRIVE_ID_TRACKS_QUERY, null, null);
        if (cursor != null && cursor.moveToFirst()) {
          do {
            Track track = myTracksProviderUtils.createTrack(cursor);
            String driveId = track.getDriveId();

            if (changes.containsKey(driveId)) {

              // Track has changed
              File driveFile = changes.get(driveId);
              if (driveFile == null) {
                Log.d(TAG, "Delete local track " + track.getName());
                myTracksProviderUtils.deleteTrack(context, track.getId());
              } else {
                if (SyncUtils.isInMyTracksAndValid(driveFile, folderId)
                    || SyncUtils.isInSharedWithMe(driveFile)) {
                  merge(track, driveFile);
                } else {
                  SyncUtils.updateTrack(myTracksProviderUtils, track, null);
                }
              }
              changes.remove(driveId);
            }
          } while (cursor.moveToNext());
        }

        // Insert valid new drive file changes as new tracks
        Iterator<String> iterator = changes.keySet().iterator();

        while (iterator.hasNext()) {
          String driveId = iterator.next();
          File file = changes.get(driveId);
          if (!SyncUtils.isInMyTracksAndValid(file, folderId)
              && !SyncUtils.isInSharedWithMeAndValid(file)) {
            iterator.remove();
          }
        }

        insertNewTracks(changes.values());
        PreferencesUtils.setLong(context, R.string.drive_largest_change_id_key, newLargestChangeId);
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
    }
  }

  /**
   * Inserts new drive files from tracks without a drive id.
   */
  private void insertNewDriveFiles() throws IOException {
    Cursor cursor = null;
    try {
      cursor = myTracksProviderUtils.getTrackCursor(SyncUtils.NO_DRIVE_ID_TRACKS_QUERY, null, null);
      long recordingTrackId = PreferencesUtils.getLong(context, R.string.recording_track_id_key);

      if (cursor != null && cursor.moveToFirst()) {
        do {
          Track track = myTracksProviderUtils.createTrack(cursor);
          if (track.getId() == recordingTrackId) {
            continue;
          }
          // If not successful, the next sync will retry again
          SyncUtils.insertDriveFile(
              drive, folderId, context, myTracksProviderUtils, track, true, true);
        } while (cursor.moveToNext());
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * Inserts new tracks from a collection of drive files.
   * 
   * @param driveFiles the drive files
   */
  private void insertNewTracks(Collection<File> driveFiles) throws IOException {
    for (File driveFile : driveFiles) {
      if (driveFile == null) {
        return;
      }
      updateTrack(-1L, driveFile);
    }
  }

  /**
   * Gets all the files from a request.
   * 
   * @param request the request
   * @param excludeSharedWithMe true to exclude shared with me files
   * @return a map of file id to file
   */
  private Map<String, File> getFiles(Files.List request, boolean excludeSharedWithMe)
      throws IOException {
    Map<String, File> idToFileMap = new HashMap<String, File>();
    do {
      FileList files = request.execute();

      for (File file : files.getItems()) {
        if (excludeSharedWithMe && file.getSharedWithMeDate() != null) {
          continue;
        }
        idToFileMap.put(file.getId(), file);
      }
      request.setPageToken(files.getNextPageToken());
    } while (request.getPageToken() != null && request.getPageToken().length() > 0);
    return idToFileMap;
  }

  /**
   * Gets the drive changes info in the My Tracks folder, including deleted
   * files.
   * 
   * @param changeId the largest change id
   * @param changes a map of drive id to file for the changes
   * @return an updated largest change id
   */
  private long getDriveChangesInfo(long changeId, Map<String, File> changes) throws IOException {
    Changes.List request = drive.changes().list().setStartChangeId(changeId + 1);
    do {
      ChangeList changeList = request.execute();
      long newId = changeList.getLargestChangeId().longValue();

      for (Change change : changeList.getItems()) {
        if (change.getDeleted()) {
          changes.put(change.getFileId(), null);
        } else {
          File file = change.getFile();
          if (file.getLabels().getTrashed()) {
            changes.put(change.getFileId(), null);
          } else {
            changes.put(change.getFileId(), file);
          }
        }
      }
      if (newId > changeId) {
        changeId = newId;
      }
      request.setPageToken(changeList.getNextPageToken());
    } while (request.getPageToken() != null && request.getPageToken().length() > 0);
    Log.d(TAG, "Got drive changes: " + changes.size() + " " + changeId);
    return changeId;
  }

  /**
   * Merges a track with a drive file.
   * 
   * @param track the track
   * @param driveFile the drive file
   */
  private void merge(Track track, File driveFile) throws IOException {
    long modifiedTime = track.getModifiedTime();
    long driveModifiedTime = driveFile.getModifiedDate().getValue();
    if (modifiedTime > driveModifiedTime) {
      Log.d(TAG, "Updating track change for track " + track.getName() + " and drive file "
          + driveFile.getTitle());
      if (!SyncUtils.updateDriveFile(
          drive, driveFile, context, myTracksProviderUtils, track, true)) {
        Log.e(TAG, "Unable to update drive file");
        track.setModifiedTime(driveModifiedTime);
        myTracksProviderUtils.updateTrack(track);
      }
    } else if (modifiedTime < driveModifiedTime) {
      Log.d(TAG, "Updating drive change for track " + track.getName() + " and drive file "
          + driveFile.getTitle());
      if (!updateTrack(track.getId(), driveFile)) {
        Log.e(TAG, "Unable to update drive change");
        // The track could have been deleted in the unsuccessful update
        track = myTracksProviderUtils.getTrack(track.getId());
        if (track != null) {
          track.setModifiedTime(driveModifiedTime);
          myTracksProviderUtils.updateTrack(track);
        }
      }
    }
  }

  /**
   * Updates a track based on a drive file. Returns true if successful.
   * 
   * @param trackId the track id. -1L to insert a new track
   * @param driveFile the drive file
   */
  private boolean updateTrack(final long trackId, File driveFile) throws IOException {
    Track track = null;
    boolean success = false;
    try {
      track = importDriveFile(trackId, driveFile);
      if (track == null) {
        return false;
      }
      File updatedDriveFile;
      String trackName = FileUtils.getName(driveFile.getTitle());
      if (SyncUtils.isInMyTracks(driveFile, folderId) && !track.getName().equals(trackName)) {
        track.setName(trackName);

        /*
         * The drive file title and the track name inside the drive file do not
         * match, update the drive file.
         */
        java.io.File file = null;
        try {
          file = SyncUtils.getTempFile(context, myTracksProviderUtils, track, true);
          updatedDriveFile = SyncUtils.updateDriveFile(
              drive, driveFile, trackName + "." + KmzTrackExporter.KMZ_EXTENSION, file, true);

          if (updatedDriveFile == null) {
            Log.e(TAG, "Unable to update drive file");
            return false;
          }
        } finally {
          if (file != null) {
            file.delete();
          }
        }
      } else {
        updatedDriveFile = driveFile;
      }

      SyncUtils.updateTrack(myTracksProviderUtils, track, updatedDriveFile);
      success = true;
      return true;
    } finally {
      if (!success) {
        // if the track is new, delete it
        if (trackId == -1L && track != null) {
          myTracksProviderUtils.deleteTrack(context, track.getId());
        }
      }
    }
  }

  /**
   * Imports drive file to track.
   * 
   * @param trackId the track id. -1L to insert a new track
   * @param driveFile the drive file
   */
  private Track importDriveFile(long trackId, File driveFile) throws IOException {
    InputStream inputStream = null;
    try {
      inputStream = downloadDriveFile(driveFile, true);
      if (inputStream == null) {
        Log.e(TAG, "Unable to import drive file. Input stream is null.");
        return null;
      }

      TrackImporter trackImporter;
      boolean useKmz = KmzTrackExporter.KMZ_EXTENSION.equals(driveFile.getFileExtension());
      if (useKmz) {
        if (trackId == -1L) {
          Uri uri = myTracksProviderUtils.insertTrack(new Track());
          trackId = Long.parseLong(uri.getLastPathSegment());
        }
        trackImporter = new KmzTrackImporter(context, trackId);
      } else {
        trackImporter = new KmlFileTrackImporter(context, trackId);
      }

      long importedId = trackImporter.importFile(inputStream);
      if (importedId == -1L) {
        Log.e(TAG, "Unable to import drive file. Imported id is -1L.");
        return null;
      }
      Track track = myTracksProviderUtils.getTrack(importedId);
      if (track == null) {
        Log.e(TAG, "Unable to import drive file. Imported track is null.");
        return null;
      } else {
        return track;
      }
    } catch (IOException e) {
      Log.e(TAG, "Unable to import drive file.", e);
      return null;
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }

  /**
   * Deletes a drive file.
   * 
   * @param driveId the drive id
   * @param canRetry true if can retry the request
   * @throws IOException
   */
  private void deleteDriveFile(String driveId, boolean canRetry) throws IOException {
    try {
      File driveFile = drive.files().get(driveId).execute();
      if (SyncUtils.isInMyTracks(driveFile, folderId)) {
        if (!driveFile.getLabels().getTrashed()) {
          drive.files().trash(driveId).execute();
        }
        // if trashed, ignore
      } else if (SyncUtils.isInSharedWithMe(driveFile)) {
        if (!driveFile.getLabels().getTrashed()) {
          drive.files().delete(driveId).execute();
        }
        // if trashed, ignore
      }
    } catch (UserRecoverableAuthIOException e) {
      throw e;
    } catch (IOException e) {
      if (canRetry) {
        deleteDriveFile(driveId, false);
        return;
      }
      Log.e(TAG, "Unable to delete Drive file for " + driveId, e);
    }
  }

  /**
   * Downloads a drive file.
   * 
   * @param driveFile the drive file
   */
  private InputStream downloadDriveFile(File driveFile, boolean canRetry) throws IOException {
    if (driveFile.getDownloadUrl() == null || driveFile.getDownloadUrl().length() == 0) {
      Log.d(TAG, "Drive file download url doesn't exist: " + driveFile.getTitle());
      return null;
    }
    try {
      HttpResponse httpResponse = drive.getRequestFactory()
          .buildGetRequest(new GenericUrl(driveFile.getDownloadUrl())).execute();
      if (httpResponse == null) {
        Log.e(TAG, "http response is null");
        return null;
      }
      return httpResponse.getContent();
    } catch (UserRecoverableAuthIOException e) {
      throw e;
    } catch (IOException e) {
      if (canRetry) {
        return downloadDriveFile(driveFile, false);
      }
      throw e;
    }
  }
}
