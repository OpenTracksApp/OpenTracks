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

import junit.framework.TestCase;

/**
 * Tests the {@link AdaptiveLocationListenerPolicy}.
 * 
 * @author youtaol
 */
public class AdaptiveLocationListenerPolicyTest extends TestCase {

  private AdaptiveLocationListenerPolicy adocationListenerPolicy;
  private static final long MIN = 1000;
  private static final long MAX = 3000;
  private static final int MIN_DISTANCE = 10;
  private static final long NEW_IDLE_TIME_BIG = 10000;
  private static final long NEW_IDLE_TIME_NORMAL = 5000;
  private static final long NEW_IDLE_TIME_SMALL = 2000;
  private static final long NEW_IDLE_TIME_LESS_THAN_MIN = 500;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    adocationListenerPolicy = new AdaptiveLocationListenerPolicy(MIN, MAX, MIN_DISTANCE);
  }

  /**
   * Tests the
   * {@link AdaptiveLocationListenerPolicy#getDesiredPollingInterval()} in four
   * situations.
   * <ul>
   * <li>The newIdleTime is bigger than max interval.</li>
   * <li>The newIdleTime is between min and max interval.</li>
   * <li>The newIdleTime is smaller than max interval.</li>
   * <li>The newIdleTime is smaller than the smallest interval unit.</li>
   * </ul>
   */
  public void testGetDesiredPollingInterval() {
    adocationListenerPolicy.updateIdleTime(NEW_IDLE_TIME_BIG);
    assertEquals(MAX, adocationListenerPolicy.getDesiredPollingInterval());

    adocationListenerPolicy.updateIdleTime(NEW_IDLE_TIME_NORMAL);
    // First get the half of NEW_IDLE_TIME_NORMAL, and then round it to the
    // nearest second.
    assertEquals((NEW_IDLE_TIME_NORMAL / 2 / 1000) * 1000,
        adocationListenerPolicy.getDesiredPollingInterval());

    adocationListenerPolicy.updateIdleTime(NEW_IDLE_TIME_SMALL);
    assertEquals(MIN, adocationListenerPolicy.getDesiredPollingInterval());

    adocationListenerPolicy.updateIdleTime(NEW_IDLE_TIME_LESS_THAN_MIN);
    assertEquals(MIN, adocationListenerPolicy.getDesiredPollingInterval());
  }

  /**
   * Tests the method {@link AdaptiveLocationListenerPolicy#getMinDistance()}.
   */
  public void testGetMinDistance() {
    assertEquals(MIN_DISTANCE, adocationListenerPolicy.getMinDistance());
  }
}
