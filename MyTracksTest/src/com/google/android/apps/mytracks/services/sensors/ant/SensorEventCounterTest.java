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
package com.google.android.apps.mytracks.services.sensors.ant;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * @author Laszlo Molnar
 */
public class SensorEventCounterTest extends AndroidTestCase {

  @SmallTest
  public void testGetEventsPerMinute() {
    SensorEventCounter sec = new SensorEventCounter();
    assertEquals(0, sec.getEventsPerMinute(0, 0, 0));
    assertEquals(0, sec.getEventsPerMinute(1, 1024, 1000));
    assertEquals(60, sec.getEventsPerMinute(2, 1024 * 2, 2000));
    assertEquals(60, sec.getEventsPerMinute(2, 1024 * 2, 2500));
    assertTrue(60 > sec.getEventsPerMinute(2, 1024 * 2, 4000));
  }
}
