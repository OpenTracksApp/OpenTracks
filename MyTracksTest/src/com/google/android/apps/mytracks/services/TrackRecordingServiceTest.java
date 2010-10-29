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
package com.google.android.apps.mytracks.services;

import android.content.Intent;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Tests for the MyTracks track recording service.
 * 
 * @author Bartlomiej Niechwiej
 */
public class TrackRecordingServiceTest
    extends ServiceTestCase<TrackRecordingService> {
	
  public TrackRecordingServiceTest() {
    super(TrackRecordingService.class);
  }
  
  @SmallTest
  public void testStartable() {
    Intent startIntent = new Intent();
    startIntent.setClass(getContext(), TrackRecordingService.class);
    startService(startIntent);
    assertNotNull(getService());
  }

  @MediumTest
  public void testBindable() {
    Intent startIntent = new Intent();
    startIntent.setClass(getContext(), TrackRecordingService.class);
    IBinder service = bindService(startIntent);
    assertNotNull(service);
  }
}
