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

package de.dennisguse.opentracks.fragments;

import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.chart.ChartView;
import de.dennisguse.opentracks.content.data.Distance;

/**
 * Tests {@link ChartFragment}.
 *
 * @author Youtao Liu
 */
//TODO Add tests that check ChartFragment
@RunWith(AndroidJUnit4.class)
public class ChartFragmentTest {

    private static final double HOURS_PER_UNIT = 60.0;

    private ChartFragment chartFragment;

    @BeforeClass
    public static void preSetUp() {
        // Prepare looper for Android's message queue
        if (Looper.myLooper() == null) Looper.prepare();
    }

    @AfterClass
    public static void finalTearDown() {
        if (Looper.myLooper() != null) Looper.myLooper().quit();
    }

    @Before
    public void setUp() {
        boolean chartByDistance = false;
        chartFragment = ChartFragment.newInstance(chartByDistance);
        chartFragment.setChartView(new ChartView(ApplicationProvider.getApplicationContext(), chartByDistance));
        chartFragment.setRecordingDistanceInterval(Distance.of(50));
    }

    @Test
    public void nothing() {
        //TODO
    }
}
