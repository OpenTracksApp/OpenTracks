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

import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorData;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.location.Location;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;

/**
 * Write track as CSV to a file. See RFC 4180 for info on CSV. Output three
 * tables.<br>
 * The first table contains the track info. Its columns are:<br>
 * "Track name","Activity type","Track description" <br>
 * <br>
 * The second table contains the markers. Its columns are:<br>
 * "Marker name","Marker type","Marker description","Latitude (deg)","Longitude
 * (deg)","Altitude (m)","Bearing (deg)","Accuracy (m)","Speed (m/s)","Time"<br>
 * <br>
 * The thrid table contains the points. Its columns are:<br>
 * "Segment","Point","Latitude (deg)","Longitude (deg)","Altitude (m)","Bearing
 * (deg)","Accuracy (m)","Speed (m/s)","Time","Power (W)","Cadence (rpm)","Heart
 * rate (bpm)","Battery level (%)"<br>
 *
 * @author Rodrigo Damazio
 */
public class CsvTrackWriter implements TrackFormatWriter {

  private static final NumberFormat SHORT_FORMAT = NumberFormat.getInstance();

  static {
    SHORT_FORMAT.setMaximumFractionDigits(4);
  }

  private final Context context;
  private PrintWriter printWriter;
  private int segmentIndex;
  private int pointIndex;

  public CsvTrackWriter(Context context) {
    this.context = context;
  }

  @Override
  public String getExtension() {
    return TrackFileFormat.CSV.getExtension();
  }

  @Override
  public void prepare(OutputStream out) {
    printWriter = new PrintWriter(out);
    segmentIndex = 0;
    pointIndex = 0;
  }

  @Override
  public void close() {
    printWriter.close();
  }

  @Override
  public void writeHeader(Track track) {
    writeCommaSeparatedLine(context.getString(R.string.generic_name),
        context.getString(R.string.track_edit_activity_type_hint),
        context.getString(R.string.generic_description));
    writeCommaSeparatedLine(track.getName(), track.getCategory(), track.getDescription());
    writeCommaSeparatedLine();
  }

  @Override
  public void writeFooter() {
    // Do nothing
  }

  @Override
  public void writeBeginWaypoints() {
    writeCommaSeparatedLine(context.getString(R.string.generic_name),
        context.getString(R.string.marker_edit_marker_type_hint),
        context.getString(R.string.generic_description),
        context.getString(R.string.description_location_latitude),
        context.getString(R.string.description_location_longitude),
        context.getString(R.string.description_location_altitude),
        context.getString(R.string.description_location_bearing),
        context.getString(R.string.description_location_accuracy),
        context.getString(R.string.description_location_speed),
        context.getString(R.string.description_time));
  }

  @Override
  public void writeEndWaypoints() {
    writeCommaSeparatedLine();
  }

  @Override
  public void writeWaypoint(Waypoint waypoint) {
    Location location = waypoint.getLocation();
    writeCommaSeparatedLine(waypoint.getName(),
        waypoint.getCategory(),
        waypoint.getDescription(),
        Double.toString(location.getLatitude()),
        Double.toString(location.getLongitude()),
        getAltitude(location),
        getBearing(location),
        getAccuracy(location),
        getSpeed(location),
        StringUtils.formatDateTimeIso8601(location.getTime()));
  }

  @Override
  public void writeBeginTrack(Track track, Location firstPoint) {
    writeCommaSeparatedLine(context.getString(R.string.description_track_segment),
        context.getString(R.string.description_track_point),
        context.getString(R.string.description_location_latitude),
        context.getString(R.string.description_location_longitude),
        context.getString(R.string.description_location_altitude),
        context.getString(R.string.description_location_bearing),
        context.getString(R.string.description_location_accuracy),
        context.getString(R.string.description_location_speed),
        context.getString(R.string.description_time),
        context.getString(R.string.description_sensor_power),
        context.getString(R.string.description_sensor_cadence),
        context.getString(R.string.description_sensor_heart_rate));
  }

  @Override
  public void writeEndTrack(Track track, Location lastPoint) {
    // Do nothing
  }

  @Override
  public void writeOpenSegment() {
    segmentIndex++;
    pointIndex = 0;
  }

  @Override
  public void writeCloseSegment() {
    // Do nothing
  }

  @Override
  public void writeLocation(Location location) {
    String power = null;
    String cadence = null;
    String heartRate = null;
    if (location instanceof MyTracksLocation) {
      SensorDataSet sensorDataSet = ((MyTracksLocation) location).getSensorDataSet();

      if (sensorDataSet != null) {
        if (sensorDataSet.hasPower()) {
          SensorData sensorData = sensorDataSet.getPower();
          if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
            power = Double.toString(sensorData.getValue());
          }
        }
        if (sensorDataSet.hasCadence()) {
          SensorData sensorData = sensorDataSet.getCadence();
          if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
            cadence = Double.toString(sensorData.getValue());
          }
        }
        if (sensorDataSet.hasHeartRate()) {
          SensorData sensorData = sensorDataSet.getHeartRate();
          if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
            heartRate = Double.toString(sensorData.getValue());
          }
        }
      }
    }
    pointIndex++;
    writeCommaSeparatedLine(Integer.toString(segmentIndex),
        Integer.toString(pointIndex),
        Double.toString(location.getLatitude()),
        Double.toString(location.getLongitude()),
        getAltitude(location),
        getBearing(location),
        getAccuracy(location),
        getSpeed(location),
        StringUtils.formatDateTimeIso8601(location.getTime()),
        power,
        cadence,
        heartRate);
  }
  
  private String getAltitude(Location location) {
    return location.hasAltitude() ? Double.toString(location.getAltitude()) : null;
  }
  
  private String getBearing(Location location) {
    return location.hasBearing() ? Double.toString(location.getBearing()) : null;
  }
  
  private String getAccuracy(Location location) {
    return location.hasAccuracy() ? SHORT_FORMAT.format(location.getAccuracy()) : null;
  }
  
  private String getSpeed(Location location) {
    return location.hasSpeed() ? SHORT_FORMAT.format(location.getSpeed()) : null;
  }
  
  /**
   * Writes a single line of a CSV file.
   *
   * @param values the values to be written as CSV
   */
  private void writeCommaSeparatedLine(String... values) {
    StringBuilder builder = new StringBuilder();
    boolean isFirst = true;
    for (String value : values) {
      if (!isFirst) {
        builder.append(',');
      }
      isFirst = false;

      builder.append('"');
      if (value != null) {
        builder.append(value.replaceAll("\"", "\"\""));
      }
      builder.append('"');
    }
    printWriter.println(builder.toString());
  }
}
