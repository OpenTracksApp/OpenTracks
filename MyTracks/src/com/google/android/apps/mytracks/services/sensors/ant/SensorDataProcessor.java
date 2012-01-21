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
 * Processes an ANT+ sensor data (counter) + timestamp pair,
 * and returns the instantaneous value of the sensor
 *
 * @author Laszlo Molnar
 */
public class SensorDataProcessor {

  /**
   * HistoryElement stores a time stamped sensor counter
   */
  private static class HistoryElement {
    final long systemTime;
    final int data;
    final int sensorTime;

    HistoryElement(long systemTime, int data, int sensorTime) {
      this.systemTime = systemTime;
      this.data = data;
      this.sensorTime = sensorTime;
    }
  }

  /**
   * Removes old data from the history.
   *
   * @param now the current system time
   * @return true if the remaining history is not empty
   */
  protected boolean removeOldHistory(long now) {
    HistoryElement h;
    while ((h = history.peek()) != null) {
      // if the first element of the list is in our desired time range then return
      if (now - h.systemTime <= HISTORY_LENGTH_MILLIS) {
        return true;
      }
      // otherwise remove the too old element, and look at the next (newer) one
      history.removeFirst();
    }
    return false;
  }

  /**
   * The latest counter value reported by the sensor
   */
  private int counter = -1;

  /**
   * The calculated instantaneous sensor value to be displayed
   */
  private int displayedValue = 0;

  private static final int ONE_SECOND_MILLIS = 1000;
  private static final int HISTORY_LENGTH_MILLIS = ONE_SECOND_MILLIS * 5;
  private static final int ONE_MINUTE_MILLIS = ONE_SECOND_MILLIS * 60;
  private static final int SENSOR_TIME_RESOLUTION = 1024; // in a second
  private static final int SENSOR_TIME_ONE_MINUTE = SENSOR_TIME_RESOLUTION * 60;

  /**
   * History of previous sensor data - oldest first
   * only the latest HISTORY_LENGTH_MILLIS milliseconds of data is stored
   */
  private LinkedList<HistoryElement> history = new LinkedList<HistoryElement>();

  /**
   * Calculates the instantaneous sensor value to be displayed
   * using the history when the sensor only resends the old data
   */
  private int getValueFromHistory(int data, long now) {
    if (!removeOldHistory(now)) {
      // there is nothing in the history, return 0
      return displayedValue = 0;
    }
    HistoryElement f = history.getFirst();
    HistoryElement l = history.getLast();
    int sensorTimeChange = (l.sensorTime - f.sensorTime) & 0xFFFF;
    int counterChange = (data - f.data) & 0xFFFF;

    // difference between now and systemTime of the oldest history entry
    // for better precision sensor timestamps are considered between
    // the first and the last history entry (could be overkill)
    int systemTimeChange = (int) (now - l.systemTime
      + (sensorTimeChange * ONE_SECOND_MILLIS) / SENSOR_TIME_RESOLUTION);

    // displayedValue is not overwritten by this calculated value
    // because it is still needed when a new sensor event arrives
    int v = (counterChange * ONE_MINUTE_MILLIS) / systemTimeChange;
    Log.d(TAG, "getValue returns (2):" + v);

    // do not return larger number than displayedValue
    return v < displayedValue ? v : displayedValue;
  }

  /**
   * Calculates the instantaneous sensor value to be displayed
   *
   * @param data sensor reported counter value
   * @param sensorTime sensor reported timestamp
   * @return the calculated value
   */
  public int getValue(int data, int sensorTime) {
    long now = System.currentTimeMillis();
    int counterChange = (data - counter) & 0xFFFF;

    Log.d(TAG, "now=" + now + " data=" + data + " sensortime=" + sensorTime);

    if (counter < 0) {
      // store the initial counter value reported by the sensor
      // the timestamp is probably out of date, so the history is not updated
      counter = data;
      return displayedValue = 0;
    }
    counter = data;

    if (counterChange != 0) {
      // if new data is arrived from the sensor ...
      if (removeOldHistory(now)) {
        // ... and the history is not empty, then use the latest entry
        HistoryElement h = history.getLast();
        int sensorTimeChange = (sensorTime - h.sensorTime) & 0xFFFF;
        counterChange = (data - h.data) & 0xFFFF;
        displayedValue = counterChange * SENSOR_TIME_ONE_MINUTE / sensorTimeChange;
      }
      history.addLast(new HistoryElement(now, data, sensorTime));
    } else if (!history.isEmpty()) {
      // the sensor has resent an old (counter,timestamp) pair,
      // but the history is not empty

      HistoryElement h = history.getLast();
      if (ONE_MINUTE_MILLIS < (now - h.systemTime) * displayedValue) {
        // Too much time has passed since the last counter change.
        // This means that a smaller value than displayedValue must be
        // returned. So the history is looked up, and a calculated
        // average number is returned.
        // Note, that displayedValue is NOT updated unless the history
        // is empty or contains outdated entries. In that case it is zeroed.
        return getValueFromHistory(data, now);
      } // else the current displayedValue is still valid, nothing to do here
    } else {
      // no new data from the sensor & the history is empty -> return 0
      displayedValue = 0;
    }

    Log.d(TAG, "getValue returns:" + displayedValue);
    return displayedValue;
  }
}
