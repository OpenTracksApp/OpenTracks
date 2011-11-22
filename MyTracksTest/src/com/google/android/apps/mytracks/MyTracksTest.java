/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.services.ServiceUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;

import java.io.File;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A unit test for {@link MyTracks} activity.
 *
 * @author Bartlomiej Niechwiej
 */
public class MyTracksTest extends ActivityInstrumentationTestCase2<MyTracks>{
  private SharedPreferences sharedPreferences;
  private TrackRecordingServiceConnection serviceConnection;

  public MyTracksTest() {
    super(MyTracks.class);
  }

  @Override
  protected void tearDown() throws Exception {
    clearSelectedAndRecordingTracks();
    waitForIdle();
    super.tearDown();
  }

  public void testInitialization_mainAction() {
    // Make sure we can start MyTracks and the activity doesn't start recording.
    assertInitialized();

    // Check if not recording.
    assertFalse(isRecording());
    assertEquals(-1, getRecordingTrackId());
    long selectedTrackId = getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());
  }

  public void testInitialization_viewActionWithNoData() {
    // Simulate start with ACTION_VIEW intent.
    Intent startIntent = new Intent();
    startIntent.setAction(Intent.ACTION_VIEW);
    setActivityIntent(startIntent);

    assertInitialized();

    // Check if not recording.
    assertFalse(isRecording());
    assertEquals(-1, getRecordingTrackId());
    long selectedTrackId = getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());
  }

  public void testInitialization_viewActionWithValidData() throws Exception {
    // Simulate start with ACTION_VIEW intent.
    Intent startIntent = new Intent();
    startIntent.setAction(Intent.ACTION_VIEW);
    Uri uri = Uri.fromFile(File.createTempFile("valid", ".gpx", getActivity().getFilesDir()));

    // TODO: Add a valid GPX.

    startIntent.setData(uri);
    setActivityIntent(startIntent);

    assertInitialized();

    // Check if not recording.
    assertFalse(isRecording());
    assertEquals(-1, getRecordingTrackId());
    long selectedTrackId = getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());

    // TODO: Finish this test.
  }

  public void testInitialization_viewActionWithInvalidData() throws Exception {
    // Simulate start with ACTION_VIEW intent.
    Intent startIntent = new Intent();
    startIntent.setAction(Intent.ACTION_VIEW);
    Uri uri = Uri.fromFile(File.createTempFile("invalid", ".gpx", getActivity().getFilesDir()));
    startIntent.setData(uri);
    setActivityIntent(startIntent);

    assertInitialized();

    // Check if not recording.
    assertFalse(isRecording());
    assertEquals(-1, getRecordingTrackId());
    long selectedTrackId = getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());

    // TODO: Finish this test.
  }

  public void testRecording_startAndStop() throws Exception {
    assertInitialized();

    // Check if not recording.
    clearSelectedAndRecordingTracks();
    waitForIdle();

    assertFalse(isRecording());
    assertEquals(-1, getRecordingTrackId());
    long selectedTrackId = getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());

    // Start a new track.
    getActivity().startRecording();
    serviceConnection.bindIfRunning();
    long recordingTrackId = awaitRecordingStatus(5000, true);
    assertTrue(recordingTrackId >= 0);

    // Wait until we are done and make sure that selectedTrack = recordingTrack.
    waitForIdle();
    assertEquals(recordingTrackId, getSharedPreferences().getLong(
        getActivity().getString(R.string.recording_track_key), -1));
    selectedTrackId = getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(recordingTrackId, selectedTrackId);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());

    // Watch for MyTracksDetails activity.
    ActivityMonitor monitor = getInstrumentation().addMonitor(
        TrackDetails.class.getName(), null, false);

    // Now, stop the track and make sure that it is still selected, but
    // no longer recording.
    getActivity().stopRecording();

    // Check if we got back MyTracksDetails activity.
    Activity activity = getInstrumentation().waitForMonitor(monitor);
    assertTrue(activity instanceof TrackDetails);

    // TODO: Update track name and other properties and test if they were
    // properly saved.

    // Simulate a click on Save button.
    final Button save = (Button) activity.findViewById(R.id.trackdetails_save);
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        save.performClick();
      }
    });  

    // Check the remaining properties.
    recordingTrackId = awaitRecordingStatus(5000, false);
    assertEquals(-1, recordingTrackId);
    assertEquals(recordingTrackId, getRecordingTrackId());
    assertEquals(recordingTrackId, getSharedPreferences().getLong(
        getActivity().getString(R.string.recording_track_key), -1));
    // Make sure this is the same track as the last recording track ID.
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());
  }

  private void assertInitialized() {
    assertNotNull(getActivity());

    serviceConnection = new TrackRecordingServiceConnection(getActivity(), null);
  }

  /**
   * Waits until the UI thread becomes idle.
   */
  private void waitForIdle() throws InterruptedException {
    // Note: We can't use getInstrumentation().waitForIdleSync() here.
    final Object semaphore = new Object();
    synchronized (semaphore) {
      final AtomicBoolean isIdle = new AtomicBoolean();
      getInstrumentation().waitForIdle(new Runnable() {
        @Override
        public void run() {
          synchronized (semaphore) {
            isIdle.set(true);
            semaphore.notify();
          }
        }
      });
      while (!isIdle.get()) {
        semaphore.wait();
      }
    }
  }

  /**
   * Clears {selected,recording}TrackId in the {@link #getSharedPreferences()}.
   */
  private void clearSelectedAndRecordingTracks() {
    Editor editor = getSharedPreferences().edit();
    editor.putLong(getActivity().getString(R.string.selected_track_key), -1);
    editor.putLong(getActivity().getString(R.string.recording_track_key), -1);

    editor.clear();
    editor.apply();
  }

  /**
   * Waits until the recording state changes to the given status.
   *
   * @param timeout the maximum time to wait, in milliseconds.
   * @param isRecording the final status to await.
   * @return the recording track ID.
   */
  private long awaitRecordingStatus(long timeout, boolean isRecording)
      throws TimeoutException, InterruptedException {
    long startTime = System.nanoTime();
    while (isRecording() != isRecording) {
      if (System.nanoTime() - startTime > timeout * 1000000) {
        throw new TimeoutException("Timeout while waiting for recording!");
      }
      Thread.sleep(20);
    }
    waitForIdle();
    assertEquals(isRecording, isRecording());
    return getRecordingTrackId();
  }

  private long getRecordingTrackId() {
    return getSharedPreferences().getLong(getActivity().getString(R.string.recording_track_key), -1);
  }

  private SharedPreferences getSharedPreferences() {
    if (sharedPreferences == null) {
      sharedPreferences = getActivity().getSharedPreferences(Constants.SETTINGS_NAME, 0);
    }
    return sharedPreferences;
  }

  private boolean isRecording() {
    return ServiceUtils.isRecording(getActivity(),
        serviceConnection.getServiceIfBound(), getSharedPreferences());
  }
}
