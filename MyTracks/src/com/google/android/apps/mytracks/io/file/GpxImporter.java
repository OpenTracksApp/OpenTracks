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
import com.google.android.apps.mytracks.util.StringUtils;

import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Imports GPX XML files to the my tracks provider.
 *
 * TODO: Show progress indication to the user.
 *
 * @author Leif Hendrik Wilden
 * @author Steffen Horlacher
 * @author Rodrigo Damazio
 */
public class GpxImporter extends DefaultHandler {

  /*
   * GPX-XML tag names and attributes.
   */
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
   * The maximum number of locations to buffer for bulk-insertion into the database.
   */
  private static final int MAX_BUFFERED_LOCATIONS = 512;

  /**
   * Utilities for accessing the contnet provider.
   */
  private final MyTracksProviderUtils providerUtils;

  /**
   * List of track ids written in the database. Only contains successfully
   * written tracks.
   */
  private final List<Long> tracksWritten;

  /**
   * Contains the current elements content.
   */
  private String content;

  /**
   * Currently reading location.
   */
  private Location location;

  /**
   * Previous location, required for calculations.
   */
  private Location lastLocation;
  
  /**
   * Currently reading track.
   */
  private Track track;

  /**
   * Statistics builder for the current track.
   */
  private TripStatisticsBuilder statsBuilder;

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
  private int numberOfLocations;

  /**
   * Number of segments already processed.
   */
  private int numberOfSegments;

  /**
   * Used to identify if a track was written to the database but not yet
   * finished successfully.
   */
  private boolean isCurrentTrackRollbackable;

  /**
   * Flag to indicate if we're inside a track's xml element.
   * Some sub elements like name may be used in other parts of the gpx file,
   * and we use this to ignore them.
   */
  private boolean isInTrackElement;

  /**
   * Counter to find out which child level of track we are processing.
   */
  private int trackChildDepth;

  /**
   * SAX-Locator to get current line information.
   */
  private Locator locator;
  private Location lastSegmentLocation;

  /**
   * Reads GPS tracks from a GPX file and writes tracks and their coordinates to
   * the database.
   * 
   * @param is a input steam with gpx-xml data
   * @return long[] array of track ids written in the database
   * @throws SAXException a parsing error
   * @throws ParserConfigurationException internal error
   * @throws IOException a file reading problem
   */
  public static long[] importGPXFile(final InputStream is,
      final MyTracksProviderUtils providerUtils)
      throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    GpxImporter handler = new GpxImporter(providerUtils);
    SAXParser parser = factory.newSAXParser();
    long[] trackIds = null;

    try {
      long start = System.currentTimeMillis();

      parser.parse(is, handler);

      long end = System.currentTimeMillis();
      Log.d(Constants.TAG, "Total import time: " + (end - start) + "ms");

      trackIds = handler.getImportedTrackIds();
    } finally {
      // delete track if not finished
      handler.rollbackUnfinishedTracks();
    }

    return trackIds;
  }

  /**
   * Constructor, requires providerUtils for writing tracks the database.
   */
  public GpxImporter(MyTracksProviderUtils providerUtils) {
    this.providerUtils = providerUtils;
    tracksWritten = new ArrayList<Long>();
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    String newContent = new String(ch, start, length);
    if (content == null) {
      content = newContent;
    } else {
      // In 99% of the cases, a single call to this method will be made for each
      // sequence of characters we're interested in, so we'll rarely be
      // concatenating strings, thus not justifying the use of a StringBuilder.
      content += newContent;
    }
  }

  @Override
  public void startElement(String uri, String localName, String name,
      Attributes attributes) throws SAXException {
    if (isInTrackElement) {
      trackChildDepth++;
      if (localName.equals(TAG_TRACK_POINT)) {
        onTrackPointElementStart(attributes);
      } else if (localName.equals(TAG_TRACK_SEGMENT)) {
        onTrackSegmentElementStart();
      } else if (localName.equals(TAG_TRACK)) {
        String msg = createErrorMessage("Invalid GPX-XML detected");
        throw new SAXException(msg);
      }
    } else if (localName.equals(TAG_TRACK)) {
      isInTrackElement = true;
      trackChildDepth = 0;
      onTrackElementStart();
    }
  }

  @Override
  public void endElement(String uri, String localName, String name)
      throws SAXException {
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
   * Create a new Track object and insert empty track in database. Track will be
   * updated with missing values later.
   */
  private void onTrackElementStart() {
    track = new Track();
    numberOfLocations = 0;

    Uri trackUri = providerUtils.insertTrack(track);
    long trackId = Long.parseLong(trackUri.getLastPathSegment());
    track.setId(trackId);
    isCurrentTrackRollbackable = true;
  }

  private void onDescriptionElementEnd() {
    track.setDescription(content.toString().trim());
  }

  private void onNameElementEnd() {
    track.setName(content.toString().trim());
  }

  /**
   * Track segment started.
   */
  private void onTrackSegmentElementStart() {
    if (numberOfSegments > 0) {
      // Add a segment separator:
      location = new Location(LocationManager.GPS_PROVIDER);
      location.setLatitude(100.0);
      location.setLongitude(100.0);
      location.setAltitude(0);
      if (lastLocation != null) {
        location.setTime(lastLocation.getTime());
      }
      insertTrackPoint(location);
      lastLocation = location;
      lastSegmentLocation = null;
      location = null;
    }

    numberOfSegments++;
  }

  /**
   * Reads trackpoint attributes and assigns them to the current location.
   *
   * @param attributes xml attributes
   */
  private void onTrackPointElementStart(Attributes attributes) throws SAXException {
    if (location != null) {
      String errorMsg = createErrorMessage("Found a track point inside another one.");
      throw new SAXException(errorMsg);
    }

    location = createLocationFromAttributes(attributes);
  }

  /**
   * Creates and returns a location with the position parsed from the given
   * attributes.
   *
   * @param attributes the attributes to parse
   * @return the created location
   * @throws SAXException if the attributes cannot be parsed
   */
  private Location createLocationFromAttributes(Attributes attributes) throws SAXException {
    String latitude = attributes.getValue(ATT_LAT);
    String longitude = attributes.getValue(ATT_LON);

    if (latitude == null || longitude == null) {
      throw new SAXException(createErrorMessage("Point with no longitude or latitude"));
    }

    // create new location and set attributes
    Location loc = new Location(LocationManager.GPS_PROVIDER);
    try {
      loc.setLatitude(Double.parseDouble(latitude));
      loc.setLongitude(Double.parseDouble(longitude));
    } catch (NumberFormatException e) {
      String msg = createErrorMessage(
          "Unable to parse lat/long: " + latitude + "/" + longitude);
      throw new SAXException(msg, e);
    }
    return loc;
  }

  /**
   * Track point finished, write in database.
   * 
   * @throws SAXException - thrown if track point is invalid
   */
  private void onTrackPointElementEnd() throws SAXException {
    if (LocationUtils.isValidLocation(location)) {
      if (statsBuilder == null) {
        // first point did not have a time, start stats builder without it
        statsBuilder = new TripStatisticsBuilder(0);
      }
      statsBuilder.addLocation(location, location.getTime());

      // insert in db
      insertTrackPoint(location);

      // first track point?
      if (lastLocation == null && numberOfSegments == 1) {
        track.setStartId(getLastPointId());
      }

      lastLocation = location;
      lastSegmentLocation = location;
      location = null;
    } else {
      // invalid location - abort import
      String msg = createErrorMessage("Invalid location detected: " + location);
      throw new SAXException(msg);
    }
  }

  private void insertTrackPoint(Location loc) {
    bufferedPointInserts[numBufferedPointInserts] = loc;
    numBufferedPointInserts++;
    numberOfLocations++;

    if (numBufferedPointInserts >= MAX_BUFFERED_LOCATIONS) {
      flushPointInserts();
    }
  }

  private void flushPointInserts() {
    if (numBufferedPointInserts <= 0) { return; }

    providerUtils.bulkInsertTrackPoints(bufferedPointInserts, numBufferedPointInserts, track.getId());
    numBufferedPointInserts = 0;
  }

  /**
   * Track segment finished.
   */
  private void onTrackSegmentElementEnd() {
    // Nothing to be done
  }

  /**
   * Track finished - update in database.
   */
  private void onTrackElementEnd() {
    if (lastLocation != null) {
      flushPointInserts();

      // Calculate statistics for the imported track and update
      statsBuilder.pauseAt(lastLocation.getTime());
      track.setStopId(getLastPointId());
      track.setNumberOfPoints(numberOfLocations);
      track.setStatistics(statsBuilder.getStatistics());
      providerUtils.updateTrack(track);
      tracksWritten.add(track.getId());
      isCurrentTrackRollbackable = false;
      lastSegmentLocation = null;
      lastLocation = null;
      statsBuilder = null;
    } else {
      // track contains no track points makes no real
      // sense to import it as we have no location
      // information -> roll back
      rollbackUnfinishedTracks();
    }
  }

  /**
   * Setting time and doing additional calculations as this is the last value
   * required. Also sets the start time for track and statistics as there is no
   * start time in the track root element.
   * 
   * @throws SAXException on parsing errors
   */
  private void onTimeElementEnd() throws SAXException {
    if (location == null) { return; }

    // Parse the time
    long time;
    try {
      time = StringUtils.parseXmlDateTime(content.trim());
    } catch (IllegalArgumentException e) {
      String msg = createErrorMessage("Unable to parse time: " + content);
      throw new SAXException(msg, e);
    }

    // Calculate derived attributes from previous point
    if (lastSegmentLocation != null) {
      long timeDifference = time - lastSegmentLocation.getTime();

      // check for negative time change
      if (timeDifference < 0) {
        Log.w(Constants.TAG, "Found negative time change.");
      } else {

        // We don't have a speed and bearing in GPX, make something up from
        // the last two points.
        // TODO GPS points tend to have some inherent imprecision,
        // speed and bearing will likely be off, so the statistics for things like
        // max speed will also be off.
        float speed = location.distanceTo(lastLocation) * 1000.0f / timeDifference;
        location.setSpeed(speed);
        location.setBearing(lastSegmentLocation.bearingTo(location));
      }
    }

    // Fill in the time
    location.setTime(time);

    // initialize start time with time of first track point
    if (statsBuilder == null) {
      statsBuilder = new TripStatisticsBuilder(time);
    }
  }

  private void onAltitudeElementEnd() throws SAXException {
    if (location != null) {
      try {
        location.setAltitude(Double.parseDouble(content));
      } catch (NumberFormatException e) {
        String msg = createErrorMessage("Unable to parse altitude: " + content);
        throw new SAXException(msg, e);
      }
    }
  }

  /**
   * Deletes the last track if it was not completely imported.
   */
  public void rollbackUnfinishedTracks() {
    if (isCurrentTrackRollbackable) {
      providerUtils.deleteTrack(track.getId());
      isCurrentTrackRollbackable = false;
    }
  }

  /**
   * Get all track ids of the tracks created by this importer run.
   * 
   * @return array of track ids
   */
  private long[] getImportedTrackIds() {
    // Convert from java.lang.Long for convenience
    long[] result = new long[tracksWritten.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = tracksWritten.get(i);
    }
    return result;
  }

  /**
   * Returns the ID of the last point inserted into the database.
   */
  private long getLastPointId() {
    flushPointInserts();
    
    return providerUtils.getLastLocationId(track.getId());
  }

  /**
   * Builds a parsing error message with current line information.
   * 
   * @param details details about the error, will be appended
   * @return error message string with current line information
   */
  private String createErrorMessage(String details) {
    StringBuffer msg = new StringBuffer();
    msg.append("Parsing error at line: ");
    msg.append(locator.getLineNumber());
    msg.append(" column: ");
    msg.append(locator.getColumnNumber());
    msg.append(". ");
    msg.append(details);
    return msg.toString();
  }
}
