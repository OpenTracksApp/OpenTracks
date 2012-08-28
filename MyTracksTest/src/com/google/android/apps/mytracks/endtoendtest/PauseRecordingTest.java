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

/**
 * Tests the pause and resume of recording.
 * 
 * @author Youtao Liu
 */
public class PauseRecordingTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;
  
  @TargetApi(15)
  public PauseRecordingTest() {
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
   * Tests the pause recording feature. Stops the recording after pause. 
   */
  public void testPauseRecording_stopAfterPause() {
    // Start recording
    EndToEndTestUtils.startRecording();
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_pause_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_stop_recording), false));
    EndToEndTestUtils.SOLO.goBack();
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_pause_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_stop_recording), false));
    
    // Pause
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_pause_track),
        true);
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_record_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_stop_recording), false));

    // Stop 
    EndToEndTestUtils.stopRecording(true);
  }

  /**
   * Tests the pause recording feature. Stops the recording after resume. 
   */
  public void testPauseRecording_stopAfterResume() {
    // Start recording
    EndToEndTestUtils.startRecording();
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_pause_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_stop_recording), false));
    EndToEndTestUtils.SOLO.goBack();
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_pause_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_stop_recording), false));
    
    // Pause
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_pause_track),
        true);
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_record_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_stop_recording), false));
    // Resume
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_record_track),
        false);
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_pause_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(activityMyTracks
        .getString(R.string.menu_stop_recording), false));
    // Stop 
    EndToEndTestUtils.stopRecording(true);
  }
  
}
