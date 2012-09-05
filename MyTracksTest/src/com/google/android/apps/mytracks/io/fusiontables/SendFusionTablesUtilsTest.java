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
package com.google.android.apps.mytracks.io.fusiontables;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;

import android.location.Location;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Tests {@link SendFusionTablesUtils}.
 *
 * @author Jimmy Shih
 */
public class SendFusionTablesUtilsTest extends TestCase {

  /**
   * Tests {@link SendFusionTablesUtils#getMapUrl(Track)} with null track.
   */
  public void testGetMapUrl_null_track() {
    assertEquals(null, SendFusionTablesUtils.getMapUrl(null));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getMapUrl(Track)} with null table id.
   */
  public void testGetMapUrl_null_table_id() {
    Track track = new Track();
    TripStatistics stats = new TripStatistics();
    stats.setBounds((int) 100.E6, (int) 10.E6, (int) 50.E6, (int) 5.E6);
    track.setTripStatistics(stats);
    track.setTableId(null);
    assertEquals(null, SendFusionTablesUtils.getMapUrl(track));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getMapUrl(Track)} with null stats.
   */
  public void testGetMapUrl_null_stats() {
    Track track = new Track();
    track.setTripStatistics(null);
    track.setTableId("123");
    assertEquals(null, SendFusionTablesUtils.getMapUrl(track));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getMapUrl(Track)} with a valid track.
   */
  public void testGetMapUrl_valid_track() {
    Track track = new Track();
    track.setNumberOfPoints(2);
    TripStatistics stats = new TripStatistics();
    stats.setBounds((int) 100.E6, (int) 10.E6, (int) 50.E6, (int) 5.E6);
    track.setTripStatistics(stats);
    track.setTableId("123");
    assertEquals(
        "https://www.google.com/fusiontables/embedviz?"
            + "viz=MAP&q=select+col0,+col1,+col2,+col3+from+123+&h=false&lat=7.500000&lng=75.000000"
            + "&z=15&t=1&l=col2", SendFusionTablesUtils.getMapUrl(track));
  }

  /**
   * Tests (@link {@link SendFusionTablesUtils#formatSqlValues(String...)} with
   * no value.
   */
  public void testFormatSqlValues_no_value() {
    assertEquals("()", SendFusionTablesUtils.formatSqlValues());
  }

  /**
   * Tests (@link {@link SendFusionTablesUtils#formatSqlValues(String...)} with
   * an empty string.
   */
  public void testFormatSqlValues_empty_string() {
    assertEquals("(\'\')", SendFusionTablesUtils.formatSqlValues(""));
  }

  /**
   * Tests (@link {@link SendFusionTablesUtils#formatSqlValues(String...)} with
   * one value.
   */
  public void testFormatSqlValues_one_value() {
    assertEquals("(\'first\')", SendFusionTablesUtils.formatSqlValues("first"));
  }

  /**
   * Tests (@link {@link SendFusionTablesUtils#formatSqlValues(String...)} with
   * two values.
   */
  public void testFormatSqlValues_two_values() {
    assertEquals(
        "(\'first\',\'second\')", SendFusionTablesUtils.formatSqlValues("first", "second"));
  }

  /**
   * Tests {@link SendFusionTablesUtils#escapeSqlString(String)} with a value
   * containing several single quotes.
   */
  public void testEscapeSqValuel() {
    String value = "let's'";
    assertEquals("let''s''", SendFusionTablesUtils.escapeSqlString(value));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getKmlPoint(Location)} with a null
   * point.
   */
  public void testGetKmlPoint_null_point() {
    assertEquals(
        "<Point><coordinates></coordinates></Point>", SendFusionTablesUtils.getKmlPoint(null));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getKmlPoint(Location)} with a valid
   * point.
   */
  public void testGetKmlPoint_valid_point() {
    Location location = new Location("test");
    location.setLongitude(10.1);
    location.setLatitude(20.2);
    location.setAltitude(30.3);
    assertEquals("<Point><coordinates>10.1,20.2,30.3</coordinates></Point>",
        SendFusionTablesUtils.getKmlPoint(location));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getKmlLineString(ArrayList)} with a null
   * locations.
   */
  public void testKmlLineString_null_locations() {
    assertEquals("<LineString><coordinates></coordinates></LineString>",
        SendFusionTablesUtils.getKmlLineString(null));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getKmlLineString(ArrayList)} with no
   * location.
   */
  public void testKmlLineString_no_location() {
    ArrayList<Location> locations = new ArrayList<Location>();
    assertEquals("<LineString><coordinates></coordinates></LineString>",
        SendFusionTablesUtils.getKmlLineString(locations));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getKmlLineString(ArrayList)} with one
   * location.
   */
  public void testKmlLineString_one_location() {
    ArrayList<Location> locations = new ArrayList<Location>();
    Location location = new Location("test");
    location.setLongitude(10.1);
    location.setLatitude(20.2);
    location.setAltitude(30.3);
    locations.add(location);
    assertEquals("<LineString><coordinates>10.1,20.2,30.3</coordinates></LineString>",
        SendFusionTablesUtils.getKmlLineString(locations));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getKmlLineString(ArrayList)} with two
   * locations.
   */
  public void testKmlLineString_two_locations() {
    ArrayList<Location> locations = new ArrayList<Location>();
    Location location1 = new Location("test");
    location1.setLongitude(10.1);
    location1.setLatitude(20.2);
    location1.setAltitude(30.3);
    locations.add(location1);

    Location location2 = new Location("test");
    location2.setLongitude(1.1);
    location2.setLatitude(2.2);
    location2.removeAltitude();
    locations.add(location2);

    assertEquals("<LineString><coordinates>10.1,20.2,30.3 1.1,2.2</coordinates></LineString>",
        SendFusionTablesUtils.getKmlLineString(locations));
  }

  /**
   * Tests {@link SendFusionTablesUtils#appendLocation(Location, StringBuilder)}
   * with no altitude.
   */
  public void testAppendLocation_no_altitude() {
    StringBuilder builder = new StringBuilder();
    Location location = new Location("test");
    location.setLongitude(10.1);
    location.setLatitude(20.2);
    location.removeAltitude();
    SendFusionTablesUtils.appendLocation(location, builder);
    assertEquals("10.1,20.2", builder.toString());
  }

  /**
   * Tests {@link SendFusionTablesUtils#appendLocation(Location, StringBuilder)}
   * with altitude.
   */
  public void testAppendLocation_has_altitude() {
    StringBuilder builder = new StringBuilder();
    Location location = new Location("test");
    location.setLongitude(10.1);
    location.setLatitude(20.2);
    location.setAltitude(30.3);
    SendFusionTablesUtils.appendLocation(location, builder);
    assertEquals("10.1,20.2,30.3", builder.toString());
  }

  /**
   * Tests {@link SendFusionTablesUtils#getTableId(InputStream)} with a null
   * inputstream.
   */
  public void testGetTableId_null() {
    assertEquals(null, SendFusionTablesUtils.getTableId(null));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getTableId(InputStream)} with an
   * inputstream containing no data.
   */
  public void testGetTableId_no_data() {
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    assertEquals(null, SendFusionTablesUtils.getTableId(inputStream));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getTableId(InputStream)} with an empty
   * inputstream.
   */
  public void testGetTableId_empty() {
    String string = "";
    InputStream inputStream = new ByteArrayInputStream(string.getBytes());
    assertEquals(null, SendFusionTablesUtils.getTableId(inputStream));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getTableId(InputStream)} with an one
   * line inputstream.
   */
  public void testGetTableId_one_line() {
    String string = "tableid";
    InputStream inputStream = new ByteArrayInputStream(string.getBytes());
    assertEquals(null, SendFusionTablesUtils.getTableId(inputStream));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getTableId(InputStream)} with an
   * inputstream not containing "tableid".
   */
  public void testGetTableId_no_table_id() {
    String string = "error\n123";
    InputStream inputStream = new ByteArrayInputStream(string.getBytes());
    assertEquals(null, SendFusionTablesUtils.getTableId(inputStream));
  }

  /**
   * Tests {@link SendFusionTablesUtils#getTableId(InputStream)} with a valid
   * inputstream.
   */
  public void testGetTableId() {
    String string = "tableid\n123";
    InputStream inputStream = null;
    inputStream = new ByteArrayInputStream(string.getBytes());
    assertEquals("123", SendFusionTablesUtils.getTableId(inputStream));
  }
}