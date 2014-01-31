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
import com.google.android.apps.mytracks.util.GoogleLocationUtils;
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

/**
 * Tests the my location button.
 * 
 * @author Youtao Liu
 */
public class MyLocationTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public MyLocationTest() {
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
   * Tests the my location button.
   */
  public void testMyLocation() {
    EndToEndTestUtils.deleteAllTracks();
    EndToEndTestUtils.createSimpleTrack(1, false);
    View myLocation = EndToEndTestUtils.SOLO.getCurrentActivity()
        .findViewById(R.id.map_my_location);
    EndToEndTestUtils.SOLO.clickOnView(myLocation);
    instrumentation.waitForIdleSync();

    if (EndToEndTestUtils.isEmulator) {
      EndToEndTestUtils.SOLO.waitForText(
          GoogleLocationUtils.getGpsDisabledMyLocationMessage(trackListActivity), 1,
          EndToEndTestUtils.SHORT_WAIT_TIME);
    } else {
      // TODO How to verify my location is shown on the map?
    }
  }
}
