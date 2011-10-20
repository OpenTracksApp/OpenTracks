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
package com.google.android.apps.mytracks.io.docs;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.maps.mytracks.R;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.content.Context;
import android.content.res.Resources;
import android.test.mock.MockContext;
import android.test.mock.MockResources;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import junit.framework.TestCase;

/**
 * Tests for {@link DocsHelper#addTrackRow}
 *
 * @author Matthew Simmons
 */
public class DocsHelper_AddTrackRowTest extends TestCase {
  private static final long TIME = 1288721514000L;

  private static class StringWritingDocsHelper extends DocsHelper {
    String writtenSheetUri = null;
    String writtenData = null;

    @Override
    protected void writeRowData(AuthManager trixAuth, String worksheetUri,
        String postText) {
      writtenSheetUri = worksheetUri;
      writtenData = postText;
    }
  }

  public void testAddTrackRow_imperial() throws Exception {
    StringWritingDocsHelper docsHelper = new StringWritingDocsHelper();
    addTrackRow(docsHelper, false);

    DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);    
    String expectedData =
      "<entry xmlns='http://www.w3.org/2005/Atom' "
      + "xmlns:gsx='http://schemas.google.com/spreadsheets/2006/extended'>"
      + "<gsx:name><![CDATA[trackName]]></gsx:name>"
      + "<gsx:description><![CDATA[trackDescription]]></gsx:description>"
      + "<gsx:date><![CDATA[" + dateFormat.format(new Date(TIME)) + "]]></gsx:date>"
      + "<gsx:totaltime><![CDATA[0:00:05]]></gsx:totaltime>"
      + "<gsx:movingtime><![CDATA[0:00:04]]></gsx:movingtime>"
      + "<gsx:distance><![CDATA[12.43]]></gsx:distance>"
      + "<gsx:distanceunit><![CDATA[mile]]></gsx:distanceunit>"
      + "<gsx:averagespeed><![CDATA[8,947.75]]></gsx:averagespeed>"
      + "<gsx:averagemovingspeed><![CDATA[11,184.68]]>"
      + "</gsx:averagemovingspeed>"
      + "<gsx:maxspeed><![CDATA[3,355.40]]></gsx:maxspeed>"
      + "<gsx:speedunit><![CDATA[mph]]></gsx:speedunit>"
      + "<gsx:elevationgain><![CDATA[19,685]]></gsx:elevationgain>"
      + "<gsx:minelevation><![CDATA[-1,640]]></gsx:minelevation>"
      + "<gsx:maxelevation><![CDATA[1,804]]></gsx:maxelevation>"
      + "<gsx:elevationunit><![CDATA[feet]]></gsx:elevationunit>"
      + "<gsx:map>"
      + "<![CDATA[https://maps.google.com/maps/ms?msa=0&msid=trackMapId]]>"
      + "</gsx:map>"
      + "</entry>";

    assertEquals(
        "https://spreadsheets.google.com/feeds/list/ssid/wsid/private/full",
        docsHelper.writtenSheetUri);
    assertEquals(expectedData, docsHelper.writtenData);
  }

  public void testAddTrackRow_metric() throws Exception {
    StringWritingDocsHelper docsHelper = new StringWritingDocsHelper();
    addTrackRow(docsHelper, true);

    // The imperial test verifies that the tags come out in the proper order,
    // and with the proper names.  We need only verify that the labels are
    // correct, and that at least one of the unit-dependent value tags is
    // correct.
    assertTrue(docsHelper.writtenData.contains(
        "<gsx:distanceunit><![CDATA[km]]></gsx:distanceunit>"));
    assertTrue(docsHelper.writtenData.contains(
        "<gsx:speedunit><![CDATA[kph]]></gsx:speedunit>"));
    assertTrue(docsHelper.writtenData.contains(
        "<gsx:elevationunit><![CDATA[meter]]></gsx:elevationunit>"));

    assertTrue(docsHelper.writtenData.contains(
        "<gsx:distance><![CDATA[20.00]]></gsx:distance>"));
  }

  /** Adds a row to the spreadsheet, using the provided helper. */
  @UsesMocks({AuthManager.class, MockResources.class, Track.class})
  private void addTrackRow(DocsHelper docsHelper, boolean useMetric)
      throws IOException {
    final Resources mockResources = AndroidMock.createMock(MockResources.class);

    if (useMetric) {
      AndroidMock.expect(mockResources.getString(R.string.kilometer))
          .andReturn("km");
      AndroidMock.expect(mockResources.getString(R.string.kilometer_per_hour))
          .andReturn("kph");
      AndroidMock.expect(mockResources.getString(R.string.meter))
          .andReturn("meter");
    } else {
      AndroidMock.expect(mockResources.getString(R.string.mile))
          .andReturn("mile");
      AndroidMock.expect(mockResources.getString(R.string.mile_per_hour))
          .andReturn("mph");
      AndroidMock.expect(mockResources.getString(R.string.feet))
          .andReturn("feet");
    }
    AndroidMock.replay(mockResources);

    Context mockContext = new MockContext() {
      @Override
      public Resources getResources() {
        return mockResources;
      }
    };

    AuthManager mockAuthManager = AndroidMock.createMock(AuthManager.class);
    AndroidMock.replay(mockAuthManager);

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
    track.setStatistics(stats);

    docsHelper.addTrackRow(mockContext, mockAuthManager, "ssid", "wsid",
        track, useMetric);
  }
}
