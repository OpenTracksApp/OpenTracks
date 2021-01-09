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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;

import de.dennisguse.opentracks.services.handlers.AdaptiveLocationListenerPolicy;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link AdaptiveLocationListenerPolicy}.
 *
 * @author youtaol
 */
@RunWith(JUnit4.class)
public class AdaptiveLocationListenerPolicyTest {

    private static final Duration MIN = Duration.ofMillis(1000);
    private static final Duration MAX = Duration.ofMillis(3000);
    private static final int MIN_DISTANCE = 10;
    private static final Duration NEW_IDLE_TIME_BIG = Duration.ofMillis(10000);
    private static final Duration NEW_IDLE_TIME_NORMAL = Duration.ofMillis(5000);
    private static final Duration NEW_IDLE_TIME_SMALL = Duration.ofMillis(2000);
    private static final Duration NEW_IDLE_TIME_LESS_THAN_MIN = Duration.ofMillis(500);
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
        assertEquals(MAX, adaptiveLocationListenerPolicy.getDesiredPollingInterval());

        adaptiveLocationListenerPolicy.updateIdleTime(NEW_IDLE_TIME_NORMAL);
        // First get the half of NEW_IDLE_TIME_NORMAL, and then round it to the nearest second.
        assertEquals(NEW_IDLE_TIME_NORMAL.dividedBy(2).getSeconds(), adaptiveLocationListenerPolicy.getDesiredPollingInterval().getSeconds());

        adaptiveLocationListenerPolicy.updateIdleTime(NEW_IDLE_TIME_SMALL);
        assertEquals(MIN, adaptiveLocationListenerPolicy.getDesiredPollingInterval());

        adaptiveLocationListenerPolicy.updateIdleTime(NEW_IDLE_TIME_LESS_THAN_MIN);
        assertEquals(MIN, adaptiveLocationListenerPolicy.getDesiredPollingInterval());
    }

    /**
     * Tests the method {@link AdaptiveLocationListenerPolicy#getMinDistance_m()}.
     */
    @Test
    public void testGetMinDistance() {
        assertEquals(MIN_DISTANCE, adaptiveLocationListenerPolicy.getMinDistance_m());
    }
}
