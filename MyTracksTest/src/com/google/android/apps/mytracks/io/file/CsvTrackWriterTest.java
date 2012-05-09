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


/**
 * Tests for {@link CsvTrackWriter}.
 *
 * @author Rodrigo Damazio
 */
public class CsvTrackWriterTest extends TrackFormatWriterTest {

  private static final String BEGIN_TAG = "\"";
  private static final String END_TAG = "\"\n";
  private static final String SEPARATOR = "\",\"";

  public void testCsvOutput() throws Exception {
    String expectedTrackHeader = getExpectedLine(
        "Name", "Activity type", "Description");
    String expectedTrack = getExpectedLine(TRACK_NAME, TRACK_CATEGORY, TRACK_DESCRIPTION);
    String expectedMarkerHeader = getExpectedLine("Name",
        "Marker type",
        "Description",
        "Latitude (deg)",
        "Longitude (deg)",
        "Altitude (m)",
        "Bearing (deg)",
        "Accuracy (m)",
        "Speed (m/s)",
        "Time");
    String expectedMarker1 = getExpectedLine(WAYPOINT1_NAME, WAYPOINT1_CATEGORY,
        WAYPOINT1_DESCRIPTION, "1.0", "-1.0", "10.0", "100.0", "1,000", "10,000",
        "1970-01-01T00:01:40.000Z");
    String expectedMarker2 = getExpectedLine(WAYPOINT2_NAME, WAYPOINT2_CATEGORY,
        WAYPOINT2_DESCRIPTION, "2.0", "-2.0", "20.0", "200.0", "2,000", "20,000",
        "1970-01-01T00:03:20.000Z");
    String expectedPointHeader = getExpectedLine("Segment", "Point", "Latitude (deg)",
        "Longitude (deg)", "Altitude (m)", "Bearing (deg)", "Accuracy (m)", "Speed (m/s)", "Time",
        "Power (W)", "Cadence (rpm)", "Heart rate (bpm)", "Battery level (%)");
    String expectedPoint1 = getExpectedLine("1", "1", "0.0", "0.0", "0.0", "0.0", "0", "0",
        "1970-01-01T00:00:00.000Z", "100.0", "200.0", "300.0", "400.0");
    String expectedPoint2 = getExpectedLine("1", "2", "1.0", "-1.0", "10.0", "100.0", "1,000",
        "10,000", "1970-01-01T00:01:40.000Z", "101.0", "201.0", "301.0", "401.0");
    String expectedPoint3 = getExpectedLine("2", "1", "2.0", "-2.0", "20.0", "200.0", "2,000",
        "20,000", "1970-01-01T00:03:20.000Z", "102.0", "202.0", "302.0", "402.0");
    String expectedPoint4 = getExpectedLine("2", "2", "3.0", "-3.0", "30.0", "300.0", "3,000",
        "30,000", "1970-01-01T00:05:00.000Z", "103.0", "203.0", "303.0", "403.0");
    String expected = expectedTrackHeader + expectedTrack + "\n" 
        + expectedMarkerHeader + expectedMarker1 + expectedMarker2 + "\n"
        + expectedPointHeader + expectedPoint1 + expectedPoint2 + expectedPoint3 + expectedPoint4;

    CsvTrackWriter writer = new CsvTrackWriter(getContext());
    assertEquals(expected, writeTrack(writer));
  }

  /**
   * Gets the expected CSV line from a list of expected values.
   * 
   * @param values expected values
   */
  private String getExpectedLine(String... values) {
    StringBuilder builder = new StringBuilder();
    builder.append(BEGIN_TAG);
    boolean first = true;
    for (String value : values) {
      if (!first) {
        builder.append(SEPARATOR);
      }
      first = false;
      builder.append(value);
    }
    builder.append(END_TAG);
    return builder.toString();
  }
}
