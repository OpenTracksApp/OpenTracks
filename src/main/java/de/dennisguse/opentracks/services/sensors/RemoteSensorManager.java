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

package de.dennisguse.opentracks.services.sensors;

import de.dennisguse.opentracks.content.sensor.SensorDataSet;

/**
 * Manage the connection to a remote sensor.
 *
 * @author Sandor Dornbush
 */
public abstract class RemoteSensorManager {

    public static final long MAX_SENSOR_DATE_SET_AGE_MS = 5000;

    private static final String TAG = RemoteSensorManager.class.getSimpleName();

    /**
     * Returns true if the sensor is enabled.
     */
    public abstract boolean isEnabled();

    /**
     * Gets the sensor data set.
     */
    public abstract SensorDataSet getSensorDataSet();

    /**
     * Starts the sensor.
     */
    public abstract void startSensor();

    /**
     * Stops the sensor.
     */
    public abstract void stopSensor();

    /**
     * Returns true if the sensor data set is valid.
     */
    public boolean isSensorDataSetValid() {
        SensorDataSet sensorDataSet = getSensorDataSet();
        if (sensorDataSet == null) {
            return false;
        }
        return sensorDataSet.isRecent(MAX_SENSOR_DATE_SET_AGE_MS);
    }
}
