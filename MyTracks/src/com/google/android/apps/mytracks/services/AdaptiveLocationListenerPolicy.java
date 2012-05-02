/*
 * Copyright 2009 Google Inc.
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


/**
 * A LocationListenerPolicy that will change based on how long the user has been
 * stationary.
 *
 * This policy will dictate a policy based on a min, max and idle time.
 * The policy will dictate an interval bounded by min and max whic is half of
 * the idle time.
 *
 * @author Sandor Dornbush
 */
public class AdaptiveLocationListenerPolicy implements LocationListenerPolicy {

  /**
   * Smallest interval this policy will dictate, in milliseconds.
   */
  private final long minInterval;

  /**
   * Largest interval this policy will dictate, in milliseconds.
   */
  private final long maxInterval;

  private final int minDistance;
  
  /**
   * The time the user has been at the current location, in milliseconds.
   */
  private long idleTime;

  /**
   * Creates a policy that will be bounded by the given min and max.
   *
   * @param min Smallest interval this policy will dictate, in milliseconds
   * @param max Largest interval this policy will dictate, in milliseconds
   */
  public AdaptiveLocationListenerPolicy(long min, long max, int minDistance) {
    this.minInterval = min;
    this.maxInterval = max;
    this.minDistance = minDistance;
  }

  /**
   * @return An interval bounded by min and max which is half of the idle time
   */
  public long getDesiredPollingInterval() {
    long desiredInterval = idleTime / 2;
    // Round to avoid setting the interval too often.
    desiredInterval = (desiredInterval / 1000) * 1000;
    return Math.max(Math.min(maxInterval, desiredInterval),
                    minInterval);
  }

  public void updateIdleTime(long newIdleTime) {
    this.idleTime = newIdleTime;
  }

  /**
   * Returns the minimum distance between updates.
   */
  public int getMinDistance() {
    return minDistance;
  }
}
