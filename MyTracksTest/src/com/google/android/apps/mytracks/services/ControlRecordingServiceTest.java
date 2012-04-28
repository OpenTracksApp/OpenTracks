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
package com.google.android.apps.mytracks.services;

import com.google.android.maps.mytracks.R;
import com.google.android.testing.mocking.UsesMocks;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.test.ServiceTestCase;

import org.easymock.EasyMock;

/**
 * Tests {@link ControlRecordingService}.
 * 
 * @author Youtao Liu
 */
public class ControlRecordingServiceTest extends ServiceTestCase<ControlRecordingService> {

  private Context context;
  private ControlRecordingService controlRecordingService;

  public ControlRecordingServiceTest() {
    super(ControlRecordingService.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getContext();
  }

  /**
   * Tests the start of {@link ControlRecordingService} and tests the method
   * {@link ControlRecordingService#onHandleIntent(Intent, ITrackRecordingService)}
   * to start a track recording.
   */
  @UsesMocks(ITrackRecordingService.class)
  public void testStartRecording() {
    Intent intent = new Intent(context, ControlRecordingService.class);
    intent.setAction(context.getString(R.string.track_action_start));
    controlRecordingService = getService();
    assertNull(controlRecordingService);
    startService(intent);
    controlRecordingService = getService();
    assertNotNull(controlRecordingService);

    ITrackRecordingService iTrackRecordingServiceMock = EasyMock
        .createStrictMock(ITrackRecordingService.class);
    try {
      EasyMock.expect(iTrackRecordingServiceMock.startNewTrack()).andReturn(1L);
      EasyMock.replay(iTrackRecordingServiceMock);
      controlRecordingService.onHandleIntent(intent, iTrackRecordingServiceMock);
      EasyMock.verify(iTrackRecordingServiceMock);
    } catch (RemoteException e) {
      fail();
    }
  }

  /**
   * Tests the method
   * {@link ControlRecordingService#onHandleIntent(Intent, ITrackRecordingService)}
   * to stop a track recording.
   */
  @UsesMocks(ITrackRecordingService.class)
  public void testStopRecording() {
    Intent intent = new Intent(context, ControlRecordingService.class);
    intent.setAction(context.getString(R.string.track_action_end));
    startService(intent);
    controlRecordingService = getService();
    ITrackRecordingService iTrackRecordingServiceMock = EasyMock
        .createStrictMock(ITrackRecordingService.class);
    try {
      iTrackRecordingServiceMock.endCurrentTrack();
      EasyMock.replay(iTrackRecordingServiceMock);
      controlRecordingService.onHandleIntent(intent, iTrackRecordingServiceMock);
      EasyMock.verify(iTrackRecordingServiceMock);
    } catch (RemoteException e) {
      fail();
    }
  }

}
