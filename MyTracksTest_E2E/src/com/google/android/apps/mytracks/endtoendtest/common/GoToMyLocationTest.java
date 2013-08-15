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


/**
 * Tests the function of go to my location.
 * 
 * @author Youtao Liu
 */
public class GoToMyLocationTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  public GoToMyLocationTest() {
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
   * Tests the menu My Location.
   */
  public void testGotoMyLocation() {
    EndToEndTestUtils.findAndClickMyLocation(activityMyTracks);
    if (EndToEndTestUtils.isEmulator) {
      String setting = activityMyTracks.getString(
          GoogleLocationUtils.isAvailable(activityMyTracks) ? R.string.gps_google_location_settings
              : R.string.gps_location_access);
      EndToEndTestUtils.SOLO.waitForText(
          activityMyTracks.getString(R.string.my_location_no_gps, setting), 1,
          EndToEndTestUtils.SHORT_WAIT_TIME);
    } else {
      // TODO How to verify the location is shown on the map.
    }
  }
  
  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
