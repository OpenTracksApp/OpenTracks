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

package com.google.android.apps.mytracks.services.tasks;

import android.test.AndroidTestCase;

/**
 * Tests for {@link AnnouncementPeriodicTaskFactory}.
 * These tests require Donut+ to run.
 *
 * @author Rodrigo Damazio
 */
public class AnnouncementPeriodicTaskFactoryTest extends AndroidTestCase {
  public void testCreate() {
    PeriodicTaskFactory factory = new AnnouncementPeriodicTaskFactory();
    PeriodicTask task = factory.create(getContext());
    assertTrue(task instanceof AnnouncementPeriodicTask);
  }
}
