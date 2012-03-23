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
public class SensorEventCounter {

  /**
   * HistoryElement stores a time stamped sensor counter
   */
  private static class HistoryElement {
    final long systemTime;
    final int counter;
    final int sensorTime;

    HistoryElement(long systemTime, int counter, int sensorTime) {
      this.systemTime = systemTime;
      this.counter = counter;
      this.sensorTime = sensorTime;
    }
  }

  /**
   * The latest counter value reported by the sensor
   */
  private int counter;

  /**
   * The calculated instantaneous sensor value to be displayed
   */
  private int eventsPerMinute;

  private static final int ONE_SECOND_MILLIS = 1000;
  private static final int HISTORY_LENGTH_MILLIS = ONE_SECOND_MILLIS * 5;
  private static final int HISTORY_MAX_LENGTH = 100;
  private static final int ONE_MINUTE_MILLIS = ONE_SECOND_MILLIS * 60;
  private static final int SENSOR_TIME_RESOLUTION = 1024; // in a second
  private static final int SENSOR_TIME_ONE_MINUTE = SENSOR_TIME_RESOLUTION * 60;

  /**
   * History of previous sensor data - oldest first
   * only the latest HISTORY_LENGTH_MILLIS milliseconds of data is stored
   */
  private LinkedList<HistoryElement> history;

  SensorEventCounter() {
    counter = -1;
    eventsPerMinute = 0;
    history = new LinkedList<HistoryElement>();
  }

  /**
   * Calculates the instantaneous sensor value to be displayed
   *
   * @param newCounter sensor reported counter value
   * @param sensorTime sensor reported timestamp
   * @return the calculated value
   */
  public int getEventsPerMinute(int newCounter, int sensorTime) {
    return getEventsPerMinute(newCounter, sensorTime, System.currentTimeMillis());
  }

  // VisibleForTesting
  protected int getEventsPerMinute(int newCounter, int sensorTime, long now) { 
    int counterChange = (newCounter - counter) & 0xFFFF;

    Log.d(TAG, "now=" + now + " counter=" + newCounter + " sensortime=" + sensorTime);

    if (counter < 0) {
      // store the initial counter value reported by the sensor
      // the timestamp is probably out of date, so the history is not updated
      counter = newCounter;
      return eventsPerMinute = 0;
    }
    counter = newCounter;

    if (counterChange != 0) {
      // if new data has arrived from the sensor ...
      if (removeOldHistory(now)) {
        // ... and the history is not empty, then use the latest entry
        HistoryElement h = history.getLast();
        int sensorTimeChange = (sensorTime - h.sensorTime) & 0xFFFF;
        counterChange = (counter - h.counter) & 0xFFFF;
        eventsPerMinute = counterChange * SENSOR_TIME_ONE_MINUTE / sensorTimeChange;
      }
      // the previous removeOldHistory() call makes the length of the history capped
      history.addLast(new HistoryElement(now, counter, sensorTime));
    } else if (!history.isEmpty()) {
      // the sensor has resent an old (counter,timestamp) pair,
      // but the history is not empty

      HistoryElement h = history.getLast();
      if (ONE_MINUTE_MILLIS < (now - h.systemTime) * eventsPerMinute) {
        // Too much time has passed since the last counter change.
        // This means that a smaller value than eventsPerMinute must be
        // returned. This value is extrapolated from the history of
        // HISTORY_LENGTH_MILLIS data.
        // Note, that eventsPerMinute is NOT updated unless the history
        // is empty or contains outdated entries. In that case it is zeroed.
        return getValueFromHistory(now);
      } // else the current eventsPerMinute is still valid, nothing to do here
    } else {
      // no new data from the sensor & the history is empty -> return 0
      eventsPerMinute = 0;
    }

    Log.d(TAG, "getEventsPerMinute returns:" + eventsPerMinute);
    return eventsPerMinute;
  }

  /**
   * Calculates the instantaneous sensor value to be displayed
   * using the history when the sensor only resends the old data
   */
  private int getValueFromHistory(long now) {
    if (!removeOldHistory(now)) {
      // there is nothing in the history, return 0
      return eventsPerMinute = 0;
    }
    HistoryElement f = history.getFirst();
    HistoryElement l = history.getLast();
    int sensorTimeChange = (l.sensorTime - f.sensorTime) & 0xFFFF;
    int counterChange = (counter - f.counter) & 0xFFFF;

    // difference between now and systemTime of the oldest history entry
    // for better precision sensor timestamps are considered between
    // the first and the last history entry (could be overkill)
    int systemTimeChange = (int) (now - l.systemTime
      + (sensorTimeChange * ONE_SECOND_MILLIS) / SENSOR_TIME_RESOLUTION);

    // eventsPerMinute is not overwritten by this calculated value
    // because it is still needed when a new sensor event arrives
    int v = (counterChange * ONE_MINUTE_MILLIS) / systemTimeChange;
    Log.d(TAG, "getEventsPerMinute returns (2):" + v);

    // do not return larger number than eventsPerMinute, because the reason
    // this function got called is that more time has passed after the last
    // sensor counter value change than the current eventsPerMinute
    // would be valid
    return v < eventsPerMinute ? v : eventsPerMinute;
  }

  /**
   * Removes old data from the history.
   *
   * @param now the current system time
   * @return true if the remaining history is not empty
   */
  private boolean removeOldHistory(long now) {
    HistoryElement h;
    while ((h = history.peek()) != null) {
      // if the first element of the list is in our desired time range then return
      if (now - h.systemTime <= HISTORY_LENGTH_MILLIS && history.size() < HISTORY_MAX_LENGTH) {
        return true;
      }
      // otherwise remove the too old element, and look at the next (newer) one
      history.removeFirst();
    }
    return false;
  }
}
