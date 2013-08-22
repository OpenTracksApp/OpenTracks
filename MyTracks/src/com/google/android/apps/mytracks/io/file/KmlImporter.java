/*
 * Copyright 2012 Google Inc.
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

import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.location.Location;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Imports a KML file.
 * 
 * @author Jimmy Shih
 */
public class KmlImporter extends AbstractImporter {

  private static final String CADENCE = "cadence";
  private static final String HEART_RATE = "heart_rate";
  private static final String POWER = "power";

  private static final String STATISTICS_STYLE = "#statistics";
  private static final String WAYPOINT_STYLE = "#waypoint";

  private static final String TAG_COORDINATES = "coordinates";
  private static final String TAG_DESCRIPTION = "description";
  private static final String TAG_GX_COORD = "gx:coord";
  private static final String TAG_GX_MULTI_TRACK = "gx:MultiTrack";
  private static final String TAG_GX_SIMPLE_ARRAY_DATA = "gx:SimpleArrayData";
  private static final String TAG_GX_TRACK = "gx:Track";
  private static final String TAG_GX_VALUE = "gx:value";
  private static final String TAG_KML = "kml";
  private static final String TAG_NAME = "name";
  private static final String TAG_PLACEMARK = "Placemark";
  private static final String TAG_STYLE_URL = "styleUrl";
  private static final String TAG_VALUE = "value";
  private static final String TAG_WHEN = "when";

  private static final String ATTRIBUTE_NAME = "name";

  private String sensorName;
  private ArrayList<Location> locationList;
  private ArrayList<Integer> cadenceList;
  private ArrayList<Integer> heartRateList;
  private ArrayList<Integer> powerList;

  /**
   * Constructor.
   * 
   * @param context the context
   * @param importTrackId track id to import to. -1L to import to a new track.
   */
  public KmlImporter(Context context, long importTrackId) {
    super(context, importTrackId);
  }

  @VisibleForTesting
  public KmlImporter(Context context, MyTracksProviderUtils myTracksProviderUtils) {
    super(context, -1L, myTracksProviderUtils);
  }

  @Override
  public void startElement(String uri, String localName, String tag, Attributes attributes)
      throws SAXException {
    if (tag.equals(TAG_PLACEMARK)) {
      onWaypointStart();
    } else if (tag.equals(TAG_GX_MULTI_TRACK)) {
      onTrackStart();
    } else if (tag.equals(TAG_GX_TRACK)) {
      onTrackSegmentStart();
    } else if (tag.equals(TAG_GX_SIMPLE_ARRAY_DATA)) {
      onSensorDataStart(attributes);
    }
  }

  @Override
  public void endElement(String uri, String localName, String tag) throws SAXException {
    if (tag.equals(TAG_KML)) {
      onFileEnd();
    } else if (tag.equals(TAG_PLACEMARK)) {
      onWaypointEnd();
    } else if (localName.equals(TAG_COORDINATES)) {
      onWaypointLocationEnd();
    } else if (tag.equals(TAG_GX_MULTI_TRACK)) {
      onTrackEnd();
    } else if (tag.equals(TAG_GX_TRACK)) {
      onTrackSegmentEnd();
    } else if (tag.equals(TAG_GX_COORD)) {
      onTrackPointEnd();
    } else if (tag.equals(TAG_GX_VALUE)) {
      onSensorValueEnd();
    } else if (tag.equals(TAG_NAME)) {
      if (content != null) {
        name = content.trim();
      }
    } else if (localName.equals(TAG_DESCRIPTION)) {
      if (content != null) {
        description = content.trim();
      }
    } else if (localName.equals(TAG_VALUE)) {
      if (content != null) {
        category = content.trim();
      }
    } else if (localName.equals(TAG_WHEN)) {
      if (content != null) {
        time = content.trim();
      }
    } else if (localName.equals(TAG_STYLE_URL)) {
      if (content != null) {
        waypointType = content.trim();
      }
    }

    // Reset element content
    content = null;
  }

  /**
   * On waypoint start.
   */
  private void onWaypointStart() {
    // Reset all Placemark variables
    name = null;
    description = null;
    category = null;
    latitude = null;
    longitude = null;
    altitude = null;
    time = null;
    waypointType = null;
  }

  /**
   * On waypoint end.
   */
  private void onWaypointEnd() throws SAXException {
    // Add a waypoint if the waypointType matches
    WaypointType type = null;
    if (WAYPOINT_STYLE.equals(waypointType)) {
      type = WaypointType.WAYPOINT;
    } else if (STATISTICS_STYLE.equals(waypointType)) {
      type = WaypointType.STATISTICS;
    }
    if (type == null) {
      return;
    }
    addWaypoint(type);
  }

  /**
   * On waypoint location end.
   */
  private void onWaypointLocationEnd() {
    if (content != null) {
      String parts[] = content.trim().split(",");
      if (parts.length != 2 && parts.length != 3) {
        return;
      }
      longitude = parts[0];
      latitude = parts[1];
      altitude = parts.length == 3 ? parts[2] : null;      
    }
  }

  @Override
  protected void onTrackSegmentStart() {
    super.onTrackSegmentStart();
    locationList = new ArrayList<Location>();
    powerList = new ArrayList<Integer>();
    cadenceList = new ArrayList<Integer>();
    heartRateList = new ArrayList<Integer>();
  }

  /**
   * On track segment end.
   */
  private void onTrackSegmentEnd() {
    // Close a track segment by inserting the segment locations
    boolean hasPower = powerList.size() == locationList.size();
    boolean hasCadence = cadenceList.size() == locationList.size();
    boolean hasHeartRate = heartRateList.size() == locationList.size();

    for (int i = 0; i < locationList.size(); i++) {
      Location location = locationList.get(i);

      if (!hasPower && !hasCadence && !hasHeartRate) {
        insertTrackPoint(location);
      } else {
        SensorDataSet.Builder builder = Sensor.SensorDataSet.newBuilder();
        if (hasPower) {
          builder.setPower(Sensor.SensorData.newBuilder()
              .setValue(powerList.get(i)).setState(Sensor.SensorState.SENDING));
        }
        if (hasCadence) {
          builder.setCadence(Sensor.SensorData.newBuilder()
              .setValue(cadenceList.get(i)).setState(Sensor.SensorState.SENDING));
        }
        if (hasHeartRate) {
          builder.setHeartRate(Sensor.SensorData.newBuilder()
              .setValue(heartRateList.get(i)).setState(Sensor.SensorState.SENDING));
        }
        SensorDataSet sensorDataSet = builder.setCreationTime(location.getTime()).build();
        MyTracksLocation myTracksLocation = new MyTracksLocation(location, sensorDataSet);
        insertTrackPoint(myTracksLocation);
      }
    }
  }

  /**
   * On track point end. gx:coord end tag.
   */
  private void onTrackPointEnd() throws SAXException {
    // Add location to locationList
    if (content == null) {
      return;
    }
    String parts[] = content.trim().split(" ");
    if (parts.length != 2 && parts.length != 3) {
      return;
    }
    longitude = parts[0];
    latitude = parts[1];
    altitude = parts.length == 3 ? parts[2] : null;

    Location location = getTrackPoint();
    if (location == null) {
      return;
    }
    locationList.add(location);
    time = null;
  }

  /**
   * On sensor data start. gx:SimpleArrayData start tag.
   * 
   * @param attributes
   */
  private void onSensorDataStart(Attributes attributes) {
    sensorName = attributes.getValue(ATTRIBUTE_NAME);
  }

  /**
   * On sensor value end. gx:value end tag.
   */
  private void onSensorValueEnd() throws SAXException {
    if (content != null) {
      int value;
      try {
        value = Integer.parseInt(content.trim());
      } catch (NumberFormatException e) {
        throw new SAXException(createErrorMessage("Unable to parse gx:value:" + content), e);
      }
      if (POWER.equals(sensorName)) {
        powerList.add(value);
      } else if (HEART_RATE.equals(sensorName)) {
        heartRateList.add(value);
      } else if (CADENCE.equals(sensorName)) {
        cadenceList.add(value);
      }
    }
  }
}
