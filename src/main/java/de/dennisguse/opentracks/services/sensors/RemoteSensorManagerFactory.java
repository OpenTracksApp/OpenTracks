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
 * A factory of {@link BluetoothRemoteSensorManager}.
 *
 * @author Sandor Dornbush
 */
public class RemoteSensorManagerFactory {

    private static BluetoothRemoteSensorManager remoteSensorManagerSystem = null;


    private RemoteSensorManagerFactory() {
    }

    /**
     * Gets the system sensor manager.
     *
     * @param context the context
     */
    public static BluetoothRemoteSensorManager getSystemSensorManager(Context context) {
        releaseSystemSensorManager();
        remoteSensorManagerSystem = getSensorManager(context);
        remoteSensorManagerSystem.start();
        return remoteSensorManagerSystem;
    }

    /**
     * Releases the system sensor manager.
     */
    public static void releaseSystemSensorManager() {
        if (remoteSensorManagerSystem != null) {
            remoteSensorManagerSystem.stop();
        }
        remoteSensorManagerSystem = null;
    }

    /**
     * Gets the sensor manager.
     *
     * @param context the context
     */
    private static BluetoothRemoteSensorManager getSensorManager(Context context) {
        return new BluetoothRemoteSensorManager(context);
    }
}
