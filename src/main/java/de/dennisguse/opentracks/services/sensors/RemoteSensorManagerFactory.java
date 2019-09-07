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

package de.dennisguse.opentracks.services.sensors;

import android.content.Context;

/**
 * A factory of {@link RemoteSensorManager}.
 *
 * @author Sandor Dornbush
 */
public class RemoteSensorManagerFactory {

    private static RemoteSensorManager remoteSensorManagerSystem = null;

    //TODO Check if still needed? Is there a missing features?
    private static RemoteSensorManager remoteSensorManagerTemporary = null;

    private RemoteSensorManagerFactory() {
    }

    /**
     * Gets the system sensor manager.
     *
     * @param context the context
     */
    public static RemoteSensorManager getSystemSensorManager(Context context) {
        releaseSensorManagerTemporary();
        releaseSystemSensorManager();
        remoteSensorManagerSystem = getSensorManager(context);
        remoteSensorManagerSystem.startSensor();
        return remoteSensorManagerSystem;
    }

    /**
     * Releases the system sensor manager.
     */
    public static void releaseSystemSensorManager() {
        if (remoteSensorManagerSystem != null) {
            remoteSensorManagerSystem.stopSensor();
        }
        remoteSensorManagerSystem = null;
    }

    /**
     * Gets the temp sensor manager.
     */
    public static RemoteSensorManager getSensorManagerTemporary(Context context) {
        releaseSensorManagerTemporary();
        if (remoteSensorManagerSystem != null) {
            return null;
        }
        remoteSensorManagerTemporary = getSensorManager(context);
        remoteSensorManagerTemporary.startSensor();
        return remoteSensorManagerTemporary;
    }

    /**
     * Releases the temp sensor manager.
     */
    private static void releaseSensorManagerTemporary() {
        if (remoteSensorManagerTemporary != null) {
            remoteSensorManagerTemporary.stopSensor();
        }
        remoteSensorManagerTemporary = null;
    }

    /**
     * Gets the sensor manager.
     *
     * @param context the context
     */
    private static RemoteSensorManager getSensorManager(Context context) {
        return new BluetoothRemoteSensorManager(context);
    }
}
