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
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;

/**
 * Tests creating new tracks.
 * 
 * @author Youtao Liu
 */
public class CreateTrackTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public CreateTrackTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    trackListActivity = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, trackListActivity);

    EndToEndTestUtils.deleteAllTracks();
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

  /**
   * Tests creating a new track and checks the relative time in
   * {@link TrackListActivity}.
   */
  public void testNewTrack() {
    // Start a recording
    EndToEndTestUtils.startRecording();
    checkRecording();

    // Stop the recording
    EndToEndTestUtils.stopRecording(false);

    // Save the track
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_save), true, true);
    instrumentation.waitForIdleSync();

    // Go back to the TrackListActivity
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();

    // Make sure "mins ago" is displayed
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        EndToEndTestUtils.getRelativeStartTimeSuffix(), 1, false, true));
  }

  /**
   * Tests creating a new track with two markers.
   */
  public void testNewTrackWithMarker() {
    // Start a recording
    EndToEndTestUtils.startRecording();
    checkRecording();

    // Create a marker
    EndToEndTestUtils.sendGps(2);
    EndToEndTestUtils.createWaypoint(0);

    // Send more gps points
    EndToEndTestUtils.sendGps(2, 2);
    EndToEndTestUtils.createWaypoint(1);

    // Go back to the TrackListActivity
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();

    // Rotate the phone
    EndToEndTestUtils.rotateCurrentActivity();

    // Stop the recording
    EndToEndTestUtils.stopRecording(false);

    // Update edit page and save
    String trackName = EndToEndTestUtils.TRACK_NAME_PREFIX + System.currentTimeMillis();
    EndToEndTestUtils.SOLO.enterText(0, trackName);
    EndToEndTestUtils.SOLO.enterText(1, EndToEndTestUtils.activityType);
    EndToEndTestUtils.SOLO.clickOnButton(trackListActivity.getString(R.string.generic_save));
    instrumentation.waitForIdleSync();

    // Check the track name in the TrackListActivity
    EndToEndTestUtils.SOLO.scrollUp();
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        trackName, 1, EndToEndTestUtils.NORMAL_WAIT_TIME, true, false));

    // Go to the TrackDetailActivity
    EndToEndTestUtils.SOLO.clickOnText(trackName);
    instrumentation.waitForIdleSync();

    // Go to Markers
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_markers), true);
    instrumentation.waitForIdleSync();

    // Make sure there are two markers
    assertTrue(EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount() == 2);
  }

  /**
   * Tests creating a new track with split markers.
   */
  public void testNewTrackWithSplit() {
    EndToEndTestUtils.changeToMetricUnits();

    EndToEndTestUtils.startRecording();
    checkRecording();

    EndToEndTestUtils.findMenuItem(
        trackListActivity.getString(R.string.menu_split_frequency), true);
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.unit_kilometer), 0);
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_ok), true, true);

    // Send gps to give a distance more than one kilometer
    EndToEndTestUtils.sendGps(50);
    assertTrue(
        EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_markers), true));
    instrumentation.waitForIdleSync();
    if (EndToEndTestUtils.isEmulator) {
      assertTrue(EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount() > 0);
    }
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();

    EndToEndTestUtils.stopRecording(true);
  }

  /**
   * Checks recording.
   */
  private void checkRecording() {
    if (EndToEndTestUtils.hasActionBar) {
      // Check the title is Recording...
      assertTrue(EndToEndTestUtils.SOLO.searchText(
          trackListActivity.getString(R.string.generic_recording)));
    }
  }
}