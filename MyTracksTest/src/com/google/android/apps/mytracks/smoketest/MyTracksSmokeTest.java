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
package com.google.android.apps.mytracks.smoketest;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.fragments.MapFragment;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.maps.MapView;
import com.google.android.maps.mytracks.R;
import com.jayway.android.robotium.solo.Solo;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

/**
 * Makes a smoke test of MyTracks. It's a total end-to-end test.
 * 
 * @author Youtao Liu
 */
public class MyTracksSmokeTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private static final String TRACK_NAME_PREFIX = "testTrackName";
  private static final String WAYPOINT_NAME = "testWaypoint";
  private static final String STATISTICS_NAME = "testStatistics";
  private static final String GPX = "gpx";
  private static final String KML = "kml";
  private static final String CSV = "csv";
  private static final String TCX = "tcx";
  private static final String MENU_MORE = "More";

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;
  private Solo solo;
  private boolean checkFirstOpenFlag = false;
  // Check whether the version of Android is ICS.
  private boolean ICSFlag = false;
  private String trackName;

  public MyTracksSmokeTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activityMyTracks = getActivity();
    solo = new Solo(instrumentation, activityMyTracks);
    // Checks if open MyTracks first time after install. If so, there would be a
    // welcome view with accept buttons. And makes sure only check once.
    if (checkFirstOpenFlag == false) {
      if ((getButtonOnScreen(activityMyTracks.getString(R.string.eula_accept)) != null)) {
        verifyFirstLaunch();
      }
      setVersionOfOS();
      checkFirstOpenFlag = true;
    }

    // Makes every track name is unique to make sure every check can be trusted.
    trackName = TRACK_NAME_PREFIX + System.currentTimeMillis();
  }

  /**
   * Switches view from {@link MapFragment} to @ ChartFragment} , then changes
   * to @ StatsFragment} . Finally back to {@link MapFragment}. And check some
   * menus in these views.
   */
  public void testSwitchViewsAndMenusOfView() {
    createSimpleTrack(3);

    solo.clickOnText(activityMyTracks.getString(R.string.track_detail_chart_tab));
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_chart_settings), true);
    assertTrue(solo.searchText(activityMyTracks.getString(R.string.chart_settings_by_distance)));
    solo.goBack();

    solo.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_satellite_mode), false);
    findMenuItem(activityMyTracks.getString(R.string.menu_sensor_state), true);
    assertTrue(solo.searchText(activityMyTracks.getString(R.string.sensor_state_last_sensor_time)));
    solo.goBack();

    solo.clickOnText(activityMyTracks.getString(R.string.track_detail_map_tab));
    sendKeys(KeyEvent.KEYCODE_MENU);
    assertTrue(findMenuItem(activityMyTracks.getString(R.string.menu_my_location), false));
  }

  /**
   * Creates a track with markers and the setting to send to google.
   */
  public void testCreateAndSendTrack() {
    // Create a track at first.
    createSimpleTrack(1);
    instrumentation.waitForIdleSync();
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_send_google), true);
    solo.waitForText(activityMyTracks.getString(R.string.send_google_title));

    ArrayList<CheckBox> checkBoxs = solo.getCurrentCheckBoxes();
    for (int i = 0; i < checkBoxs.size(); i++) {
      if (checkBoxs.get(i).isChecked()) {
        solo.clickOnCheckBox(i);
      }
    }
    instrumentation.waitForIdleSync();
    solo.clickOnText(activityMyTracks
        .getString(R.string.send_google_send_now));
    assertTrue(solo.waitForText(activityMyTracks
        .getString(R.string.send_google_no_service_selected)));
    // Stop here for do not really send.
  }

  /**
   * Tests saving a track to SD card as a GPX file.
   */
  public void testSaveToSDCard_GPX() {
    createSimpleTrack(0);
    saveTrackToSDCard(GPX);
  }

  /**
   * Tests saving a track to SD card as a KML file.
   */
  public void testSaveToSDCard_KML() {
    createSimpleTrack(0);
    saveTrackToSDCard(KML);
  }

  /**
   * Tests saving a track to SD card as a CSV file.
   */
  public void testSaveToSDCard_CSV() {
    createSimpleTrack(0);
    saveTrackToSDCard(CSV);
  }

  /**
   * Tests saving a track to SD card as a TCX file.
   */
  public void testSaveToSDCard_TCX() {
    createSimpleTrack(0);
    saveTrackToSDCard(TCX);
  }

  /**
   * Tests editing a track.
   */
  public void testEditTrack() {
    createSimpleTrack(0);
    instrumentation.waitForIdleSync();
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_edit), true);
    String newTrackName = TRACK_NAME_PREFIX + "_new" + System.currentTimeMillis();
    String newType = "type" + newTrackName;
    String newDesc = "desc" + newTrackName;

    sendKeys(KeyEvent.KEYCODE_DEL);
    solo.enterText(0, newTrackName);
    sendKeys(KeyEvent.KEYCODE_TAB);
    solo.enterText(1, newType);
    sendKeys(KeyEvent.KEYCODE_TAB);
    solo.enterText(2, newDesc);

    solo.clickOnButton(activityMyTracks.getString(R.string.generic_save));
    // Goes back to track list.
    solo.goBack();

    assertTrue(solo.searchText(newTrackName));
    assertTrue(solo.searchText(newDesc));
  }

  /**
   * Tests the menu My Location.
   */
  public void testGotoMyLocation() {
    createSimpleTrack(1);
    solo.sendKey(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_my_location), true);
    // TODO How to verify the location is shown on the map.
  }

  /**
   * Changes some settings and then reset them.
   */
  public void testSetting() {
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    instrumentation.waitForIdleSync();

    // Reset all settings at first.
    solo.clickOnText(activityMyTracks.getString(R.string.settings_reset));
    solo.clickOnButton(activityMyTracks.getString(R.string.generic_ok));
    // Change a setting of display.
    solo.clickOnText(activityMyTracks.getString(R.string.settings_display));
    solo.waitForText(activityMyTracks.getString(R.string.settings_display_metric));
    ArrayList<CheckBox> displayCheckBoxs = solo.getCurrentCheckBoxes();
    boolean useMetric = displayCheckBoxs.get(0).isChecked();
    solo.clickOnCheckBox(0);
    solo.goBack();
    // Change a setting of sharing.
    solo.clickOnText(activityMyTracks.getString(R.string.settings_sharing));
    solo.waitForText(activityMyTracks.getString(R.string.settings_sharing_allow_access));
    ArrayList<CheckBox> sharingCheckBoxs = solo.getCurrentCheckBoxes();
    boolean newMapsPublic = sharingCheckBoxs.get(0).isChecked();
    solo.clickOnCheckBox(0);
    solo.goBack();

    // Reset all settings.
    solo.clickOnText(activityMyTracks.getString(R.string.settings_reset));
    solo.clickOnButton(activityMyTracks.getString(R.string.generic_ok));

    // Check settings.
    solo.clickOnText(activityMyTracks.getString(R.string.settings_display));
    solo.waitForText(activityMyTracks.getString(R.string.settings_display_metric));
    displayCheckBoxs = solo.getCurrentCheckBoxes();
    assertEquals(useMetric, displayCheckBoxs.get(0).isChecked());

    solo.goBack();
    solo.clickOnText(activityMyTracks.getString(R.string.settings_sharing));
    solo.waitForText(activityMyTracks.getString(R.string.settings_sharing_allow_access));
    sharingCheckBoxs = solo.getCurrentCheckBoxes();
    assertEquals(newMapsPublic, sharingCheckBoxs.get(0).isChecked());
  }

  /**
   * Creates one track with a two locations, a way point and a statistics point.
   */
  public void testCreateTrackWithMarkers() {
    startRecording();
    instrumentation.waitForIdleSync();
    SmokeTestUtils.sendGps(2);
    createWaypointAndStatistics();
    SmokeTestUtils.sendGps(2);
    // Back to tracks list.
    solo.goBack();
    stopRecording(false);
    solo.enterText(0, trackName);
    solo.clickOnButton(activityMyTracks.getString(R.string.generic_save));

    instrumentation.waitForIdleSync();
    // Check the new track
    assertTrue(solo.waitForText(trackName, 1, 5000, true, false));
  }

  /**
   * Deletes all tracks.
   */
  public void testDeleteTracks() {
    createSimpleTrack(0);
    solo.goBack();
    instrumentation.waitForIdleSync();
    // There is at least one track.
    ArrayList<ListView> trackListView = solo.getCurrentListViews();
    assertTrue(trackListView.size() > 0);
    assertTrue(trackListView.get(0).getCount() > 0);

    solo.sendKey(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_delete_all), true);
    solo.clickOnButton(activityMyTracks.getString(R.string.generic_ok));
    instrumentation.waitForIdleSync();
    // There is no track now.
    trackListView = solo.getCurrentListViews();
    assertEquals(0, trackListView.get(0).getCount());
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
  public void testExportAndImportTracks() {
    // Creates a new track with 3 gps data.
    createSimpleTrack(3);
    solo.goBack();
    instrumentation.waitForIdleSync();
    // Creates a empty track.
    createSimpleTrack(0);
    solo.goBack();
    instrumentation.waitForIdleSync();
    // Deletes all exported gpx and kml tracks.
    deleteExportedFiles(GPX);
    deleteExportedFiles(KML);
    int gpxFilesNumber = 0;
    File[] allGpxFiles = getExportedFiles(GPX);
    // For the first export, there is no MyTracks folder.
    if (allGpxFiles != null) {
      gpxFilesNumber = getExportedFiles(GPX).length;
    }

    // Gets track number in current track list of MyTracks.
    int trackNumber = solo.getCurrentListViews().get(0).getCount();

    // No file to imported.
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_import_all), true);
    solo.waitForText(activityMyTracks.getString(R.string.import_no_file,
        FileUtils.buildExternalDirectoryPath(GPX)));
    solo.clickOnButton(activityMyTracks.getString(R.string.generic_ok));

    // Clicks to export tracks(At least one track) to Gpx files.
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_export_all), true);
    solo.clickOnText(String.format(activityMyTracks.getString(R.string.menu_export_all_format),
        GPX.toUpperCase()));
    solo.waitForText(activityMyTracks.getString(R.string.export_success));
    // Checks export file.
    assertEquals(gpxFilesNumber + trackNumber, getExportedFiles(GPX).length);
    instrumentation.waitForIdleSync();

    // Clicks to import track.
    gpxFilesNumber = getExportedFiles(GPX).length;
    trackNumber = solo.getCurrentListViews().get(0).getCount();

    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_import_all), true);
    // Waits for the prefix of import success string is much faster than wait
    // the whole string.
    solo.waitForText(activityMyTracks.getString(R.string.import_success).split("%")[0]);
    // Checks import tracks should be equal with the sum of trackNumber and
    // gpxFilesNumber;
    solo.clickOnText(activityMyTracks.getString(R.string.generic_ok));
    instrumentation.waitForIdleSync();
    assertEquals(trackNumber + gpxFilesNumber, solo.getCurrentListViews().get(0).getCount());

    // Clicks to export tracks(At least two tracks) to KML files.
    gpxFilesNumber = getExportedFiles(GPX).length;
    trackNumber = solo.getCurrentListViews().get(0).getCount();
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_export_all), true);
    solo.clickOnText(String.format(activityMyTracks.getString(R.string.menu_export_all_format),
        KML.toUpperCase()));
    solo.waitForText(activityMyTracks.getString(R.string.export_success));
    // Checks export file.
    assertEquals(gpxFilesNumber, getExportedFiles(GPX).length);
    assertEquals(trackNumber, getExportedFiles(KML).length);
  }

  /**
   * Tests the switch between satellite mode and map mode.
   */
  public void testSatelliteAndMapView() {
    startRecording();
    instrumentation.waitForIdleSync();
    // Check current mode.
    boolean isMapMode = true;
    sendKeys(KeyEvent.KEYCODE_MENU);
    // If can not find switch menu in top menu, click More menu.
    if (!findMenuItem(activityMyTracks.getString(R.string.menu_satellite_mode), false)) {
      isMapMode = false;
    }

    // Switches to satellite mode if it's map mode now..
    if (isMapMode) {
      solo.clickOnText(activityMyTracks.getString(R.string.menu_satellite_mode));
    } else {
      solo.clickOnText(activityMyTracks.getString(R.string.menu_map_mode));
    }

    isMapMode = !isMapMode;
    ArrayList<View> allViews = solo.getViews();
    for (View view : allViews) {
      if (view instanceof MapView) {
        if (isMapMode) {
          assertFalse(((MapView) view).isSatellite());
        } else {
          assertTrue(((MapView) view).isSatellite());
        }
      }
    }
    instrumentation.waitForIdleSync();
    // Switches back.
    sendKeys(KeyEvent.KEYCODE_MENU);
    instrumentation.waitForIdleSync();
    if (isMapMode) {
      solo.clickOnText(activityMyTracks.getString(R.string.menu_satellite_mode));
    } else {
      solo.clickOnText(activityMyTracks.getString(R.string.menu_map_mode));
    }
    isMapMode = !isMapMode;
    allViews = solo.getViews();
    for (View view : allViews) {
      if (view instanceof MapView) {
        if (isMapMode) {
          assertFalse(((MapView) view).isSatellite());
        } else {
          assertTrue(((MapView) view).isSatellite());
        }
      }
    }

    stopRecording(true);
  }

  /**
   * Tests following items in More menu.
   * <ul>
   * <li>Tests the aggregated statistics activity.</li>
   * <li>Tests the Sensor state activity.</li>
   * <li>Tests the help menu.</li>
   * </ul>
   */
  public void testSomeMenuItems() {
    // Menu in TrackListActivity.
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_aggregated_statistics), true);
    solo.waitForText(activityMyTracks.getString(R.string.stat_total_distance));
    solo.goBack();

    startRecording();
    instrumentation.waitForIdleSync();
    // Menu in TrackDetailActivity.
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_sensor_state), true);
    solo.waitForText(activityMyTracks.getString(R.string.sensor_state_last_sensor_time));

    solo.goBack();
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_help), true);
    solo.clickOnText(activityMyTracks.getString(R.string.help_about));
    solo.clickOnText(activityMyTracks.getString(R.string.generic_ok));
    solo.clickOnText(activityMyTracks.getString(R.string.generic_ok));

    stopRecording(true);
  }

  /**
   * Tests the long distance and more than 100000 points.
   */
  public void testBigTrack() {
    /*
     * System.out.println(activityMyTracks.getString(R.string.
     * announcement_frequency_key)); sendKeys(KeyEvent.KEYCODE_MENU);
     * solo.clickOnText(activityMyTracks.getString(R.string.menu_record_track));
     * SmokeTestUtils.PAUSE = 0; SmokeTestUtils.sendGps(120000);
     * createWaypointAndStatistics(); SmokeTestUtils.sendGps(2);
     * sendKeys(KeyEvent.KEYCODE_MENU);
     * solo.clickOnText(activityMyTracks.getString
     * (R.string.menu_stop_recording)); solo.enterText(0, trackName);
     * solo.clickOnButton(activityMyTracks.getString(R.string.generic_save));
     */
  }

  /**
   * Creates a way point and a statistics during track recording.
   */
  private void createWaypointAndStatistics() {
    sendKeys(KeyEvent.KEYCODE_MENU);
    findMenuItem(activityMyTracks.getString(R.string.menu_markers), true);

    solo.clickOnButton(activityMyTracks.getString(R.string.marker_list_insert_waypoint));
    solo.clickOnText(activityMyTracks.getString(R.string.marker_detail_marker_name));
    solo.enterText(0, WAYPOINT_NAME);
    solo.clickOnButton(activityMyTracks.getString(R.string.generic_save));

    solo.clickOnButton(activityMyTracks.getString(R.string.marker_list_insert_statistics));
    solo.clickOnText(activityMyTracks.getString(R.string.marker_detail_marker_name));
    solo.enterText(0, STATISTICS_NAME);
    solo.clickOnButton(activityMyTracks.getString(R.string.generic_save));

    assertTrue(solo.searchText(WAYPOINT_NAME));
    assertTrue(solo.searchText(STATISTICS_NAME));

    solo.goBack();
  }

  @Override
  protected void tearDown() throws Exception {
    solo.finishOpenedActivities();
    super.tearDown();
  }

  /**
   * Accepts terms and configures units.
   */
  private void verifyFirstLaunch() {
    solo.clickOnText(activityMyTracks.getString(R.string.eula_accept));
    // Clicks for welcome.
    solo.clickOnText(activityMyTracks.getString(R.string.generic_ok));
    // Clicks for choose units.
    solo.clickOnText(activityMyTracks.getString(R.string.generic_ok));
    instrumentation.waitForIdleSync();
  }

  /**
   * Creates a simple track which can be used by subsequent test.
   * 
   * @param numberOfGpsData number of simulated Gps data.
   */
  private void createSimpleTrack(int numberOfGpsData) {
    startRecording();
    SmokeTestUtils.sendGps(numberOfGpsData);
    stopRecording(true);
  }

  /**
   * Starts recoding track.
   */
  private void startRecording() {
    if (ICSFlag) {
      Button startButton = getButtonOnScreen(activityMyTracks.getString(R.string.menu_record_track));
      // In case a track is recording.
      if (startButton == null) {
        Button stopButton = getButtonOnScreen(activityMyTracks
            .getString(R.string.menu_stop_recording));
        solo.clickOnView(stopButton);
        startButton = getButtonOnScreen(activityMyTracks.getString(R.string.menu_record_track));
      }
      solo.clickOnView(startButton);
    } else {
      sendKeys(KeyEvent.KEYCODE_MENU);
      if (!solo.searchText(activityMyTracks.getString(R.string.menu_record_track))) {
        // Check if in TrackDetailActivity.
        if (solo.searchText(activityMyTracks.getString(R.string.menu_play))) {
          solo.goBack();
        } else {
          // In case a track is recording.
          solo.clickOnText(activityMyTracks.getString(R.string.menu_stop_recording));
          sendKeys(KeyEvent.KEYCODE_MENU);
        }
      }
      instrumentation.waitForIdleSync();
      solo.clickOnText(activityMyTracks.getString(R.string.menu_record_track));
    }
  }

  /**
   * Stop recoding track.
   * 
   * @param isSave ture means should save this track.
   */
  private void stopRecording(boolean isSave) {
    instrumentation.waitForIdleSync();
    if (ICSFlag) {
      solo.clickOnView(getButtonOnScreen(activityMyTracks.getString(R.string.menu_stop_recording)));
    } else {
      sendKeys(KeyEvent.KEYCODE_MENU);
      solo.clickOnText(activityMyTracks.getString(R.string.menu_stop_recording));
    }
    if (isSave) {
      instrumentation.waitForIdleSync();
      solo.clickLongOnText(activityMyTracks.getString(R.string.generic_save));
    }
  }

  /**
   * Deletes a kind of track in MyTracks folder.
   * 
   * @param trackKind the kind of track
   */
  private void deleteExportedFiles(String trackKind) {
    File[] allFiles = (new File(FileUtils.buildExternalDirectoryPath(trackKind.toLowerCase())))
        .listFiles();
    if (allFiles != null) {
      for (File oneFile : allFiles) {
        oneFile.delete();
      }
    }
  }

  /**
   * Gets a kind of exported files.
   * 
   * @param trackKind the kind of track
   * @return files array of such kind of exported tracks
   */
  private File[] getExportedFiles(final String trackKind) {
    String filePath = FileUtils.buildExternalDirectoryPath(trackKind);
    FileFilter filter = new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        if (pathname.getName().indexOf("." + trackKind) > 0) { return true; }
        return false;
      }
    };
    return (new File(filePath)).listFiles(filter);
  }

  /**
   * Save a track to a kind of file in SD card.
   * 
   * @param trackKind the kind of track
   */
  private void saveTrackToSDCard(String trackKind) {
    deleteExportedFiles(trackKind);
    instrumentation.waitForIdleSync();
    sendKeys(KeyEvent.KEYCODE_MENU);
    instrumentation.waitForIdleSync();
    solo.clickOnText(activityMyTracks.getString(R.string.menu_save));
    solo.clickOnText(trackKind.toUpperCase());
    solo.waitForText(activityMyTracks.getString(R.string.generic_success_title));
    assertEquals(1, getExportedFiles(trackKind).length);
  }

  /**
   * Checks if a button is existed in the screen.
   * 
   * @param buttonName the name string of the button.
   * @return the button to search, and null means can not find the button.
   */
  private Button getButtonOnScreen(String buttonName) {
    ArrayList<Button> currentButtons = solo.getCurrentButtons();
    Button resultButton = null;
    for (Button button : currentButtons) {
      if (((String) button.getText()).equalsIgnoreCase(buttonName)) { return button; }
    }
    return resultButton;
  }

  /**
   * Check whether the version of Android is ICS.
   */
  private void setVersionOfOS() {
    // If can find record button without pressing Menu, it should be ICS.
    Button startButton = getButtonOnScreen(activityMyTracks.getString(R.string.menu_record_track));
    Button stopButton = getButtonOnScreen(activityMyTracks.getString(R.string.menu_stop_recording));
    if (startButton != null || stopButton != null) {
      ICSFlag = true;
    } else {
      sendKeys(KeyEvent.KEYCODE_MENU);
      if (solo.searchText(activityMyTracks.getString(R.string.menu_record_track))
          || solo.searchText(activityMyTracks.getString(R.string.menu_stop_recording))) {
        ICSFlag = false;
      } else {
        fail();
      }
      solo.goBack();
    }
  }

  /**
   * Find an item in the menu.
   * 
   * @param menuName the name of item
   * @param click true means need click this menu
   * @return true if find this menu
   */
  private boolean findMenuItem(String menuName, boolean click) {
    boolean findResult = false;
    if (solo.searchText(menuName)) {
      findResult = true;
    } else if (solo.searchText(MENU_MORE)) {
      solo.clickOnText(MENU_MORE);
      findResult = solo.searchText(menuName);
    } else {
      findResult = false;
    }
    if (findResult && click) {
      solo.clickOnText(menuName);
    }
    return findResult;
  }
}
