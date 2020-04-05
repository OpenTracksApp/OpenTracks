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

package de.dennisguse.opentracks.services.tasks;

import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link AnnouncementPeriodicTaskFactory}.
 *
 * @author Rodrigo Damazio
 */
@RunWith(AndroidJUnit4.class)
public class AnnouncementPeriodicTaskFactoryTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @BeforeClass
    public static void preSetUp() {
        // Prepare looper for Android's message queue
        if (Looper.myLooper() == null) Looper.prepare();
    }

    @AfterClass
    public static void finalTearDown() {
        if (Looper.myLooper() != null) Looper.myLooper().quit();
    }

    @Test
    public void testCreate() {
        PeriodicTaskFactory factory = new AnnouncementPeriodicTaskFactory();
        PeriodicTask task = factory.create(context);
        Assert.assertTrue(task instanceof AnnouncementPeriodicTask);
    }
}
