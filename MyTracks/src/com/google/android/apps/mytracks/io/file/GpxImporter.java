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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.location.Location;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Imports a GPX file.
 * 
 * @author Jimmy Shih
 */
public class GpxImporter extends AbstractImporter {

  private static final String TAG_DESCRIPTION = "desc";
  private static final String TAG_COMMENT = "cmt";
  private static final String TAG_ELEVATION = "ele";
  private static final String TAG_GPX = "gpx";
  private static final String TAG_NAME = "name";
  private static final String TAG_TIME = "time";
  private static final String TAG_TRACK = "trk";
  private static final String TAG_TRACK_POINT = "trkpt";
  private static final String TAG_TRACK_SEGMENT = "trkseg";
  private static final String TAG_TYPE = "type";
  private static final String TAG_WAYPOINT = "wpt";

  private static final String ATTRIBUTE_LAT = "lat";
  private static final String ATTRIBUTE_LON = "lon";
  
  /**
   * Constructor.
   * 
   * @param context the context
   * @param importTrackId track id to import to. -1L to import to a new track.
   */
  public GpxImporter(Context context, long importTrackId) {
    super(context, importTrackId);
  }

  @VisibleForTesting
  public GpxImporter(Context context, MyTracksProviderUtils myTracksProviderUtils) {
    super(context, -1L, myTracksProviderUtils);
  }

  @Override
  public void startElement(String uri, String localName, String tag, Attributes attributes)
      throws SAXException {
    if (tag.equals(TAG_WAYPOINT)) {
      onWaypointStart(attributes);
    } else if (tag.equals(TAG_TRACK)) {
      onTrackStart();
    } else if (tag.equals(TAG_TRACK_SEGMENT)) {
      onTrackSegmentStart();
    } else if (tag.equals(TAG_TRACK_POINT)) {
      onTrackPointStart(attributes);
    }
  }

  @Override
  public void endElement(String uri, String localName, String tag) throws SAXException {
    if (tag.equals(TAG_GPX)) {
      onFileEnd();
    } else if (tag.equals(TAG_WAYPOINT)) {
      onWaypointEnd();
    } else if (tag.equals(TAG_TRACK)) {
      onTrackEnd();
    } else if (tag.equals(TAG_TRACK_POINT)) {
      onTrackPointEnd();
    } else if (tag.equals(TAG_NAME)) {
      if (content != null) {
        name = content.trim();
      }
    } else if (tag.equals(TAG_DESCRIPTION)) {
      if (content != null) {
        description = content.trim();
      }
    } else if (tag.equals(TAG_TYPE)) {
      if (content != null) {
        category = content.trim();
      }
    } else if (tag.equals(TAG_TIME)) {
      if (content != null) {
        time = content.trim();
      }
    } else if (tag.equals(TAG_ELEVATION)) {
      if (content != null) {
        altitude = content.trim();
      }
    } else if (tag.equals(TAG_COMMENT)) {
      if (content != null) {
        waypointType = content.trim();
      }
    }

    // Reset element content
    content = null;
  }

  @Override
  protected void onTrackStart() throws SAXException {
    super.onTrackStart();
    name = null;
    description = null;
    category = null;
  }

  /**
   * On track point start.
   * 
   * @param attributes the attributes
   */
  private void onTrackPointStart(Attributes attributes) {
    latitude = attributes.getValue(ATTRIBUTE_LAT);
    longitude = attributes.getValue(ATTRIBUTE_LON);
    altitude = null;
    time = null;
  }

  /**
   * On track point end.
   */
  private void onTrackPointEnd() throws SAXException {
    Location location = getTrackPoint();
    if (location == null) {
      return;
    }
    insertTrackPoint(location);
  }

  /**
   * On waypoint start.
   * 
   * @param attributes the attributes
   */
  private void onWaypointStart(Attributes attributes) {
    name = null;
    description = null;
    category = null;
    latitude = attributes.getValue(ATTRIBUTE_LAT);
    longitude = attributes.getValue(ATTRIBUTE_LON);
    altitude = null;
    time = null;
    waypointType = null;
  }

  /**
   * On waypoint end.
   */
  private void onWaypointEnd() throws SAXException {
    addWaypoint(WaypointType.STATISTICS.name().equals(waypointType) ? WaypointType.STATISTICS
        : WaypointType.WAYPOINT);
  }
}
