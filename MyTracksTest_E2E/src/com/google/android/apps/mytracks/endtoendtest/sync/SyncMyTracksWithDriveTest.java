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
package com.google.android.apps.mytracks.endtoendtest.sync;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.apps.mytracks.endtoendtest.GoogleUtils;
import com.google.android.maps.mytracks.R;
import com.google.api.services.drive.Drive;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;

import java.io.IOException;

/**
 * Tests how MyTracks to sync with Google Drive.
 * 
 * @author Youtao Liu
 */
public class SyncMyTracksWithDriveTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  public static Drive drive;
  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public SyncMyTracksWithDriveTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    trackListActivity = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, trackListActivity);
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME);
    drive = SyncTestUtils.getGoogleDrive(trackListActivity.getApplicationContext());
  }

  /**
   * Deletes all tracks in Google Drive and checks in MyTracks.
   * 
   * @throws IOException
   */
  public void testDeleteAllTracksInDrive() throws IOException {
    EndToEndTestUtils.deleteAllTracks();
    EndToEndTestUtils.createTrackIfEmpty(0, true);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_sync_now), true);
    SyncTestUtils.checkFilesNumber(1, drive);

    SyncTestUtils.removeKMLFiles(drive);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_sync_now), true);
    SyncTestUtils.checkTracksNumber(0);
  }
}
