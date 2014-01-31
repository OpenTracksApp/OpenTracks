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
 * Tests delete tracks.
 * 
 * @author Youtao Liu
 */
public class DeleteTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public DeleteTest() {
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
   * Deletes all tracks.
   */
  public void testDeleteAllTracks() {
    EndToEndTestUtils.createSimpleTrack(1, true);

    assertTrue(EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount() > 0);

    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_delete_all), true);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.rotateCurrentActivity();
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_yes), true, true);
    EndToEndTestUtils.waitTextToDisappear(
        trackListActivity.getString(R.string.generic_progress_title));
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.track_list_empty_message)));
    assertEquals(0, EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount());
  }

  /**
   * Deletes one track.
   */
  public void testDeleteOneTrack() {
    EndToEndTestUtils.createSimpleTrack(1, true);

    ListView listView = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0);
    int trackCount = listView.getCount();
    assertTrue(trackCount > 0);

    EndToEndTestUtils.SOLO.clickOnView(listView.getChildAt(0));
    EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.track_detail_chart_tab));
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_delete), true);
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_yes), true, true);
    EndToEndTestUtils.waitTextToDisappear(
        trackListActivity.getString(R.string.generic_progress_title));
    assertEquals(
        trackCount - 1, EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount());
  }

  /**
   * Deletes multiple tracks in {@link TrackListActivity}.
   */
  public void testDeleteMultiple() {
    if (!EndToEndTestUtils.hasActionBar) {
      return;
    }

    // Create 3 tracks
    for (int i = 0; i < 3; i++) {
      EndToEndTestUtils.createSimpleTrack(0, true);
    }

    ListView listView = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0);
    int trackCount = listView.getCount();
    assertTrue(trackCount > 2);

    // Select 3 tracks
    EndToEndTestUtils.SOLO.clickLongOnView(listView.getChildAt(0));
    EndToEndTestUtils.SOLO.clickOnView(listView.getChildAt(1));
    EndToEndTestUtils.SOLO.clickOnView(listView.getChildAt(2));

    // Deselect the first one
    EndToEndTestUtils.SOLO.clickOnView(listView.getChildAt(0));

    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_delete), true);
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_yes), true, true);
    EndToEndTestUtils.waitTextToDisappear(
        trackListActivity.getString(R.string.generic_progress_title));
    assertEquals(
        trackCount - 2, EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount());
  }

  /**
   * Deletes a recording track.
   */
  public void testDeleteRecordingTrack() {
    EndToEndTestUtils.startRecording();
    EndToEndTestUtils.checkUnderRecording();
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_delete), true);
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_yes), true, true);
    EndToEndTestUtils.waitTextToDisappear(
        trackListActivity.getString(R.string.generic_progress_title));
    EndToEndTestUtils.checkNotRecording();
  }
}
