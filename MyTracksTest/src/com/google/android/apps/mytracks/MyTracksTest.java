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

import com.google.android.apps.mytracks.services.ITrackRecordingService;
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
  public MyTracksTest() {
    super(MyTracks.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    MyTracks.clearInstance();
    assertNull(MyTracks.getInstance());
  }

  @Override
  protected void tearDown() throws Exception {
    clearSelectedAndRecordingTracks();
    waitForIdle();
    super.tearDown();
  }
  
  public void testInitialization_mainAction() {
    // Make sure we can start MyTracks and the activity doesn't start recording.
    assertNotNull(getActivity());
    assertNotNull(MyTracks.getInstance());
    assertNotNull(getActivity().getSharedPreferences());
    
    // Check if not recording.
    assertFalse(getActivity().isRecording());
    assertEquals(-1, getActivity().getRecordingTrackId());
    long selectedTrackId = getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());
  }
  
  public void testInitialization_viewActionWithNoData() {
    // Simulate start with ACTION_VIEW intent.
    Intent startIntent = new Intent();
    startIntent.setAction(Intent.ACTION_VIEW);
    setActivityIntent(startIntent);
    
    assertNotNull(getActivity());
    assertNotNull(MyTracks.getInstance());
    assertNotNull(getActivity().getSharedPreferences());
    
    // Check if not recording.
    assertFalse(getActivity().isRecording());
    assertEquals(-1, getActivity().getRecordingTrackId());
    long selectedTrackId = getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());
  }
  
  public void testInitialization_viewActionWithValidData() throws Exception {
    // Simulate start with ACTION_VIEW intent.
    Intent startIntent = new Intent();
    startIntent.setAction(Intent.ACTION_VIEW);
    Uri uri = Uri.fromFile(File.createTempFile("valid", ".gpx"));

    // TODO: Add a valid GPX.
    
    startIntent.setData(uri);
    setActivityIntent(startIntent);
    
    assertNotNull(getActivity());
    assertNotNull(MyTracks.getInstance());
    assertNotNull(getActivity().getSharedPreferences());
    
    // Check if not recording.
    assertFalse(getActivity().isRecording());
    assertEquals(-1, getActivity().getRecordingTrackId());
    long selectedTrackId = getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());
    
   // TODO: Finish this test.
  }
  
  public void testInitialization_viewActionWithInvalidData() throws Exception {
    // Simulate start with ACTION_VIEW intent.
    Intent startIntent = new Intent();
    startIntent.setAction(Intent.ACTION_VIEW);
    Uri uri = Uri.fromFile(File.createTempFile("invalid", ".gpx"));
    startIntent.setData(uri);
    setActivityIntent(startIntent);
    
    assertNotNull(getActivity());
    assertNotNull(MyTracks.getInstance());
    assertNotNull(getActivity().getSharedPreferences());
    
    // Check if not recording.
    assertFalse(getActivity().isRecording());
    assertEquals(-1, getActivity().getRecordingTrackId());
    long selectedTrackId = getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());
    
    // TODO: Finish this test.
  }
  
  public void testRecording_startAndStop() throws Exception {
    // Make sure we can start MyTracks and the activity doesn't start recording.
    assertNotNull(getActivity());
    assertNotNull(MyTracks.getInstance());
    assertNotNull(getActivity().getSharedPreferences());

    // Check if not recording.
    clearSelectedAndRecordingTracks();    
    waitForIdle();

    assertFalse(getActivity().isRecording());
    assertEquals(-1, getActivity().getRecordingTrackId());
    long selectedTrackId = getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());

    // Start a new track.
    getActivity().startRecording();
    long recordingTrackId = awaitRecordingStatus(5000, true);
    assertTrue(recordingTrackId >= 0);
    
    // Wait until we are done and make sure that selectedTrack = recordingTrack.
    waitForIdle();
    assertEquals(recordingTrackId, getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.recording_track_key), -1));
    selectedTrackId = getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(recordingTrackId, selectedTrackId);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());
    
    // Watch for MyTracksDetails activity. 
    ActivityMonitor monitor = getInstrumentation().addMonitor(
        MyTracksDetails.class.getName(), null, false);

    // Now, stop the track and make sure that it is still selected, but
    // no longer recording.
    getActivity().stopRecording();
    
    // Check if we got back MyTracksDetails activity. 
    Activity activity = getInstrumentation().waitForMonitor(monitor);
    assertTrue(activity instanceof MyTracksDetails);
    
    // TODO: Update track name and other properties and test if they were
    // properly saved.
    
    // Simulate a click on Save button.
    Button save = (Button) activity.findViewById(R.id.trackdetails_save);
    save.performClick();

    // Check the remaining properties.
    recordingTrackId = awaitRecordingStatus(5000, false);
    assertEquals(-1, recordingTrackId);
    assertEquals(recordingTrackId, getActivity().getRecordingTrackId());
    assertEquals(recordingTrackId, getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.recording_track_key), -1));
    // Make sure this is the same track as the last recording track ID.
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());
  }
  
  public void testRecording_changePreferences() throws Exception {
    // Make sure we can start MyTracks and the activity doesn't start recording.
    assertNotNull(getActivity());
    assertNotNull(MyTracks.getInstance());
    assertNotNull(getActivity().getSharedPreferences());
    
    // Check if not recording.
    clearSelectedAndRecordingTracks();    
    waitForIdle();
    assertFalse(getActivity().isRecording());
    assertEquals(-1, getActivity().getRecordingTrackId());
    long selectedTrackId = getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());
    
    // Start a new track.
    getActivity().startRecording();
    long recordingTrackId = awaitRecordingStatus(5000, true);
    assertTrue(recordingTrackId >= 0);
    
    // Wait until we are done and make sure that selectedTrack = recordingTrack.
    waitForIdle();
    assertEquals(recordingTrackId, getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.recording_track_key), -1));
    selectedTrackId = getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.selected_track_key), -1);
    assertEquals(recordingTrackId, selectedTrackId);
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());

    // Change shared preferences and observe if the service notices the change.
    Editor editor = getActivity().getSharedPreferences().edit();
    editor.putInt(getActivity().getString(R.string.announcement_frequency_key),
        1);
    editor.putInt(getActivity().getString(R.string.split_frequency_key), 1);
    editor.putInt(
        getActivity().getString(R.string.signal_sampling_frequency_key), 1);
    editor.commit();

    // Notify the service about changed preferences.
    ITrackRecordingService service = getActivity().getTrackRecordingService();
    assertNotNull(service);
    service.sharedPreferenceChanged(null);
    
    // TODO: Test if the service has updated its preferences.
    
    // Watch for MyTracksDetails activity. 
    ActivityMonitor monitor = getInstrumentation().addMonitor(
        MyTracksDetails.class.getName(), null, false);
    
    // Now, stop the track and make sure that it is still selected, but
    // no longer recording.
    getActivity().stopRecording();

    // Check if we got back MyTracksDetails activity. 
    Activity activity = getInstrumentation().waitForMonitor(monitor);
    assertTrue(activity instanceof MyTracksDetails);
    // Simulate a click on Save button.
    Button save = (Button) activity.findViewById(R.id.trackdetails_save);
    save.performClick();    

    // Check if after stopping the service all properties are up to date.
    recordingTrackId = awaitRecordingStatus(5000, false);
    assertEquals(-1, recordingTrackId);
    assertEquals(recordingTrackId, getActivity().getRecordingTrackId());
    assertEquals(recordingTrackId, getActivity().getSharedPreferences().getLong(
        getActivity().getString(R.string.recording_track_key), -1));
    // Make sure this is the same track as the last recording track ID.
    assertEquals(selectedTrackId, getActivity().getSelectedTrackId());
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
   * Clears {selected,recording}TrackId in the {@link SharedPreferences}.
   */
  private void clearSelectedAndRecordingTracks() {
    Editor editor = getActivity().getSharedPreferences().edit();
    editor.putLong(getActivity().getString(R.string.selected_track_key), -1);
    editor.putLong(getActivity().getString(R.string.recording_track_key), -1);
    
    editor.clear();
    editor.commit();
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
    while (getActivity().isRecording() != isRecording) {
      if (System.nanoTime() - startTime > timeout * 1000000) {
        throw new TimeoutException("Timeout while waiting for recording!");
      }
      Thread.sleep(20);
    }
    waitForIdle();
    assertEquals(isRecording, getActivity().isRecording());
    return getActivity().getRecordingTrackId();
  }
}
