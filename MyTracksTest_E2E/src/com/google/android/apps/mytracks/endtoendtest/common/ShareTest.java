/*
 * Copyright 2013 Google Inc.
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
import com.google.android.apps.mytracks.endtoendtest.GoogleUtils;
import com.google.android.apps.mytracks.endtoendtest.sync.SyncTestUtils;
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.CheckBox;

import java.util.ArrayList;

/**
 * Tests the share feature.
 * 
 * @author youtaol
 */
public class ShareTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  public ShareTest() {
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
   * Tests the share one track.
   */
  public void testShare() {
    EndToEndTestUtils.resetAllSettings(activityMyTracks, false);
    EndToEndTestUtils.createTrackIfEmpty(0, false);

    // Click share again and confirm the share.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_share), true);
    // Whether can found any account.
    if (EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.send_google_no_account_title), 1,
        EndToEndTestUtils.VERY_SHORT_WAIT_TIME)) {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_ok));
      return;
    }

    boolean isAccount2Bound = false;
    // If Choose account dialog prompt, choose the first account.
    if (EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.send_google_choose_account_title), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)) {
      // Whether can found account2.
      if (EndToEndTestUtils.SOLO.waitForText(GoogleUtils.ACCOUNT_NAME_2, 1,
          EndToEndTestUtils.VERY_SHORT_WAIT_TIME)) {
        isAccount2Bound = true;
      }
      EndToEndTestUtils.SOLO.clickOnText(GoogleUtils.ACCOUNT_NAME_1);
      EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true,
          true);
    }

    // Check all check boxes.
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.share_track_title)));
    ArrayList<CheckBox> checkBoxs = EndToEndTestUtils.SOLO.getCurrentViews(CheckBox.class);
    for (int i = 0; i < checkBoxs.size(); i++) {
      if (!checkBoxs.get(i).isChecked()) {
        EndToEndTestUtils.SOLO.clickOnView(checkBoxs.get(i));
        instrumentation.waitForIdleSync();
      }
    }

    // Input account to share and click OK button.
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.share_track_emails_hint)));
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, GoogleUtils.ACCOUNT_NAME_2);
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);

    // Waiting the send is finish.
    while (EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.generic_progress_title), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)) {}

    assertTrue(EndToEndTestUtils.SOLO.waitForText(EndToEndTestUtils.SOLO
        .getString(R.string.share_track_share_url_title)));
    EndToEndTestUtils.SOLO.goBack();

    // Make more checks if the second account is also bound with this device.
    if (isAccount2Bound) {
      EndToEndTestUtils.SOLO.goBack();
      SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_2);
      EndToEndTestUtils.findMenuItem(
          EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);

      assertTrue(EndToEndTestUtils.SOLO.waitForText(
          activityMyTracks.getString(R.string.track_list_shared_with_me), 1,
          EndToEndTestUtils.SUPER_LONG_WAIT_TIME));
      assertTrue(EndToEndTestUtils.SOLO.waitForText(EndToEndTestUtils.trackName, 1,
          EndToEndTestUtils.SUPER_LONG_WAIT_TIME));
    }
  }
}