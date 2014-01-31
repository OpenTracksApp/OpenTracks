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

/**
 * Tests editing a track.
 * 
 * @author Youtao Liu
 */
public class EditTrackTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public EditTrackTest() {
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
   * Tests editing a track.
   */
  public void testEditTrack() {
    EndToEndTestUtils.deleteAllTracks();

    EndToEndTestUtils.createSimpleTrack(1, false);

    // Click on the Edit menu item
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_edit), true);

    // Rotate phone
    EndToEndTestUtils.rotateCurrentActivity();
    EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(R.string.generic_save));

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
        trackListActivity.getString(R.string.track_edit_activity_type_hint));
    int walkingActivityTypeIndex = 1;
    EndToEndTestUtils.SOLO.clickOnView(EndToEndTestUtils.SOLO.getImage(walkingActivityTypeIndex));
    EndToEndTestUtils.SOLO.clickOnButton(trackListActivity.getString(R.string.generic_ok));

    // The activity type should be replaced by the walking activity type
    assertFalse(EndToEndTestUtils.SOLO.searchText(newActivityType));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        trackListActivity.getString(R.string.activity_type_walking)));

    // Save
    EndToEndTestUtils.SOLO.clickOnButton(trackListActivity.getString(R.string.generic_save));
    instrumentation.waitForIdleSync();

    // Go back to the TrackListActivity
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.searchText(newName));
    assertTrue(EndToEndTestUtils.SOLO.searchText(newDescription));
  }
}