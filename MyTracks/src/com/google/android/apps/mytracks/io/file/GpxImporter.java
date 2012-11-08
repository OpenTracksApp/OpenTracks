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
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.services.TrackRecordingService;
import com.google.android.apps.mytracks.stats.TripStatisticsUpdater;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.StringUtils;

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

  // GPX tag names and attributes
  private static final String TAG_ALTITUDE = "ele";
  private static final String TAG_DESCRIPTION = "desc";
  private static final String TAG_NAME = "name";
  private static final String TAG_TIME = "time";
  private static final String TAG_TRACK = "trk";
  private static final String TAG_TRACK_POINT = "trkpt";
  private static final Object TAG_TRACK_SEGMENT = "trkseg";

  private static final String ATT_LAT = "lat";
  private static final String ATT_LON = "lon";

  // The maximum number of buffered locations for bulk-insertion
  private static final int MAX_BUFFERED_LOCATIONS = 512;

  private final MyTracksProviderUtils myTracksProviderUtils;
  private final int minRecordingDistance;

  // List of successfully imported track ids
  private final List<Long> tracksIds;

  // The SAX locator to get the current line information
  private Locator locator;

  // The current element content
  private String content;

  // True if if we're inside a track's xml element
  private boolean isInTrackElement = false;

  // The current child depth for the current track
  private int trackChildDepth = 0;

  // The current track
  private Track track;

  // True if the current track parsing is finished
  private boolean isCurrentTrackFinished = true;

  // The number of track segments processed for the current track
  private int numberOfTrackSegments = 0;

  // The number of locations processed for the current track
  private int numberOfLocations = 0;

  // The trip statistics updater for the current track
  private TripStatisticsUpdater tripStatisticsUpdater;
  
  // True if the current track has a start time
  private boolean hasStartTime;
  
  // The import time
  private long importTime;

  // The current location
  private Location location;

  // The last location in the current segment
  private Location lastLocationInSegment;

  // The buffered locations
  private Location[] bufferedLocations = new Location[MAX_BUFFERED_LOCATIONS];

  // The number of buffered locations
  private int numBufferedLocations = 0;

  /**
   * Reads GPS tracks from a GPX file and writes tracks and their coordinates to
   * the database.
   * 
   * @param inputStream the input stream for the GPX file
   * @param myTracksProviderUtils my tracks provider utils
   * @param minRecordingDistance the min recording distance
   * @return long[] array of track ids written to the database.
   */
  public static long[] importGPXFile(InputStream inputStream,
      MyTracksProviderUtils myTracksProviderUtils, int minRecordingDistance)
      throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    SAXParser saxParser = saxParserFactory.newSAXParser();
    GpxImporter gpxImporter = new GpxImporter(myTracksProviderUtils, minRecordingDistance);
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

  public GpxImporter(MyTracksProviderUtils myTracksProviderUtils, int minRecordingDistance) {
    this.myTracksProviderUtils = myTracksProviderUtils;
    this.minRecordingDistance = minRecordingDistance;
    tracksIds = new ArrayList<Long>();
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
    if (isInTrackElement) {
      trackChildDepth++;
      if (localName.equals(TAG_TRACK)) {
        throw new SAXException(createErrorMessage("Invalid GPX. Already inside a track."));
      } else if (localName.equals(TAG_TRACK_SEGMENT)) {
        onTrackSegmentElementStart();
      } else if (localName.equals(TAG_TRACK_POINT)) {
        onTrackPointElementStart(attributes);
      } 
    } else if (localName.equals(TAG_TRACK)) {
      isInTrackElement = true;
      trackChildDepth = 0;
      onTrackElementStart();
    }
  }

  @Override
  public void endElement(String uri, String localName, String name) throws SAXException {
    if (!isInTrackElement) {
      content = null;
      return;
    }

    if (localName.equals(TAG_TRACK)) {
      onTrackElementEnd();
      isInTrackElement = false;
      trackChildDepth = 0;
    } else if (localName.equals(TAG_NAME)) {
      // we are only interested in the first level name element
      if (trackChildDepth == 1) {
        onNameElementEnd();
      }
    } else if (localName.equals(TAG_DESCRIPTION)) {
      // we are only interested in the first level description element
      if (trackChildDepth == 1) {
        onDescriptionElementEnd();
      }
    } else if (localName.equals(TAG_TRACK_SEGMENT)) {
      onTrackSegmentElementEnd();
    } else if (localName.equals(TAG_TRACK_POINT)) {
      onTrackPointElementEnd();
    } else if (localName.equals(TAG_ALTITUDE)) {
      onAltitudeElementEnd();
    } else if (localName.equals(TAG_TIME)) {
      onTimeElementEnd();
    }
    trackChildDepth--;

    // reset element content
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
    if (!isCurrentTrackFinished && track != null) {
      myTracksProviderUtils.deleteTrack(track.getId());
      isCurrentTrackFinished = true;
    }
  }

  /**
   * On track element start.
   */
  private void onTrackElementStart() {
    track = new Track();
    Uri uri = myTracksProviderUtils.insertTrack(track);
    track.setId(Long.parseLong(uri.getLastPathSegment()));
    isCurrentTrackFinished = false;
    numberOfTrackSegments = 0;
    numberOfLocations = 0;
    tripStatisticsUpdater = null;
  }

  /**
   * On track element end.
   */
  private void onTrackElementEnd() {
    flushPoints();
    if (tripStatisticsUpdater == null) {
      long now = System.currentTimeMillis();
      tripStatisticsUpdater = new TripStatisticsUpdater(now);
      tripStatisticsUpdater.updateTime(now);
    }
    track.setStopId(getLastPointId());
    track.setTripStatistics(tripStatisticsUpdater.getTripStatistics());
    track.setNumberOfPoints(numberOfLocations);
    myTracksProviderUtils.updateTrack(track);
    tracksIds.add(track.getId());
    isCurrentTrackFinished = true;
  }

  /**
   * On name element end.
   */
  private void onNameElementEnd() {
    if (content != null) {
      track.setName(content.toString().trim());
    }
  }

  /**
   * On description element end.
   */
  private void onDescriptionElementEnd() {
    if (content != null) {
      track.setDescription(content.toString().trim());
    }
  }

  /**
   * On track segment start.
   */
  private void onTrackSegmentElementStart() {
    numberOfTrackSegments++;
    // If not the first segment, add a pause separator if there is at least one
    // location in the last segment
    if (numberOfTrackSegments > 1 && lastLocationInSegment != null) {
      insertPoint(createNewLocation(
          TrackRecordingService.PAUSE_LATITUDE, 0.0, lastLocationInSegment.getTime()));
    }
    location = null;
    lastLocationInSegment = null;
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
    if (location != null) {
      throw new SAXException(createErrorMessage("Found a track point inside another one."));
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
   * On track point element end.
   */
  private void onTrackPointElementEnd() throws SAXException {
    if (!LocationUtils.isValidLocation(location)) {
      throw new SAXException(createErrorMessage("Invalid location detected: " + location));
    }

    if (numberOfTrackSegments > 1 && lastLocationInSegment == null) {
      // If not the first segment, add a resume separator before adding the
      // first location.
      insertPoint(
          createNewLocation(TrackRecordingService.RESUME_LATITUDE, 0.0, location.getTime()));
    }

    // insert in db
    insertPoint(location);

    // set the start id if necessary
    if (track.getStartId() == -1L) {
      track.setStartId(getFirstPointId());
    }

    lastLocationInSegment = location;
    location = null;
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

    // Calculate derived attributes from previous point
    if (lastLocationInSegment != null && lastLocationInSegment.getTime() != 0) {
      long timeDifference = time - lastLocationInSegment.getTime();

      // check for negative time change
      if (timeDifference <= 0) {
        Log.w(Constants.TAG, "Time difference not postive.");
      } else {

        /*
         * We don't have a speed and bearing in GPX, make something up from the
         * last two points. GPS points tend to have some inherent imprecision,
         * speed and bearing will likely be off, so the statistics for things
         * like max speed will also be off.
         */
        float speed = lastLocationInSegment.distanceTo(location) * 1000.0f / timeDifference;
        location.setSpeed(speed);
      }
      location.setBearing(lastLocationInSegment.bearingTo(location));
    }
  }

  /**
   * Creates a new location
   * @param latitude location latitude
   * @param longitude location longitude
   * @param time location time
   */
  private Location createNewLocation(double latitude, double longitude, long time) {
    Location loc = new Location(LocationManager.GPS_PROVIDER);
    loc.setLatitude(latitude);
    loc.setLongitude(longitude);
    loc.setAltitude(0.0f);
    loc.setTime(time);
    loc.removeAccuracy();
    loc.removeBearing();
    loc.removeSpeed();
    return loc;
  }

  /**
   * Inserts a point.
   * 
   * @param newLocation the location
   */
  private void insertPoint(Location newLocation) {
    if (tripStatisticsUpdater == null) {
      hasStartTime = newLocation.getTime() != -1L;
      importTime = System.currentTimeMillis();
      tripStatisticsUpdater = new TripStatisticsUpdater(
          hasStartTime ? newLocation.getTime() : importTime);
    }
    if (!hasStartTime) {
      newLocation.setTime(importTime);
    }
    tripStatisticsUpdater.addLocation(newLocation, minRecordingDistance);
    
    bufferedLocations[numBufferedLocations] = newLocation;
    numBufferedLocations++;
    numberOfLocations++;

    if (numBufferedLocations >= MAX_BUFFERED_LOCATIONS) {
      flushPoints();
    }
  }

  /**
   * Flushes the points to the database.
   */
  private void flushPoints() {
    if (numBufferedLocations <= 0) {
      return;
    }
    myTracksProviderUtils.bulkInsertTrackPoint(
        bufferedLocations, numBufferedLocations, track.getId());
    numBufferedLocations = 0;
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
   * Gets the first point id inserted into the database.
   */
  private long getFirstPointId() {
    flushPoints();
    return myTracksProviderUtils.getFirstTrackPointId(track.getId());
  }

  /**
   * Gets the last point id inserted into the database.
   */
  private long getLastPointId() {
    flushPoints();
    return myTracksProviderUtils.getLastTrackPointId(track.getId());
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
}
