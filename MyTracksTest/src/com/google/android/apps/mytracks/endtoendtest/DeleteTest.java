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
package com.google.android.apps.mytracks.endtoendtest;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.maps.mytracks.R;

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Tests the delete of MyTracks.
 * 
 * @author Youtao Liu
 */
public class DeleteTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  @TargetApi(16)
  public DeleteTest() {
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
   * Deletes all tracks.
   */
  public void testDeleteAllTracks() {
    EndToEndTestUtils.createTrackIfEmpty(1, true);
    instrumentation.waitForIdleSync();
    // There is at least one track.
    ArrayList<ListView> trackListView = EndToEndTestUtils.SOLO.getCurrentListViews();
    assertTrue(trackListView.size() > 0);
    assertTrue(trackListView.get(0).getCount() > 0);

    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_delete_all), true);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.rotateAllActivities();
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.track_list_empty_message)));
    // There is no track now.
    trackListView = EndToEndTestUtils.SOLO.getCurrentListViews();
    assertEquals(0, trackListView.get(0).getCount());
    // Export when there is no track.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_save_all), true);
    EndToEndTestUtils.SOLO
        .clickOnText(String.format(activityMyTracks.getString(R.string.menu_save_format),
            EndToEndTestUtils.GPX.toUpperCase()));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.sd_card_save_error_no_track)));
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
  }

  /**
   * Deletes only one track.
   */
  public void testDeleteSingleTrack() {
    EndToEndTestUtils.createTrackIfEmpty(1, true);
    instrumentation.waitForIdleSync();
    // Get the number of track( or tracks)
    ArrayList<ListView> trackListView = EndToEndTestUtils.SOLO.getCurrentListViews();
    int trackNumberOld = trackListView.get(0).getCount();

    EndToEndTestUtils.SOLO.clickOnView(trackListView.get(0).getChildAt(0));
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_delete), true);
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
    trackListView = EndToEndTestUtils.SOLO.getCurrentListViews();
    int trackNumberNew = trackListView.get(0).getCount();
    assertEquals(trackNumberOld - 1, trackNumberNew);
  }

  /**
   * Deletes a track which is under recording.
   */
  public void testDeleteOneTrackUnderRecording() {
    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.checkUnderRecording();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_delete), true);
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.checkNotRecording();
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
