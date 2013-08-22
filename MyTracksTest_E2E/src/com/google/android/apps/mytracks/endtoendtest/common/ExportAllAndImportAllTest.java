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

import com.google.android.apps.mytracks.TrackDetailActivity;
import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Instrumentation;
import android.location.Location;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Tests the import and export of MyTracks.
 * 
 * @author Youtao Liu
 */
public class ExportAllAndImportAllTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;
  private int trackNumber = 0;

  private final static String tracksName = "testTrackName1373523959524";
  private final static String maxAltitude = "33.3";
  private final static String minAltitude = "22.2";

  public ExportAllAndImportAllTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activityMyTracks = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, activityMyTracks);
    trackNumber = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount();
    EndToEndTestUtils.createTrackIfEmpty(1, true);
  }

  /**
   * Tests export and import tracks after creating a track with Gps signal and
   * an empty track.
   */
  public void testExportAllAndImportAllTracks_GPX_KML() {
    // Create a new track with 3 gps data.
    EndToEndTestUtils.createTrackIfEmpty(3, true);
    instrumentation.waitForIdleSync();

    // Create a empty track.
    EndToEndTestUtils.createSimpleTrack(0, true);
    instrumentation.waitForIdleSync();
    checkExportAndImport();
  }

  /**
   * Tests saving tracks to SD card as a CSV files.
   */
  public void testExportAll_CSV() {
    EndToEndTestUtils.saveAllTrackToSdCard(EndToEndTestUtils.CSV);
    assertEquals(trackNumber, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.CSV).length);
  }

  /**
   * Tests saving tracks to SD card as a TCX files.
   */
  public void testExportAll_TCX() {
    EndToEndTestUtils.saveAllTrackToSdCard(EndToEndTestUtils.TCX);
    assertEquals(trackNumber, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.TCX).length);
  }

  /**
   * Tests export and import track. Then check the properties of this track.
   */
  public void testExportAllAndImportAllTrack_properties_GPX() {
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
    exportTracks(EndToEndTestUtils.GPX);
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
  public void testExportAllAndImportAllTracks_pausedTrack_GPX_KML() {
    EndToEndTestUtils.deleteAllTracks();
    // Create a new track with 3 gps data.
    EndToEndTestUtils.createTrackWithPause(3);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();

    // Create an empty track.
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
    trackNumber = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount();

    // No Gpx file to imported.
    importTracks(EndToEndTestUtils.GPX);
    EndToEndTestUtils.SOLO.waitForText(getImportErrorMessage(0, 0, EndToEndTestUtils.GPX));
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);

    // Click to export tracks(At least one track) to Gpx files.
    exportTracks(EndToEndTestUtils.GPX);

    // Check export Gpx file.
    assertEquals(gpxFilesNumber + trackNumber,
        EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length);
    instrumentation.waitForIdleSync();

    // Click to import Gpx track.
    gpxFilesNumber = EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length;
    trackNumber = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount();

    importTracks(EndToEndTestUtils.GPX);
    EndToEndTestUtils.rotateCurrentActivity();
    checkImport();

    assertTrue(EndToEndTestUtils.SOLO.waitForText(EndToEndTestUtils.trackName));
    assertEquals(trackNumber + gpxFilesNumber,
        EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount());

    // Click to export tracks(At least two tracks) to KML files.
    gpxFilesNumber = EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length;
    trackNumber = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount();

    exportTracks(EndToEndTestUtils.KML);

    // Check export files.
    assertEquals(gpxFilesNumber, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.GPX).length);
    assertEquals(trackNumber, EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.KML).length);

    // Click to import KML track.
    int KMLFilesNumber = EndToEndTestUtils.getExportedFiles(EndToEndTestUtils.KML).length;
    instrumentation.waitForIdleSync();
    trackNumber = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount();

    importTracks(EndToEndTestUtils.KML);
    checkImport();
    EndToEndTestUtils.SOLO.waitForActivity(TrackListActivity.class);

    assertEquals(trackNumber + KMLFilesNumber,
        EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount());
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

  private String getSaveSuccessMessage(int count, String type) {
    String tracks = activityMyTracks.getResources().getQuantityString(R.plurals.tracks, count,
        count);
    String directoryDisplayName = FileUtils.getDirectoryDisplayName(type);
    return activityMyTracks.getString(R.string.export_external_storage_success, tracks,
        directoryDisplayName);
  }

  private String getImportSuccessMessage(int count, String type) {
    String files = activityMyTracks.getResources().getQuantityString(R.plurals.files, count, count);
    String directoryDisplayName = FileUtils.getDirectoryDisplayName(type);
    return activityMyTracks.getString(R.string.import_success, files, directoryDisplayName);
  }

  private String getImportErrorMessage(int count, int total, String type) {
    String files = activityMyTracks.getResources().getQuantityString(R.plurals.files, total, total);
    String directoryDisplayName = FileUtils.getDirectoryDisplayName(type);
    return activityMyTracks.getString(R.string.import_error, count, files, directoryDisplayName);
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

  /**
   * Exports tracks to external storage.
   * 
   * @param fileType the file type, can be GPX/KML/TCX/CSV currently
   */
  private void exportTracks(String fileType) {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_export_all), true);
    EndToEndTestUtils.SOLO.clickOnText(fileType.toUpperCase());
    EndToEndTestUtils.getButtonOnScreen(
        EndToEndTestUtils.activityMytracks.getString(R.string.generic_ok), true, true);
    EndToEndTestUtils.SOLO.waitForText(getSaveSuccessMessage(2, fileType));
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
  }

  /**
   * Test export/import one track and checks the detail of this track.
   */
  public void testExportImport_checkTrackDetails() {
    EndToEndTestUtils.deleteAllTracks();
    EndToEndTestUtils.deleteExportedFiles(EndToEndTestUtils.GPX);
    EndToEndTestUtils.deleteExportedFiles(EndToEndTestUtils.KML);
    EndToEndTestUtils.deleteExportedFiles(EndToEndTestUtils.TCX);
    EndToEndTestUtils.deleteExportedFiles(EndToEndTestUtils.CSV);
    writeGPXTrackFile();

    importTracks(EndToEndTestUtils.GPX);
    checkImport();
    checkSingleTrack();
    EndToEndTestUtils.deleteExportedFiles(EndToEndTestUtils.GPX);

    exportTracks(EndToEndTestUtils.KML);
    exportTracks(EndToEndTestUtils.GPX);
    exportTracks(EndToEndTestUtils.TCX);
    exportTracks(EndToEndTestUtils.CSV);

    EndToEndTestUtils.deleteAllTracks();
    importTracks(EndToEndTestUtils.GPX);
    checkImport();
    checkSingleTrack();
    EndToEndTestUtils.deleteAllTracks();
    importTracks(EndToEndTestUtils.KML);
    checkImport();
    checkSingleTrack();
  }

  /**
   * Checks the status of import.
   */
  private void checkImport() {
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
  }

  /**
   * Checks the detail of one track.
   */
  private void checkSingleTrack() {
    if (!PreferencesUtils.isMetricUnits(activityMyTracks)) {
      EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.track_detail_stats_tab));
      assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
          .getString(R.string.settings_stats_units_title)));

      // Change preferred units.
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.settings_stats_units_title));
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.unit_kilometer));
      EndToEndTestUtils.SOLO.goBack();
      EndToEndTestUtils.SOLO.goBack();
    }
    PreferencesUtils.setBoolean(activityMyTracks, R.string.stats_show_grade_elevation_key, true);

    EndToEndTestUtils.SOLO.clickOnText(tracksName);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.waitForActivity(TrackDetailActivity.class,
        EndToEndTestUtils.LONG_WAIT_TIME);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    instrumentation.waitForIdleSync();

    float acceptDeviation = 0.1f;
    Activity detailActivity = EndToEndTestUtils.SOLO.getCurrentActivity();
    float minAltitudeActual = Float.parseFloat(((TextView) detailActivity.findViewById(
        R.id.stats_elevation_min).findViewById(R.id.stats_value)).getText().toString());
    float maxAltitudeActual = Float.parseFloat(((TextView) detailActivity.findViewById(
        R.id.stats_elevation_max).findViewById(R.id.stats_value)).getText().toString());
    float distanceActual = Float.parseFloat(((TextView) detailActivity.findViewById(
        R.id.stats_distance).findViewById(R.id.stats_value)).getText().toString());
    float averageSpeedActual = Float.parseFloat(((TextView) detailActivity.findViewById(
        R.id.stats_average_speed).findViewById(R.id.stats_value)).getText().toString());
    // Seconds.
    int timeSpan = 9;

    float distance = 0;
    double initialLat = 39.30;
    double initialLng = 116;
    Location start = new Location("gps");
    start.setLatitude(initialLat);
    start.setLongitude(initialLng);
    for (int i = 1; i < 10; i++) {
      Location end = new Location("gps");
      end.setLatitude(initialLat - 0.0005 * i);
      end.setLongitude(initialLng + 0.0005 * i);
      distance = distance + end.distanceTo(start) / 1000;
      start = end;
    }

    // KM/H
    float averageSpeed = distance * 3600 / timeSpan;
    assertEquals(Float.parseFloat(minAltitude), minAltitudeActual);
    assertEquals(Float.parseFloat(maxAltitude), maxAltitudeActual);
    Log.i(EndToEndTestUtils.LOG_TAG, distance + ":" + distanceActual);
    assertTrue((distance - distanceActual) / distance < acceptDeviation);
    assertTrue((averageSpeed - averageSpeedActual) / averageSpeed < acceptDeviation);

    EndToEndTestUtils.SOLO.goBack();
  }

  /**
   * Writes a GPX track file to MyTracks folder.
   */
  private void writeGPXTrackFile() {
    String fileName = tracksName + ".gpx";
    String fileContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> "
        + "<gpx "
        + "version=\"1.1\" "
        + "creator=\"Created by Google My Tracks on Android.\" "
        + "xmlns=\"http://www.topografix.com/GPX/1/1\" "
        + "xmlns:topografix=\"http://www.topografix.com/GPX/Private/TopoGrafix/0/1\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.topografix.com/GPX/Private/TopoGrafix/0/1 http://www.topografix.com/GPX/Private/TopoGrafix/0/1/topografix.xsd\"> "
        + "<metadata> " + "<name><![CDATA[testTrackName1373523959524]]></name> "
        + "<desc><![CDATA[testTrackDesc1373523959524]]></desc> " + "</metadata> " + "<trk> "
        + "<name><![CDATA[testTrackName1373523959524]]></name> "
        + "<desc><![CDATA[testTrackDesc1373523959524]]></desc> "
        + "<type><![CDATA[TestActivity]]></type> "
        + "<extensions><topografix:color>c0c0c0</topografix:color></extensions> " + "<trkseg> "
        + "<trkpt lat=\"39.30\" lon=\"116\"> " + "<time>2013-07-10T08:00:00.000Z</time> <ele>"
        + minAltitude
        + "</ele>"
        + "</trkpt> "
        + "<trkpt lat=\"39.2995\" lon=\"116.0005\"> "
        + "<time>2013-07-10T08:00:01.000Z</time> "
        + "</trkpt> "
        + "<trkpt lat=\"39.299\" lon=\"116.001\"> "
        + "<time>2013-07-10T08:00:02.000Z</time> "
        + "</trkpt> "
        + "<trkpt lat=\"39.2985\" lon=\"116.0015\"> "
        + "<time>2013-07-10T08:00:03.000Z</time> "
        + "</trkpt> "
        + "<trkpt lat=\"39.298\" lon=\"116.002\"> "
        + "<time>2013-07-10T08:00:04.000Z</time> "
        + "</trkpt> "
        + "<trkpt lat=\"39.2975\" lon=\"116.0025\"> "
        + "<time>2013-07-10T08:00:05.000Z</time> "
        + "</trkpt> "
        + "<trkpt lat=\"39.297\" lon=\"116.003\"> "
        + "<time>2013-07-10T08:00:06.000Z</time> "
        + "</trkpt> "
        + "<trkpt lat=\"39.2965\" lon=\"116.0035\"> "
        + "<time>2013-07-10T08:00:07.000Z</time> "
        + "</trkpt> "
        + "<trkpt lat=\"39.296\" lon=\"116.004\"> "
        + "<time>2013-07-10T08:00:08.000Z</time> "
        + "</trkpt> "
        + "<trkpt lat=\"39.2955\" lon=\"116.0045\"> "
        + "<time>2013-07-10T08:00:09.000Z</time> <ele>"
        + maxAltitude
        + "</ele>"
        + "</trkpt> "
        + "</trkseg> " + "</trk> " + "</gpx>";

    try {
      File file = new File(FileUtils.getDirectoryPath(EndToEndTestUtils.GPX.toLowerCase())
          + File.separator + fileName);
      FileOutputStream fop = new FileOutputStream(file);
      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }
      // get the content in bytes
      byte[] contentInBytes = fileContent.getBytes();
      fop.write(contentInBytes);
      fop.flush();
      fop.close();
      Log.i(EndToEndTestUtils.LOG_TAG, file.getAbsolutePath());
    } catch (IOException e) {
      fail();
    }
  }
}