/*
 * Copyright 2008 Google Inc.
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
package com.google.android.apps.mytracks.content;

import static com.google.android.apps.mytracks.lib.MyTracksLibConstants.TAG;

import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.protobuf.InvalidProtocolBufferException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Helper class providing easy access to locations and tracks in the
 * MyTracksProvider. All static members.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracksProviderUtilsImpl implements MyTracksProviderUtils {

  private final ContentResolver contentResolver;
  private final String authority; // authority of the content provider

  private int defaultCursorBatchSize = 2000;

  public MyTracksProviderUtilsImpl(ContentResolver contentResolver, String authority) {
    this.contentResolver = contentResolver;
    this.authority = authority;
  }
  
  /**
   * Gets the tracks table URI.
   */
  private Uri getTracksUri() {
    return authority.equals(DATABASE_AUTHORITY) ? TracksColumns.DATABASE_CONTENT_URI
        : TracksColumns.CONTENT_URI;
  }

  /**
   * Gets the track points table URI.
   */
  private Uri getTrackPointsUri() {
    return authority.equals(DATABASE_AUTHORITY) ? TrackPointsColumns.DATABASE_CONTENT_URI
        : TrackPointsColumns.CONTENT_URI;
  }

  /**
   * Gets the waypoints table URI.
   */
  private Uri getWaypointsUri() {
    return authority.equals(DATABASE_AUTHORITY) ? WaypointsColumns.DATABASE_CONTENT_URI
        : WaypointsColumns.CONTENT_URI;
  }
  
  /**
   * Creates the ContentValues for a given location object.
   *
   * @param location a given location
   * @param trackId the id of the track it belongs to
   * @return a filled in ContentValues object
   */
  private static ContentValues createContentValues(
      Location location, long trackId) {
    ContentValues values = new ContentValues();
    values.put(TrackPointsColumns.TRACKID, trackId);
    values.put(TrackPointsColumns.LATITUDE,
        (int) (location.getLatitude() * 1E6));
    values.put(TrackPointsColumns.LONGITUDE,
        (int) (location.getLongitude() * 1E6));
    // This is an ugly hack for Samsung phones that don't properly populate the
    // time field.
    values.put(TrackPointsColumns.TIME,
        (location.getTime() == 0)
            ? System.currentTimeMillis()
            : location.getTime());
    if (location.hasAltitude()) {
      values.put(TrackPointsColumns.ALTITUDE, location.getAltitude());
    }
    if (location.hasBearing()) {
      values.put(TrackPointsColumns.BEARING, location.getBearing());
    }
    if (location.hasAccuracy()) {
      values.put(TrackPointsColumns.ACCURACY, location.getAccuracy());
    }
    if (location.hasSpeed()) {
      values.put(TrackPointsColumns.SPEED, location.getSpeed());
    }
    if (location instanceof MyTracksLocation) {
      MyTracksLocation mtLocation = (MyTracksLocation) location;
      if (mtLocation.getSensorDataSet() != null) {
        values.put(TrackPointsColumns.SENSOR, mtLocation.getSensorDataSet().toByteArray());
      }
    }
    return values;
  }

  @Override
  public ContentValues createContentValues(Track track) {
    ContentValues values = new ContentValues();
    TripStatistics stats = track.getStatistics();

    // Values id < 0 indicate no id is available:
    if (track.getId() >= 0) {
      values.put(TracksColumns._ID, track.getId());
    }
    values.put(TracksColumns.NAME, track.getName());
    values.put(TracksColumns.DESCRIPTION, track.getDescription());
    values.put(TracksColumns.MAPID, track.getMapId());
    values.put(TracksColumns.TABLEID, track.getTableId());
    values.put(TracksColumns.CATEGORY, track.getCategory());
    values.put(TracksColumns.NUMPOINTS, track.getNumberOfPoints());
    values.put(TracksColumns.STARTID, track.getStartId());
    values.put(TracksColumns.STARTTIME, stats.getStartTime());
    values.put(TracksColumns.STOPTIME, stats.getStopTime());
    values.put(TracksColumns.STOPID, track.getStopId());
    values.put(TracksColumns.TOTALDISTANCE, stats.getTotalDistance());
    values.put(TracksColumns.TOTALTIME, stats.getTotalTime());
    values.put(TracksColumns.MOVINGTIME, stats.getMovingTime());
    values.put(TracksColumns.MAXLAT, stats.getTop());
    values.put(TracksColumns.MINLAT, stats.getBottom());
    values.put(TracksColumns.MAXLON, stats.getRight());
    values.put(TracksColumns.MINLON, stats.getLeft());
    values.put(TracksColumns.AVGSPEED, stats.getAverageSpeed());
    values.put(TracksColumns.AVGMOVINGSPEED, stats.getAverageMovingSpeed());
    values.put(TracksColumns.MAXSPEED, stats.getMaxSpeed());
    values.put(TracksColumns.MINELEVATION, stats.getMinElevation());
    values.put(TracksColumns.MAXELEVATION, stats.getMaxElevation());
    values.put(TracksColumns.ELEVATIONGAIN, stats.getTotalElevationGain());
    values.put(TracksColumns.MINGRADE, stats.getMinGrade());
    values.put(TracksColumns.MAXGRADE, stats.getMaxGrade());
    return values;
  }

  private static ContentValues createContentValues(Waypoint waypoint) {
    ContentValues values = new ContentValues();

    // Values id < 0 indicate no id is available:
    if (waypoint.getId() >= 0) {
      values.put(WaypointsColumns._ID, waypoint.getId());
    }
    values.put(WaypointsColumns.NAME, waypoint.getName());
    values.put(WaypointsColumns.DESCRIPTION, waypoint.getDescription());
    values.put(WaypointsColumns.CATEGORY, waypoint.getCategory());
    values.put(WaypointsColumns.ICON, waypoint.getIcon());
    values.put(WaypointsColumns.TRACKID, waypoint.getTrackId());
    values.put(WaypointsColumns.TYPE, waypoint.getType());
    values.put(WaypointsColumns.LENGTH, waypoint.getLength());
    values.put(WaypointsColumns.DURATION, waypoint.getDuration());
    values.put(WaypointsColumns.STARTID, waypoint.getStartId());
    values.put(WaypointsColumns.STOPID, waypoint.getStopId());

    TripStatistics stats = waypoint.getStatistics();
    if (stats != null) {
      values.put(WaypointsColumns.TOTALDISTANCE, stats.getTotalDistance());
      values.put(WaypointsColumns.TOTALTIME, stats.getTotalTime());
      values.put(WaypointsColumns.MOVINGTIME, stats.getMovingTime());
      values.put(WaypointsColumns.AVGSPEED, stats.getAverageSpeed());
      values.put(WaypointsColumns.AVGMOVINGSPEED, stats.getAverageMovingSpeed());
      values.put(WaypointsColumns.MAXSPEED, stats.getMaxSpeed());
      values.put(WaypointsColumns.MINELEVATION, stats.getMinElevation());
      values.put(WaypointsColumns.MAXELEVATION, stats.getMaxElevation());
      values.put(WaypointsColumns.ELEVATIONGAIN, stats.getTotalElevationGain());
      values.put(WaypointsColumns.MINGRADE, stats.getMinGrade());
      values.put(WaypointsColumns.MAXGRADE, stats.getMaxGrade());
      values.put(WaypointsColumns.STARTTIME, stats.getStartTime());
    }

    Location location = waypoint.getLocation();
    if (location != null) {
      values.put(WaypointsColumns.LATITUDE,
          (int) (location.getLatitude() * 1E6));
      values.put(WaypointsColumns.LONGITUDE,
          (int) (location.getLongitude() * 1E6));
      values.put(WaypointsColumns.TIME, location.getTime());
      if (location.hasAltitude()) {
        values.put(WaypointsColumns.ALTITUDE, location.getAltitude());
      }
      if (location.hasBearing()) {
        values.put(WaypointsColumns.BEARING, location.getBearing());
      }
      if (location.hasAccuracy()) {
        values.put(WaypointsColumns.ACCURACY, location.getAccuracy());
      }
      if (location.hasSpeed()) {
        values.put(WaypointsColumns.SPEED, location.getSpeed());
      }
    }

    return values;
  }

  @Override
  public Location createLocation(Cursor cursor) {
    Location location = new MyTracksLocation("");
    fillLocation(cursor, location);
    return location;
  }

  /**
   * A cache of track column indices.
   */
  private static class CachedTrackColumnIndices {
    public final int idxId;
    public final int idxLatitude;
    public final int idxLongitude;
    public final int idxAltitude;
    public final int idxTime;
    public final int idxBearing;
    public final int idxAccuracy;
    public final int idxSpeed;
    public final int idxSensor;

    public CachedTrackColumnIndices(Cursor cursor) {
      idxId = cursor.getColumnIndex(TrackPointsColumns._ID);
      idxLatitude = cursor.getColumnIndexOrThrow(TrackPointsColumns.LATITUDE);
      idxLongitude = cursor.getColumnIndexOrThrow(TrackPointsColumns.LONGITUDE);
      idxAltitude = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALTITUDE);
      idxTime = cursor.getColumnIndexOrThrow(TrackPointsColumns.TIME);
      idxBearing = cursor.getColumnIndexOrThrow(TrackPointsColumns.BEARING);
      idxAccuracy = cursor.getColumnIndexOrThrow(TrackPointsColumns.ACCURACY);
      idxSpeed = cursor.getColumnIndexOrThrow(TrackPointsColumns.SPEED);
      idxSensor = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR);
    }
  }

  private void fillLocation(Cursor cursor, CachedTrackColumnIndices columnIndices,
      Location location) {
    location.reset();

    if (!cursor.isNull(columnIndices.idxLatitude)) {
      location.setLatitude(1. * cursor.getInt(columnIndices.idxLatitude) / 1E6);
    }
    if (!cursor.isNull(columnIndices.idxLongitude)) {
      location.setLongitude(1. * cursor.getInt(columnIndices.idxLongitude) / 1E6);
    }
    if (!cursor.isNull(columnIndices.idxAltitude)) {
      location.setAltitude(cursor.getFloat(columnIndices.idxAltitude));
    }
    if (!cursor.isNull(columnIndices.idxTime)) {
      location.setTime(cursor.getLong(columnIndices.idxTime));
    }
    if (!cursor.isNull(columnIndices.idxBearing)) {
      location.setBearing(cursor.getFloat(columnIndices.idxBearing));
    }
    if (!cursor.isNull(columnIndices.idxSpeed)) {
      location.setSpeed(cursor.getFloat(columnIndices.idxSpeed));
    }
    if (!cursor.isNull(columnIndices.idxAccuracy)) {
      location.setAccuracy(cursor.getFloat(columnIndices.idxAccuracy));
    }
    if (location instanceof MyTracksLocation &&
        !cursor.isNull(columnIndices.idxSensor)) {
      MyTracksLocation mtLocation = (MyTracksLocation) location;
      // TODO get the right buffer.
      Sensor.SensorDataSet sensorData;
      try {
        sensorData = Sensor.SensorDataSet.parseFrom(cursor.getBlob(columnIndices.idxSensor));
        mtLocation.setSensorData(sensorData);
      } catch (InvalidProtocolBufferException e) {
        Log.w(TAG, "Failed to parse sensor data.", e);
      }
    }
  }

  @Override
  public void fillLocation(Cursor cursor, Location location) {
    CachedTrackColumnIndices columnIndicies = new CachedTrackColumnIndices(cursor);
    fillLocation(cursor, columnIndicies, location);
  }

  @Override
  public Track createTrack(Cursor cursor) {
    int idxId = cursor.getColumnIndexOrThrow(TracksColumns._ID);
    int idxName = cursor.getColumnIndexOrThrow(TracksColumns.NAME);
    int idxDescription =
        cursor.getColumnIndexOrThrow(TracksColumns.DESCRIPTION);
    int idxMapId = cursor.getColumnIndexOrThrow(TracksColumns.MAPID);
    int idxTableId = cursor.getColumnIndexOrThrow(TracksColumns.TABLEID);
    int idxCategory = cursor.getColumnIndexOrThrow(TracksColumns.CATEGORY);
    int idxStartId = cursor.getColumnIndexOrThrow(TracksColumns.STARTID);
    int idxStartTime = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
    int idxStopTime = cursor.getColumnIndexOrThrow(TracksColumns.STOPTIME);
    int idxStopId = cursor.getColumnIndexOrThrow(TracksColumns.STOPID);
    int idxNumPoints = cursor.getColumnIndexOrThrow(TracksColumns.NUMPOINTS);
    int idxMaxlat = cursor.getColumnIndexOrThrow(TracksColumns.MAXLAT);
    int idxMinlat = cursor.getColumnIndexOrThrow(TracksColumns.MINLAT);
    int idxMaxlon = cursor.getColumnIndexOrThrow(TracksColumns.MAXLON);
    int idxMinlon = cursor.getColumnIndexOrThrow(TracksColumns.MINLON);

    int idxTotalDistance =
        cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
    int idxTotalTime = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
    int idxMovingTime = cursor.getColumnIndexOrThrow(TracksColumns.MOVINGTIME);
    int idxMaxSpeed = cursor.getColumnIndexOrThrow(TracksColumns.MAXSPEED);
    int idxMinElevation =
        cursor.getColumnIndexOrThrow(TracksColumns.MINELEVATION);
    int idxMaxElevation =
        cursor.getColumnIndexOrThrow(TracksColumns.MAXELEVATION);
    int idxElevationGain =
        cursor.getColumnIndexOrThrow(TracksColumns.ELEVATIONGAIN);
    int idxMinGrade = cursor.getColumnIndexOrThrow(TracksColumns.MINGRADE);
    int idxMaxGrade = cursor.getColumnIndexOrThrow(TracksColumns.MAXGRADE);

    Track track = new Track();
    TripStatistics stats = track.getStatistics();
    if (!cursor.isNull(idxId)) {
      track.setId(cursor.getLong(idxId));
    }
    if (!cursor.isNull(idxName)) {
      track.setName(cursor.getString(idxName));
    }
    if (!cursor.isNull(idxDescription)) {
      track.setDescription(cursor.getString(idxDescription));
    }
    if (!cursor.isNull(idxMapId)) {
      track.setMapId(cursor.getString(idxMapId));
    }
    if (!cursor.isNull(idxTableId)) {
      track.setTableId(cursor.getString(idxTableId));
    }
    if (!cursor.isNull(idxCategory)) {
      track.setCategory(cursor.getString(idxCategory));
    }
    if (!cursor.isNull(idxStartId)) {
      track.setStartId(cursor.getInt(idxStartId));
    }
    if (!cursor.isNull(idxStartTime)) {
      stats.setStartTime(cursor.getLong(idxStartTime));
    }
    if (!cursor.isNull(idxStopTime)) {
      stats.setStopTime(cursor.getLong(idxStopTime));
    }
    if (!cursor.isNull(idxStopId)) {
      track.setStopId(cursor.getInt(idxStopId));
    }
    if (!cursor.isNull(idxNumPoints)) {
      track.setNumberOfPoints(cursor.getInt(idxNumPoints));
    }
    if (!cursor.isNull(idxTotalDistance)) {
      stats.setTotalDistance(cursor.getFloat(idxTotalDistance));
    }
    if (!cursor.isNull(idxTotalTime)) {
      stats.setTotalTime(cursor.getLong(idxTotalTime));
    }
    if (!cursor.isNull(idxMovingTime)) {
      stats.setMovingTime(cursor.getLong(idxMovingTime));
    }
    if (!cursor.isNull(idxMaxlat)
        && !cursor.isNull(idxMinlat)
        && !cursor.isNull(idxMaxlon)
        && !cursor.isNull(idxMinlon)) {
      int top = cursor.getInt(idxMaxlat);
      int bottom = cursor.getInt(idxMinlat);
      int right = cursor.getInt(idxMaxlon);
      int left = cursor.getInt(idxMinlon);
      stats.setBounds(left, top, right, bottom);
    }
    if (!cursor.isNull(idxMaxSpeed)) {
      stats.setMaxSpeed(cursor.getFloat(idxMaxSpeed));
    }
    if (!cursor.isNull(idxMinElevation)) {
      stats.setMinElevation(cursor.getFloat(idxMinElevation));
    }
    if (!cursor.isNull(idxMaxElevation)) {
      stats.setMaxElevation(cursor.getFloat(idxMaxElevation));
    }
    if (!cursor.isNull(idxElevationGain)) {
      stats.setTotalElevationGain(cursor.getFloat(idxElevationGain));
    }
    if (!cursor.isNull(idxMinGrade)) {
      stats.setMinGrade(cursor.getFloat(idxMinGrade));
    }
    if (!cursor.isNull(idxMaxGrade)) {
      stats.setMaxGrade(cursor.getFloat(idxMaxGrade));
    }
    return track;
  }

  @Override
  public Waypoint createWaypoint(Cursor cursor) {
    int idxId = cursor.getColumnIndexOrThrow(WaypointsColumns._ID);
    int idxName = cursor.getColumnIndexOrThrow(WaypointsColumns.NAME);
    int idxDescription =
        cursor.getColumnIndexOrThrow(WaypointsColumns.DESCRIPTION);
    int idxCategory = cursor.getColumnIndexOrThrow(WaypointsColumns.CATEGORY);
    int idxIcon = cursor.getColumnIndexOrThrow(WaypointsColumns.ICON);
    int idxTrackId = cursor.getColumnIndexOrThrow(WaypointsColumns.TRACKID);
    int idxType = cursor.getColumnIndexOrThrow(WaypointsColumns.TYPE);
    int idxLength = cursor.getColumnIndexOrThrow(WaypointsColumns.LENGTH);
    int idxDuration = cursor.getColumnIndexOrThrow(WaypointsColumns.DURATION);
    int idxStartTime = cursor.getColumnIndexOrThrow(WaypointsColumns.STARTTIME);
    int idxStartId = cursor.getColumnIndexOrThrow(WaypointsColumns.STARTID);
    int idxStopId = cursor.getColumnIndexOrThrow(WaypointsColumns.STOPID);

    int idxTotalDistance =
        cursor.getColumnIndexOrThrow(WaypointsColumns.TOTALDISTANCE);
    int idxTotalTime = cursor.getColumnIndexOrThrow(WaypointsColumns.TOTALTIME);
    int idxMovingTime =
        cursor.getColumnIndexOrThrow(WaypointsColumns.MOVINGTIME);
    int idxMaxSpeed = cursor.getColumnIndexOrThrow(WaypointsColumns.MAXSPEED);
    int idxMinElevation =
        cursor.getColumnIndexOrThrow(WaypointsColumns.MINELEVATION);
    int idxMaxElevation =
        cursor.getColumnIndexOrThrow(WaypointsColumns.MAXELEVATION);
    int idxElevationGain =
        cursor.getColumnIndexOrThrow(WaypointsColumns.ELEVATIONGAIN);
    int idxMinGrade = cursor.getColumnIndexOrThrow(WaypointsColumns.MINGRADE);
    int idxMaxGrade = cursor.getColumnIndexOrThrow(WaypointsColumns.MAXGRADE);

    int idxLatitude = cursor.getColumnIndexOrThrow(WaypointsColumns.LATITUDE);
    int idxLongitude = cursor.getColumnIndexOrThrow(WaypointsColumns.LONGITUDE);
    int idxAltitude = cursor.getColumnIndexOrThrow(WaypointsColumns.ALTITUDE);
    int idxTime = cursor.getColumnIndexOrThrow(WaypointsColumns.TIME);
    int idxBearing = cursor.getColumnIndexOrThrow(WaypointsColumns.BEARING);
    int idxAccuracy = cursor.getColumnIndexOrThrow(WaypointsColumns.ACCURACY);
    int idxSpeed = cursor.getColumnIndexOrThrow(WaypointsColumns.SPEED);

    Waypoint waypoint = new Waypoint();

    if (!cursor.isNull(idxId)) {
      waypoint.setId(cursor.getLong(idxId));
    }
    if (!cursor.isNull(idxName)) {
      waypoint.setName(cursor.getString(idxName));
    }
    if (!cursor.isNull(idxDescription)) {
      waypoint.setDescription(cursor.getString(idxDescription));
    }
    if (!cursor.isNull(idxCategory)) {
      waypoint.setCategory(cursor.getString(idxCategory));
    }
    if (!cursor.isNull(idxIcon)) {
      waypoint.setIcon(cursor.getString(idxIcon));
    }
    if (!cursor.isNull(idxTrackId)) {
      waypoint.setTrackId(cursor.getLong(idxTrackId));
    }
    if (!cursor.isNull(idxType)) {
      waypoint.setType(cursor.getInt(idxType));
    }
    if (!cursor.isNull(idxLength)) {
      waypoint.setLength(cursor.getDouble(idxLength));
    }
    if (!cursor.isNull(idxDuration)) {
      waypoint.setDuration(cursor.getLong(idxDuration));
    }
    if (!cursor.isNull(idxStartId)) {
      waypoint.setStartId(cursor.getLong(idxStartId));
    }
    if (!cursor.isNull(idxStopId)) {
      waypoint.setStopId(cursor.getLong(idxStopId));
    }

    TripStatistics stats = new TripStatistics();
    boolean hasStats = false;
    if (!cursor.isNull(idxStartTime)) {
      stats.setStartTime(cursor.getLong(idxStartTime));
      hasStats = true;
    }
    if (!cursor.isNull(idxTotalDistance)) {
      stats.setTotalDistance(cursor.getFloat(idxTotalDistance));
      hasStats = true;
    }
    if (!cursor.isNull(idxTotalTime)) {
      stats.setTotalTime(cursor.getLong(idxTotalTime));
      hasStats = true;
    }
    if (!cursor.isNull(idxMovingTime)) {
      stats.setMovingTime(cursor.getLong(idxMovingTime));
      hasStats = true;
    }
    if (!cursor.isNull(idxMaxSpeed)) {
      stats.setMaxSpeed(cursor.getFloat(idxMaxSpeed));
      hasStats = true;
    }
    if (!cursor.isNull(idxMinElevation)) {
      stats.setMinElevation(cursor.getFloat(idxMinElevation));
      hasStats = true;
    }
    if (!cursor.isNull(idxMaxElevation)) {
      stats.setMaxElevation(cursor.getFloat(idxMaxElevation));
      hasStats = true;
    }
    if (!cursor.isNull(idxElevationGain)) {
      stats.setTotalElevationGain(cursor.getFloat(idxElevationGain));
      hasStats = true;
    }
    if (!cursor.isNull(idxMinGrade)) {
      stats.setMinGrade(cursor.getFloat(idxMinGrade));
      hasStats = true;
    }
    if (!cursor.isNull(idxMaxGrade)) {
      stats.setMaxGrade(cursor.getFloat(idxMaxGrade));
      hasStats = true;
    }
    if (hasStats) {
      waypoint.setStatistics(stats);
    }

    Location location = new Location("");
    if (!cursor.isNull(idxLatitude) && !cursor.isNull(idxLongitude)) {
      location.setLatitude(1. * cursor.getInt(idxLatitude) / 1E6);
      location.setLongitude(1. * cursor.getInt(idxLongitude) / 1E6);
    }
    if (!cursor.isNull(idxAltitude)) {
      location.setAltitude(cursor.getFloat(idxAltitude));
    }
    if (!cursor.isNull(idxTime)) {
      location.setTime(cursor.getLong(idxTime));
    }
    if (!cursor.isNull(idxBearing)) {
      location.setBearing(cursor.getFloat(idxBearing));
    }
    if (!cursor.isNull(idxSpeed)) {
      location.setSpeed(cursor.getFloat(idxSpeed));
    }
    if (!cursor.isNull(idxAccuracy)) {
      location.setAccuracy(cursor.getFloat(idxAccuracy));
    }
    waypoint.setLocation(location);
    return waypoint;
  }

  @Override
  public void deleteAllTracks() {
    contentResolver.delete(getTracksUri(), null, null);
    contentResolver.delete(getTrackPointsUri(), null, null);
    contentResolver.delete(getWaypointsUri(), null, null);
  }

  @Override
  public void deleteTrack(long trackId) {
    Track track = getTrack(trackId);
    if (track != null) {
      contentResolver.delete(getTrackPointsUri(),
          "_id>=" + track.getStartId() + " AND _id<=" + track.getStopId(), null);
    }
    contentResolver.delete(getWaypointsUri(), WaypointsColumns.TRACKID + "=" + trackId, null);
    contentResolver.delete(getTracksUri(), "_id=" + trackId, null);
  }

  @Override
  public void deleteWaypoint(long waypointId,
      DescriptionGenerator descriptionGenerator) {
    final Waypoint deletedWaypoint = getWaypoint(waypointId);
    if (deletedWaypoint != null
        && deletedWaypoint.getType() == Waypoint.TYPE_STATISTICS) {
      final Waypoint nextWaypoint =
          getNextStatisticsWaypointAfter(deletedWaypoint);
      if (nextWaypoint != null) {
        Log.d(TAG, "Correcting marker " + nextWaypoint.getId()
            + " after deleted marker " + deletedWaypoint.getId());
        nextWaypoint.getStatistics().merge(deletedWaypoint.getStatistics());
        nextWaypoint.setDescription(
            descriptionGenerator.generateWaypointDescription(nextWaypoint));
        if (!updateWaypoint(nextWaypoint)) {
          Log.w(TAG, "Update of marker was unsuccessful.");
        }
      } else {
        Log.d(TAG, "No statistics marker after the deleted one was found.");
      }
    }
    contentResolver.delete(getWaypointsUri(), "_id=" + waypointId, null);
  }

  @Override
  public Waypoint getNextStatisticsWaypointAfter(Waypoint waypoint) {
    final String selection = WaypointsColumns._ID + ">" + waypoint.getId()
        + " AND " + WaypointsColumns.TRACKID + "=" + waypoint.getTrackId()
        + " AND " + WaypointsColumns.TYPE + "=" + Waypoint.TYPE_STATISTICS;
    final String sortOrder = WaypointsColumns._ID + " LIMIT 1";
    Cursor cursor = null;
    try {
      cursor = contentResolver.query(
          getWaypointsUri(),
          null /*projection*/,
          selection,
          null /*selectionArgs*/,
          sortOrder);
      if (cursor != null && cursor.moveToFirst()) {
        return createWaypoint(cursor);
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Caught unexpected exception.", e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return null;
  }

  @Override
  public boolean updateWaypoint(Waypoint waypoint) {
    try {
      final int rows = contentResolver.update(
          getWaypointsUri(),
          createContentValues(waypoint),
          "_id=" + waypoint.getId(),
          null /*selectionArgs*/);
      return rows == 1;
    } catch (RuntimeException e) {
      Log.e(TAG, "Caught unexpected exception.", e);
    }
    return false;
  }

  /**
   * Finds a locations from the provider by the given selection.
   *
   * @param select a selection argument that identifies a unique location
   * @return the fist location matching, or null if not found
   */
  private Location findLocationBy(String select) {
    Cursor cursor = null;
    try {
      cursor = contentResolver.query(getTrackPointsUri(), null, select, null, null);
      if (cursor != null && cursor.moveToNext()) {
        return createLocation(cursor);
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Caught an unexpeceted exception.", e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return null;
  }

  /**
   * Finds a track from the provider by the given selection.
   *
   * @param select a selection argument that identifies a unique track
   * @return the first track matching, or null if not found
   */
  private Track findTrackBy(String select) {
    Cursor cursor = null;
    try {
      cursor = contentResolver.query(getTracksUri(), null, select, null, null);
      if (cursor != null && cursor.moveToNext()) {
        return createTrack(cursor);
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Caught unexpected exception.", e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return null;
  }

  @Override
  public Location getLastLocation() {
    return findLocationBy("_id=(select max(_id) from trackpoints)");
  }

  @Override
  public Waypoint getFirstWaypoint(long trackId) {
    if (trackId < 0) {
      return null;
    }

    Cursor cursor = contentResolver.query(
        getWaypointsUri(),
        null /*projection*/,
        "trackid=" + trackId,
        null /*selectionArgs*/,
        "_id LIMIT 1");
    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {
          return createWaypoint(cursor);
        }
      } catch (RuntimeException e) {
        Log.w(TAG, "Caught an unexpected exception.", e);
      } finally {
        cursor.close();
      }
    }
    return null;
  }

  @Override
  public Waypoint getWaypoint(long waypointId) {
    if (waypointId < 0) {
      return null;
    }

    Cursor cursor = contentResolver.query(
        getWaypointsUri(),
        null /*projection*/,
        "_id=" + waypointId,
        null /*selectionArgs*/,
        null /*sortOrder*/);
    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {
          return createWaypoint(cursor);
        }
      } catch (RuntimeException e) {
        Log.w(TAG, "Caught an unexpected exception.", e);
      } finally {
        cursor.close();
      }
    }
    return null;
  }

  @Override
  public long getLastLocationId(long trackId) {
    if (trackId < 0) {
      return -1;
    }

    final String[] projection = {"_id"};
    Cursor cursor = contentResolver.query(
        getTrackPointsUri(),
        projection,
        "_id=(select max(_id) from trackpoints WHERE trackid=" + trackId + ")",
        null /*selectionArgs*/,
        null /*sortOrder*/);
    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {
          return cursor.getLong(
              cursor.getColumnIndexOrThrow(TrackPointsColumns._ID));
        }
      } catch (RuntimeException e) {
        Log.w(TAG, "Caught an unexpected exception.", e);
      } finally {
        cursor.close();
      }
    }
    return -1;
  }

  @Override
  public long getFirstWaypointId(long trackId) {
    if (trackId < 0) {
      return -1;
    }

    final String[] projection = {"_id"};
    Cursor cursor = contentResolver.query(
        getWaypointsUri(),
        projection,
        "trackid=" + trackId,
        null /*selectionArgs*/,
        "_id LIMIT 1" /*sortOrder*/);
    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {
          return cursor.getLong(
              cursor.getColumnIndexOrThrow(WaypointsColumns._ID));
        }
      } catch (RuntimeException e) {
        Log.w(TAG, "Caught an unexpected exception.", e);
      } finally {
        cursor.close();
      }
    }
    return -1;
  }

  @Override
  public long getLastWaypointId(long trackId) {
    if (trackId < 0) {
      return -1;
    }

    final String[] projection = {"_id"};
    Cursor cursor = contentResolver.query(
        getWaypointsUri(),
        projection,
        WaypointsColumns.TRACKID + "=" + trackId,
        null /*selectionArgs*/,
        "_id DESC LIMIT 1" /*sortOrder*/);
    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {
          return cursor.getLong(
              cursor.getColumnIndexOrThrow(WaypointsColumns._ID));
        }
      } catch (RuntimeException e) {
        Log.w(TAG, "Caught an unexpected exception.", e);
      } finally {
        cursor.close();
      }
    }
    return -1;
  }

  @Override
  public Track getLastTrack() {
    Cursor cursor = null;
    try {
      cursor = contentResolver.query(
          getTracksUri(), null, "_id=(select max(_id) from tracks)", null, null);
      if (cursor != null && cursor.moveToNext()) {
        return createTrack(cursor);
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Caught an unexpected exception.", e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return null;
  }

  @Override
  public long getLastTrackId() {
    String[] proj = { TracksColumns._ID };
    Cursor cursor = contentResolver.query(
        getTracksUri(), proj, "_id=(select max(_id) from tracks)", null, null);
    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {
          return cursor.getLong(
              cursor.getColumnIndexOrThrow(TracksColumns._ID));
        }
      } finally {
        cursor.close();
      }
    }
    return -1;
  }

  @Override
  public Location getLocation(long id) {
    if (id < 0) {
      return null;
    }

    String selection = TrackPointsColumns._ID + "=" + id;
    return findLocationBy(selection);
  }

  @Override
  public Cursor getLocationsCursor(long trackId, long minTrackPointId,
      int maxLocations, boolean descending) {
    if (trackId < 0) {
      return null;
    }

    String selection;
    if (minTrackPointId >= 0) {
      selection = String.format("%s=%d AND %s%s%d",
          TrackPointsColumns.TRACKID, trackId, TrackPointsColumns._ID,
          descending ? "<=" : ">=", minTrackPointId);
    } else {
      selection = String.format("%s=%d", TrackPointsColumns.TRACKID, trackId);
    }

    String sortOrder = "_id " + (descending ? "DESC" : "ASC");
    if (maxLocations > 0) {
      sortOrder += " LIMIT " + maxLocations;
    }

    return contentResolver.query(getTrackPointsUri(), null, selection, null, sortOrder);
  }

  @Override
  public Cursor getWaypointsCursor(long trackId, long minWaypointId,
      int maxWaypoints) {
    if (trackId < 0) {
      return null;
    }

    String selection;
    if (minWaypointId > 0) {
      selection = String.format("%s=%d AND %s>=%d",
          WaypointsColumns.TRACKID, trackId,
          WaypointsColumns._ID, minWaypointId);
    } else {
      selection = String.format("%s=%d",
          WaypointsColumns.TRACKID, trackId);
    }

    String sortOrder = "_id ASC";
    if (maxWaypoints > 0) {
      sortOrder += " LIMIT " + maxWaypoints;
    }

    return contentResolver.query(getWaypointsUri(), null, selection, null, sortOrder);
  }

  @Override
  public Track getTrack(long id) {
    if (id < 0) {
      return null;
    }

    String select = TracksColumns._ID + "=" + id;
    return findTrackBy(select);
  }

  @Override
  public List<Track> getAllTracks() {
    Cursor cursor = getTracksCursor(null);
    ArrayList<Track> tracks = new ArrayList<Track>();
    if (cursor != null) {
      tracks.ensureCapacity(cursor.getCount());

      if (cursor.moveToFirst()) {
        do {
          tracks.add(createTrack(cursor));
        } while(cursor.moveToNext());
      }

      cursor.close();
    }

    return tracks;
  }

  @Override
  public Cursor getTracksCursor(String selection) {
    Cursor cursor = contentResolver.query(getTracksUri(), null, selection, null, "_id");
    return cursor;
  }

  @Override
  public Uri insertTrack(Track track) {
    Log.d(TAG, "MyTracksProviderUtilsImpl.insertTrack");
    return contentResolver.insert(getTracksUri(), createContentValues(track));
  }

  @Override
  public Uri insertTrackPoint(Location location, long trackId) {
    Log.d(TAG, "MyTracksProviderUtilsImpl.insertTrackPoint");
    return contentResolver.insert(getTrackPointsUri(), createContentValues(location, trackId));
  }

  @Override
  public int bulkInsertTrackPoints(Location[] locations, int length, long trackId) {
    if (length == -1) { length = locations.length; }

    ContentValues[] values = new ContentValues[length];
    for (int i = 0; i < length; i++) {
      values[i] = createContentValues(locations[i], trackId);
    }

    return contentResolver.bulkInsert(getTrackPointsUri(), values);
  }

  @Override
  public Uri insertWaypoint(Waypoint waypoint) {
    Log.d(TAG, "MyTracksProviderUtilsImpl.insertWaypoint");
    waypoint.setId(-1);
    return contentResolver.insert(getWaypointsUri(), createContentValues(waypoint));
  }

  @Override
  public boolean trackExists(long id) {
    if (id < 0) {
      return false;
    }

    Cursor cursor = null;
    try {
      final String[] projection = { TracksColumns._ID };
      cursor = contentResolver.query(
          getTracksUri(),
          projection,
          TracksColumns._ID + "=" + id/*selection*/,
          null/*selectionArgs*/,
          null/*sortOrder*/);
      if (cursor != null && cursor.moveToNext()) {
        return true;
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return false;
  }

  @Override
  public void updateTrack(Track track) {
    Log.d(TAG, "MyTracksProviderUtilsImpl.updateTrack");
    contentResolver.update(
        getTracksUri(), createContentValues(track), "_id=" + track.getId(), null);
  }

  @Override
  public LocationIterator getLocationIterator(final long trackId, final long startTrackPointId,
      final boolean descending, final LocationFactory locationFactory) {
    if (locationFactory == null) {
      throw new IllegalArgumentException("Expecting non-null locationFactory");
    }
    return new LocationIterator() {
      private long lastTrackPointId = startTrackPointId;
      private Cursor cursor = getCursor(startTrackPointId);
      private final CachedTrackColumnIndices columnIndices = cursor != null ?
          new CachedTrackColumnIndices(cursor) : null;

      private Cursor getCursor(long trackPointId) {
        return getLocationsCursor(trackId, trackPointId, defaultCursorBatchSize, descending);
      }

      private boolean advanceCursorToNextBatch() {
        long pointId = lastTrackPointId + (descending ? -1 : 1);
        Log.d(TAG, "Advancing cursor point ID: " + pointId);
        cursor.close();
        cursor = getCursor(pointId);
        return cursor != null;
      }

      @Override
      public long getLocationId() {
        return lastTrackPointId;
      }

      @Override
      public boolean hasNext() {
        if (cursor == null) {
          return false;
        }
        if (cursor.isAfterLast()) {
          return false;
        }
        if (cursor.isLast()) {
          // If the current batch size was less that max, we can safely return, otherwise
          // we need to advance to the next batch.
          return cursor.getCount() == defaultCursorBatchSize &&
              advanceCursorToNextBatch() && !cursor.isAfterLast();
        }

        return true;
      }

      @Override
      public Location next() {
        if (cursor == null ||
            !(cursor.moveToNext() || advanceCursorToNextBatch() || cursor.moveToNext())) {
          throw new NoSuchElementException();
        }

        lastTrackPointId = cursor.getLong(columnIndices.idxId);
        Location location = locationFactory.createLocation();
        fillLocation(cursor, columnIndices, location);

        return location;
      }

      @Override
      public void close() {
        if (cursor != null) {
          cursor.close();
          cursor = null;
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  // @VisibleForTesting
  void setDefaultCursorBatchSize(int defaultCursorBatchSize) {
    this.defaultCursorBatchSize = defaultCursorBatchSize;
  }
}
