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
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Instrumentation;
import android.location.Location;
import android.location.LocationManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Tests export all and import all.
 * 
 * @author Youtao Liu
 */
public class ExportAllAndImportAllTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private static final String TAG = ExportAllAndImportAllTest.class.getSimpleName();
  private static final String GPX_FILE_TRACK_NAME = "TestTrackName1373523959524";
  private static final int GPX_FILE_TIME_INTERVAL = 9; // seconds
  private static final double GPX_FILE_INIT_LATITUDE = 39.30;
  private static final double GPX_FILE_INIT_LONGITUDE = 116.0;
  private static final String GPX_FILE_ELEVATION_MAX = "33.3";
  private static final String GPX_FILE_ELEVATION_MIN = "22.2";

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public ExportAllAndImportAllTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    trackListActivity = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, trackListActivity);
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

  /**
   * Tests export all as KML.
   */
  public void testExport_kml() {
    testExport(TrackFileFormat.KML);
  }

  /**
   * Tests export all as GPX.
   */
  public void testExport_gpx() {
    testExport(TrackFileFormat.GPX);
  }

  /**
   * Tests export all as CSV.
   */
  public void testExport_csv() {
    testExport(TrackFileFormat.CSV);
  }

  /**
   * Tests export all as TCX.
   */
  public void testExport_tcx() {
    testExport(TrackFileFormat.TCX);
  }

  /**
   * Test import all KML when there is no file.
   */
  public void testImportNoFile_kml() {
    testImportNoFile(TrackFileFormat.KML);
  }

  /**
   * Test import all GPX when there is no file.
   */
  public void testImportNoFile_gpx() {
    testImportNoFile(TrackFileFormat.GPX);
  }

  /**
   * Tests export all as KML and import all KML when there is only one track.
   */
  public void testExportImportOne_kml() {
    testExportImportOne(TrackFileFormat.KML);
  }

  /**
   * Tests export all as GPX and import all GPX when there is only one track.
   */
  public void testExportImportOne_gpx() {
    testExportImportOne(TrackFileFormat.GPX);
  }

  /**
   * Tests export all as KML and import all KML when there is only one track and
   * the track contains markers.
   */
  public void testExportImportOneWithMarker_kml() {
    testExportImportOneWithMarker(TrackFileFormat.KML);
  }

  /**
   * Tests export all as GPX and import all GPX when there is only one track and
   * the track contains markers.
   */
  public void testExportImportOneWithMarker_gpx() {
    testExportImportOneWithMarker(TrackFileFormat.GPX);
  }

  /**
   * Tests export all as KML and import all KML when there are multiple tracks.
   */
  public void testExportImportMultiple_kml() {
    testExportImportMultiple(TrackFileFormat.KML);
  }

  /**
   * Tests export all as GPX and import all GPX when there are multiple tracks.
   */
  public void testExportImportMultiple_gpx() {
    testExportImportMultiple(TrackFileFormat.GPX);
  }

  /**
   * Tests export all as KML and import all KML when there are multiple tracks
   * and the tracks contain pauses.
   */
  public void testExportImportMultipleWithPause_kml() {
    testExportImportMultipleWithPause(TrackFileFormat.KML);
  }

  /**
   * Tests export all as GPX and import all GPX when there are multiple tracks
   * and the tracks contain pauses.
   */
  public void testExportImportMultipleWithPause_gpx() {
    testExportImportMultipleWithPause(TrackFileFormat.GPX);
  }

  /**
   * Tests export all.
   * 
   * @param trackFileFormat the track file format
   */
  private void testExport(TrackFileFormat trackFileFormat) {
    deleteExternalStorageFiles(trackFileFormat);
    EndToEndTestUtils.deleteAllTracks();

    // Create a new track with 1 gps point
    EndToEndTestUtils.createTrackIfEmpty(1, true);

    assertEquals(1, EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount());

    exportTracks(trackFileFormat, 1);
    assertEquals(1, getExternalStorageFiles(trackFileFormat).length);
  }

  /**
   * Tests import all when there is no file.
   * 
   * @param trackFileFormat the track file format
   */
  private void testImportNoFile(TrackFileFormat trackFileFormat) {
    deleteExternalStorageFiles(trackFileFormat);
    importTracks(trackFileFormat);
    EndToEndTestUtils.SOLO.waitForText(getImportErrorMessage(trackFileFormat));
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_ok), true, true);
  }

  /**
   * Tests export all and import all with one track.
   * 
   * @param trackFileFormat the track file format
   */
  private void testExportImportOne(TrackFileFormat trackFileFormat) {
    EndToEndTestUtils.changeToMetricUnits();
    showGradeElevation();

    addTrackFromGpxFile();
    exportImport(trackFileFormat);
    checkTrackFromGpxFile();
  }

  /**
   * Tests export all and import all with one track containing markers.
   * 
   * @param trackFileFormat the track file format
   */
  private void testExportImportOneWithMarker(TrackFileFormat trackFileFormat) {
    addTrackWithMarker();

    exportImport(trackFileFormat);

    // Check name, activity type, and description
    assertTrue(EndToEndTestUtils.SOLO.waitForText(EndToEndTestUtils.trackName));
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.trackName);
    EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.track_detail_chart_tab));
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_edit), true);
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.trackName));
    assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.activityType));
    assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.trackDescription));
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.generic_cancel));

    // Check markers
    if (EndToEndTestUtils.hasGpsSignal) {
      EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_markers), true);

      // The first marker
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_NAME + 1));
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_TYPE + 1));
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_DESCRIPTION + 1));

      // The second marker
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_NAME + 2));
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_TYPE + 2));
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.WAYPOINT_DESCRIPTION + 2));
    }
  }

  /**
   * Tests export all and import all with multiple tracks.
   * 
   * @param trackFileFormat the track file format
   */
  private void testExportImportMultiple(TrackFileFormat trackFileFormat) {
    addTracks();
    exportImport(trackFileFormat);
  }

  /**
   * Tests export all and import all with multiple tracks containing pauses.
   * 
   * @param trackFileFormat the track file format
   */
  private void testExportImportMultipleWithPause(TrackFileFormat trackFileFormat) {
    addTracksWithPause();
    exportImport(trackFileFormat);
  }

  /**
   * Adds one track by importing it from a GPX file.
   */
  private void addTrackFromGpxFile() {
    EndToEndTestUtils.deleteAllTracks();
    deleteExternalStorageFiles(TrackFileFormat.GPX);
    createOneGpxFile();
    importTracks(TrackFileFormat.GPX);
    checkImportSuccess();
    checkTrackFromGpxFile();
  }

  /**
   * Adds one track with markers.
   */
  private void addTrackWithMarker() {
    EndToEndTestUtils.deleteAllTracks();

    // Create a new track with two markers
    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();

    // Send gps before creating the first marker
    EndToEndTestUtils.sendGps(2);
    EndToEndTestUtils.createWaypoint(0);
    EndToEndTestUtils.sendGps(2, 2);
    EndToEndTestUtils.createWaypoint(1);
    EndToEndTestUtils.sendGps(2, 4);

    // Back to the tracks list
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.stopRecording(true);
  }

  /**
   * Adds multiple tracks.
   */
  private void addTracks() {
    EndToEndTestUtils.deleteAllTracks();

    // Create a new track with 3 gps points
    EndToEndTestUtils.createTrackIfEmpty(3, true);
    instrumentation.waitForIdleSync();

    // Create an empty track
    EndToEndTestUtils.createSimpleTrack(0, true);
    instrumentation.waitForIdleSync();

    assertEquals(2, EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount());
  }

  /**
   * Adds multiple tracks with pauses.
   */
  private void addTracksWithPause() {
    EndToEndTestUtils.deleteAllTracks();

    // Create a new track with 3 gps points
    EndToEndTestUtils.createTrackWithPause(3);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();

    // Create an empty track
    EndToEndTestUtils.createTrackWithPause(0);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();

    assertEquals(2, EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount());
  }

  /**
   * Export all and import all.
   * 
   * @param trackFileFormat the track file format
   */
  private void exportImport(TrackFileFormat trackFileFormat) {
    deleteExternalStorageFiles(trackFileFormat);
    assertEquals(0, getExternalStorageFiles(trackFileFormat).length);

    int trackCount = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount();

    // Export all
    exportTracks(trackFileFormat, trackCount);
    assertEquals(trackCount, getExternalStorageFiles(trackFileFormat).length);

    if (trackCount == 1) {
      EndToEndTestUtils.deleteAllTracks();
    }

    // Import all
    importTracks(trackFileFormat);
    EndToEndTestUtils.rotateCurrentActivity();
    checkImportSuccess();
    EndToEndTestUtils.SOLO.waitForActivity(TrackListActivity.class);
    assertEquals(trackCount == 1 ? 1 : trackCount * 2,
        EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount());
  }

  /**
   * Exports all tracks.
   * 
   * @param trackFileFormat the track file format
   * @param trackCount the track count
   */
  private void exportTracks(TrackFileFormat trackFileFormat, int trackCount) {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_export_all), true);
    EndToEndTestUtils.instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.clickOnText(trackFileFormat.name());
    EndToEndTestUtils.getButtonOnScreen(
        EndToEndTestUtils.trackListActivity.getString(R.string.generic_ok), true, true);
    EndToEndTestUtils.SOLO.waitForText(getExportSuccessMessage(trackCount, trackFileFormat));
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
  }

  /**
   * Imports all tracks.
   * 
   * @param trackFileFormat the track file format
   */
  private void importTracks(TrackFileFormat trackFileFormat) {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_import_all), true);
    EndToEndTestUtils.SOLO.clickOnText(trackFileFormat.name());
    EndToEndTestUtils.getButtonOnScreen(
        EndToEndTestUtils.trackListActivity.getString(R.string.generic_ok), true, true);
  }

  /**
   * Gets the export all success message.
   * 
   * @param count the count of successfully exported tracks
   * @param trackFileFormat the track file format
   */
  private String getExportSuccessMessage(int count, TrackFileFormat trackFileFormat) {
    String trackCount = trackListActivity.getResources()
        .getQuantityString(R.plurals.tracks, count, count);
    String path = FileUtils.getPathDisplayName(trackFileFormat.getExtension());
    return trackListActivity.getString(R.string.export_external_storage_success, trackCount, path);
  }

  /**
   * Gets the import all error message.
   * 
   * @param trackFileFormat the track file format
   */
  private String getImportErrorMessage(TrackFileFormat trackFileFormat) {
    String fileCount = trackListActivity.getResources().getQuantityString(R.plurals.files, 0, 0);
    String path = FileUtils.getPathDisplayName(trackFileFormat.getExtension());
    return trackListActivity.getString(R.string.import_error, 0, fileCount, path);
  }

  /**
   * Checks that import all is success.
   */
  private void checkImportSuccess() {
    // Waiting for the prefix is much faster than waiting for the whole string
    EndToEndTestUtils.waitTextToDisappear(
        trackListActivity.getString(R.string.generic_progress_title));
    EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.import_success).split("%")[0]);
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
  }

  /**
   * Creates one GPX file in the MyTracks folder.
   */
  private void createOneGpxFile() {
    String fileName = GPX_FILE_TRACK_NAME + "." + TrackFileFormat.GPX.getExtension();
    String fileContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " + "<gpx "
        + "version=\"1.1\" " + "creator=\"Created by Google My Tracks on Android.\" "
        + "xmlns=\"http://www.topografix.com/GPX/1/1\" "
        + "xmlns:topografix=\"http://www.topografix.com/GPX/Private/TopoGrafix/0/1\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1"
        + " http://www.topografix.com/GPX/1/1/gpx.xsd"
        + " http://www.topografix.com/GPX/Private/TopoGrafix/0/1"
        + " http://www.topografix.com/GPX/Private/TopoGrafix/0/1/topografix.xsd\"> " + "<metadata> "
        + "<name><![CDATA[" + GPX_FILE_TRACK_NAME + "]]></name> "
        + "<desc><![CDATA[testTrackDescription1373523959524]]></desc> " + "</metadata> " + "<trk> "
        + "<name><![CDATA[" + GPX_FILE_TRACK_NAME + "]]></name> "
        + "<desc><![CDATA[testTrackDescription1373523959524]]></desc> "
        + "<type><![CDATA[TestActivityType]]></type> "
        + "<extensions><topografix:color>c0c0c0</topografix:color></extensions> " + "<trkseg> "
        + "<trkpt lat=\"" + GPX_FILE_INIT_LATITUDE + "\" lon=\"" + GPX_FILE_INIT_LONGITUDE + "\"> "
        + "<time>2013-07-10T08:00:00.000Z</time> <ele>" + GPX_FILE_ELEVATION_MIN + "</ele>"
        + "</trkpt> " + "<trkpt lat=\"39.2995\" lon=\"116.0005\"> "
        + "<time>2013-07-10T08:00:01.000Z</time> " + "</trkpt> "
        + "<trkpt lat=\"39.299\" lon=\"116.001\"> " + "<time>2013-07-10T08:00:02.000Z</time> "
        + "</trkpt> " + "<trkpt lat=\"39.2985\" lon=\"116.0015\"> "
        + "<time>2013-07-10T08:00:03.000Z</time> " + "</trkpt> "
        + "<trkpt lat=\"39.298\" lon=\"116.002\"> " + "<time>2013-07-10T08:00:04.000Z</time> "
        + "</trkpt> " + "<trkpt lat=\"39.2975\" lon=\"116.0025\"> "
        + "<time>2013-07-10T08:00:05.000Z</time> " + "</trkpt> "
        + "<trkpt lat=\"39.297\" lon=\"116.003\"> " + "<time>2013-07-10T08:00:06.000Z</time> "
        + "</trkpt> " + "<trkpt lat=\"39.2965\" lon=\"116.0035\"> "
        + "<time>2013-07-10T08:00:07.000Z</time> " + "</trkpt> "
        + "<trkpt lat=\"39.296\" lon=\"116.004\"> " + "<time>2013-07-10T08:00:08.000Z</time> "
        + "</trkpt> " + "<trkpt lat=\"39.2955\" lon=\"116.0045\"> "
        + "<time>2013-07-10T08:00:09.000Z</time> <ele>" + GPX_FILE_ELEVATION_MAX + "</ele>"
        + "</trkpt> " + "</trkseg> " + "</trk> " + "</gpx>";
    FileOutputStream fileOutputStream = null;
    try {
      File file = new File(
          FileUtils.getPath(TrackFileFormat.GPX.getExtension()) + File.separator + fileName);

      if (!file.exists()) {
        file.createNewFile();
      }
      fileOutputStream = new FileOutputStream(file);
      fileOutputStream.write(fileContent.getBytes());
      fileOutputStream.flush();
    } catch (IOException e) {
      Log.e(TAG, "Unable to write GPX file", e);
      fail();
    } finally {
      if (fileOutputStream != null) {
        try {
          fileOutputStream.close();
        } catch (IOException e) {
          Log.e(TAG, "Unable to close GPX file", e);
          fail();
        }
      }
    }
  }

  /**
   * Checks track imported from a GPX file.
   */
  private void checkTrackFromGpxFile() {
    EndToEndTestUtils.SOLO.clickOnText(GPX_FILE_TRACK_NAME);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.waitForActivity(
        TrackDetailActivity.class, EndToEndTestUtils.LONG_WAIT_TIME);
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.track_detail_stats_tab));
    instrumentation.waitForIdleSync();

    double acceptDeviation = 0.1f;
    Activity activity = EndToEndTestUtils.SOLO.getCurrentActivity();
    double elevationMin = Double.parseDouble(((TextView) activity.findViewById(
        R.id.stats_elevation_min).findViewById(R.id.stats_value)).getText().toString());
    double elevationMax = Double.parseDouble(((TextView) activity.findViewById(
        R.id.stats_elevation_max).findViewById(R.id.stats_value)).getText().toString());
    double distance = Double.parseDouble(((TextView) activity.findViewById(R.id.stats_distance)
        .findViewById(R.id.stats_value)).getText().toString());
    double averageSpeed = Double.parseDouble(((TextView) activity.findViewById(
        R.id.stats_average_speed).findViewById(R.id.stats_value)).getText().toString());

    assertEquals(Double.parseDouble(GPX_FILE_ELEVATION_MIN), elevationMin);
    assertEquals(getGpxFileElevationMax(), elevationMax);

    // in km
    double calculateDistance = getGpxFileDistance();

    // in km/h
    double calculateAverageSpeed = calculateDistance
        / (GPX_FILE_TIME_INTERVAL + UnitConversions.S_TO_MIN * UnitConversions.MIN_TO_HR);

    assertTrue((calculateDistance - distance) / calculateDistance < acceptDeviation);
    assertTrue((calculateAverageSpeed - averageSpeed) / calculateAverageSpeed < acceptDeviation);

    EndToEndTestUtils.SOLO.goBack();
  }

  /**
   * Gets the GPX file distance in km.
   */
  private double getGpxFileDistance() {
    double distance = 0.0;
    double latitude = GPX_FILE_INIT_LATITUDE;
    double longitude = GPX_FILE_INIT_LONGITUDE;

    Location current = new Location(LocationManager.GPS_PROVIDER);
    current.setLatitude(latitude);
    current.setLongitude(longitude);
    for (int i = 1; i < 10; i++) {
      Location next = new Location(LocationManager.GPS_PROVIDER);
      next.setLatitude(latitude - 0.0005 * i);
      next.setLongitude(longitude + 0.0005 * i);
      distance = distance + next.distanceTo(current) * UnitConversions.M_TO_KM;
      current = next;
    }
    return distance;
  }

  /**
   * Gets the GPX file elevation max.
   */
  private double getGpxFileElevationMax() {
    /*
     * Returns the highest value of all the weighted averages. Since there are
     * only two elevation data points, the highest value is the weighted average
     * of both points.
     */
    return (Double.parseDouble(GPX_FILE_ELEVATION_MIN) + Double.parseDouble(GPX_FILE_ELEVATION_MAX))
        / 2.0;
  }

  /**
   * Gets the external storage files.
   * 
   * @param trackFileFormat the track file format
   */
  private File[] getExternalStorageFiles(final TrackFileFormat trackFileFormat) {
    FileFilter filter = new FileFilter() {
        @Override
      public boolean accept(File file) {
        String suffix = trackFileFormat == TrackFileFormat.KML ? ".kmz"
            : "." + trackFileFormat.getExtension();
        return file.getName().endsWith(suffix);
      }
    };
    File dir = new File(FileUtils.getPath(trackFileFormat.getExtension()));
    File[] files = dir.listFiles(filter);
    return files == null ? new File[0] : files;
  }

  /**
   * Deletes the external storage files.
   * 
   * @param trackFileFormat the track file format
   */
  private void deleteExternalStorageFiles(TrackFileFormat trackFileFormat) {
    File dir = new File(FileUtils.getPath(trackFileFormat.getExtension()));
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      file.delete();
    }
  }

  /**
   * Shows grade elevation.
   */
  private void showGradeElevation() {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.track_detail_stats_tab));

    String gradeElevation = trackListActivity.getString(R.string.settings_stats_grade_elevation);
    if (!EndToEndTestUtils.SOLO.isCheckBoxChecked(gradeElevation)) {
      EndToEndTestUtils.SOLO.clickOnText(gradeElevation);
    }
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
  }
}