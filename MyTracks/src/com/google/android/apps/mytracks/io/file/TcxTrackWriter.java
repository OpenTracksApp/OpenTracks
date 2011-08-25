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
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.util.SystemUtils;

import android.content.Context;
import android.location.Location;
import android.os.Build;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Write out a a track in the Garmin training center database, tcx format.
 * As defined by:
 * http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2
 *
 * The TCX file written by this class has been verified as compatible with
 * Garmin Training Center 3.5.3.
 *
 * @author Sandor Dornbush
 * @author Dominik Ršttsches
 */
public class TcxTrackWriter implements TrackFormatWriter {
  protected static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  // These are the only sports allowed by the TCX v2 specification for fields
  // of type Sport_t.
  private static final String TCX_SPORT_BIKING = "Biking";
  private static final String TCX_SPORT_RUNNING = "Running";
  private static final String TCX_SPORT_OTHER = "Other";

  // Values for fields of type Build_t/Type.
  private static final String TCX_TYPE_RELEASE = "Release";
  private static final String TCX_TYPE_INTERNAL = "Internal";

  private final SimpleDateFormat timestampFormatter;
  private final Context context;

  private PrintWriter pw = null;
  private Track track;

  // Determines whether to encode cadence value as running or cycling cadence.
  private boolean sportIsCycling;

  public TcxTrackWriter(Context context) {
    this.context = context;

    timestampFormatter = new SimpleDateFormat(TIMESTAMP_FORMAT);
    timestampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @SuppressWarnings("hiding")
  @Override
  public void prepare(Track track, OutputStream out) {
    this.track = track;
    this.pw = new PrintWriter(out);
    this.sportIsCycling = categoryToTcxSport(track.getCategory()).equals(TCX_SPORT_BIKING);
  }

  @Override
  public void close() {
    if (pw != null) {
      pw.close();
      pw = null;
    }
  }

  @Override
  public String getExtension() {
    return TrackFileFormat.TCX.getExtension();
  }

  @Override
  public void writeHeader() {
    if (pw == null) {
      return;
    }
    pw.format("<?xml version=\"1.0\" encoding=\"%s\" standalone=\"no\" ?>\n",
        Charset.defaultCharset().name());
    pw.print("<TrainingCenterDatabase ");
    pw.print("xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\" ");
    pw.print("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
    pw.print("xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2 ");
    pw.println("http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\">");
    pw.println();
  }

  @Override
  public void writeBeginTrack(Location firstPoint) {
    if (pw == null) {
      return;
    }

    String startTime = timestampFormatter.format(track.getStatistics().getStartTime());

    pw.println("  <Activities>");
    pw.format("    <Activity Sport=\"%s\">\n", categoryToTcxSport(track.getCategory()));
    pw.format("      <Id>%s</Id>\n", startTime);
    pw.format("      <Lap StartTime=\"%s\">\n", startTime);
    pw.print("        <TotalTimeSeconds>");
    pw.print(track.getStatistics().getTotalTime() / 1000);
    pw.println("</TotalTimeSeconds>");
    pw.print("        <DistanceMeters>");
    pw.print(track.getStatistics().getTotalDistance());
    pw.println("</DistanceMeters>");
    // TODO max speed etc.
    // Calories are a required element just put in 0.
    pw.print("<Calories>0</Calories>");
    pw.println("<Intensity>Active</Intensity>");
    pw.println("<TriggerMethod>Manual</TriggerMethod>");
  }

  @Override
  public void writeOpenSegment() {
    if (pw != null) {
      pw.println("      <Track>");
    }
  }

  @Override
  public void writeLocation(Location location) {
    if (pw == null) {
      return;
    }
    pw.println("        <Trackpoint>");
    Date d = new Date(location.getTime());
    pw.println("          <Time>" + timestampFormatter.format(d) + "</Time>");
    pw.println("          <Position>");

    pw.print("            <LatitudeDegrees>");
    pw.print(location.getLatitude());
    pw.println("</LatitudeDegrees>");

    pw.print("            <LongitudeDegrees>");
    pw.print(location.getLongitude());
    pw.println("</LongitudeDegrees>");

    pw.println("          </Position>");
    pw.print("          <AltitudeMeters>");
    pw.print(location.getAltitude());
    pw.println("</AltitudeMeters>");

    if (location instanceof MyTracksLocation) {
      SensorDataSet sensorData = ((MyTracksLocation) location).getSensorDataSet();
      if (sensorData != null) {
        if (sensorData.hasHeartRate()
            && sensorData.getHeartRate().getState() == Sensor.SensorState.SENDING
            && sensorData.getHeartRate().hasValue()) {
          pw.print("          <HeartRateBpm>");
          pw.print("<Value>");
          pw.print(sensorData.getHeartRate().getValue());
          pw.print("</Value>");
          pw.println("</HeartRateBpm>");
        }
        
        boolean cadenceAvailable = sensorData.hasCadence()
          && sensorData.getCadence().getState() == Sensor.SensorState.SENDING
          && sensorData.getCadence().hasValue();

        // TCX Trackpoint_t contains a sequence. Thus, the legacy XML element
        // <Cadence> needs to be put before <Extensions>.
        // This field should only be used for the case that activity was marked as biking.
        // Otherwise cadence is interpreted as running cadence data which
        // is written in the <Extensions> as <RunCadence>.
        if (sportIsCycling && cadenceAvailable) {
          pw.print("          <Cadence>");
          pw.print(Math.min(254, sensorData.getCadence().getValue()));
          pw.println("</Cadence>");
        }

        boolean powerAvailable = sensorData.hasPower()
          && sensorData.getPower().getState() == Sensor.SensorState.SENDING
          && sensorData.getPower().hasValue();
        
        if(powerAvailable || (!sportIsCycling && cadenceAvailable)) {
          pw.print("          <Extensions>");
          pw.print("<TPX xmlns=\"http://www.garmin.com/xmlschemas/ActivityExtension/v2\">");

          // RunCadence needs to be put before power in order to be understood
          // by Garmin Training Center.
          if (!sportIsCycling && cadenceAvailable) {
            pw.print("<RunCadence>");
            pw.print(Math.min(254, sensorData.getCadence().getValue()));
            pw.print("</RunCadence>");
          }
          
          if (powerAvailable) {
            pw.print("<Watts>");
            pw.print(sensorData.getPower().getValue());
            pw.print("</Watts>");
          }

          pw.println("</TPX></Extensions>");
        }
      }
    }
    pw.println("        </Trackpoint>");
  }

  @Override
  public void writeCloseSegment() {
    if (pw != null) {
      pw.println("      </Track>");
    }
  }

  @Override
  public void writeEndTrack(Location lastPoint) {
    if (pw == null) {
      return;
    }
    pw.println("      </Lap>");
    pw.print("      <Creator xsi:type=\"Device_t\">");
    pw.format("<Name>My Tracks running on %s</Name>\n", Build.MODEL);

    // The following code is correct.  ID is inconsistently capitalized in the
    // TCX schema.
    pw.println("<UnitId>0</UnitId>");
    pw.println("<ProductID>0</ProductID>");
    
    writeVersion();

    pw.println("</Creator>");
    pw.println("    </Activity>");
    pw.println("  </Activities>");
  }

  @Override
  public void writeFooter() {
    if (pw == null) {
      return;
    }
    pw.println("  <Author xsi:type=\"Application_t\">");

    // We put the version in the name because there isn't a better place for
    // it.  The TCX schema tightly defined the Version tag, so we can't put it
    // there.  They've similarly constrained the PartNumber tag, so it can't go
    // there either.
    pw.format("<Name>My Tracks %s by Google</Name>\n", SystemUtils.getMyTracksVersion(context));

    pw.println("<Build>");

    writeVersion();

    pw.format("<Type>%s</Type>\n", SystemUtils.isRelease(context) ? TCX_TYPE_RELEASE
        : TCX_TYPE_INTERNAL);
    pw.println("</Build>");
    pw.format("<LangID>%s</LangID>\n", Locale.getDefault().getLanguage());
    pw.println("<PartNumber>000-00000-00</PartNumber>");
    pw.println("</Author>");
    pw.println("</TrainingCenterDatabase>");
  }

  @Override
  public void writeWaypoint(Waypoint waypoint) {
    // TODO Write out the waypoints somewhere.
  }
  
  private void writeVersion() {
    if (pw == null) {
      return;
    }
    
    // Splitting the myTracks version code into VersionMajor, VersionMinor and BuildMajor
    // to fit the integer type requirement for these fields in the TCX spec. 
    // Putting a string like "x.x.x" into VersionMajor breaks XML validation.
    // We also set the BuildMinor version to 1 if this is a development build to
    // signify that this build is newer than the one associated with the
    // version code given in BuildMajor.
    
    String[] myTracksVersionComponents = SystemUtils.getMyTracksVersion(context).split("\\.");

    pw.println("<Version>");
    pw.format("<VersionMajor>%d</VersionMajor>\n", Integer.valueOf(myTracksVersionComponents[0]));
    pw.format("<VersionMinor>%d</VersionMinor>\n", Integer.valueOf(myTracksVersionComponents[1]));
    // TCX schema says these are optional but http://connect.garmin.com only accepts 
    // the TCX file when they are present.
    pw.format("<BuildMajor>%d</BuildMajor>\n", Integer.valueOf(myTracksVersionComponents[2]));
    pw.format("<BuildMinor>%d</BuildMinor>\n", SystemUtils.isRelease(context) ? 0 : 1);
    pw.println("</Version>");
  }

  private String categoryToTcxSport(String category) {
    category = category.trim();
    if (category.equalsIgnoreCase(TCX_SPORT_RUNNING)) {
      return TCX_SPORT_RUNNING;
    } else if (category.equalsIgnoreCase(TCX_SPORT_BIKING)) {
      return TCX_SPORT_BIKING;
    } else {
      return TCX_SPORT_OTHER;
    }
  }
}
