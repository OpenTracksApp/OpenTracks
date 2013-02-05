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
import com.google.android.apps.mytracks.io.file.KmlImporter;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
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
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SyncAdapter to sync tracks with Google Drive.
 * 
 * @author Jimmy Shih
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

  private static final String TAG = SyncAdapter.class.getSimpleName();

  // drive.about.get fields to get the largestChangeId
  private static final String ABOUT_GET_FIELDS = "largestChangeId";

  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;
  private Drive drive;
  private String driveAccountName; // the account name associated with the drive

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

    GoogleAccountCredential credential = SyncUtils.getCredential(context, account.name);
    if (credential == null) {
      return;
    }

    if (drive == null || !driveAccountName.equals(account.name)) {
      drive = SyncUtils.getDriveService(credential);
      driveAccountName = account.name;
    }

    String folderId = SyncUtils.getMyTracksFolder(context, drive);
    if (folderId == null) {
      return;
    }

    try {
      long largestChangeId = PreferencesUtils.getLong(
          context, R.string.drive_largest_change_id_key);
      if (largestChangeId == PreferencesUtils.DRIVE_LARGEST_CHANGE_ID_DEFAULT) {
        performInitialSync(folderId);
      } else {
        performIncrementalSync(folderId, largestChangeId);
      }
      insertNewTracks(folderId);
    } catch (Exception e) {
      Log.e(TAG, "Exception", e);
    }
  }

  /**
   * Performs initial sync.
   * 
   * @param folderId the folder id
   */
  private void performInitialSync(String folderId) throws Exception {

    // Get the largest change id first to avoid race conditions
    About about = drive.about().get().setFields(ABOUT_GET_FIELDS).execute();
    long largestChangeId = about.getLargestChangeId();

    // Get all the KML files in the "My Drive:/My Tracks" folder
    Files.List request = drive.files().list()
        .setQ(String.format(Locale.US, SyncUtils.MY_TRACKS_FOLDER_FILES_QUERY, folderId));
    Map<String, File> idToFileMap = getFiles(request);
    insertNewDriveFiles(idToFileMap.values());

    // Get all the KML files in the "Shared with me:/" folder
    request = drive.files().list().setQ(SyncUtils.SHARED_WITH_ME_FILES_QUERY);
    idToFileMap = getFiles(request);
    insertNewDriveFiles(idToFileMap.values());

    PreferencesUtils.setLong(context, R.string.drive_largest_change_id_key, largestChangeId);
  }

  /**
   * Performs incremental sync.
   * 
   * @param folderId the folder id
   * @param largestChangeId the largest change id
   */
  private void performIncrementalSync(String folderId, long largestChangeId) throws Exception {

    // Move to trash in Drive all deleted tracks
    String driveDeletedList = PreferencesUtils.getString(
        context, R.string.drive_deleted_list_key, PreferencesUtils.DRIVE_DELETED_LIST_DEFAULT);
    String deletedIds[] = TextUtils.split(driveDeletedList, ";");
    for (String id : deletedIds) {
      try {
        File driveFile = drive.files().get(id).execute();
        if (SyncUtils.isInFolder(driveFile, folderId)) {
          if (!driveFile.getLabels().getTrashed()) {
            drive.files().trash(id).execute();
          }
          // if trashed, ignore
        } else if (SyncUtils.isSharedWithMe(driveFile)) {
          drive.files().delete(id).execute();
        }
      } catch (IOException e) {
        // safe to ignore
      }
    }
    PreferencesUtils.setString(
        context, R.string.drive_deleted_list_key, PreferencesUtils.DRIVE_DELETED_LIST_DEFAULT);

    Map<String, File> changes = new HashMap<String, File>();
    largestChangeId = getDriveChanges(folderId, largestChangeId, changes);

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
              myTracksProviderUtils.deleteTrack(track.getId());
            } else {
              mergeFiles(track, driveFile);
            }
            changes.remove(driveId);
          } else {
            if (!track.isSharedWithMe()) {

              // Handle the case the track has changed
              File driveFile = drive.files().get(driveId).execute();
              if (SyncUtils.isValid(driveFile, folderId)) {
                mergeFiles(track, driveFile);
              } else {
                /*
                 * Track has a drive id, but the drive id is no longer valid.
                 * E.g., the file is moved to another folder. Clear the drive
                 * id.
                 */
                track.setDriveId("");
                track.setModifiedTime(-1L);
                myTracksProviderUtils.updateTrack(track);
              }
            }
          }
        } while (cursor.moveToNext());
      }

      // Handle new drive files
      insertNewDriveFiles(changes.values());
      PreferencesUtils.setLong(context, R.string.drive_largest_change_id_key, largestChangeId);
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
    } catch (RemoteException e) {
      Log.e(TAG, "RemoteException", e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * Inserts new tracks.
   * 
   * @param folderId the folder id
   */
  private void insertNewTracks(String folderId) throws IOException {
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
          // Note, will retry on the next sync if unable to add drive file
          SyncUtils.addDriveFile(context, myTracksProviderUtils, drive, folderId, track);
        } while (cursor.moveToNext());
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * Inserts new Google Drive files.
   * 
   * @param driveFiles a collection of drive files to insert
   */
  private void insertNewDriveFiles(Collection<File> driveFiles) throws Exception {
    for (File driveFile : driveFiles) {
      if (driveFile != null) {
        InputStream inputStream = downloadDriveFile(driveFile);
        // TODO: should retry if inputStream is null
        if (inputStream != null) {
          KmlImporter kmlImporter = new KmlImporter(context, -1L);
          long[] tracksIds = kmlImporter.importFile(inputStream);
          if (tracksIds.length == 1) {
            Track track = myTracksProviderUtils.getTrack(tracksIds[0]);
            track.setDriveId(driveFile.getId());
            track.setModifiedTime(driveFile.getModifiedDate().getValue());
            track.setSharedWithMe(driveFile.getSharedWithMeDate() != null);
            myTracksProviderUtils.updateTrack(track);
            Log.d(TAG, "Add from Google Drive " + track.getName());
          }
          // Ignore if tracksId.length != 1
        }
      }
    }
  }

  /**
   * Gets all the files from a request.
   * 
   * @param request the request
   * @return a map of file id to file
   */
  private Map<String, File> getFiles(Files.List request) {
    Map<String, File> idToFileMap = new HashMap<String, File>();
    do {
      try {
        FileList files = request.execute();

        for (File file : files.getItems()) {
          idToFileMap.put(file.getId(), file);
        }
        request.setPageToken(files.getNextPageToken());
      } catch (IOException e) {
        Log.e(TAG, "IOException", e);
        request.setPageToken(null);
      }
    } while (request.getPageToken() != null && request.getPageToken().length() > 0);
    return idToFileMap;
  }

  /**
   * Gets the Drive changes in the My Tracks folder. Includes deleted files.
   * 
   * @param folderId the folder id
   * @param changeId the largest change id
   * @param changes a map of drive id to file for the changes
   * @return an updated largest change id
   */
  private long getDriveChanges(String folderId, long changeId, Map<String, File> changes) {
    try {
      Changes.List request = drive.changes().list().setStartChangeId(changeId + 1);
      do {
        ChangeList changeList = request.execute();
        long newId = changeList.getLargestChangeId().longValue();

        for (Change change : changeList.getItems()) {
          if (change.getDeleted()) {
            changes.put(change.getFileId(), null);
          } else {
            File file = change.getFile();
            if (SyncUtils.isInFolder(file, folderId)) {
              if (file.getLabels().getTrashed()) {
                changes.put(change.getFileId(), null);
              } else {
                changes.put(change.getFileId(), file);
              }
            } else if (SyncUtils.isSharedWithMe(file)) {
              changes.put(change.getFileId(), file);
            }
          }
        }
        if (newId > changeId) {
          changeId = newId;
        }
        request.setPageToken(changeList.getNextPageToken());
      } while (request.getPageToken() != null && request.getPageToken().length() > 0);
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
    }
    Log.d(TAG, "Got drive changes: " + changes.size() + " - " + changeId);
    return changeId;
  }

  /**
   * Merges a track with a drive file.
   * 
   * @param track the track
   * @param driveFile the drive file
   */
  private void mergeFiles(Track track, File driveFile) throws Exception {
    long modifiedTime = track.getModifiedTime();
    long driveModifiedTime = driveFile.getModifiedDate().getValue();
    if (modifiedTime > driveModifiedTime) {
      Log.d(TAG, "Updating track change " + track.getName());
      if (!SyncUtils.updateDriveFile(context, myTracksProviderUtils, drive, driveFile, track)) {

        // TODO: Should inform the user if cannot update the file
        track.setModifiedTime(driveModifiedTime);
        myTracksProviderUtils.updateTrack(track);
      }
    } else if (modifiedTime < driveModifiedTime) {
      Log.d(TAG, "Updating drive change " + track.getName());
      InputStream inputStream = downloadDriveFile(driveFile);
      if (inputStream == null) {

        // TODO: Should retry if cannot download
        Log.e(TAG, "Unable to update drive change. Input stream is null for " + track.getName());
        track.setModifiedTime(driveModifiedTime);
        myTracksProviderUtils.updateTrack(track);
      } else {
        KmlImporter kmlImporter = new KmlImporter(context, track.getId());
        long[] tracksIds = kmlImporter.importFile(inputStream);
        if (tracksIds.length == 1) {
          Track newTrack = myTracksProviderUtils.getTrack(tracksIds[0]);
          newTrack.setDriveId(driveFile.getId());
          newTrack.setModifiedTime(driveModifiedTime);
          newTrack.setSharedWithMe(driveFile.getSharedWithMeDate() != null);
          myTracksProviderUtils.updateTrack(newTrack);
        } else {

          /*
           * TODO: Should revert the track back to the original.
           */
          Log.e(
              TAG, "Unable to update drive change. Imported size is not 1 for " + track.getName());
        }
      }
    }
  }

  /**
   * Downloads a drive file.
   * 
   * @param driveFile the drive file
   */
  private InputStream downloadDriveFile(File driveFile) {
    if (driveFile.getDownloadUrl() != null && driveFile.getDownloadUrl().length() > 0) {
      try {
        HttpResponse httpResponse = drive.getRequestFactory()
            .buildGetRequest(new GenericUrl(driveFile.getDownloadUrl())).execute();
        if (httpResponse != null) {
          return httpResponse.getContent();
        } else {
          Log.e(TAG, "http response is null");
          return null;
        }
      } catch (IOException e) {
        Log.e(TAG, "IOException", e);
        return null;
      }
    } else {
      Log.d(TAG, "Drive file download url doesn't exist: " + driveFile.getTitle());
      return null;
    }
  }
}
