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
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.model.File;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Tests the two-ways sync of MyTracks and Google Drive.
 * 
 * @author Youtao Liu
 */
public class SyncDriveWithMyTracksTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  public SyncDriveWithMyTracksTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activityMyTracks = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, activityMyTracks);
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME);
  }

  /**
   * Deletes all tracks in MyTracks and checks in Google Drive. Then creates one
   * tracks in MyTracks and checks it in Google Drive.
   * 
   * @throws IOException
   */
  public void testDeleteAllTracksInMyTracks() throws IOException {
    EndToEndTestUtils.deleteAllTracks();
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_sync_now), true);
    SyncTestUtils.checkFilesNumber(0);
  }

  /**
   * Creates one empty track and non-empty track in MyTracks and then check them
   * in Google Drive.
   * 
   * @throws IOException
   */
  public void testCreateTracksInMyTracks() throws IOException {
    EndToEndTestUtils.deleteAllTracks();
    EndToEndTestUtils.createSimpleTrack(0, true);
    EndToEndTestUtils.createSimpleTrack(3, true);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_sync_now), true);
    SyncTestUtils.checkFilesNumber(2);
  }

  /**
   * Creates one empty track and non-empty track in MyTracks and then check them
   * in Google Drive.
   * 
   * @throws IOException
   */
  public void testEditTrackInMyTracks() throws IOException {
    EndToEndTestUtils.createTrackIfEmpty(3, true);

    // Sync this track.
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_sync_now), true);
    SyncTestUtils.updateDriveData(activityMyTracks.getApplicationContext());
    assertTrue(SyncTestUtils.checkFile(EndToEndTestUtils.trackName, true));
    String oldTrack = getContentOfFile(SyncTestUtils.getFile(EndToEndTestUtils.trackName));

    String oldTrackName = EndToEndTestUtils.trackName;

    // Edit this track.
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.trackName);
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_edit), true);
    String newTrackName = EndToEndTestUtils.TRACK_NAME_PREFIX + "_new" + System.currentTimeMillis();
    String newType = EndToEndTestUtils.activityType + newTrackName;
    String newDesc = "desc" + newTrackName;
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.generic_save));
    sendKeys(KeyEvent.KEYCODE_DEL);
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, newTrackName);
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(1, newType);
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(2, newDesc);
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_save));
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();

    // Sync again.
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_sync_now), true);
    SyncTestUtils.updateDriveData(activityMyTracks.getApplicationContext());
    assertFalse(SyncTestUtils.checkFile(oldTrackName, false));
    assertTrue(SyncTestUtils.checkFile(newTrackName, true));
    String newTrack = getContentOfFile(SyncTestUtils.getFile(newTrackName));

    // Check.
    assertNotSame(oldTrack, newTrack);
    assertTrue(newTrack.indexOf(newTrackName) > 0);
    assertTrue(newTrack.indexOf(newType) > 0);
    assertTrue(newTrack.indexOf(newDesc) > 0);
  }

  private String getContentOfFile(File file) throws IOException {
    SyncTestUtils.updateDriveData(activityMyTracks.getApplicationContext());
    HttpResponse resp = SyncTestUtils.drive.getRequestFactory()
        .buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
    InputStream response = resp.getContent();
    BufferedReader br = new BufferedReader(new InputStreamReader(response));

    StringBuilder sb = new StringBuilder();

    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    String fileContent = sb.toString();

    br.close();
    return fileContent;
  }
}
