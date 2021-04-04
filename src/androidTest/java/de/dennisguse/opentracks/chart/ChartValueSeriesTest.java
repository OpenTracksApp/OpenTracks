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
package de.dennisguse.opentracks.chart;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.R;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link ChartValueSeries}.
 *
 * @author Sandor Dornbush
 */
@RunWith(AndroidJUnit4.class)
public class ChartValueSeriesTest {
    private ChartValueSeries series;

    @Before
    public void setUp() {
        series = new ChartValueSeries(
                ApplicationProvider.getApplicationContext(),
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new int[]{100, 1000},
                R.string.description_altitude_metric,
                R.string.description_altitude_imperial,
                R.color.chart_altitude_fill,
                R.color.chart_altitude_border,
                15,
                18) {
            @Override
            double extractDataFromChartPoint(@NonNull ChartPoint chartPoint) {
                return chartPoint.getAltitude();
            }

            @Override
            protected boolean drawIfChartPointHasNoData() {
                return false;
            }
        };
    }

    @Test
    public void testInitialConditions() {
        assertEquals(1, series.getInterval());
        assertEquals(0, series.getMinMarkerValue());
        assertEquals(5, series.getMaxMarkerValue());
        assertTrue(series.isEnabled());
    }

    @Test
    public void testEnabled() {
        series.setEnabled(false);
        assertFalse(series.isEnabled());
    }

    @Test
    public void testSmallUpdates() {
        series.update(new ChartPoint(0));
        series.update(new ChartPoint(10));
        series.updateDimension();
        assertEquals(100, series.getInterval());
        assertEquals(0, series.getMinMarkerValue());
        assertEquals(500, series.getMaxMarkerValue());
    }

    @Test
    public void testBigUpdates() {
        series.update(new ChartPoint(0));
        series.update(new ChartPoint(901));
        series.updateDimension();
        assertEquals(1000, series.getInterval());
        assertEquals(0, series.getMinMarkerValue());
        assertEquals(5000, series.getMaxMarkerValue());
    }

    @Test
    public void testNotZeroBasedUpdates() {
        series.update(new ChartPoint(220));
        series.update(new ChartPoint(250));
        series.updateDimension();
        assertEquals(100, series.getInterval());
        assertEquals(200, series.getMinMarkerValue());
        assertEquals(700, series.getMaxMarkerValue());
    }
}
