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
 * This is an interface for classes that will manage the location listener policy.
 *
 * @author Sandor Dornbush
 */
interface LocationListenerPolicy {

    /**
     * Returns the polling interval this policy would like at this moment.
     *
     * @return the polling interval
     */
    Duration getDesiredPollingInterval();

    /**
     * Returns the minimum distance between updates.
     */
    int getMinDistance_m();

    /**
     * Notifies the amount of time the user has been idle at his current location.
     *
     * @param idleTime the time that the user has been idle at his current location
     */
    void updateIdleTime(Duration idleTime);
}
