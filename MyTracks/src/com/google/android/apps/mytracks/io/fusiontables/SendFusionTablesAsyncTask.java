// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.io.fusiontables;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.DescriptionGenerator;
import com.google.android.apps.mytracks.content.DescriptionGeneratorImpl;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.android.apps.mytracks.io.sendtogoogle.AbstractSendAsyncTask;
import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.maps.mytracks.R;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.Fusiontables.Query.Sql;
import com.google.api.services.fusiontables.model.Column;
import com.google.api.services.fusiontables.model.PointStyle;
import com.google.api.services.fusiontables.model.StyleFunction;
import com.google.api.services.fusiontables.model.StyleSetting;
import com.google.api.services.fusiontables.model.Table;
import com.google.api.services.fusiontables.model.Template;

import android.accounts.Account;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AsyncTask to send a track to Google Fusion Tables.
 * 
 * @author Jimmy Shih
 */
public class SendFusionTablesAsyncTask extends AbstractSendAsyncTask {

  private static final String TAG = SendFusionTablesAsyncTask.class.getSimpleName();
  private static final int MAX_POINTS_PER_UPLOAD = 2048;
  private static final int PROGRESS_CREATE_TABLE = 0;
  private static final int PROGRESS_SET_STYLE = 5;
  private static final int PROGRESS_UPLOAD_DATA_MIN = 10;
  private static final int PROGRESS_UPLOAD_DATA_MAX = 90;
  private static final int PROGRESS_UPLOAD_WAYPOINTS = 95;
  private static final int PROGRESS_COMPLETE = 100;

  // See
  // http://support.google.com/fusiontables/bin/answer.py?hl=en&answer=185991
  private static final String MARKER_TYPE_START = "large_green";
  private static final String MARKER_TYPE_END = "large_red";
  private static final String MARKER_TYPE_WAYPOINT = "large_blue";
  private static final String MARKER_TYPE_STATISTICS = "large_yellow";

  private final long trackId;
  private final Account account;
  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;

  int currentSegment;

  public SendFusionTablesAsyncTask(
      SendFusionTablesActivity activity, long trackId, Account account) {
    super(activity);
    this.trackId = trackId;
    this.account = account;
    context = activity.getApplicationContext();
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
  }

  @Override
  protected void closeConnection() {}

  @Override
  protected boolean performTask() {
    try {
      // Reset the per upload states
      currentSegment = 1;

      GoogleAccountCredential credential = SendToGoogleUtils.getGoogleAccountCredential(
          context, account.name, SendToGoogleUtils.FUSION_TABLES_SCOPE);
      if (credential == null) {
        return false;
      }
      Fusiontables fusiontables = new Fusiontables.Builder(
          AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();

      Track track = myTracksProviderUtils.getTrack(trackId);
      if (track == null) {
        Log.d(TAG, "No track for " + trackId);
        return false;
      }
      
      // Create a new table
      publishProgress(PROGRESS_CREATE_TABLE);
      String tableId = createNewTable(fusiontables, track);
      if (tableId == null) {
        return retryTask();
      }

      publishProgress(PROGRESS_SET_STYLE);
      setStyle(fusiontables, tableId);
      setTemplate(fusiontables, tableId);
      if (!setPermission(track, tableId)) {
        Log.d(TAG, "Cannot set permission for table " + tableId);
        return false;
      }
      
      // Upload all the track points plus the start and end markers
      publishProgress(PROGRESS_UPLOAD_DATA_MIN);
      if (!uploadAllTrackPoints(fusiontables, tableId, track)) {
        return false;
      }

      // Upload all the waypoints
      publishProgress(PROGRESS_UPLOAD_WAYPOINTS);
      if (!uploadWaypoints(fusiontables, tableId)) {
        return false;
      }

      publishProgress(PROGRESS_COMPLETE);
      return true;

    } catch (UserRecoverableAuthException e) {
      SendToGoogleUtils.sendNotification(
          context, account.name, e.getIntent(), SendToGoogleUtils.FUSION_TABLES_NOTIFICATION_ID);
      return false;
    } catch (GoogleAuthException e) {
      return retryTask();
    } catch (UserRecoverableAuthIOException e) {
      SendToGoogleUtils.sendNotification(
          context, account.name, e.getIntent(), SendToGoogleUtils.FUSION_TABLES_NOTIFICATION_ID);
      return false;
    } catch (IOException e) {
      return retryTask();
    }
  }

  @Override
  protected void invalidateToken() {}

  /**
   * Creates a new table.
   * 
   * @param fusiontables fusion tables
   * @param track the track
   * @return the table id if success.
   */
  private String createNewTable(Fusiontables fusiontables, Track track) throws IOException {
    Table table = new Table();
    table.setName(track.getName());
    table.setDescription(track.getDescription());
    table.setIsExportable(true);
    table.setColumns(Arrays.asList(new Column().setName("name").setType("STRING"),
        new Column().setName("description").setType("STRING"),
        new Column().setName("geometry").setType("LOCATION"),
        new Column().setName("icon").setType("STRING")));
    return fusiontables.table().insert(table).execute().getTableId();
  }
  
  private void setStyle(Fusiontables fusiontables, String tableId) throws IOException {
    StyleFunction styleFunction = new StyleFunction();
    styleFunction.setColumnName("icon");
    PointStyle pointStyle = new PointStyle();
    pointStyle.setIconStyler(styleFunction);
    StyleSetting styleSetting = new StyleSetting();
    styleSetting.setTableId(tableId);
    styleSetting.setMarkerOptions(pointStyle);
    
    fusiontables.style().insert(tableId, styleSetting).execute();
  }
  
  private void setTemplate(Fusiontables fusiontables, String tableId) throws IOException {
    Template template = new Template();
    template.setTableId(tableId);
    template.setAutomaticColumnNames(Arrays.asList("name", "description"));
    fusiontables.template().insert(tableId, template).execute();
  }

  private boolean setPermission(Track track, String tableId) throws IOException, GoogleAuthException {
    boolean defaultTablePublic = PreferencesUtils.getBoolean(context,
        R.string.export_google_fusion_tables_public_key,
        PreferencesUtils.EXPORT_GOOGLE_FUSION_TABLES_PUBLIC_DEFAULT);
    if (!defaultTablePublic) {
      return true;
    }
    GoogleAccountCredential driveCredential = SendToGoogleUtils.getGoogleAccountCredential(
        context, account.name, SendToGoogleUtils.DRIVE_SCOPE);
    if (driveCredential == null) {
      return false;
    }
    Drive drive = SyncUtils.getDriveService(driveCredential);
    Permission permission = new Permission();
    permission.setRole("reader");
    permission.setType("anyone");
    permission.setValue("");   
    drive.permissions().insert(tableId, permission).execute();
    
    shareUrl = SendFusionTablesUtils.getMapUrl(track, tableId);
    return true;
  }
  
  /**
   * Uploads all the points in a track. *
   * 
   * @param fusiontables fusion tables
   * @param tableId the table id
   * @param track the track
   * @return true if success.
   */
  private boolean uploadAllTrackPoints(Fusiontables fusiontables, String tableId, Track track)
      throws IOException {
    Cursor cursor = null;
    try {
      cursor = myTracksProviderUtils.getTrackPointCursor(trackId, -1L, -1, false);
      if (cursor == null) {
        Log.d(TAG, "Location cursor is null");
        return false;
      }

      int count = cursor.getCount();
      List<Location> locations = new ArrayList<Location>(MAX_POINTS_PER_UPLOAD);
      Location lastValidLocation = null;
      boolean sentStartMarker = false;

      for (int i = 0; i < count; i++) {
        cursor.moveToPosition(i);

        Location location = myTracksProviderUtils.createTrackPoint(cursor);
        locations.add(location);

        if (LocationUtils.isValidLocation(location)) {
          lastValidLocation = location;
        }

        if (!sentStartMarker && lastValidLocation != null) {
          // Create a start marker
          String name = context.getString(R.string.marker_label_start, track.getName());
          createNewPoint(fusiontables, tableId, name, "", lastValidLocation, MARKER_TYPE_START);
          sentStartMarker = true;
        }

        // Upload periodically
        int readCount = i + 1;
        if (readCount % MAX_POINTS_PER_UPLOAD == 0) {
          if (!prepareAndUploadPoints(fusiontables, tableId, track, locations, false)) {
            Log.d(TAG, "Unable to upload points");
            return false;
          }
          updateProgress(readCount, count);
          locations.clear();
        }
      }

      // Do a final upload with the remaining locations
      if (!prepareAndUploadPoints(fusiontables, tableId, track, locations, true)) {
        Log.d(TAG, "Unable to upload points");
        return false;
      }

      // Create an end marker
      if (lastValidLocation != null) {
        String name = context.getString(R.string.marker_label_end, track.getName());
        DescriptionGenerator descriptionGenerator = new DescriptionGeneratorImpl(context);
        String description = descriptionGenerator.generateTrackDescription(track, null, null, true);
        createNewPoint(
            fusiontables, tableId, name, description, lastValidLocation, MARKER_TYPE_END);
      }
      return true;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * Prepares and uploads a list of locations from a track.
   * 
   * @param fusiontables fusion tables
   * @param tableId the table id
   * @param track the track
   * @param locations the locations from the track
   * @param lastBatch true if it is the last batch of locations
   */
  private boolean prepareAndUploadPoints(Fusiontables fusiontables, String tableId, Track track,
      List<Location> locations, boolean lastBatch) throws IOException {
    // Prepare locations
    ArrayList<Track> splitTracks = SendToGoogleUtils.prepareLocations(track, locations);

    // Upload segments
    boolean onlyOneSegment = lastBatch && currentSegment == 1 && splitTracks.size() == 1;
    for (Track splitTrack : splitTracks) {
      if (!onlyOneSegment) {
        splitTrack.setName(context.getString(
            R.string.send_google_track_part_label, splitTrack.getName(), currentSegment));
      }
      createNewLineString(fusiontables, tableId, splitTrack);
      currentSegment++;
    }
    return true;
  }

  /**
   * Uploads all the waypoints.
   * 
   * @param fusiontables fusion tables
   * @param tableId the table id
   * @return true if success.
   * @throws IOException
   */
  private boolean uploadWaypoints(Fusiontables fusiontables, String tableId) throws IOException {
    Cursor cursor = null;
    try {
      cursor = myTracksProviderUtils.getWaypointCursor(
          trackId, -1L, Constants.MAX_LOADED_WAYPOINTS_POINTS);
      if (cursor != null && cursor.moveToFirst()) {
        /*
         * This will skip the first waypoint (it carries the stats for the
         * track).
         */
        while (cursor.moveToNext()) {
          Waypoint wpt = myTracksProviderUtils.createWaypoint(cursor);
          String type = wpt.getType() == WaypointType.STATISTICS ? MARKER_TYPE_STATISTICS
              : MARKER_TYPE_WAYPOINT;
          String description = wpt.getDescription().replaceAll("\n", "<br>");
          createNewPoint(
              fusiontables, tableId, wpt.getName(), description, wpt.getLocation(), type);
        }
      }
      return true;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * Creates a new row in Google Fusion Tables representing a marker as a point.
   * 
   * @param fusiontables fusion tables
   * @param tableId the table id
   * @param name the marker name
   * @param description the marker description
   * @param location the marker location
   * @param type the marker type
   */
  private void createNewPoint(Fusiontables fusiontables, String tableId, String name,
      String description, Location location, String type) throws IOException {
    String values = SendFusionTablesUtils.formatSqlValues(
        name, description, SendFusionTablesUtils.getKmlPoint(location), type);
    Sql sql = fusiontables.query()
        .sql("INSERT INTO " + tableId + " (name,description,geometry,icon) VALUES " + values);
    sql.execute();
  }

  /**
   * Creates a new row in Google Fusion Tables representing the track as a line
   * segment.
   * 
   * @param fusiontables fusion tables
   * @param tableId the table id
   * @param track the track
   */
  private void createNewLineString(Fusiontables fusiontables, String tableId, Track track)
      throws IOException {
    String values = SendFusionTablesUtils.formatSqlValues(track.getName(), track.getDescription(),
        SendFusionTablesUtils.getKmlLineString(track.getLocations()));
    String sql = "INSERT INTO " + tableId + " (name,description,geometry) VALUES " + values;
    HttpContent content = ByteArrayContent.fromString(null, "sql=" + sql);
    GoogleUrl url = new GoogleUrl("https://www.googleapis.com/fusiontables/v1/query");
    fusiontables.getRequestFactory().buildPostRequest(url, content).execute();
  }

  /**
   * Updates the progress based on the number of locations uploaded.
   * 
   * @param uploaded the number of uploaded locations
   * @param total the number of total locations
   */
  private void updateProgress(int uploaded, int total) {
    double totalPercentage = (double) uploaded / total;
    double scaledPercentage = totalPercentage
        * (PROGRESS_UPLOAD_DATA_MAX - PROGRESS_UPLOAD_DATA_MIN) + PROGRESS_UPLOAD_DATA_MIN;
    publishProgress((int) scaledPercentage);
  }
}
