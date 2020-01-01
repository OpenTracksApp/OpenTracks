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
package de.dennisguse.opentracks.content.data;

import android.location.Location;

import de.dennisguse.opentracks.content.sensor.SensorDataSet;

/**
 * This class extends the standard Android location with extra information.
 *
 * @author Sandor Dornbush
 */
public class TrackPoint extends Location {

    private SensorDataSet sensorDataSet = null;

    public TrackPoint(Location location, SensorDataSet sensorDataSet) {
        super(location);
        this.sensorDataSet = sensorDataSet;
    }

    public TrackPoint(String provider) {
        super(provider);
    }

    public SensorDataSet getSensorDataSet() {
        return sensorDataSet;
    }

    public void setSensorDataSet(SensorDataSet sensorDataSet) {
        this.sensorDataSet = sensorDataSet;
    }

    public void reset() {
        super.reset();
        sensorDataSet = null;
    }
}
