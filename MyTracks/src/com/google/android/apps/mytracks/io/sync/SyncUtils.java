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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.io.file.TrackWriter;
import com.google.android.apps.mytracks.io.sendtogoogle.PermissionCallback;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.maps.mytracks.R;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.List;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Utilites for Google Drive sync.
 * 
 * @author Jimmy Shih
 */
public class SyncUtils {

  public static final int DRIVE_PERMISSION_REQUEST_CODE = 1;

  public static final String DRIVE_IDS_QUERY = TracksColumns.DRIVEID + " IS NOT NULL AND "
      + TracksColumns.DRIVEID + "!=''";
  public static final String NO_DRIVE_ID_QUERY = TracksColumns.DRIVEID + " IS NULL OR "
      + TracksColumns.DRIVEID + "=''";

  public static final String KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";
  public static final String GET_KML_FILES_QUERY = "'%s' in parents and mimeType = '"
      + KML_MIME_TYPE + "' and trashed = false";

  private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
  private static final String GET_MY_TRACKS_FOLDER_QUERY =
      "'root' in parents and title = '%s' and mimeType = '" + FOLDER_MIME_TYPE
      + "' and trashed = false";

  private static final String TAG = SyncUtils.class.getSimpleName();
  private static final String SYNC_AUTHORITY = "com.google.android.maps.mytracks";
  private static final int NOTIFICATION_ID = 0;

  private SyncUtils() {}

  /**
   * Checks permission by an activity. Will start an activity to request
   * permission with request code {@link #DRIVE_PERMISSION_REQUEST_CODE}.
   * 
   * @param activity the activity
   * @param accountName the account name
   * @param permissionCallback the permission callback
   */
  public static void checkPermissionByActivity(final Activity activity, final String accountName,
      final PermissionCallback permissionCallback) {
    Thread thread = new Thread(new Runnable() {
        @Override
      public void run() {
        try {
          getGoogleAccountCredential(activity, accountName);
          activity.runOnUiThread(new Runnable() {
              @Override
            public void run() {
              permissionCallback.onSuccess();
            }
          });
        } catch (UserRecoverableAuthException e) {
          activity.startActivityForResult(e.getIntent(), DRIVE_PERMISSION_REQUEST_CODE);
        } catch (IOException e) {
          Log.e(TAG, "IOException", e);
          activity.runOnUiThread(new Runnable() {
              @Override
            public void run() {
              permissionCallback.onFailure();
            }
          });
        } catch (GoogleAuthException e) {
          Log.e(TAG, "GoogleAuthException", e);
          activity.runOnUiThread(new Runnable() {
              @Override
            public void run() {
              permissionCallback.onFailure();
            }
          });
        }
      }
    });
    thread.start();
  }

  /**
   * Gets the drive credential. Needs to be run in a background thread.
   * 
   * @param context the context
   * @param accountName the account name
   */
  public static GoogleAccountCredential getCredential(Context context, String accountName) {
    try {
      return getGoogleAccountCredential(context, accountName);
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
   * Cancels any notification to request drive permission.
   * 
   * @param context the context
   */
  public static void cancelNotification(Context context) {
    NotificationManager notificationManager = (NotificationManager) context.getSystemService(
        Context.NOTIFICATION_SERVICE);
    notificationManager.cancel(NOTIFICATION_ID);
  }

  /**
   * Syncs now for the current account.
   * 
   * @param context the context
   */
  public static void syncNow(Context context) {
    Account[] accounts = AccountManager.get(context).getAccountsByType(Constants.ACCOUNT_TYPE);
    String googleAccount = PreferencesUtils.getString(
        context, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    for (Account account : accounts) {
      if (account.name.equals(googleAccount)) {
        ContentResolver.cancelSync(account, SYNC_AUTHORITY);
        ContentResolver.requestSync(account, SYNC_AUTHORITY, new Bundle());
        break;
      }
    }
  }

  /**
   * Disables sync.
   * 
   * @param account the account
   */
  public static void disableSync(Account account) {
    ContentResolver.cancelSync(account, SYNC_AUTHORITY);
    ContentResolver.setIsSyncable(account, SYNC_AUTHORITY, 0);
    ContentResolver.setSyncAutomatically(account, SYNC_AUTHORITY, false);
  }

  /**
   * Enables sync.
   * 
   * @param account the account
   */
  public static void enableSync(Account account) {
    ContentResolver.setIsSyncable(account, SYNC_AUTHORITY, 1);
    ContentResolver.setSyncAutomatically(account, SYNC_AUTHORITY, true);
    ContentResolver.requestSync(account, SYNC_AUTHORITY, new Bundle());
  }

  /**
   * Gets the drive service.
   * 
   * @param credential the credential
   */
  public static Drive getDriveService(GoogleAccountCredential credential) {
    return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
        .build();
  }

  /**
   * Gets the My Tracks folder. Creates one if necessary.
   * 
   * @param context the context
   * @param drive the drive
   */
  public static String getMyTracksFolder(Context context, Drive drive) {
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
   * Returns true if the drive file is a kml file in the My Tracks folder and
   * not trashed.
   * 
   * @param driveFile the drive file
   * @param folderId the My Tracks folder id
   */
  public static boolean isDriveFileValid(File driveFile, String folderId) {
    if (isInFolder(driveFile, folderId)) {
      return !driveFile.getLabels().getTrashed();
    }
    return false;
  }

  /**
   * Returns true if the drive file is a kml file in the My Tracks folder.
   * 
   * @param driveFile the drive file
   * @param folderId the My Tracks folder id
   */
  public static boolean isInFolder(File driveFile, String folderId) {
    if (driveFile == null) {
      return false;
    }
    if (!SyncUtils.KML_MIME_TYPE.equals(driveFile.getMimeType())) {
      return false;
    }
    for (ParentReference parentReference : driveFile.getParents()) {
      String id = parentReference.getId();
      if (id != null && id.equals(folderId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds a Drive file.
   * 
   * @param context the context
   * @param myTracksProviderUtils the myTracksProviderUtils
   * @param drive the drive
   * @param folderId the folder id
   * @param track the track
   */
  public static boolean addDriveFile(Context context, MyTracksProviderUtils myTracksProviderUtils,
      Drive drive, String folderId, Track track) throws IOException {
    java.io.File file = getFile(context, myTracksProviderUtils, track);
    if (file == null) {
      Log.e(TAG, "Unable to add Drive file. File is null for track " + track.getName());
      return false;
    }

    try {
      Log.d(TAG, "Add Drive file for track " + track.getName());
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

      File uploadedFile = drive.files().insert(newMetaData, fileContent).execute();
      if (uploadedFile == null) {
        Log.e(TAG, "Unable to add Drive file. Uploaded file is null for track " + track.getName());
        return false;
      }
      track.setDriveId(uploadedFile.getId());
      track.setModifiedTime(uploadedFile.getModifiedDate().getValue());
      myTracksProviderUtils.updateTrack(track);
      return true;
    } finally {
      file.delete();
    }
  }

  /**
   * Updates a Drive file.
   * 
   * @param context the context
   * @param myTracksProviderUtils the myTracksProviderUtils
   * @param drive the drive
   * @param driveFile the drive file
   * @param track the track
   */
  public static boolean updateDriveFile(Context context,
      MyTracksProviderUtils myTracksProviderUtils, Drive drive, File driveFile, Track track)
      throws IOException {
    Log.d(TAG, "Update drive file for track " + track.getName());
    java.io.File file = SyncUtils.getFile(context, myTracksProviderUtils, track);

    if (file == null) {
      Log.e(TAG, "Unable to update drive file. File is null for track " + track.getName());
      return false;
    } else {
      try {
        FileContent fileContent = new FileContent(KML_MIME_TYPE, file);

        driveFile.setTitle(track.getName() + "." + TrackFileFormat.KML.getExtension());
        File updatedFile = drive.files()
            .update(driveFile.getId(), driveFile, fileContent).execute();
        if (updatedFile == null) {
          Log.e(TAG,
              "Unable to update drive file. Updated file is null for track " + track.getName());
          return false;
        }
        long newModifiedTime = updatedFile.getModifiedDate().getValue();
        track.setModifiedTime(newModifiedTime);
        myTracksProviderUtils.updateTrack(track);
        return true;
      } finally {
        file.delete();
      }
    }
  }

  /**
   * Gets the google account credential.
   * 
   * @param context the context
   * @param accountName the account name
   */
  private static GoogleAccountCredential getGoogleAccountCredential(
      Context context, String accountName) throws IOException, GoogleAuthException {
    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
        context, DriveScopes.DRIVE);
    credential.setSelectedAccountName(accountName);
    credential.getToken();
    return credential;
  }

  /**
   * Gets a file from a track.
   * 
   * @param context the context
   * @param myTracksProviderUtils the myTracksProviderUtils
   * @param track the track
   */
  private static java.io.File getFile(
      Context context, MyTracksProviderUtils myTracksProviderUtils, Track track)
      throws FileNotFoundException {
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
