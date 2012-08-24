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
import com.google.android.apps.mytracks.stats.TripStatisticsBuilder;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
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

  // GPX tag names and attributes.
  private static final String TAG_TRACK = "trk";
  private static final String TAG_TRACK_POINT = "trkpt";
  private static final Object TAG_TRACK_SEGMENT = "trkseg";
  private static final String TAG_NAME = "name";
  private static final String TAG_DESCRIPTION = "desc";
  private static final String TAG_ALTITUDE = "ele";
  private static final String TAG_TIME = "time";
  private static final String ATT_LAT = "lat";
  private static final String ATT_LON = "lon";

  /**
   * The maximum number of locations to buffer for bulk-insertion into the
   * database.
   */
  private static final int MAX_BUFFERED_LOCATIONS = 512;

  /**
   * Utilities for accessing the content provider.
   */
  private final MyTracksProviderUtils myTracksProviderUtils;

  /**
   * List of track ids written in the database. Only contains successfully
   * written tracks.
   */
  private final List<Long> tracksIds;

  /**
   * Contains the current elements content.
   */
  private String content;

  /**
   * Currently location.
   */
  private Location location;

  /**
   * Last location.
   */
  private Location lastLocation;

  /**
   * Last segment location.
   */
  private Location lastSegmentLocation;

  /**
   * Currently reading track.
   */
  private Track track;

  /**
   * Statistics builder for the current track.
   */
  private TripStatisticsBuilder tripStatisticsBuilder;

  /**
   * Buffer of locations to be bulk-inserted into the database.
   */
  private Location[] bufferedPointInserts = new Location[MAX_BUFFERED_LOCATIONS];

  /**
   * Number of locations buffered to be inserted into the database.
   */
  private int numBufferedPointInserts = 0;

  /**
   * Number of locations already processed.
   */
  private int numberOfLocations = 0;

  /**
   * Number of segments already processed.
   */
  private int numberOfSegments = 0;

  /**
   * Used to identify if a track was written to the database but not yet
   * finished successfully.
   */
  private boolean isCurrentTrackRollbackable = false;

  /**
   * Flag to indicate if we're inside a track's xml element. Some sub elements
   * like name may be used in other parts of the GPX file, and we use this to
   * ignore them.
   */
  private boolean isInTrackElement = false;

  /**
   * Counter to find out which child level of the track we are processing.
   */
  private int trackChildDepth = 0;

  /**
   * SAX locator to get the current line information.
   */
  private Locator locator;

  /**
   * Reads GPS tracks from a GPX file and writes tracks and their coordinates to
   * the database.
   * 
   * @param inputStream an input stream for the GPX file
   * @param myTracksProviderUtils my tracks provider utils
   * @return long[] array of track ids written to the database.
   */
  public static long[] importGPXFile(
      final InputStream inputStream, final MyTracksProviderUtils myTracksProviderUtils)
      throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    SAXParser saxParser = saxParserFactory.newSAXParser();
    GpxImporter gpxImporter = new GpxImporter(myTracksProviderUtils);
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
      // Delete track if not finished
      gpxImporter.rollbackUnfinishedTracks();
    }

    return trackIds;
  }

  public GpxImporter(MyTracksProviderUtils myTracksProviderUtils) {
    this.myTracksProviderUtils = myTracksProviderUtils;
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
      if (localName.equals(TAG_TRACK_POINT)) {
        onTrackPointElementStart(attributes);
      } else if (localName.equals(TAG_TRACK_SEGMENT)) {
        onTrackSegmentElementStart();
      } else if (localName.equals(TAG_TRACK)) {
        throw new SAXException(createErrorMessage("Invalid GPX detected."));
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

    // process these elements only as sub-elements of track
    if (localName.equals(TAG_TRACK_POINT)) {
      onTrackPointElementEnd();
    } else if (localName.equals(TAG_ALTITUDE)) {
      onAltitudeElementEnd();
    } else if (localName.equals(TAG_TIME)) {
      onTimeElementEnd();
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
    } else if (localName.equals(TAG_TRACK)) {
      onTrackElementEnd();
      isInTrackElement = false;
      trackChildDepth = 0;
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
    if (isCurrentTrackRollbackable) {
      myTracksProviderUtils.deleteTrack(track.getId());
      isCurrentTrackRollbackable = false;
    }
  }

  /**
   * On track element start.
   */
  private void onTrackElementStart() {
    track = new Track();
    numberOfLocations = 0;

    Uri uri = myTracksProviderUtils.insertTrack(track);
    long trackId = Long.parseLong(uri.getLastPathSegment());
    track.setId(trackId);
    isCurrentTrackRollbackable = true;
  }

  /**
   * On track element end.
   */
  private void onTrackElementEnd() {
    if (lastLocation != null) {
      flushPoints();

      // Calculate statistics for the imported track
      tripStatisticsBuilder.updateTime(lastLocation.getTime());
      track.setStopId(getLastPointId());
    } else {
      tripStatisticsBuilder = new TripStatisticsBuilder(0L);
      tripStatisticsBuilder.updateTime(0L);
    }
    track.setTripStatistics(tripStatisticsBuilder.getTripStatistics());
    track.setNumberOfPoints(numberOfLocations);
    myTracksProviderUtils.updateTrack(track);
    tracksIds.add(track.getId());
    isCurrentTrackRollbackable = false;
    lastSegmentLocation = null;
    lastLocation = null;
    tripStatisticsBuilder = null;
  }

  /**
   * On track segment start.
   */
  private void onTrackSegmentElementStart() {
    if (numberOfSegments > 0) {
      // Add a segment separator
      location = new Location(LocationManager.GPS_PROVIDER);
      location.setLatitude(100.0);
      location.setLongitude(100.0);
      location.setAltitude(0);
      if (lastLocation != null) {
        location.setTime(lastLocation.getTime());
      }
      insertPoint(location);
      lastLocation = location;
      lastSegmentLocation = null;
      location = null;
    }
    numberOfSegments++;
  }

  /**
   * On track segment element end.
   */
  private void onTrackSegmentElementEnd() {
    // Nothing to be done
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
    
    Location newLocation = new Location(LocationManager.GPS_PROVIDER);
    try {
      newLocation.setLatitude(Double.parseDouble(latitude));
      newLocation.setLongitude(Double.parseDouble(longitude));
    } catch (NumberFormatException e) {
      throw new SAXException(createErrorMessage(
          "Unable to parse latitude/longitude: " + latitude + "/" + longitude), e);
    }
    location = newLocation;
  }

  /**
   * On track point element end.
   */
  private void onTrackPointElementEnd() throws SAXException {
    if (LocationUtils.isValidLocation(location)) {
      if (tripStatisticsBuilder == null) {
        // first point did not have a time, start stats builder without it
        tripStatisticsBuilder = new TripStatisticsBuilder(0);
      }
      tripStatisticsBuilder.addLocation(location, PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT);

      // insert in db
      insertPoint(location);

      // first track point?
      if (lastLocation == null && numberOfSegments == 1) {
        track.setStartId(getLastPointId());
      }

      lastLocation = location;
      lastSegmentLocation = location;
      location = null;
    } else {
      // invalid location - abort import
      throw new SAXException(createErrorMessage("Invalid location detected: " + location));
    }
  }

  /**
   * On time element end. Sets location time and doing additional calculations
   * as this is the last value required for the location. Also sets the start time
   * for the trip statistics builder as there is no start time in the track root
   * element.
   */
  private void onTimeElementEnd() throws SAXException {
    if (location == null) {
      return;
    }

    if (content == null) {
      return;
    }
      
    // Parse the time
    long time;
    try {
      time = StringUtils.getTime(content.trim());
    } catch (IllegalArgumentException e) {
      throw new SAXException(createErrorMessage("Unable to parse time: " + content), e);
    }

    // Calculate derived attributes from previous point
    if (lastSegmentLocation != null) {
      long timeDifference = time - lastSegmentLocation.getTime();

      // check for negative time change
      if (timeDifference < 0) {
        Log.w(Constants.TAG, "Found negative time change.");
      } else {

        /*
         * We don't have a speed and bearing in GPX, make something up from the
         * last two points. GPS points tend to have some inherent imprecision,
         * speed and bearing will likely be off, so the statistics for things
         * like max speed will also be off.
         */
        float speed = location.distanceTo(lastLocation) * 1000.0f / timeDifference;
        location.setSpeed(speed);
        location.setBearing(lastSegmentLocation.bearingTo(location));
      }
    }

    // Fill in the time
    location.setTime(time);

    // initialize start time with time of first track point
    if (tripStatisticsBuilder == null) {
      tripStatisticsBuilder = new TripStatisticsBuilder(time);
    }
  }

  /**
   * On altitude element end.
   */
  private void onAltitudeElementEnd() throws SAXException {
    if (location != null && content != null) {
      try {
        location.setAltitude(Double.parseDouble(content));
      } catch (NumberFormatException e) {
        throw new SAXException(createErrorMessage("Unable to parse altitude: " + content), e);
      }
    }
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
   * Inserts a point.
   * 
   * @param newLocation the location
   */
  private void insertPoint(Location newLocation) {
    bufferedPointInserts[numBufferedPointInserts] = newLocation;
    numBufferedPointInserts++;
    numberOfLocations++;

    if (numBufferedPointInserts >= MAX_BUFFERED_LOCATIONS) {
      flushPoints();
    }
  }

  /**
   * Flushes the points to the database.
   */
  private void flushPoints() {
    if (numBufferedPointInserts <= 0) {
      return;
    }
    myTracksProviderUtils.bulkInsertTrackPoint(
        bufferedPointInserts, numBufferedPointInserts, track.getId());
    numBufferedPointInserts = 0;
  }

  /**
   * Gets the imported track ids.
   *
   */
  private long[] getImportedTrackIds() {
    long[] result = new long[tracksIds.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = tracksIds.get(i);
    }
    return result;
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
