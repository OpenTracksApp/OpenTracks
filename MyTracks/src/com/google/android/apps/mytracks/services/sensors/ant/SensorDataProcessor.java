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
package com.google.android.apps.mytracks.services.sensors.ant;

import static com.google.android.apps.mytracks.Constants.TAG;
import android.util.Log;
import java.util.LinkedList;

/**
 * Processes an ANT sensor data (counter) + timestamp pair,
 * and returns the instantaneous value of the sensor
 *
 * @author Laszlo Molnar
 */
public class SensorDataProcessor {

  /**
   * History is used for looking back at the old data
   * when no new data is present, but an instantaneous value is needed
   */
  private class HistoryElement {
    public long sysTime;
    public int data;
    public int sensorTime;

    HistoryElement(long sys, int d, int sens) {
      sysTime = sys;
      data = d;
      sensorTime = sens;
    }
  };

  // Removes old data from the history.
  // Returns true if the remaining history is not empty.
  protected boolean removeOldHistory(long now) {
    HistoryElement h;
    while ((h = history.peek()) != null) {
      if (now - h.sysTime <= historyLengthMillis) {
        return true;
      }
      history.removeFirst();
    }
    return false;
  }

  private int counter = -1;  // the latest counter value reported by the sensor
  private int actValue = 0;
  public static final int historyLengthMillis = 5000; // 5 sec
  private LinkedList<HistoryElement> history = new LinkedList<HistoryElement>();

  public int getValue(int data, int sensorTime) {
    long now = System.currentTimeMillis();
    int dDelta = (data - counter) & 0xFFFF;

    Log.d(TAG, "now=" + now + " data=" + data + " sensortime=" + sensorTime);

    if (counter < 0) {
       // store the actual counter value from the sensor
      counter = data;
      return actValue = 0;
    }
    counter = data;

    if (dDelta != 0) {
      if (removeOldHistory(now)) {
        HistoryElement h = history.getLast();
        actValue = ((int) ((data - h.data) & 0xFFFF)) * 1024 * 60
                  / (int) ((sensorTime - h.sensorTime) & 0xFFFF);
      }
      history.addLast(new HistoryElement(now, data, sensorTime));
    } else if (!history.isEmpty()) {
      HistoryElement h = history.getLast();
      if (60000 < (now - h.sysTime) * actValue) {
        if (!removeOldHistory(now)) {
          actValue = 0;
        } else {
          HistoryElement f = history.getFirst();
          HistoryElement l = history.getLast();
          int sDelta = (l.sensorTime - f.sensorTime) & 0xFFFF;
          int cDelta = (data - f.data) & 0xFFFF;

          // the saved actValue is not overwritten by this
          // the returned value is computed from the history
          int v = (int) (cDelta * 60 * 1000 / (now - l.sysTime + (sDelta / 1024) * 1000));
          Log.d(TAG, "getValue returns (2):" + v);
          return v < actValue ? v : actValue;
        }
      } else {
        // the current actValue is still valid, nothing to do here
      }
    } else {
      actValue = 0;
    }

    Log.d(TAG, "getValue returns:" + actValue);
    return actValue;
  }
}
