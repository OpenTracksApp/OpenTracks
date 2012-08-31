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
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.maps.mytracks.R;

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;

import java.io.File;

/**
 * Tests the import and export of MyTracks.
 * 
 * @author Youtao Liu
 */
public class ExportAndImportTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  @TargetApi(8)
  public ExportAndImportTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activityMyTracks = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, activityMyTracks);
  }

  /**
   * Tests export and import tracks after creating a track with Gps signal and 
   * an empty track.
   */
  public void testExportAndImportTracks() {
    // Create a new track with 3 gps data.
    EndToEndTestUtils.createTrackIfEmpty(3, true);
    instrumentation.waitForIdleSync();
    // Create a empty track.
    EndToEndTestUtils.createSimpleTrack(0);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    checkExportAndImport();
  }
  
  /**
   * Tests export and import tracks after creating a track with Gps signal and 
   * an empty track. Both tracks are paused during recording.
   */
  public void testExportAndImportTracks_pause() {
    EndToEndTestUtils.deleteAllTracks();
    // Create a new track with 3 gps data.
    EndToEndTestUtils.createTrackWithPause(3);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();   
    
    // Create a empty track.
    EndToEndTestUtils.createTrackWithPause(0);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    checkExportAndImport();
  }
  
  /**
   * Tests export and import tracks.
   * <ul>
   * <li>Create two tracks, one of them is empty(Have no Gps data).</li>
   * <li>Tests import when there is no track file.</li>
   * <li>Tests export tracks to Gpx files.</li>
   * <li>Tests import files to tracks.</li>
   * <li>Tests export tracks to Kml files.</li>
   * </ul>
   */
  private void checkExportAndImport() {
    // Delete all exported gpx and kml tracks.
    EndToEndTestUtils.deleteExportedFiles(EndToEndTestUtils.GPX);
    EndToEndTestUtils.deleteExportedFiles(EndToEndTestUtils.KML);
    int gpxFilesNumber = 0;
    File[] allGpxFiles = EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX);
    // For the first export, there is no MyTracks folder.
    if (allGpxFiles != null) {
      gpxFilesNumber = EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length;
    }

    // Get track number in current track list of MyTracks.
    int trackNumber = EndToEndTestUtils.SOLO.getCurrentListViews().get(0).getCount();

    // No file to imported.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_import), true);
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.sd_card_import_error_no_file,
        FileUtils.buildExternalDirectoryPath(EndToEndTestUtils.GPX)));
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);

    // Click to export tracks(At least one track) to Gpx files.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_save_all), true);
    EndToEndTestUtils.SOLO
        .clickOnText(String.format(activityMyTracks.getString(R.string.menu_save_format),
            EndToEndTestUtils.GPX.toUpperCase()));
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.sd_card_save_success));
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    // Check export file.
    assertEquals(gpxFilesNumber + trackNumber,
        EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length);
    instrumentation.waitForIdleSync();

    // Click to import track.
    gpxFilesNumber = EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length;
    trackNumber = EndToEndTestUtils.SOLO.getCurrentListViews().get(0).getCount();

    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_import), true);
    EndToEndTestUtils.rotateAllActivities();
    // Wait for the prefix of import success string is much faster than wait
    // the whole string.
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.sd_card_import_success_count).split(
        "%")[0]);
    // Check import tracks should be equal with the sum of trackNumber and
    // gpxFilesNumber;
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.waitForText(EndToEndTestUtils.trackName));
    assertEquals(trackNumber + gpxFilesNumber, EndToEndTestUtils.SOLO.getCurrentListViews().get(0)
        .getCount());

    // Click to export tracks(At least two tracks) to KML files.
    gpxFilesNumber = EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length;
    trackNumber = EndToEndTestUtils.SOLO.getCurrentListViews().get(0).getCount();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_save_all), true);
    EndToEndTestUtils.SOLO
        .clickOnText(String.format(activityMyTracks.getString(R.string.menu_save_format),
            EndToEndTestUtils.KML.toUpperCase()));
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.sd_card_save_success));
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    // Check export files.
    assertEquals(gpxFilesNumber, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length);
    assertEquals(trackNumber, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.KML).length);
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
