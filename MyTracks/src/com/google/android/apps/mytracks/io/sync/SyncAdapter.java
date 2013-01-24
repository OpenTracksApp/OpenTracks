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
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.io.file.TrackWriter;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.maps.mytracks.R;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Changes;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.List;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import android.accounts.Account;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
  private static final String KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";
  private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
  private static final String GET_KML_FILES_QUERY = "'%s' in parents and mimeType = '"
      + KML_MIME_TYPE + "' and trashed = false";
  private static final String GET_MY_TRACKS_FOLDER_QUERY =
      "'root' in parents and title = '%s' and mimeType = '" + FOLDER_MIME_TYPE
      + "' and trashed = false";
  private static final int NOTIFICATION_ID = 0;

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

    GoogleAccountCredential credential = getCredential(account.name);
    if (credential == null) {
      return;
    }

    drive = getDriveService(account, credential);
    if (drive == null) {
      return;
    }

    String folderId = getMyTracksFolder();
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
   * Gets the google account credential for an account.
   * 
   * @param accountName the account name
   */
  private GoogleAccountCredential getCredential(String accountName) {
    try {
      return SyncUtils.checkDrivePermission(context, accountName);
    } catch (UserRecoverableAuthException e) {
      Intent intent = e.getIntent();
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_FROM_BACKGROUND);

      PendingIntent pendingIntent = PendingIntent.getActivity(
          context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setAutoCancel(
          true).setContentIntent(pendingIntent)
          .setContentText(context.getString(R.string.permission_request_message, accountName))
          .setContentTitle(context.getString(R.string.permission_request_title))
          .setSmallIcon(android.R.drawable.ic_dialog_alert)
          .setTicker(context.getString(R.string.permission_request_title));
      NotificationManager notificationManager = (NotificationManager) context.getSystemService(
          Context.NOTIFICATION_SERVICE);
      notificationManager.notify(NOTIFICATION_ID, builder.build());
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
    } catch (GoogleAuthException e) {
      Log.e(TAG, "GoogleAuthException", e);
    }
    return null;
  }

  /**
   * Gets the drive service.
   * 
   * @param account the account
   * @param credential the credential
   */
  private Drive getDriveService(Account account, GoogleAccountCredential credential) {
    if (drive == null || !driveAccountName.equals(account.name)) {
      drive = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
          .build();
      driveAccountName = account.name;
    }
    return drive;
  }

  /**
   * Gets the My Tracks folder.
   */
  private String getMyTracksFolder() {
    try {
      String folderName = context.getString(R.string.my_tracks_app_name);
      List list = drive.files()
          .list().setQ(String.format(Locale.US, GET_MY_TRACKS_FOLDER_QUERY, folderName));
      FileList result = list.execute();
      for (File file : result.getItems()) {
        if (file.getTitle().equals(folderName)) {
          return file.getId();
        }
      }
      File file = new File();
      file.setTitle(folderName);
      file.setMimeType(FOLDER_MIME_TYPE);
      return drive.files().insert(file).execute().getId();
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
      return null;
    }
  }

  /**
   * Performs initial sync.
   * 
   * @param folderId the folder id
   */
  private void performInitialSync(String folderId) throws Exception {
  
    // Get the largest change Id first to avoid race conditions
    About about = drive.about().get().setFields(ABOUT_GET_FIELDS).execute();
    long largestChangeId = about.getLargestChangeId();

    // Get all drive files
    Files.List request = drive.files()
        .list().setQ(String.format(Locale.US, GET_KML_FILES_QUERY, folderId));
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

    // Handle new drive files
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
      drive.files().trash(id).execute();
    }
    PreferencesUtils.setString(
        context, R.string.drive_deleted_list_key, PreferencesUtils.DRIVE_DELETED_LIST_DEFAULT);

    Map<String, File> changes = new HashMap<String, File>();
    largestChangeId = getDriveChanges(folderId, largestChangeId, changes);

    try {
      // Get all the local tracks with drive file id
      Cursor cursor = myTracksProviderUtils.getTrackCursor(SyncUtils.DRIVE_IDS_QUERY, null, null);
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

            // Handle the case the track has changed
            File driveFile = drive.files().get(driveId).execute();
            mergeFiles(track, driveFile);
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
    }
  }

  /**
   * Inserts new tracks to Drive.
   * 
   * @param folderId the folder id
   */
  private void insertNewTracks(String folderId) throws IOException {

    // Get tracks without driveid
    Cursor cursor = myTracksProviderUtils.getTrackCursor(SyncUtils.NO_DRIVE_ID_QUERY, null, null);
    long recordingTrackId = PreferencesUtils.getLong(context, R.string.recording_track_id_key);

    if (cursor != null && cursor.moveToFirst()) {
      do {
        Track track = myTracksProviderUtils.createTrack(cursor);
        if (track.getId() == recordingTrackId) {
          continue;
        }

        java.io.File file = getFile(track);
        if (file == null) {
          Log.e(TAG, "Unable to insert new track. File is null for  " + track.getName());
          continue;
        }

        try {
          Log.d(TAG, "Add to Google Drive " + track.getName());
          FileContent fileContent = new FileContent(KML_MIME_TYPE, file);

          // file's parent
          ParentReference parentReference = new ParentReference();
          parentReference.setId(folderId);
          ArrayList<ParentReference> parents = new ArrayList<ParentReference>();
          parents.add(parentReference);

          // file's metadata
          File newMetaData = new File();
          newMetaData.setTitle(track.getName() + "." + TrackFileFormat.KML.getExtension());
          newMetaData.setMimeType(KML_MIME_TYPE);
          newMetaData.setParents(parents);

          File insertedFile = drive.files().insert(newMetaData, fileContent).execute();
          if (insertedFile == null) {
            Log.e(TAG, "Unable to insert new track. Inserted file is null for  " + track.getName());
          } else {
            track.setDriveId(insertedFile.getId());
            track.setModifiedTime(insertedFile.getModifiedDate().getValue());
            myTracksProviderUtils.updateTrack(track);
          }
        } finally {
          file.delete();
        }
      } while (cursor.moveToNext());
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
        if (inputStream != null) {
          KmlImporter kmlImporter = new KmlImporter(context, -1L);
          long[] tracksIds = kmlImporter.importFile(inputStream);
          if (tracksIds.length == 1) {
            Track track = myTracksProviderUtils.getTrack(tracksIds[0]);
            track.setDriveId(driveFile.getId());
            track.setModifiedTime(driveFile.getModifiedDate().getValue());
            myTracksProviderUtils.updateTrack(track);
            Log.d(TAG, "Add from Google Drive " + track.getName());
          }
        }
      }
    }
  }

  /**
   * Gets the Drive changes.
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
            if (KML_MIME_TYPE.equals(file.getMimeType())) {
              for (ParentReference parentReference : file.getParents()) {
                if (parentReference.getId().equals(folderId)) {
                  changes.put(change.getFileId(), file.getLabels().getTrashed() ? null : file);
                }
              }
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
      java.io.File file = getFile(track);

      long newModifiedTime = -1L;
      if (file == null) {

        // Do not retry, skip this track change
        Log.e(TAG, "Unable to update track change. File is null for  " + track.getName());
        newModifiedTime = driveModifiedTime;
      } else {
        try {
          FileContent fileContent = new FileContent(KML_MIME_TYPE, file);

          driveFile.setTitle(track.getName() + "." + TrackFileFormat.KML.getExtension());
          File updatedFile = drive.files()
              .update(driveFile.getId(), driveFile, fileContent).execute();
          if (updatedFile == null) {

            // Do no retry, skip this track change
            Log.e(TAG, "Unable to update track change. Update file is null for " + track.getName());
            newModifiedTime = driveModifiedTime;
          } else {
            newModifiedTime = updatedFile.getModifiedDate().getValue();
          }
        } finally {
          file.delete();
        }
      }
      if (modifiedTime != -1L) {
        track.setModifiedTime(newModifiedTime);
        myTracksProviderUtils.updateTrack(track);
      }
    } else if (modifiedTime < driveModifiedTime) {
      Log.d(TAG, "Updating drive change " + track.getName());
      InputStream inputStream = downloadDriveFile(driveFile);
      Track newTrack = null;
      if (inputStream == null) {

        // Do not retry, skip this drive change
        Log.e(TAG, "Unable to update drive change. Input stream is null for " + track.getName());
        newTrack = track;
      } else {
        KmlImporter kmlImporter = new KmlImporter(context, track.getId());
        long[] tracksIds = kmlImporter.importFile(inputStream);
        if (tracksIds.length == 1) {
          newTrack = myTracksProviderUtils.getTrack(tracksIds[0]);
        } else {

          /*
           * Do not retry, skip this drive change. Note at this point, the track
           * waypoints and track points are deleted.
           */
          Log.e(
              TAG, "Unable to update drive change. Imported size is not 1 for " + track.getName());
          newTrack = track;
        }
      }

      if (newTrack != null) {
        newTrack.setDriveId(driveFile.getId());
        newTrack.setModifiedTime(driveModifiedTime);
        myTracksProviderUtils.updateTrack(newTrack);
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

  /**
   * Gets a file from a track.
   * 
   * @param track the track
   */
  private java.io.File getFile(Track track) throws FileNotFoundException {
    TrackFileFormat trackFileFormat = TrackFileFormat.KML;

    java.io.File directory = new java.io.File(
        context.getCacheDir(), trackFileFormat.getExtension());
    if (!FileUtils.ensureDirectoryExists(directory)) {
      Log.d(TAG, "Unable to create " + directory.getAbsolutePath());
      return null;
    }

    java.io.File file = new java.io.File(directory,
        FileUtils.buildUniqueFileName(directory, track.getName(), trackFileFormat.getExtension()));

    TrackWriter trackWriter = new TrackWriter(
        context, myTracksProviderUtils, track, trackFileFormat, null);
    trackWriter.writeTrack(new FileOutputStream(file));
    if (trackWriter.wasSuccess()) {
      return file;
    }
    Log.d(TAG, "Unable to get file for track " + track.getName());
    return null;
  }
}
