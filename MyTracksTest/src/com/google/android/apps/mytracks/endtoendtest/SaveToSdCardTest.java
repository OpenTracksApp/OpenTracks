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
package com.google.android.apps.mytracks.endtoendtest;

import com.google.android.apps.mytracks.TrackListActivity;

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;

/**
 * Tests sending a track to SD card.
 * 
 * @author Youtao Liu
 */
public class SaveToSdCardTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  @TargetApi(8)
  public SaveToSdCardTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activityMyTracks = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, activityMyTracks);
    EndToEndTestUtils.createTrackIfEmpty(1, false);
  }

  /**
   * Tests saving a track to SD card as a GPX file.
   */
  public void testSaveToSdCard_GPX() {
    EndToEndTestUtils.saveTrackToSdCard(EndToEndTestUtils.GPX);
    assertEquals(1, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length);
  }

  /**
   * Tests saving a track to SD card as a KML file.
   */
  public void testSaveToSdCard_KML() {
    EndToEndTestUtils.saveTrackToSdCard(EndToEndTestUtils.KML);
    assertEquals(1, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.KML).length);
  }

  /**
   * Tests saving a track to SD card as a CSV file.
   */
  public void testSaveToSdCard_CSV() {
    EndToEndTestUtils.saveTrackToSdCard(EndToEndTestUtils.CSV);
    assertEquals(1, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.CSV).length);
  }

  /**
   * Tests saving a track to SD card as a TCX file.
   */
  public void testSaveToSdCard_TCX() {
    EndToEndTestUtils.saveTrackToSdCard(EndToEndTestUtils.TCX);
    assertEquals(1, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.TCX).length);
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
