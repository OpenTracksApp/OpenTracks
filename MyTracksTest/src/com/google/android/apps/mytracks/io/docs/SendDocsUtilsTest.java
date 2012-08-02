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
package com.google.android.apps.mytracks.io.docs;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.gdata.docs.SpreadsheetsClient.WorksheetEntry;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.wireless.gdata.data.Entry;

import android.test.AndroidTestCase;

/**
 * Tests {@link SendDocsUtils}.
 * 
 * @author Jimmy Shih
 */
public class SendDocsUtilsTest extends AndroidTestCase {

  private static final long TIME = 1288721514000L;

  /**
   * Tests {@link SendDocsUtils#getEntryId(Entry)} with a valid id.
   */
  public void testGetEntryId_valid() {
    Entry entry = new Entry();
    entry.setId("https://docs.google.com/feeds/documents/private/full/spreadsheet%3A123");
    assertEquals("123", SendDocsUtils.getEntryId(entry));
  }

  /**
   * Tests {@link SendDocsUtils#getEntryId(Entry)} with an invalid id.
   */
  public void testGetEntryId_invalid() {
    Entry entry = new Entry();
    entry.setId("123");
    assertEquals(null, SendDocsUtils.getEntryId(entry));
  }

  /**
   * Tests {@link SendDocsUtils#getNewSpreadsheetId(String)} with a valid id.
   */
  public void testGetNewSpreadsheetId_valid_id() {
    assertEquals("123", SendDocsUtils.getNewSpreadsheetId(
        "<id>https://docs.google.com/feeds/documents/private/full/spreadsheet%3A123</id>"));
  }

  /**
   * Tests {@link SendDocsUtils#getNewSpreadsheetId(String)} without an open
   * tag, <id>.
   */
  public void testGetNewSpreadsheetId_no_open_tag() {
    assertEquals(null, SendDocsUtils.getNewSpreadsheetId(
        "https://docs.google.com/feeds/documents/private/full/spreadsheet%3A123</id>"));
  }

  /**
   * Tests {@link SendDocsUtils#getNewSpreadsheetId(String)} without an end tag,
   * </id>.
   */
  public void testGetNewSpreadsheetId_no_end_tag() {
    assertEquals(null, SendDocsUtils.getNewSpreadsheetId(
        "<id>https://docs.google.com/feeds/documents/private/full/spreadsheet%3A123"));
  }

  /**
   * Tests {@link SendDocsUtils#getNewSpreadsheetId(String)} without the url
   * prefix,
   * https://docs.google.com/feeds/documents/private/full/spreadsheet%3A.
   */
  public void testGetNewSpreadsheetId_no_prefix() {
    assertEquals(null, SendDocsUtils.getNewSpreadsheetId("<id>123</id>"));
  }

  /**
   * Tests {@link SendDocsUtils#getWorksheetEntryId(WorksheetEntry)} with a
   * valid entry.
   */
  public void testGetWorksheetEntryId_valid() {
    WorksheetEntry entry = new WorksheetEntry();
    entry.setId("/123");
    assertEquals("123", SendDocsUtils.getWorksheetEntryId(entry));
  }

  /**
   * Tests {@link SendDocsUtils#getWorksheetEntryId(WorksheetEntry)} with a
   * invalid entry.
   */
  public void testGetWorksheetEntryId_invalid() {
    WorksheetEntry entry = new WorksheetEntry();
    entry.setId("123");
    assertEquals(null, SendDocsUtils.getWorksheetEntryId(entry));
  }

  /**
   * Tests {@link SendDocsUtils#getRowContent(Track, boolean,
   * android.content.Context)} with metric units.
   */
  public void testGetRowContent_metric() throws Exception {
    Track track = getTrack();
    String expectedData = "<entry xmlns='http://www.w3.org/2005/Atom' "
        + "xmlns:gsx='http://schemas.google.com/spreadsheets/2006/extended'>"
        + "<gsx:name><![CDATA[trackName]]></gsx:name>"
        + "<gsx:description><![CDATA[trackDescription]]></gsx:description>" + "<gsx:date><![CDATA["
        + StringUtils.formatDateTime(getContext(), TIME) + "]]></gsx:date>"
        + "<gsx:totaltime><![CDATA[0:00:05]]></gsx:totaltime>"
        + "<gsx:movingtime><![CDATA[0:00:04]]></gsx:movingtime>"
        + "<gsx:distance><![CDATA[20.00]]></gsx:distance>"
        + "<gsx:distanceunit><![CDATA[km]]></gsx:distanceunit>"
        + "<gsx:averagespeed><![CDATA[14,400.00]]></gsx:averagespeed>"
        + "<gsx:averagemovingspeed><![CDATA[18,000.00]]>" + "</gsx:averagemovingspeed>"
        + "<gsx:maxspeed><![CDATA[5,400.00]]></gsx:maxspeed>"
        + "<gsx:speedunit><![CDATA[km/h]]></gsx:speedunit>"
        + "<gsx:elevationgain><![CDATA[6,000]]></gsx:elevationgain>"
        + "<gsx:minelevation><![CDATA[-500]]></gsx:minelevation>"
        + "<gsx:maxelevation><![CDATA[550]]></gsx:maxelevation>"
        + "<gsx:elevationunit><![CDATA[m]]></gsx:elevationunit>" 
        + "<gsx:map><![CDATA[https://maps.google.com/maps/ms?msa=0&msid=trackMapId]]></gsx:map>"
        + "<gsx:fusiontable><![CDATA[-]]></gsx:fusiontable>"
        + "</entry>";
    assertEquals(expectedData, SendDocsUtils.getRowContent(track, true, getContext()));
  }

  /**
   * Tests {@link SendDocsUtils#getRowContent(Track, boolean,
   * android.content.Context)} with imperial units.
   */
  public void testGetRowContent_imperial() throws Exception {
    Track track = getTrack();
    String expectedData = "<entry xmlns='http://www.w3.org/2005/Atom' "
        + "xmlns:gsx='http://schemas.google.com/spreadsheets/2006/extended'>"
        + "<gsx:name><![CDATA[trackName]]></gsx:name>"
        + "<gsx:description><![CDATA[trackDescription]]></gsx:description>" + "<gsx:date><![CDATA["
        + StringUtils.formatDateTime(getContext(), TIME) + "]]></gsx:date>"
        + "<gsx:totaltime><![CDATA[0:00:05]]></gsx:totaltime>"
        + "<gsx:movingtime><![CDATA[0:00:04]]></gsx:movingtime>"
        + "<gsx:distance><![CDATA[12.43]]></gsx:distance>"
        + "<gsx:distanceunit><![CDATA[mi]]></gsx:distanceunit>"
        + "<gsx:averagespeed><![CDATA[8,947.75]]></gsx:averagespeed>"
        + "<gsx:averagemovingspeed><![CDATA[11,184.68]]>" + "</gsx:averagemovingspeed>"
        + "<gsx:maxspeed><![CDATA[3,355.40]]></gsx:maxspeed>"
        + "<gsx:speedunit><![CDATA[mi/h]]></gsx:speedunit>"
        + "<gsx:elevationgain><![CDATA[19,685]]></gsx:elevationgain>"
        + "<gsx:minelevation><![CDATA[-1,640]]></gsx:minelevation>"
        + "<gsx:maxelevation><![CDATA[1,804]]></gsx:maxelevation>"
        + "<gsx:elevationunit><![CDATA[ft]]></gsx:elevationunit>" 
        + "<gsx:map><![CDATA[https://maps.google.com/maps/ms?msa=0&msid=trackMapId]]></gsx:map>"
        + "<gsx:fusiontable><![CDATA[-]]></gsx:fusiontable>"
        + "</entry>";
    assertEquals(expectedData, SendDocsUtils.getRowContent(track, false, getContext()));
  }

  /**
   * Gets a track for testing {@link SendDocsUtils#getRowContent(Track, boolean,
   * android.content.Context)}.
   */
  private Track getTrack() {
    TripStatistics stats = new TripStatistics();
    stats.setStartTime(TIME);
    stats.setTotalTime(5000);
    stats.setMovingTime(4000);
    stats.setTotalDistance(20000);
    stats.setMaxSpeed(1500);
    stats.setTotalElevationGain(6000);
    stats.setMinElevation(-500);
    stats.setMaxElevation(550);

    Track track = new Track();
    track.setName("trackName");
    track.setDescription("trackDescription");
    track.setMapId("trackMapId");
    track.setTripStatistics(stats);
    return track;
  }

  /**
   * Tests {@link SendDocsUtils#appendTag(StringBuilder, String, String)} with
   * repeated calls.
   */
  public void testAppendTag() {
    StringBuilder stringBuilder = new StringBuilder();
    SendDocsUtils.appendTag(stringBuilder, "name1", "value1");
    assertEquals("<gsx:name1><![CDATA[value1]]></gsx:name1>", stringBuilder.toString());

    SendDocsUtils.appendTag(stringBuilder, "name2", "value2");
    assertEquals(
        "<gsx:name1><![CDATA[value1]]></gsx:name1><gsx:name2><![CDATA[value2]]></gsx:name2>",
        stringBuilder.toString());
  }

  /**
   * Tests {@link SendDocsUtils#getDistance(double, boolean)} with metric units.
   */
  public void testGetDistance_metric() {
    assertEquals("1.22", SendDocsUtils.getDistance(1222.3, true));
  }

  /**
   * Tests {@link SendDocsUtils#getDistance(double, boolean)} with imperial
   * units.
   */
  public void testGetDistance_imperial() {
    assertEquals("0.76", SendDocsUtils.getDistance(1222.3, false));
  }

  /**
   * Tests {@link SendDocsUtils#getSpeed(double, boolean)} with metric units.
   */
  public void testGetSpeed_metric() {
    assertEquals("15.55", SendDocsUtils.getSpeed(4.32, true));
  }

  /**
   * Tests {@link SendDocsUtils#getSpeed(double, boolean)} with imperial units.
   */
  public void testGetSpeed_imperial() {
    assertEquals("9.66", SendDocsUtils.getSpeed(4.32, false));
  }

  /**
   * Tests {@link SendDocsUtils#getElevation(double, boolean)} with metric
   * units.
   */
  public void testGetElevation_metric() {
    assertEquals("3", SendDocsUtils.getElevation(3.456, true));
  }

  /**
   * Tests {@link SendDocsUtils#getElevation(double, boolean)} with imperial
   * units.
   */
  public void testGetElevation_imperial() {
    assertEquals("11", SendDocsUtils.getElevation(3.456, false));
  }
}
