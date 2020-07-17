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

package de.dennisguse.opentracks.services.handlers;

/**
 * A {@link LocationListenerPolicy} that will change based on how long the user has been stationary.
 * This policy will dictate a policy based on a min, max and idle time.
 * The policy will dictate an interval bounded by min and max, and is half of the idle time.
 *
 * @author Sandor Dornbush
 */
public class AdaptiveLocationListenerPolicy implements LocationListenerPolicy {

    private final long minInterval_ms;
    private final long maxInterval_ms;
    private final int minDistance_m;

    // The time the user has been idle at the current location, in milliseconds.
    private long idleTime;

    /**
     * Creates a policy that will be bounded by the given minInterval_ms and maxInterval_ms.
     *
     * @param minInterval_ms the smallest interval this policy will dictate
     * @param maxInterval_ms the largest interval this policy will dictate
     * @param minDistance_m the minimum distance
     */
    public AdaptiveLocationListenerPolicy(long minInterval_ms, long maxInterval_ms, int minDistance_m) {
        this.minInterval_ms = minInterval_ms;
        this.maxInterval_ms = maxInterval_ms;
        this.minDistance_m = minDistance_m;
    }

    /*
     * Returns an interval half of the idle time, but bounded by minInterval_ms and maxInterval_ms.
     */
    @Override
    public long getDesiredPollingInterval() {
        long desiredInterval_ms = idleTime / 2;
        // Round to second to avoid setting the interval too often
        desiredInterval_ms = (desiredInterval_ms / 1000) * 1000;
        return Math.max(Math.min(maxInterval_ms, desiredInterval_ms), minInterval_ms);
    }

    @Override
    public int getMinDistance_m() {
        return minDistance_m;
    }

    @Override
    public void updateIdleTime(long newIdleTime) {
        idleTime = newIdleTime;
    }
}
