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
package com.google.android.apps.mytracks.endtoendtest.common;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.maps.mytracks.R;

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
    EndToEndTestUtils.createSimpleTrack(0, true);
    instrumentation.waitForIdleSync();
    checkExportAndImport();
  }

  /**
   * Tests export and import track. Then check the properties of this track.
   */
  public void testExportAndImportTrack_properties() {
    EndToEndTestUtils.deleteAllTracks();

    // Create a new track with two markers.
    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();

    // Send Gps before creating marker.
    EndToEndTestUtils.sendGps(2);
    EndToEndTestUtils.createWaypoint(0);
    EndToEndTestUtils.sendGps(2, 2);
    EndToEndTestUtils.createWaypoint(1);
    EndToEndTestUtils.sendGps(2, 4);

    // Back to tracks list.
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.stopRecording(true);

    // Export this track and then delete it.
    EndToEndTestUtils.deleteExportedFiles(EndToEndTestUtils.GPX);

    // Click to export tracks(At least one track) to Gpx files.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_export_all), true);
    EndToEndTestUtils.SOLO.clickOnText(String.format(
        activityMyTracks.getString(R.string.export_external_storage_option),
        EndToEndTestUtils.GPX.toUpperCase()));
    EndToEndTestUtils.getButtonOnScreen(
        EndToEndTestUtils.activityMytracks.getString(R.string.generic_ok), true, true);
    EndToEndTestUtils.SOLO.waitForText(getSaveSuccessMessage(1, EndToEndTestUtils.GPX));
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    EndToEndTestUtils.deleteAllTracks();

    // Import this tracks.
    importTracks(EndToEndTestUtils.GPX);
    EndToEndTestUtils.SOLO.waitForText(getImportSuccessMessage(1, EndToEndTestUtils.GPX));

    /*
     * Check import tracks should be equal with the sum of trackNumber and
     * gpxFilesNumber;
     */
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();

    // Check the track name, activity and description.
    assertTrue(EndToEndTestUtils.SOLO.waitForText(EndToEndTestUtils.trackName));
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.trackName);
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.track_detail_chart_tab));
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_edit), true);
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.trackName));
    assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.activityType));
    assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.trackDesc));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_cancel));

    if (EndToEndTestUtils.hasGpsSingal) {
      // Check the markers.
      EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_markers), true);

      // The first marker.
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_NAME + 1));
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_TYPE + 1));
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_DESC + 1));

      // The second marker.
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_NAME + 2));
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_TYPE + 2));
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_DESC + 2));
    }
  }

  /**
   * Tests export and import tracks after creating a track with Gps signal and
   * an empty track. Both tracks are paused during recording.
   */
  public void testExportAndImportTracks_pausedTrack() {
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
   * <li>Tests import Gpx files to tracks.</li>
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

    // No Gpx file to imported.
    importTracks(EndToEndTestUtils.GPX);
    EndToEndTestUtils.SOLO.waitForText(getImportErrorMessage(0, 0, EndToEndTestUtils.GPX));
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);

    // Click to export tracks(At least one track) to Gpx files.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_export_all), true);
    EndToEndTestUtils.SOLO.clickOnText(String.format(
        activityMyTracks.getString(R.string.export_external_storage_option),
        EndToEndTestUtils.GPX.toUpperCase()));
    EndToEndTestUtils.getButtonOnScreen(
        EndToEndTestUtils.activityMytracks.getString(R.string.generic_ok), true, true);
    EndToEndTestUtils.SOLO.waitForText(getSaveSuccessMessage(1, EndToEndTestUtils.GPX));
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);

    // Check export Gpx file.
    assertEquals(gpxFilesNumber + trackNumber,
        EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length);
    instrumentation.waitForIdleSync();

    // Click to import Gpx track.
    gpxFilesNumber = EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length;
    trackNumber = EndToEndTestUtils.SOLO.getCurrentListViews().get(0).getCount();

    importTracks(EndToEndTestUtils.GPX);
    EndToEndTestUtils.rotateAllActivities();

    /*
     * Wait for the prefix of import success string is much faster than wait the
     * whole string.
     */
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.import_success).split(
        "%")[0]);

    /*
     * Check import tracks should be equal with the sum of trackNumber and
     * gpxFilesNumber;
     */
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.waitForText(EndToEndTestUtils.trackName));
    assertEquals(trackNumber + gpxFilesNumber, EndToEndTestUtils.SOLO.getCurrentListViews().get(0)
        .getCount());

    // Click to export tracks(At least two tracks) to KML files.
    gpxFilesNumber = EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length;
    trackNumber = EndToEndTestUtils.SOLO.getCurrentListViews().get(0).getCount();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_export_all), true);
    EndToEndTestUtils.SOLO.clickOnText(String.format(
        activityMyTracks.getString(R.string.export_external_storage_option),
        EndToEndTestUtils.KML.toUpperCase()));
    EndToEndTestUtils.getButtonOnScreen(
        EndToEndTestUtils.activityMytracks.getString(R.string.generic_ok), true, true);
    EndToEndTestUtils.SOLO.waitForText(getSaveSuccessMessage(2, EndToEndTestUtils.KM));
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);

    // Check export files.
    assertEquals(gpxFilesNumber, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length);
    assertEquals(trackNumber, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.KML).length);

    // Click to import KML track.
    int KMLFilesNumber = EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.KML).length;
    instrumentation.waitForIdleSync();
    trackNumber = EndToEndTestUtils.SOLO.getCurrentListViews().get(0).getCount();

    importTracks(EndToEndTestUtils.KML);

    /*
     * Wait for the prefix of import success string is much faster than wait the
     * whole string.
     */
    EndToEndTestUtils.waitTextToDisappear(activityMyTracks
        .getString(R.string.generic_progress_title));
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.import_success).split(
        "%")[0]);

    /*
     * Check import KML tracks should be equal with the sum of trackNumber and
     * KMLFilesNumber;
     */
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
    assertEquals(trackNumber + KMLFilesNumber, EndToEndTestUtils.SOLO.getCurrentListViews().get(0)
        .getCount());
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

  private String getSaveSuccessMessage(int count, String type) {
    String tracks = activityMyTracks.getResources()
        .getQuantityString(R.plurals.tracks, count, count);
    String directoryName = FileUtils.buildExternalDirectoryPath(type);
    return activityMyTracks.getString(
        R.string.export_external_storage_success, tracks, directoryName);
  }

  private String getImportSuccessMessage(int count, String type) {
    String files = activityMyTracks.getResources().getQuantityString(R.plurals.files, count, count);
    String directoryName = FileUtils.buildExternalDirectoryPath(type);
    return activityMyTracks.getString(R.string.import_success, files, directoryName);
  }

  private String getImportErrorMessage(int count, int total, String type) {
    String files = activityMyTracks.getResources().getQuantityString(R.plurals.files, total, total);
    String directoryName = FileUtils.buildExternalDirectoryPath(type);
    return activityMyTracks.getString(R.string.import_error, count, files, directoryName);
  }

  /**
   * Imports tracks from external storage.
   * 
   * @param fileType the file type, can be GPX and KML currently
   */
  private void importTracks(String fileType) {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_import_all), true);
    EndToEndTestUtils.SOLO.clickOnText(fileType.toUpperCase());
    EndToEndTestUtils.getButtonOnScreen(
        EndToEndTestUtils.activityMytracks.getString(R.string.generic_ok), true, true);
  }
}
