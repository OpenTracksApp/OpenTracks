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

import java.time.Duration;

/**
 * This is a simple location listener policy that will always dictate the same polling interval.
 *
 * @author Sandor Dornbush
 */
public class AbsoluteLocationListenerPolicy implements LocationListenerPolicy {

    private final Duration interval;

    /**
     * Constructor.
     *
     * @param interval the interval to request for gps signal
     */
    public AbsoluteLocationListenerPolicy(Duration interval) {
        this.interval = interval;
    }

    @Override
    public Duration getDesiredPollingInterval() {
        return interval;
    }

    @Override
    public int getMinDistance_m() {
        return 0;
    }

    @Override
    public void updateIdleTime(Duration idleTime) {
        // Ignore
    }
}
