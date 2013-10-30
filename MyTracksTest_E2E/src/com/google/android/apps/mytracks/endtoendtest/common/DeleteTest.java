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

import java.util.ArrayList;

/**
 * Tests the delete of MyTracks.
 * 
 * @author Youtao Liu
 */
public class DeleteTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

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
    ArrayList<ListView> trackListView = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class);
    assertTrue(trackListView.size() > 0);
    assertTrue(trackListView.get(0).getCount() > 0);

    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_delete_all), true);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.rotateCurrentActivity();
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_yes), true,
        true);
    EndToEndTestUtils.waitTextToDisappear(activityMyTracks
        .getString(R.string.generic_progress_title));
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.track_list_empty_message)));
    // There is no track now.
    trackListView = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class);
    assertEquals(0, trackListView.get(0).getCount());
    // Export when there is no track.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_export_all), true);
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.GPX.toUpperCase());
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    EndToEndTestUtils.waitTextToDisappear(activityMyTracks
        .getString(R.string.generic_progress_title));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.export_external_storage_no_track)));
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
    ArrayList<ListView> trackListView = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class);
    int trackNumberOld = trackListView.get(0).getCount();

    EndToEndTestUtils.SOLO.clickOnView(trackListView.get(0).getChildAt(0));
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.track_detail_chart_tab));
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_delete), true);
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_yes), true,
        true);
    EndToEndTestUtils.waitTextToDisappear(activityMyTracks
        .getString(R.string.generic_progress_title));
    trackListView = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class);
    int trackNumberNew = trackListView.get(0).getCount();
    assertEquals(trackNumberOld - 1, trackNumberNew);
  }

  /**
   * Deletes multiple tracks in {@link TrackListActivity}.
   */
  public void testMultipleDeleteInTrackList() {
    if (!EndToEndTestUtils.hasActionBar) {
      return;
    }

    // Get the number of track( or tracks)
    ArrayList<ListView> trackListView = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class);
    int trackNumber = trackListView.get(0).getCount();

    if (trackNumber < 3) {
      for (int i = 0; i < 3 - trackNumber; i++) {
        EndToEndTestUtils.createSimpleTrack(0, true);

      }
    }

    // Get the number of track( or tracks)
    trackListView = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class);
    int trackNumberOld = trackListView.get(0).getCount();

    assertTrue(trackNumberOld > 2);

    // Select 3 tracks.
    EndToEndTestUtils.SOLO.clickLongOnView(trackListView.get(0).getChildAt(0));
    assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks.getString(
        R.string.list_item_selected, 1)));
    EndToEndTestUtils.SOLO.clickOnView(trackListView.get(0).getChildAt(1));
    assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks.getString(
        R.string.list_item_selected, 2)));
    EndToEndTestUtils.SOLO.clickOnView(trackListView.get(0).getChildAt(2));
    assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks.getString(
        R.string.list_item_selected, 3)));

    // Deselect the first one.
    EndToEndTestUtils.SOLO.clickOnView(trackListView.get(0).getChildAt(0));
    assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks.getString(
        R.string.list_item_selected, 2)));

    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_delete), true);
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_yes), true,
        true);

    EndToEndTestUtils.waitTextToDisappear(activityMyTracks
        .getString(R.string.generic_progress_title));
    EndToEndTestUtils.waitTextToDisappear(activityMyTracks
        .getString(R.string.track_delete_progress_message));
    int trackNumberNew = trackNumberOld;
    long startTime = System.currentTimeMillis();
    // Wait a few seconds for the delete.
    while (trackNumberNew != trackNumberOld - 2
        && (System.currentTimeMillis() - startTime) < EndToEndTestUtils.SHORT_WAIT_TIME) {
      trackListView = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class);
      trackNumberNew = trackListView.get(0).getCount();
      EndToEndTestUtils.sleep(EndToEndTestUtils.VERY_SHORT_WAIT_TIME);
    }
    assertEquals(trackNumberOld - 2, trackNumberNew);
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
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_yes), true,
        true);
    EndToEndTestUtils.waitTextToDisappear(activityMyTracks
        .getString(R.string.generic_progress_title));
    EndToEndTestUtils.checkNotRecording();
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
