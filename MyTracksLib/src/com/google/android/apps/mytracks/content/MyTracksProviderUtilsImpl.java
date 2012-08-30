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

import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
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
 * {@link MyTracksProviderUtils} implementation.
 * 
 * @author Leif Hendrik Wilden
 */
public class MyTracksProviderUtilsImpl implements MyTracksProviderUtils {

  private static final String TAG = MyTracksProviderUtilsImpl.class.getSimpleName();

  private final ContentResolver contentResolver;
  private int defaultCursorBatchSize = 2000;

  public MyTracksProviderUtilsImpl(ContentResolver contentResolver) {
    this.contentResolver = contentResolver;
  }

  @Override
  public Track createTrack(Cursor cursor) {
    int idIndex = cursor.getColumnIndexOrThrow(TracksColumns._ID);
    int nameIndex = cursor.getColumnIndexOrThrow(TracksColumns.NAME);
    int descriptionIndex = cursor.getColumnIndexOrThrow(TracksColumns.DESCRIPTION);
    int categoryIndex = cursor.getColumnIndexOrThrow(TracksColumns.CATEGORY);
    int startIdIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTID);
    int stopIdIndex = cursor.getColumnIndexOrThrow(TracksColumns.STOPID);
    int startTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
    int stopTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STOPTIME);
    int numPointsIndex = cursor.getColumnIndexOrThrow(TracksColumns.NUMPOINTS);
    int totalDistanceIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
    int totalTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
    int movingTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.MOVINGTIME);
    int minLatIndex = cursor.getColumnIndexOrThrow(TracksColumns.MINLAT);
    int maxLatIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAXLAT);
    int minLonIndex = cursor.getColumnIndexOrThrow(TracksColumns.MINLON);
    int maxLonIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAXLON);
    int maxSpeedIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAXSPEED);
    int minElevationIndex = cursor.getColumnIndexOrThrow(TracksColumns.MINELEVATION);
    int maxElevationIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAXELEVATION);
    int elevationGainIndex = cursor.getColumnIndexOrThrow(TracksColumns.ELEVATIONGAIN);
    int minGradeIndex = cursor.getColumnIndexOrThrow(TracksColumns.MINGRADE);
    int maxGradeIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAXGRADE);
    int mapIdIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAPID);
    int tableIdIndex = cursor.getColumnIndexOrThrow(TracksColumns.TABLEID);
    int iconIndex = cursor.getColumnIndexOrThrow(TracksColumns.ICON);
  
    Track track = new Track();
    TripStatistics tripStatistics = track.getTripStatistics();
    if (!cursor.isNull(idIndex)) {
      track.setId(cursor.getLong(idIndex));
    }
    if (!cursor.isNull(nameIndex)) {
      track.setName(cursor.getString(nameIndex));
    }
    if (!cursor.isNull(descriptionIndex)) {
      track.setDescription(cursor.getString(descriptionIndex));
    }
    if (!cursor.isNull(categoryIndex)) {
      track.setCategory(cursor.getString(categoryIndex));
    }
    if (!cursor.isNull(startIdIndex)) {
      track.setStartId(cursor.getLong(startIdIndex));
    }
    if (!cursor.isNull(stopIdIndex)) {
      track.setStopId(cursor.getLong(stopIdIndex));
    }
    if (!cursor.isNull(startTimeIndex)) {
      tripStatistics.setStartTime(cursor.getLong(startTimeIndex));
    }
    if (!cursor.isNull(stopTimeIndex)) {
      tripStatistics.setStopTime(cursor.getLong(stopTimeIndex));
    }
    if (!cursor.isNull(numPointsIndex)) {
      track.setNumberOfPoints(cursor.getInt(numPointsIndex));
    }
    if (!cursor.isNull(totalDistanceIndex)) {
      tripStatistics.setTotalDistance(cursor.getFloat(totalDistanceIndex));
    }
    if (!cursor.isNull(totalTimeIndex)) {
      tripStatistics.setTotalTime(cursor.getLong(totalTimeIndex));
    }
    if (!cursor.isNull(movingTimeIndex)) {
      tripStatistics.setMovingTime(cursor.getLong(movingTimeIndex));
    }
    if (!cursor.isNull(minLatIndex) && !cursor.isNull(maxLatIndex) && !cursor.isNull(minLonIndex)
        && !cursor.isNull(maxLonIndex)) {
      int bottom = cursor.getInt(minLatIndex);
      int top = cursor.getInt(maxLatIndex);
      int left = cursor.getInt(minLonIndex);
      int right = cursor.getInt(maxLonIndex);
      tripStatistics.setBounds(left, top, right, bottom);
    }
    if (!cursor.isNull(maxSpeedIndex)) {
      tripStatistics.setMaxSpeed(cursor.getFloat(maxSpeedIndex));
    }
    if (!cursor.isNull(minElevationIndex)) {
      tripStatistics.setMinElevation(cursor.getFloat(minElevationIndex));
    }
    if (!cursor.isNull(maxElevationIndex)) {
      tripStatistics.setMaxElevation(cursor.getFloat(maxElevationIndex));
    }
    if (!cursor.isNull(elevationGainIndex)) {
      tripStatistics.setTotalElevationGain(cursor.getFloat(elevationGainIndex));
    }
    if (!cursor.isNull(minGradeIndex)) {
      tripStatistics.setMinGrade(cursor.getFloat(minGradeIndex));
    }
    if (!cursor.isNull(maxGradeIndex)) {
      tripStatistics.setMaxGrade(cursor.getFloat(maxGradeIndex));
    }
    if (!cursor.isNull(mapIdIndex)) {
      track.setMapId(cursor.getString(mapIdIndex));
    }
    if (!cursor.isNull(tableIdIndex)) {
      track.setTableId(cursor.getString(tableIdIndex));
    }
    if (!cursor.isNull(iconIndex)) {
      track.setIcon(cursor.getString(iconIndex));
    }
    return track;
  }

  @Override
  public void deleteAllTracks() {
    contentResolver.delete(TrackPointsColumns.CONTENT_URI, null, null);
    contentResolver.delete(WaypointsColumns.CONTENT_URI, null, null);
    // Delete tracks last since it triggers a database vaccum call 
    contentResolver.delete(TracksColumns.CONTENT_URI, null, null);
  }

  @Override
  public void deleteTrack(long trackId) {
    Track track = getTrack(trackId);
    if (track != null) {
      String where = TrackPointsColumns._ID + ">=? AND " + TrackPointsColumns._ID + "<=?";
      String[] selectionArgs = new String[] { Long.toString(track.getStartId()), Long.toString(
          track.getStopId()) };
      contentResolver.delete(TrackPointsColumns.CONTENT_URI, where, selectionArgs);
    }
    contentResolver.delete(WaypointsColumns.CONTENT_URI, WaypointsColumns.TRACKID + "=?",
        new String[] { Long.toString(trackId) });
    // Delete tracks last since it triggers a database vaccum call
    contentResolver.delete(TracksColumns.CONTENT_URI, TracksColumns._ID + "=?", new String[] {
        Long.toString(trackId) });
  }

  @Override
  public List<Track> getAllTracks() {
    Cursor cursor = getTrackCursor(null, null, null, TracksColumns._ID);
    ArrayList<Track> tracks = new ArrayList<Track>();
    if (cursor != null) {
      tracks.ensureCapacity(cursor.getCount());
      if (cursor.moveToFirst()) {
        do {
          tracks.add(createTrack(cursor));
        } while (cursor.moveToNext());
      }
      cursor.close();
    }
    return tracks;
  }

  @Override
  public Track getLastTrack() {
    Cursor cursor = null;
    try {
      String selection = TracksColumns._ID + "=(select max(" + TracksColumns._ID + ") from "
          + TracksColumns.TABLE_NAME + ")";
      cursor = getTrackCursor(null, selection, null, TracksColumns._ID);
      if (cursor != null && cursor.moveToNext()) {
        return createTrack(cursor);
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return null;
  }

  @Override
  public Track getTrack(long trackId) {
    if (trackId < 0) {
      return null;
    }
    Cursor cursor = null;
    try {
      cursor = getTrackCursor(null, TracksColumns._ID + "=?",
          new String[] { Long.toString(trackId) }, TracksColumns._ID);
      if (cursor != null && cursor.moveToNext()) {
        return createTrack(cursor);
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return null;
  }

  @Override
  public Cursor getTrackCursor(String selection, String[] selectionArgs, String sortOrder) {
    return getTrackCursor(null, selection, selectionArgs, sortOrder);
  }

  @Override
  public Uri insertTrack(Track track) {
    return contentResolver.insert(TracksColumns.CONTENT_URI, createContentValues(track));
  }

  @Override
  public void updateTrack(Track track) {
    contentResolver.update(TracksColumns.CONTENT_URI, createContentValues(track),
        TracksColumns._ID + "=?", new String[] { Long.toString(track.getId()) });
  }

  private ContentValues createContentValues(Track track) {
    ContentValues values = new ContentValues();
    TripStatistics tripStatistics = track.getTripStatistics();

    // Value < 0 indicates no id is available
    if (track.getId() >= 0) {
      values.put(TracksColumns._ID, track.getId());
    }
    values.put(TracksColumns.NAME, track.getName());
    values.put(TracksColumns.DESCRIPTION, track.getDescription());
    values.put(TracksColumns.CATEGORY, track.getCategory());
    values.put(TracksColumns.STARTID, track.getStartId());
    values.put(TracksColumns.STOPID, track.getStopId());
    values.put(TracksColumns.STARTTIME, tripStatistics.getStartTime());
    values.put(TracksColumns.STOPTIME, tripStatistics.getStopTime());
    values.put(TracksColumns.NUMPOINTS, track.getNumberOfPoints());
    values.put(TracksColumns.TOTALDISTANCE, tripStatistics.getTotalDistance());
    values.put(TracksColumns.TOTALTIME, tripStatistics.getTotalTime());
    values.put(TracksColumns.MOVINGTIME, tripStatistics.getMovingTime());
    values.put(TracksColumns.MINLAT, tripStatistics.getBottom());
    values.put(TracksColumns.MAXLAT, tripStatistics.getTop());
    values.put(TracksColumns.MINLON, tripStatistics.getLeft());
    values.put(TracksColumns.MAXLON, tripStatistics.getRight());
    values.put(TracksColumns.AVGSPEED, tripStatistics.getAverageSpeed());
    values.put(TracksColumns.AVGMOVINGSPEED, tripStatistics.getAverageMovingSpeed());
    values.put(TracksColumns.MAXSPEED, tripStatistics.getMaxSpeed());
    values.put(TracksColumns.MINELEVATION, tripStatistics.getMinElevation());
    values.put(TracksColumns.MAXELEVATION, tripStatistics.getMaxElevation());
    values.put(TracksColumns.ELEVATIONGAIN, tripStatistics.getTotalElevationGain());
    values.put(TracksColumns.MINGRADE, tripStatistics.getMinGrade());
    values.put(TracksColumns.MAXGRADE, tripStatistics.getMaxGrade());
    values.put(TracksColumns.MAPID, track.getMapId());
    values.put(TracksColumns.TABLEID, track.getTableId());
    values.put(TracksColumns.ICON, track.getIcon());
    return values;
  }

  /**
   * Gets a track cursor.
   * 
   * @param projection the projection
   * @param selection the selection
   * @param selectionArgs the selection arguments
   * @param sortOrder the sort oder
   */
  private Cursor getTrackCursor(
      String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    return contentResolver.query(
        TracksColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
  }

  @Override
  public Waypoint createWaypoint(Cursor cursor) {
    int idIndex = cursor.getColumnIndexOrThrow(WaypointsColumns._ID);
    int nameIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.NAME);
    int descriptionIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.DESCRIPTION);
    int categoryIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.CATEGORY);
    int iconIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.ICON);
    int trackIdIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.TRACKID);
    int typeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.TYPE);
    int lengthIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.LENGTH);
    int durationIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.DURATION);
    int startTimeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.STARTTIME);
    int startIdIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.STARTID);
    int stopIdIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.STOPID);
    int longitudeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.LONGITUDE);
    int latitudeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.LATITUDE);
    int timeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.TIME);
    int altitudeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.ALTITUDE);
    int accuracyIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.ACCURACY);
    int speedIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.SPEED);
    int bearingIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.BEARING);
    int totalDistanceIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.TOTALDISTANCE);
    int totalTimeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.TOTALTIME);
    int movingTimeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.MOVINGTIME);
    int maxSpeedIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.MAXSPEED);
    int minElevationIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.MINELEVATION);
    int maxElevationIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.MAXELEVATION);
    int elevationGainIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.ELEVATIONGAIN);
    int minGradeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.MINGRADE);
    int maxGradeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.MAXGRADE);
  
    Waypoint waypoint = new Waypoint();
  
    if (!cursor.isNull(idIndex)) {
      waypoint.setId(cursor.getLong(idIndex));
    }
    if (!cursor.isNull(nameIndex)) {
      waypoint.setName(cursor.getString(nameIndex));
    }
    if (!cursor.isNull(descriptionIndex)) {
      waypoint.setDescription(cursor.getString(descriptionIndex));
    }
    if (!cursor.isNull(categoryIndex)) {
      waypoint.setCategory(cursor.getString(categoryIndex));
    }
    if (!cursor.isNull(iconIndex)) {
      waypoint.setIcon(cursor.getString(iconIndex));
    }
    if (!cursor.isNull(trackIdIndex)) {
      waypoint.setTrackId(cursor.getLong(trackIdIndex));
    }
    if (!cursor.isNull(typeIndex)) {
      waypoint.setType(cursor.getInt(typeIndex));
    }
    if (!cursor.isNull(lengthIndex)) {
      waypoint.setLength(cursor.getFloat(lengthIndex));
    }
    if (!cursor.isNull(durationIndex)) {
      waypoint.setDuration(cursor.getLong(durationIndex));
    }
    if (!cursor.isNull(startIdIndex)) {
      waypoint.setStartId(cursor.getLong(startIdIndex));
    }
    if (!cursor.isNull(stopIdIndex)) {
      waypoint.setStopId(cursor.getLong(stopIdIndex));
    }
  
    Location location = new Location("");
    if (!cursor.isNull(longitudeIndex) && !cursor.isNull(latitudeIndex)) {
      location.setLongitude(((double) cursor.getInt(longitudeIndex)) / 1E6);
      location.setLatitude(((double) cursor.getInt(latitudeIndex)) / 1E6);
    }
    if (!cursor.isNull(timeIndex)) {
      location.setTime(cursor.getLong(timeIndex));
    }
    if (!cursor.isNull(altitudeIndex)) {
      location.setAltitude(cursor.getFloat(altitudeIndex));
    }
    if (!cursor.isNull(accuracyIndex)) {
      location.setAccuracy(cursor.getFloat(accuracyIndex));
    }
    if (!cursor.isNull(speedIndex)) {
      location.setSpeed(cursor.getFloat(speedIndex));
    }
    if (!cursor.isNull(bearingIndex)) {
      location.setBearing(cursor.getFloat(bearingIndex));
    }
    waypoint.setLocation(location);
  
    TripStatistics tripStatistics = new TripStatistics();
    boolean hasTripStatistics = false;
    if (!cursor.isNull(startTimeIndex)) {
      tripStatistics.setStartTime(cursor.getLong(startTimeIndex));
      hasTripStatistics = true;
    }
    if (!cursor.isNull(totalDistanceIndex)) {
      tripStatistics.setTotalDistance(cursor.getFloat(totalDistanceIndex));
      hasTripStatistics = true;
    }
    if (!cursor.isNull(totalTimeIndex)) {
      tripStatistics.setTotalTime(cursor.getLong(totalTimeIndex));
      hasTripStatistics = true;
    }
    if (!cursor.isNull(movingTimeIndex)) {
      tripStatistics.setMovingTime(cursor.getLong(movingTimeIndex));
      hasTripStatistics = true;
    }
    if (!cursor.isNull(maxSpeedIndex)) {
      tripStatistics.setMaxSpeed(cursor.getFloat(maxSpeedIndex));
      hasTripStatistics = true;
    }
    if (!cursor.isNull(minElevationIndex)) {
      tripStatistics.setMinElevation(cursor.getFloat(minElevationIndex));
      hasTripStatistics = true;
    }
    if (!cursor.isNull(maxElevationIndex)) {
      tripStatistics.setMaxElevation(cursor.getFloat(maxElevationIndex));
      hasTripStatistics = true;
    }
    if (!cursor.isNull(elevationGainIndex)) {
      tripStatistics.setTotalElevationGain(cursor.getFloat(elevationGainIndex));
      hasTripStatistics = true;
    }
    if (!cursor.isNull(minGradeIndex)) {
      tripStatistics.setMinGrade(cursor.getFloat(minGradeIndex));
      hasTripStatistics = true;
    }
    if (!cursor.isNull(maxGradeIndex)) {
      tripStatistics.setMaxGrade(cursor.getFloat(maxGradeIndex));
      hasTripStatistics = true;
    }
  
    if (hasTripStatistics) {
      waypoint.setTripStatistics(tripStatistics);
    }
    return waypoint;
  }

  @Override
  public void deleteWaypoint(long waypointId, DescriptionGenerator descriptionGenerator) {
    final Waypoint waypoint = getWaypoint(waypointId);
    if (waypoint != null && waypoint.getType() == Waypoint.TYPE_STATISTICS) {
      final Waypoint nextWaypoint = getNextStatisticsWaypointAfter(waypoint);
      if (nextWaypoint == null) {
        Log.d(TAG, "Unable to find the next statistics marker after deleting one.");
      } else {
        nextWaypoint.getTripStatistics().merge(waypoint.getTripStatistics());
        nextWaypoint.setDescription(
            descriptionGenerator.generateWaypointDescription(nextWaypoint.getTripStatistics()));
        if (!updateWaypoint(nextWaypoint)) {
          Log.e(TAG, "Unable to update the next statistics marker after deleting one.");
        }
      }
    }
    contentResolver.delete(WaypointsColumns.CONTENT_URI, WaypointsColumns._ID + "=?",
        new String[] { Long.toString(waypointId) });
  }

  @Override
  public long getFirstWaypointId(long trackId) {
    if (trackId < 0) {
      return -1L;
    }
    Cursor cursor = null;
    try {
      cursor = getWaypointCursor(new String[] { WaypointsColumns._ID },
          WaypointsColumns.TRACKID + "=?", new String[] { Long.toString(trackId) },
          WaypointsColumns._ID, 1);
      if (cursor != null && cursor.moveToFirst()) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(WaypointsColumns._ID));
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return -1L;
  }

  @Override
  public Waypoint getLastStatisticsWaypoint(long trackId) {
    if (trackId < 0) {
      return null;
    }
    Cursor cursor = null;
    try {
      String selection = WaypointsColumns.TRACKID + "=? AND " + WaypointsColumns.TYPE + "="
          + Waypoint.TYPE_STATISTICS;
      String[] selectionArgs = new String[] { Long.toString(trackId) };
      cursor = getWaypointCursor(
          null, selection, selectionArgs, WaypointsColumns._ID + " DESC", 1);
      if (cursor != null && cursor.moveToFirst()) {
        return createWaypoint(cursor);
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return null;
  }

  @Override
  public int getNextWaypointNumber(long trackId, boolean statistics) {
    if (trackId < 0) {
      return -1;
    }
    Cursor cursor = null;
    try {
      String[] projection = { WaypointsColumns._ID };
      String selection = WaypointsColumns.TRACKID + "=?  AND " + WaypointsColumns.TYPE + "=?";
      int type = statistics ? Waypoint.TYPE_STATISTICS : Waypoint.TYPE_WAYPOINT;
      String[] selectionArgs = new String[] { Long.toString(trackId), Integer.toString(type) };
      cursor = getWaypointCursor(projection, selection, selectionArgs, WaypointsColumns._ID, 0);
      if (cursor != null) {
        int count = cursor.getCount();
        /*
         * For statistics markers, the first marker is for the track statistics,
         * so return the count as the next user visible number.
         */
        return statistics ? count : count + 1;
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return -1;
  }

  @Override
  public Waypoint getWaypoint(long waypointId) {
    if (waypointId < 0) {
      return null;
    }
    Cursor cursor = null;
    try {
      cursor = getWaypointCursor(null, WaypointsColumns._ID + "=?",
          new String[] { Long.toString(waypointId) }, WaypointsColumns._ID, 0);
      if (cursor != null && cursor.moveToFirst()) {
        return createWaypoint(cursor);
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return null;
  }

  @Override
  public Cursor getWaypointCursor(
      String selection, String[] selectionArgs, String sortOrder, int maxWaypoints) {
    return getWaypointCursor(null, selection, selectionArgs, sortOrder, maxWaypoints);
  }

  @Override
  public Cursor getWaypointCursor(long trackId, long minWaypointId, int maxWaypoints) {
    if (trackId < 0) {
      return null;
    }

    String selection;
    String[] selectionArgs;
    if (minWaypointId >= 0) {
      selection = WaypointsColumns.TRACKID + "=? AND " + WaypointsColumns._ID + ">=?";
      selectionArgs = new String[] { Long.toString(trackId), Long.toString(minWaypointId) };
    } else {
      selection = WaypointsColumns.TRACKID + "=?";
      selectionArgs = new String[] { Long.toString(trackId) };
    }
    return getWaypointCursor(null, selection, selectionArgs, WaypointsColumns._ID, maxWaypoints);
  }

  @Override
  public Uri insertWaypoint(Waypoint waypoint) {
    waypoint.setId(-1L);
    return contentResolver.insert(WaypointsColumns.CONTENT_URI, createContentValues(waypoint));
  }

  @Override
  public boolean updateWaypoint(Waypoint waypoint) {
    int rows = contentResolver.update(WaypointsColumns.CONTENT_URI, createContentValues(waypoint),
        WaypointsColumns._ID + "=?", new String[] { Long.toString(waypoint.getId()) });
    return rows == 1;
  }

  ContentValues createContentValues(Waypoint waypoint) {
    ContentValues values = new ContentValues();
  
    // Value < 0 indicates no id is available
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
  
    Location location = waypoint.getLocation();
    if (location != null) {
      values.put(WaypointsColumns.LONGITUDE, (int) (location.getLongitude() * 1E6));
      values.put(WaypointsColumns.LATITUDE, (int) (location.getLatitude() * 1E6));
      values.put(WaypointsColumns.TIME, location.getTime());
      if (location.hasAltitude()) {
        values.put(WaypointsColumns.ALTITUDE, location.getAltitude());
      }
      if (location.hasAccuracy()) {
        values.put(WaypointsColumns.ACCURACY, location.getAccuracy());
      }
      if (location.hasSpeed()) {
        values.put(WaypointsColumns.SPEED, location.getSpeed());
      }
      if (location.hasBearing()) {
        values.put(WaypointsColumns.BEARING, location.getBearing());
      }
    }
  
    TripStatistics tripStatistics = waypoint.getTripStatistics();
    if (tripStatistics != null) {
      values.put(WaypointsColumns.STARTTIME, tripStatistics.getStartTime());
      values.put(WaypointsColumns.TOTALDISTANCE, tripStatistics.getTotalDistance());
      values.put(WaypointsColumns.TOTALTIME, tripStatistics.getTotalTime());
      values.put(WaypointsColumns.MOVINGTIME, tripStatistics.getMovingTime());
      values.put(WaypointsColumns.AVGSPEED, tripStatistics.getAverageSpeed());
      values.put(WaypointsColumns.AVGMOVINGSPEED, tripStatistics.getAverageMovingSpeed());
      values.put(WaypointsColumns.MAXSPEED, tripStatistics.getMaxSpeed());
      values.put(WaypointsColumns.MINELEVATION, tripStatistics.getMinElevation());
      values.put(WaypointsColumns.MAXELEVATION, tripStatistics.getMaxElevation());
      values.put(WaypointsColumns.ELEVATIONGAIN, tripStatistics.getTotalElevationGain());
      values.put(WaypointsColumns.MINGRADE, tripStatistics.getMinGrade());
      values.put(WaypointsColumns.MAXGRADE, tripStatistics.getMaxGrade());
    }
    return values;
  }

  private Waypoint getNextStatisticsWaypointAfter(Waypoint waypoint) {
    Cursor cursor = null;
    try {
      String selection = WaypointsColumns._ID + ">?  AND " + WaypointsColumns.TRACKID + "=? AND "
          + WaypointsColumns.TYPE + "=" + Waypoint.TYPE_STATISTICS;
      String[] selectionArgs = new String[] {
          Long.toString(waypoint.getId()), Long.toString(waypoint.getTrackId()) };
      cursor = getWaypointCursor(null, selection, selectionArgs, WaypointsColumns._ID, 1);
      if (cursor != null && cursor.moveToFirst()) {
        return createWaypoint(cursor);
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return null;
  }

  /**
   * Gets a waypoint cursor.
   * 
   * @param projection the projection
   * @param selection the selection
   * @param selectionArgs the selection args
   * @param sortOrder the sort order
   * @param maxWaypoints the maximum number of waypoints
   */
  private Cursor getWaypointCursor(String[] projection, String selection, String[] selectionArgs,
      String sortOrder, int maxWaypoints) {
    if (sortOrder == null) {
      sortOrder = WaypointsColumns._ID;
    }
    if (maxWaypoints > 0) {
      sortOrder += " LIMIT " + maxWaypoints;
    }
    return contentResolver.query(
        WaypointsColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
  }

  @Override
  public int bulkInsertTrackPoint(Location[] locations, int length, long trackId) {
    if (length == -1) {
      length = locations.length;
    }
    ContentValues[] values = new ContentValues[length];
    for (int i = 0; i < length; i++) {
      values[i] = createContentValues(locations[i], trackId);
    }
    return contentResolver.bulkInsert(TrackPointsColumns.CONTENT_URI, values);
  }

  @Override
  public Location createTrackPoint(Cursor cursor) {
    Location location = new MyTracksLocation("");
    fillTrackPoint(cursor, new CachedTrackPointsIndexes(cursor), location);
    return location;
  }

  @Override
  public long getFirstTrackPointId(long trackId) {
    if (trackId < 0) {
      return -1L;
    }
    Cursor cursor = null;
    try {
      String selection = TrackPointsColumns._ID + "=(select min(" + TrackPointsColumns._ID
          + ") from " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID
          + "=?)";
      String[] selectionArgs = new String[] { Long.toString(trackId) };
      cursor = getTrackPointCursor(new String[] { TrackPointsColumns._ID }, selection,
          selectionArgs, TrackPointsColumns._ID);
      if (cursor != null && cursor.moveToFirst()) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID));
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return -1L;
  }
  
  @Override
  public long getLastTrackPointId(long trackId) {
    if (trackId < 0) {
      return -1L;
    }
    Cursor cursor = null;
    try {
      String selection = TrackPointsColumns._ID + "=(select max(" + TrackPointsColumns._ID
          + ") from " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID
          + "=?)";
      String[] selectionArgs = new String[] { Long.toString(trackId) };
      cursor = getTrackPointCursor(new String[] { TrackPointsColumns._ID }, selection,
          selectionArgs, TrackPointsColumns._ID);
      if (cursor != null && cursor.moveToFirst()) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID));
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return -1L;
  }

  @Override
  public Location getLastValidTrackPoint(long trackId) {
    if (trackId < 0) {
      return null;
    }
    String selection = TrackPointsColumns._ID + "=(select max(" + TrackPointsColumns._ID + ") from "
        + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID + "=? AND "
        + TrackPointsColumns.LATITUDE + "<=90000000)";
    String[] selectionArgs = new String[] { Long.toString(trackId) };
    return findTrackPointBy(selection, selectionArgs);
  }

  @Override
  public Cursor getTrackPointCursor(
      long trackId, long startTrackPointId, int maxLocations, boolean descending) {
    if (trackId < 0) {
      return null;
    }
  
    String selection;
    String[] selectionArgs;
    if (startTrackPointId >= 0) {
      String comparison = descending ? "<=" : ">=";
      selection = TrackPointsColumns.TRACKID + "=? AND " + TrackPointsColumns._ID + comparison
          + "?";
      selectionArgs = new String[] { Long.toString(trackId), Long.toString(startTrackPointId) };
    } else {
      selection = TrackPointsColumns.TRACKID + "=?";
      selectionArgs = new String[] { Long.toString(trackId) };
    }
  
    String sortOrder = TrackPointsColumns._ID;
    if (descending) {
      sortOrder += " DESC";
    }
    if (maxLocations > 0) {
      sortOrder += " LIMIT " + maxLocations;
    }
    return getTrackPointCursor(null, selection, selectionArgs, sortOrder);
  }

  @Override
  public LocationIterator getTrackPointLocationIterator(final long trackId, final long startTrackPointId,
      final boolean descending, final LocationFactory locationFactory) {
    if (locationFactory == null) {
      throw new IllegalArgumentException("locationFactory is null");
    }
    return new LocationIterator() {
      private long lastTrackPointId = -1L;
      private Cursor cursor = getCursor(startTrackPointId);
      private final CachedTrackPointsIndexes
          indexes = cursor != null ? new CachedTrackPointsIndexes(cursor)
              : null;
  
      /**
       * Gets the track point cursor.
       * 
       * @param trackPointId the starting track point id
       */
      private Cursor getCursor(long trackPointId) {
        return getTrackPointCursor(trackId, trackPointId, defaultCursorBatchSize, descending);
      }
  
      /**
       * Advances the cursor to the next batch. Returns true if successful.
       */
      private boolean advanceCursorToNextBatch() {
        long trackPointId = lastTrackPointId == -1L ? -1L : lastTrackPointId + (descending ? -1
            : 1);
        Log.d(TAG, "Advancing track point id: " + trackPointId);
        cursor.close();
        cursor = getCursor(trackPointId);
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
          if (cursor.getCount() != defaultCursorBatchSize) {
            return false;
          }
          return advanceCursorToNextBatch() && !cursor.isAfterLast();
        }
        return true;
      }
  
        @Override
      public Location next() {
        if (cursor == null) {
          throw new NoSuchElementException();
        }
        if (!cursor.moveToNext()) {
          if (!advanceCursorToNextBatch() || !cursor.moveToNext()) {
            throw new NoSuchElementException();
          }
        }
        lastTrackPointId = cursor.getLong(indexes.idIndex);
        Location location = locationFactory.createLocation();
        fillTrackPoint(cursor, indexes, location);
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

  @Override
  public Uri insertTrackPoint(Location location, long trackId) {
    return contentResolver.insert(
        TrackPointsColumns.CONTENT_URI, createContentValues(location, trackId));
  }

  /**
   * Creates the {@link ContentValues} for a {@link Location}.
   * 
   * @param location the location
   * @param trackId the track id
   */
  private ContentValues createContentValues(Location location, long trackId) {
    ContentValues values = new ContentValues();
    values.put(TrackPointsColumns.TRACKID, trackId);
    values.put(TrackPointsColumns.LONGITUDE, (int) (location.getLongitude() * 1E6));
    values.put(TrackPointsColumns.LATITUDE, (int) (location.getLatitude() * 1E6));
  
    // Hack for Samsung phones that don't properly populate the time field
    long time = location.getTime();
    if (time == 0) {
      time = System.currentTimeMillis();
    }
    values.put(TrackPointsColumns.TIME, time);
    if (location.hasAltitude()) {
      values.put(TrackPointsColumns.ALTITUDE, location.getAltitude());
    }
    if (location.hasAccuracy()) {
      values.put(TrackPointsColumns.ACCURACY, location.getAccuracy());
    }
    if (location.hasSpeed()) {
      values.put(TrackPointsColumns.SPEED, location.getSpeed());
    }
    if (location.hasBearing()) {
      values.put(TrackPointsColumns.BEARING, location.getBearing());
    }
  
    if (location instanceof MyTracksLocation) {
      MyTracksLocation myTracksLocation = (MyTracksLocation) location;
      if (myTracksLocation.getSensorDataSet() != null) {
        values.put(TrackPointsColumns.SENSOR, myTracksLocation.getSensorDataSet().toByteArray());
      }
    }
    return values;
  }

  /**
   * Fills a track point from a cursor.
   * 
   * @param cursor the cursor pointing to a location.
   * @param indexes the cached track points indexes
   * @param location the track point
   */
  private void fillTrackPoint(Cursor cursor, CachedTrackPointsIndexes indexes, Location location) {
    location.reset();
  
    if (!cursor.isNull(indexes.longitudeIndex)) {
      location.setLongitude(((double) cursor.getInt(indexes.longitudeIndex)) / 1E6);
    }
    if (!cursor.isNull(indexes.latitudeIndex)) {
      location.setLatitude(((double) cursor.getInt(indexes.latitudeIndex)) / 1E6);
    }
    if (!cursor.isNull(indexes.timeIndex)) {
      location.setTime(cursor.getLong(indexes.timeIndex));
    }
    if (!cursor.isNull(indexes.altitudeIndex)) {
      location.setAltitude(cursor.getFloat(indexes.altitudeIndex));
    }
    if (!cursor.isNull(indexes.accuracyIndex)) {
      location.setAccuracy(cursor.getFloat(indexes.accuracyIndex));
    }
    if (!cursor.isNull(indexes.speedIndex)) {
      location.setSpeed(cursor.getFloat(indexes.speedIndex));
    }
    if (!cursor.isNull(indexes.bearingIndex)) {
      location.setBearing(cursor.getFloat(indexes.bearingIndex));
    }
    if (location instanceof MyTracksLocation && !cursor.isNull(indexes.sensorIndex)) {
      MyTracksLocation myTracksLocation = (MyTracksLocation) location;
      try {
        myTracksLocation.setSensorDataSet(
            SensorDataSet.parseFrom(cursor.getBlob(indexes.sensorIndex)));
      } catch (InvalidProtocolBufferException e) {
        Log.w(TAG, "Failed to parse sensor data.", e);
      }
    }
  }

  private Location findTrackPointBy(String selection, String[] selectionArgs) {
    Cursor cursor = null;
    try {
      cursor = getTrackPointCursor(null, selection, selectionArgs, TrackPointsColumns._ID);
      if (cursor != null && cursor.moveToNext()) {
        return createTrackPoint(cursor);
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return null;
  }

  /**
   * Gets a track point cursor.
   * 
   * @param projection the projection
   * @param selection the selection
   * @param selectionArgs the selection arguments
   * @param sortOrder the sort order
   */
  private Cursor getTrackPointCursor(
      String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    return contentResolver.query(
        TrackPointsColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
  }

  /**
   * A cache of track points indexes.
   */
  private static class CachedTrackPointsIndexes {
    public final int idIndex;
    public final int longitudeIndex;
    public final int latitudeIndex;
    public final int timeIndex;
    public final int altitudeIndex;
    public final int accuracyIndex;
    public final int speedIndex;
    public final int bearingIndex;
    public final int sensorIndex;

    public CachedTrackPointsIndexes(Cursor cursor) {
      idIndex = cursor.getColumnIndex(TrackPointsColumns._ID);
      longitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.LONGITUDE);
      latitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.LATITUDE);
      timeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.TIME);
      altitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALTITUDE);
      accuracyIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.ACCURACY);
      speedIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SPEED);
      bearingIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.BEARING);
      sensorIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR);
    }
  }

  /**
   * Sets the default cursor batch size. For testing purpose.
   * 
   * @param defaultCursorBatchSize the default cursor batch size
   */
  void setDefaultCursorBatchSize(int defaultCursorBatchSize) {
    this.defaultCursorBatchSize = defaultCursorBatchSize;
  }
}