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
import android.location.LocationManager;

import de.dennisguse.opentracks.content.sensor.SensorDataSet;

/**
 * This class extends the standard Android location with extra information.
 *
 * @author Sandor Dornbush
 */
//TODO Check if we can drop inheritance from Location and use attributes instead.
public class TrackPoint extends Location {

    public static TrackPoint createPause() {
        TrackPoint pause = new TrackPoint(LocationManager.GPS_PROVIDER);
        pause.setLongitude(0);
        pause.setLatitude(TrackPointsColumns.PAUSE_LATITUDE);
        pause.setTime(System.currentTimeMillis());
        return pause;
    }

    public static TrackPoint createResume() {
        TrackPoint resume = new TrackPoint(LocationManager.GPS_PROVIDER);
        resume.setLongitude(0);
        resume.setLatitude(TrackPointsColumns.RESUME_LATITUDE);
        resume.setTime(System.currentTimeMillis());
        return resume;
    }

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
