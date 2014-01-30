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
import android.view.KeyEvent;
import android.widget.ListView;

/**
 * Tests creating a track with markers, and editing and sending send a track.
 * 
 * @author Youtao Liu
 */
public class CreateTrackTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  public CreateTrackTest() {
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
   * Tests editing a track.
   */
  public void testEditTrack() {
    // Create a track and open in TrackEditActivitiy
    EndToEndTestUtils.createTrackIfEmpty(1, false);
    instrumentation.waitForIdleSync();

    // Click on the Edit menu item
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_edit), true);
    instrumentation.waitForIdleSync();

    // Rotate phone
    EndToEndTestUtils.rotateCurrentActivity();
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.generic_save));

    String newName = EndToEndTestUtils.TRACK_NAME_PREFIX + "_new" + System.currentTimeMillis();
    String newActivityType = EndToEndTestUtils.activityType;
    String newDescription = "new_description_" + newName;

    // Clear name field
    sendKeys(KeyEvent.KEYCODE_DEL);
    // Enter new name
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, newName);
    // Enter new activity type
    EndToEndTestUtils.SOLO.clearEditText(1);
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(1, newActivityType);
    // Enter new description
    EndToEndTestUtils.SOLO.clearEditText(2);
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(2, newDescription);

    // Change activity type using the activity picker
    assertTrue(EndToEndTestUtils.SOLO.searchText(newActivityType));
    EndToEndTestUtils.SOLO.clickOnView(EndToEndTestUtils.SOLO.getCurrentActivity()
        .findViewById(R.id.track_edit_activity_type_icon));
    EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.track_edit_activity_type_hint));
    int walkingActivityTypeIndex = 1;
    EndToEndTestUtils.SOLO.clickOnView(EndToEndTestUtils.SOLO.getImage(walkingActivityTypeIndex));
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_ok));
    // The activity type should be replaced by the walking activity type
    assertFalse(EndToEndTestUtils.SOLO.searchText(newActivityType));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        activityMyTracks.getString(R.string.activity_type_walking)));

    // Save
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_save));
    instrumentation.waitForIdleSync();

    // Go back to the TrackListActivity
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.searchText(newName));
    assertTrue(EndToEndTestUtils.SOLO.searchText(newDescription));
  }

  /**
   * Creates a track with one marker.
   */
  public void testCreateTrackWithMarker() {
    
    // Start recording
    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();

    // Send GPS points
    EndToEndTestUtils.sendGps(2);
    if (EndToEndTestUtils.hasActionBar) {
      // Check the title is Recording...
      assertTrue(EndToEndTestUtils.SOLO.searchText(
          activityMyTracks.getString(R.string.generic_recording)));
    }

    // Create marker
    EndToEndTestUtils.createWaypoint(0);
    EndToEndTestUtils.sendGps(2, 2);

    // Go back to the TrackListActivity
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();

    // Rotate phone
    EndToEndTestUtils.rotateCurrentActivity();

    // Stops recording
    EndToEndTestUtils.stopRecording(false);

    // Update Edit page and Save
    String name = EndToEndTestUtils.TRACK_NAME_PREFIX + System.currentTimeMillis();
    EndToEndTestUtils.SOLO.enterText(0, name);
    EndToEndTestUtils.SOLO.enterText(1, EndToEndTestUtils.activityType);
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_save));
    instrumentation.waitForIdleSync();

    // Check the new track name in TrackListActivity
    EndToEndTestUtils.SOLO.scrollUp();
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        name, 1, EndToEndTestUtils.NORMAL_WAIT_TIME, true, false));
  }

  /**
   * Tests displaying relative time like "mins ago" for a new track.
   */
  public void testNewTrackRelativeTime() {
    
    // Delete all tracks
    EndToEndTestUtils.deleteAllTracks();
    
    // Reset all settings
    EndToEndTestUtils.resetAllSettings(activityMyTracks, false);

    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.track_list_empty_message)));
    
    // Start a new recording
    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    
    // Stops the recording
    EndToEndTestUtils.stopRecording(false);
    instrumentation.waitForIdleSync();
    
    // Save the track
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_save), true,
        true);
    instrumentation.waitForIdleSync();
    
    // Go back to the TrackListActivity
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    
    // Make sure "mins ago" is displayed
    assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.RELATIVE_START_TIME_POSTFIX, 1,
        false, true));
  }

  /**
   * Tests whether the split marker is created as setting.
   */
  public void testSplitSetting() {
    EndToEndTestUtils.startRecording();

    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_split_frequency), true);
    boolean isFoundKM = EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.KM);
    if (isFoundKM) {
      EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.KM, 0);
    } else {
      EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.MILE, 0);
    }

    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    
    // Send Gps to give a distance more than one kilometer or one mile.
    EndToEndTestUtils.sendGps(50);
    assertTrue(EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_markers),
        true));
    instrumentation.waitForIdleSync();
    if (EndToEndTestUtils.hasGpsSignal) {
      assertTrue(EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount() > 0);
    } else {
      assertTrue(EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount() == 0);
    }
    EndToEndTestUtils.SOLO.goBack();

    EndToEndTestUtils.stopRecording(true);
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }
}