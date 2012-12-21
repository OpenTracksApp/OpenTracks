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

package com.google.android.apps.mytracks.io.file;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.DescriptionGeneratorImpl;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.android.apps.mytracks.services.TrackRecordingService;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.stats.TripStatisticsUpdater;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.TrackIconUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Imports GPX file to My Tracks.
 * 
 * @author Leif Hendrik Wilden
 * @author Steffen Horlacher
 * @author Rodrigo Damazio
 */
public class GpxImporter extends DefaultHandler {

  /**
   * Data for the current track.
   * 
   * @author Jimmy Shih
   */
  private class TrackData {

    // The current track
    Track track = new Track();

    // The parsed depth for the current track
    int parsedDepth = 0;

    // The number of segments processed for the current track
    int numberOfSegments = 0;

    /*
     * The last location in the current segment. Null if the current segment
     * doesn't have a last location.
     */
    Location lastLocationInCurrentSegment;

    // The number of locations processed for the current track
    int numberOfLocations = 0;

    // The trip statistics updater for the current track
    TripStatisticsUpdater tripStatisticsUpdater;

    // The default location time for the current track. -1L to not use it.
    long defaultLocationTime = -1L;

    // The buffered locations
    Location[] bufferedLocations = new Location[MAX_BUFFERED_LOCATIONS];

    // The number of buffered locations
    int numBufferedLocations = 0;
  }

  // GPX tags
  private static final String TAG_ALTITUDE = "ele";
  private static final String TAG_DESCRIPTION = "desc";
  private static final String TAG_COMMENT = "cmt";
  private static final String TAG_GPX = "gpx";
  private static final String TAG_NAME = "name";
  private static final String TAG_TIME = "time";
  private static final String TAG_TRACK = "trk";
  private static final String TAG_TRACK_POINT = "trkpt";
  private static final String TAG_TRACK_SEGMENT = "trkseg";
  private static final String TAG_TYPE = "type";
  private static final String TAG_WAYPOINT = "wpt";

  // GPX attributes
  private static final String ATT_LAT = "lat";
  private static final String ATT_LON = "lon";

  // The maximum number of buffered locations for bulk-insertion
  private static final int MAX_BUFFERED_LOCATIONS = 512;

  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;
  private final int minRecordingDistance;
  private final List<Long> tracksIds;

  // The SAX locator to get the current line information
  private Locator locator;

  // The current element content
  private String content;

  // The current track data
  private TrackData trackData = null;

  // The current waypoint
  private Waypoint currentWaypoint;

  // The list of waypoints
  private List<Waypoint> waypoints = new ArrayList<Waypoint>();

  /*
   * The current location, used for both track point location and waypoint
   * location.
   */
  private Location location;

  /**
   * Reads GPS tracks from a GPX file and writes tracks, waypoints, and track
   * points to the database.
   * 
   * @param context the context
   * @param inputStream the input stream for the GPX file
   * @param myTracksProviderUtils my tracks provider utils
   * @param minRecordingDistance the min recording distance
   * @return long[] array of track ids written to the database.
   */
  public static long[] importGPXFile(Context context, InputStream inputStream,
      MyTracksProviderUtils myTracksProviderUtils, int minRecordingDistance)
      throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    SAXParser saxParser = saxParserFactory.newSAXParser();
    GpxImporter gpxImporter = new GpxImporter(context, myTracksProviderUtils, minRecordingDistance);
    long[] trackIds = new long[0];

    try {
      long start = System.currentTimeMillis();

      saxParser.parse(inputStream, gpxImporter);

      long end = System.currentTimeMillis();
      Log.d(Constants.TAG, "Total import time: " + (end - start) + "ms");

      trackIds = gpxImporter.getImportedTrackIds();
      if (trackIds.length == 0) {
        throw new IOException("No track imported.");
      }
    } finally {
      // Delete the current track if not finished
      gpxImporter.rollbackUnfinishedTracks();
    }
    return trackIds;
  }

  public GpxImporter(
      Context context, MyTracksProviderUtils myTracksProviderUtils, int minRecordingDistance) {
    this.context = context;
    this.myTracksProviderUtils = myTracksProviderUtils;
    this.minRecordingDistance = minRecordingDistance;
    this.tracksIds = new ArrayList<Long>();
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    String newContent = new String(ch, start, length);
    if (content == null) {
      content = newContent;
    } else {
      /*
       * In 99% of the cases, a single call to this method will be made for each
       * sequence of characters we're interested in, so we'll rarely be
       * concatenating strings, thus not justifying the use of a StringBuilder.
       */
      content += newContent;
    }
  }

  @Override
  public void startElement(String uri, String localName, String name, Attributes attributes)
      throws SAXException {
    if (trackData != null) {
      trackData.parsedDepth++;
      if (localName.equals(TAG_TRACK)) {
        throw new SAXException(createErrorMessage("Invalid GPX. Already inside a track."));
      } else if (localName.equals(TAG_TRACK_SEGMENT)) {
        onTrackSegmentElementStart();
      } else if (localName.equals(TAG_TRACK_POINT)) {
        onTrackPointElementStart(attributes);
      }
    } else {
      if (localName.equals(TAG_TRACK)) {
        onTrackElementStart();
      } else if (localName.equals(TAG_WAYPOINT)) {
        onWaypointElementStart(attributes);
      }
    }
  }

  @Override
  public void endElement(String uri, String localName, String name) throws SAXException {
    if (localName.equals(TAG_NAME)) {
      onNameElementEnd();
    } else if (localName.equals(TAG_DESCRIPTION)) {
      onDescriptionElementEnd();
    } else if (localName.equals(TAG_TYPE)) {
      onTypeElementEnd();
    } else if (localName.equals(TAG_ALTITUDE)) {
      onAltitudeElementEnd();
    } else if (localName.equals(TAG_TIME)) {
      onTimeElementEnd();
    } else if (localName.equals(TAG_GPX)) {
      onGpxElementEnd();
    }

    if (trackData != null) {
      trackData.parsedDepth--;
      if (localName.equals(TAG_TRACK)) {
        onTrackElementEnd();
      } else if (localName.equals(TAG_TRACK_SEGMENT)) {
        onTrackSegmentElementEnd();
      } else if (localName.equals(TAG_TRACK_POINT)) {
        onTrackPointElementEnd();
      }
    } else {
      if (localName.equals(TAG_WAYPOINT)) {
        onWaypointElementEnd();
      } else if (localName.equals(TAG_COMMENT)) {
        onCommentElementEnd();
      }
    }

    // Reset element content
    content = null;
  }

  @Override
  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }

  /**
   * Rolls back last track if possible.
   */
  public void rollbackUnfinishedTracks() {
    if (trackData != null) {
      myTracksProviderUtils.deleteTrack(trackData.track.getId());
      trackData = null;
    }
  }

  /**
   * On gpx element end.
   */
  private void onGpxElementEnd() {
    int size = tracksIds.size();
    if (size > 0) {
      long trackId = tracksIds.get(size - 1);
      Track track = myTracksProviderUtils.getTrack(trackId);
      if (track != null) {
        addWaypoints(track);
      }
    }
  }

  /**
   * On track element start.
   */
  private void onTrackElementStart() {
    trackData = new TrackData();
    Uri uri = myTracksProviderUtils.insertTrack(trackData.track);
    trackData.track.setId(Long.parseLong(uri.getLastPathSegment()));
  }

  /**
   * On track element end.
   */
  private void onTrackElementEnd() {
    flushPoints(trackData);
    if (trackData.tripStatisticsUpdater == null) {
      long now = System.currentTimeMillis();
      trackData.tripStatisticsUpdater = new TripStatisticsUpdater(now);
      trackData.tripStatisticsUpdater.updateTime(now);
    }
    trackData.track.setTripStatistics(trackData.tripStatisticsUpdater.getTripStatistics());
    trackData.track.setNumberOfPoints(trackData.numberOfLocations);
    myTracksProviderUtils.updateTrack(trackData.track);
    tracksIds.add(trackData.track.getId());
    insertFirstWaypoint(trackData.track);
    trackData = null;
  }

  /**
   * On track segment start.
   */
  private void onTrackSegmentElementStart() {
    trackData.numberOfSegments++;

    /*
     * If not the first segment, add a pause separator if there is at least one
     * location in the last segment.
     */
    if (trackData.numberOfSegments > 1 && trackData.lastLocationInCurrentSegment != null) {
      insertPoint(createNewLocation(TrackRecordingService.PAUSE_LATITUDE, 0.0,
          trackData.lastLocationInCurrentSegment.getTime()));
    }
    trackData.lastLocationInCurrentSegment = null;
  }

  /**
   * On track segment element end.
   */
  private void onTrackSegmentElementEnd() {
    // Nothing needs to be done
  }

  /**
   * On track point element start.
   * 
   * @param attributes the attributes
   */
  private void onTrackPointElementStart(Attributes attributes) throws SAXException {
    parseLocation(attributes);
  }

  /**
   * On track point element end.
   */
  private void onTrackPointElementEnd() throws SAXException {
    if (!LocationUtils.isValidLocation(location)) {
      throw new SAXException(createErrorMessage("Invalid location detected: " + location));
    }

    if (trackData.numberOfSegments > 1 && trackData.lastLocationInCurrentSegment == null) {
      /*
       * If not the first segment, add a resume separator before adding the
       * first location.
       */
      insertPoint(
          createNewLocation(TrackRecordingService.RESUME_LATITUDE, 0.0, location.getTime()));
    }

    insertPoint(location);

    if (trackData.track.getStartId() == -1L) {
      // Flush the location to set the track start id and the track end id
      flushPoints(trackData);
    }

    trackData.lastLocationInCurrentSegment = location;
    location = null;
  }

  /**
   * On waypoint element start.
   * 
   * @param attributes the attributes
   */
  private void onWaypointElementStart(Attributes attributes) throws SAXException {
    if (currentWaypoint != null) {
      throw new SAXException(createErrorMessage("Found waypoint inside another waypoint."));
    }
    parseLocation(attributes);
    currentWaypoint = new Waypoint();
  }

  /**
   * On waypoint element end.
   */
  private void onWaypointElementEnd() throws SAXException {
    if (!LocationUtils.isValidLocation(location)) {
      throw new SAXException(createErrorMessage("Invalid location detected: " + location));
    }
    currentWaypoint.setLocation(location);
    waypoints.add(currentWaypoint);
    location = null;
    currentWaypoint = null;
  }

  /**
   * On name element end.
   */
  private void onNameElementEnd() {
    if (content != null) {
      String name = content.toString().trim();
      if (trackData != null && trackData.parsedDepth == 1) {
        trackData.track.setName(name);
      } else if (currentWaypoint != null) {
        currentWaypoint.setName(name);
      }
    }
  }

  /**
   * On description element end.
   */
  private void onDescriptionElementEnd() {
    if (content != null) {
      String description = content.toString().trim();
      if (trackData != null && trackData.parsedDepth == 1) {
        trackData.track.setDescription(description);
      } else if (currentWaypoint != null) {
        currentWaypoint.setDescription(description);
      }
    }
  }

  /**
   * On type element end.
   */
  private void onTypeElementEnd() {
    if (content != null) {
      String type = content.toString().trim();
      if (trackData != null && trackData.parsedDepth == 1) {
        trackData.track.setCategory(type);
        trackData.track.setIcon(TrackIconUtils.getIconValue(context, type));
      } else if (currentWaypoint != null) {
        currentWaypoint.setCategory(type);
      }
    }
  }

  /**
   * On altitude element end.
   */
  private void onAltitudeElementEnd() throws SAXException {
    if (location == null || content == null) {
      return;
    }

    try {
      location.setAltitude(Double.parseDouble(content));
    } catch (NumberFormatException e) {
      throw new SAXException(createErrorMessage("Unable to parse altitude: " + content), e);
    }
  }

  /**
   * On time element end. Sets location time and doing additional calculations
   * as this is the last value required for the location. Also sets the start
   * time for the trip statistics builder as there is no start time in the track
   * root element.
   */
  private void onTimeElementEnd() throws SAXException {
    if (location == null || content == null) {
      return;
    }

    // Parse the time
    long time;
    try {
      time = StringUtils.getTime(content.trim());
    } catch (IllegalArgumentException e) {
      throw new SAXException(createErrorMessage("Unable to parse time: " + content), e);
    }
    location.setTime(time);

    // Calculate derived attributes from the previous point
    if (trackData != null && trackData.lastLocationInCurrentSegment != null
        && trackData.lastLocationInCurrentSegment.getTime() != 0) {
      long timeDifference = time - trackData.lastLocationInCurrentSegment.getTime();

      // Check for negative time change
      if (timeDifference <= 0) {
        Log.w(Constants.TAG, "Time difference not postive.");
      } else {

        /*
         * We don't have a speed and bearing in GPX, make something up from the
         * last two points. GPS points tend to have some inherent imprecision,
         * speed and bearing will likely be off, so the statistics for things
         * like max speed will also be off.
         */
        float speed = trackData.lastLocationInCurrentSegment.distanceTo(location) * 1000.0f
            / timeDifference;
        location.setSpeed(speed);
      }
      location.setBearing(trackData.lastLocationInCurrentSegment.bearingTo(location));
    }
  }

  /**
   * On comment element end.
   */
  private void onCommentElementEnd() {
    if (content != null) {
      String comment = content.toString().trim();
      if (currentWaypoint != null) {
        WaypointType waypointType = WaypointType.STATISTICS.name().equals(comment) ? WaypointType.STATISTICS
            : WaypointType.WAYPOINT;
        currentWaypoint.setType(waypointType);
  
      }
    }
  }

  /**
   * Parses a location.
   * 
   * @param attributes the attributes
   */
  private void parseLocation(Attributes attributes) throws SAXException {
    if (location != null) {
      throw new SAXException(createErrorMessage("Found a location inside another one."));
    }
    String latitude = attributes.getValue(ATT_LAT);
    String longitude = attributes.getValue(ATT_LON);

    if (latitude == null || longitude == null) {
      throw new SAXException(createErrorMessage("Point with no longitude or latitude."));
    }
    double latitudeValue;
    double longitudeValue;
    try {
      latitudeValue = Double.parseDouble(latitude);
      longitudeValue = Double.parseDouble(longitude);
    } catch (NumberFormatException e) {
      throw new SAXException(
          createErrorMessage("Unable to parse latitude/longitude: " + latitude + "/" + longitude),
          e);
    }
    location = createNewLocation(latitudeValue, longitudeValue, -1L);
  }

  /**
   * Creates a new location
   * 
   * @param latitude location latitude
   * @param longitude location longitude
   * @param time location time
   */
  private Location createNewLocation(double latitude, double longitude, long time) {
    Location newLocation = new Location(LocationManager.GPS_PROVIDER);
    newLocation.setLatitude(latitude);
    newLocation.setLongitude(longitude);
    newLocation.setAltitude(0.0f);
    newLocation.setTime(time);
    newLocation.removeAccuracy();
    newLocation.removeBearing();
    newLocation.removeSpeed();
    return newLocation;
  }

  /**
   * Inserts a point.
   * 
   * @param newLocation the location
   */
  private void insertPoint(Location newLocation) {
    if (trackData.tripStatisticsUpdater == null) {
      // Set the default location time if the newLocation does not have a time
      trackData.defaultLocationTime = newLocation.getTime() == -1L ? System.currentTimeMillis()
          : -1L;
      trackData.tripStatisticsUpdater = new TripStatisticsUpdater(
          newLocation.getTime() != -1L ? newLocation.getTime() : trackData.defaultLocationTime);
    }
    if (trackData.defaultLocationTime != -1L) {
      newLocation.setTime(trackData.defaultLocationTime);
    }
    trackData.tripStatisticsUpdater.addLocation(newLocation, minRecordingDistance);

    trackData.bufferedLocations[trackData.numBufferedLocations] = newLocation;
    trackData.numBufferedLocations++;
    trackData.numberOfLocations++;

    if (trackData.numBufferedLocations >= MAX_BUFFERED_LOCATIONS) {
      flushPoints(trackData);
    }
  }

  /**
   * Flushes the points to the database.
   * 
   * @param data the track data
   */
  private void flushPoints(TrackData data) {
    if (data.numBufferedLocations <= 0) {
      return;
    }
    myTracksProviderUtils.bulkInsertTrackPoint(
        data.bufferedLocations, data.numBufferedLocations, data.track.getId());
    data.numBufferedLocations = 0;
    if (data.track.getStartId() == -1L) {
      data.track.setStartId(myTracksProviderUtils.getFirstTrackPointId(data.track.getId()));
    }
    data.track.setStopId(myTracksProviderUtils.getLastTrackPointId(data.track.getId()));
  }

  /**
   * Gets the imported track ids.
   */
  private long[] getImportedTrackIds() {
    long[] result = new long[tracksIds.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = tracksIds.get(i);
    }
    return result;
  }

  /**
   * Creates an error message.
   * 
   * @param message the message
   */
  private String createErrorMessage(String message) {
    return String.format(Locale.US, "Parsing error at line: %d column: %d. %s",
        locator.getLineNumber(), locator.getColumnNumber(), message);
  }

  /**
   * Inserts the first track waypoint, the track statistics waypoint.
   * 
   * @param track the track
   */
  private void insertFirstWaypoint(Track track) {
    String name = context.getString(R.string.marker_split_name_format, 0);
    String category = "";
    TripStatisticsUpdater updater = new TripStatisticsUpdater(
        track.getTripStatistics().getStartTime());
    TripStatistics tripStatistics = updater.getTripStatistics();
    String description = new DescriptionGeneratorImpl(context).generateWaypointDescription(
        tripStatistics);
    String icon = context.getString(R.string.marker_statistics_icon_url);
    double length = 0.0;
    long duration = 0L;
    Location waypointLocation = new Location("");
    waypointLocation.setLatitude(100);
    waypointLocation.setLongitude(180);
    Waypoint waypoint = new Waypoint(name, description, category, icon, track.getId(),
        WaypointType.STATISTICS, length, duration, -1L, -1L, waypointLocation, tripStatistics);
    myTracksProviderUtils.insertWaypoint(waypoint);
  }

  /**
   * Adds waypoints to a track.
   * 
   * @param track the track
   */
  private void addWaypoints(Track track) {
    Cursor trackPointCursor = null;
    try {
      trackPointCursor = myTracksProviderUtils.getTrackPointCursor(track.getId(), -1L, -1, false);
      if (trackPointCursor == null) {
        return;
      }
      int waypointPosition = -1;
      Waypoint waypoint = null;
      int trackPointPosition = -1;
      Location trackPoint = null;
      TripStatisticsUpdater trackTripStatisticstrackUpdater = new TripStatisticsUpdater(
          track.getTripStatistics().getStartTime());
      TripStatisticsUpdater markerTripStatisticsUpdater = new TripStatisticsUpdater(
          track.getTripStatistics().getStartTime());

      while (true) {
        if (waypoint == null) {
          waypointPosition++;
          waypoint = waypointPosition < waypoints.size() ? waypoints.get(waypointPosition) : null;
          if (waypoint == null) {
            // No more waypoints
            return;
          }
        }
        if (trackPoint == null) {
          trackPointPosition++;
          trackPoint = trackPointCursor.moveToPosition(trackPointPosition) ? myTracksProviderUtils
              .createTrackPoint(trackPointCursor)
              : null;
          if (trackPoint == null) {
            // No more track points. Ignore the rest of the waypoints.
            return;
          }
          trackTripStatisticstrackUpdater.addLocation(trackPoint, minRecordingDistance);
          markerTripStatisticsUpdater.addLocation(trackPoint, minRecordingDistance);
        }
        if (waypoint.getLocation().getTime() > trackPoint.getTime()) {
          trackPoint = null;
        } else if (waypoint.getLocation().getTime() < trackPoint.getTime()) {
          waypoint = null;
        } else {
          // The waypoint location time matches the track point time
          if (trackPoint.getLatitude() == waypoint.getLocation().getLatitude()
              && trackPoint.getLongitude() == waypoint.getLocation().getLongitude()) {

            // Get tripStatistics, description, and icon
            TripStatistics tripStatistics;
            String description;
            String icon;
            if (waypoint.getType() == WaypointType.STATISTICS) {
              tripStatistics = markerTripStatisticsUpdater.getTripStatistics();
              markerTripStatisticsUpdater = new TripStatisticsUpdater(trackPoint.getTime());
              description = new DescriptionGeneratorImpl(context).generateWaypointDescription(
                  tripStatistics);
              icon = context.getString(R.string.marker_statistics_icon_url);
            } else {
              tripStatistics = null;
              description = waypoint.getDescription();
              icon = context.getString(R.string.marker_waypoint_icon_url);
            }

            // Get length and duration
            double length = trackTripStatisticstrackUpdater.getTripStatistics().getTotalDistance();
            long duration = trackTripStatisticstrackUpdater.getTripStatistics().getTotalTime();

            // Insert waypoint
            Waypoint newWaypoint = new Waypoint(waypoint.getName(), description,
                waypoint.getCategory(), icon, track.getId(), waypoint.getType(), length, duration,
                -1L, -1L, trackPoint, tripStatistics);
            myTracksProviderUtils.insertWaypoint(newWaypoint);
          }
          waypoint = null;
        }
      }
    } finally {
      if (trackPointCursor != null) {
        trackPointCursor.close();
      }
    }
  }
}
