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
package de.dennisguse.opentracks.services;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.dennisguse.opentracks.services.handlers.AdaptiveLocationListenerPolicy;

/**
 * Tests the {@link AdaptiveLocationListenerPolicy}.
 *
 * @author youtaol
 */
@RunWith(JUnit4.class)
public class AdaptiveLocationListenerPolicyTest {

    private static final long MIN = 1000;
    private static final long MAX = 3000;
    private static final int MIN_DISTANCE = 10;
    private static final long NEW_IDLE_TIME_BIG = 10000;
    private static final long NEW_IDLE_TIME_NORMAL = 5000;
    private static final long NEW_IDLE_TIME_SMALL = 2000;
    private static final long NEW_IDLE_TIME_LESS_THAN_MIN = 500;
    private AdaptiveLocationListenerPolicy adaptiveLocationListenerPolicy;

    @Before
    public void setUp() {
        adaptiveLocationListenerPolicy = new AdaptiveLocationListenerPolicy(MIN, MAX, MIN_DISTANCE);
    }

    /**
     * Tests the {@link AdaptiveLocationListenerPolicy#getDesiredPollingInterval()} in four situations.
     * <ul>
     * <li>The newIdleTime is bigger than max interval.</li>
     * <li>The newIdleTime is between min and max interval.</li>
     * <li>The newIdleTime is smaller than max interval.</li>
     * <li>The newIdleTime is smaller than the smallest interval unit.</li>
     * </ul>
     */
    @Test
    public void testGetDesiredPollingInterval() {
        adaptiveLocationListenerPolicy.updateIdleTime(NEW_IDLE_TIME_BIG);
        Assert.assertEquals(MAX, adaptiveLocationListenerPolicy.getDesiredPollingInterval());

        adaptiveLocationListenerPolicy.updateIdleTime(NEW_IDLE_TIME_NORMAL);
        // First get the half of NEW_IDLE_TIME_NORMAL, and then round it to the nearest second.
        Assert.assertEquals((NEW_IDLE_TIME_NORMAL / 2 / 1000) * 1000, adaptiveLocationListenerPolicy.getDesiredPollingInterval());

        adaptiveLocationListenerPolicy.updateIdleTime(NEW_IDLE_TIME_SMALL);
        Assert.assertEquals(MIN, adaptiveLocationListenerPolicy.getDesiredPollingInterval());

        adaptiveLocationListenerPolicy.updateIdleTime(NEW_IDLE_TIME_LESS_THAN_MIN);
        Assert.assertEquals(MIN, adaptiveLocationListenerPolicy.getDesiredPollingInterval());
    }

    /**
     * Tests the method {@link AdaptiveLocationListenerPolicy#getMinDistance_m()}.
     */
    @Test
    public void testGetMinDistance() {
        Assert.assertEquals(MIN_DISTANCE, adaptiveLocationListenerPolicy.getMinDistance_m());
    }
}
