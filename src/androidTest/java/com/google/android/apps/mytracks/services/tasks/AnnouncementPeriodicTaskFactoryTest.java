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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * Tests for {@link AnnouncementPeriodicTaskFactory}.
 * These tests require Donut+ to run.
 *
 * @author Rodrigo Damazio
 */
@RunWith(AndroidJUnit4.class)
public class AnnouncementPeriodicTaskFactoryTest {
    public void testCreate() {
        PeriodicTaskFactory factory = new AnnouncementPeriodicTaskFactory();
        PeriodicTask task = factory.create(InstrumentationRegistry.getInstrumentation().getContext());
        Assert.assertTrue(task instanceof AnnouncementPeriodicTask);
    }
}
