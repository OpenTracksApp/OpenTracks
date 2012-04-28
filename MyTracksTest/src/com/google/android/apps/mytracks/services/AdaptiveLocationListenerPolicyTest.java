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
  private long min = AdaptiveLocationListenerPolicy.SMALLEST_INTERVAL_UNIT;
  private long max = AdaptiveLocationListenerPolicy.SMALLEST_INTERVAL_UNIT * 3;
  private int minDistance = 10;
  private long newIdleTimeBig = AdaptiveLocationListenerPolicy.SMALLEST_INTERVAL_UNIT * 10;
  private long newIdleTimeNormal = AdaptiveLocationListenerPolicy.SMALLEST_INTERVAL_UNIT * 5;
  private long newIdleTimeSmall = AdaptiveLocationListenerPolicy.SMALLEST_INTERVAL_UNIT * 2;
  private long newIdleTimeLessThanMin = AdaptiveLocationListenerPolicy.SMALLEST_INTERVAL_UNIT / 2;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    adocationListenerPolicy = new AdaptiveLocationListenerPolicy(min, max, minDistance);
  }

  /**
   * Tests the
   * {@link AdaptiveLocationListenerPolicy#getDesiredPollingInterval()} in four situations.
   * <ul>
   * <li>The newIdleTime is bigger than max interval.</li>
   * <li>The newIdleTime is between min and max interval.</li>
   * <li>The newIdleTime is smaller than max interval.</li>
   * <li>The newIdleTime is smaller than the smallest interval unit.</li>
   * </ul> 
   */
  public void testGetDesiredPollingInterval() {
    adocationListenerPolicy.updateIdleTime(newIdleTimeBig);
    assertEquals(max, adocationListenerPolicy.getDesiredPollingInterval());

    adocationListenerPolicy.updateIdleTime(newIdleTimeNormal);
    assertEquals((newIdleTimeNormal / 2 / AdaptiveLocationListenerPolicy.SMALLEST_INTERVAL_UNIT)
        * AdaptiveLocationListenerPolicy.SMALLEST_INTERVAL_UNIT,
        adocationListenerPolicy.getDesiredPollingInterval());
    
    adocationListenerPolicy.updateIdleTime(newIdleTimeSmall);
    assertEquals(min, adocationListenerPolicy.getDesiredPollingInterval());

    adocationListenerPolicy.updateIdleTime(newIdleTimeLessThanMin);
    assertEquals(min, adocationListenerPolicy.getDesiredPollingInterval());
  }
  
  /**
   * Tests the method {@link AdaptiveLocationListenerPolicy#getMinDistance()}.
   */
  public void testGetMinDistance() {
    assertEquals(minDistance, adocationListenerPolicy.getMinDistance());
  }
}
