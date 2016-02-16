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

import java.util.LinkedList;

/**
 * A counter that processes an Ant+ sensor data (count + event time) and returns
 * the instantaneous cadence value.
 *
 * @author Laszlo Molnar
 */
public class CadenceCounter {

  private static final int MILLIS_PER_MINUTE = 60000;
  private static final int MAX_HISTORY_TIME_IN_MILLIS = 5000; // 5 seconds
  private static final int MAX_HISTORY_SIZE = 100;
  private static final int EVENT_TIME_PER_MINUTE = 60 * 1024;

  /**
   * Cadence data.
   * 
   * @author Laszlo Molnar
   */
  private static class CadenceData {
    final long systemTime;
    final int count;
    final int eventTime;

    private CadenceData(long systemTime, int count, int eventTime) {
      this.systemTime = systemTime;
      this.count = count;
      this.eventTime = eventTime;
    }
  }

  // The last count
  private int lastCount;

  // The last calculated cadence
  private int eventsPerMinute;

  // The history of the previous sensor data, oldest first
  private LinkedList<CadenceData> history;

  public CadenceCounter() {
    lastCount = -1;
    eventsPerMinute = 0;
    history = new LinkedList<CadenceData>();
  }

  /**
   * Gets the cadence value.
   * 
   * @param count count
   * @param eventTime event time
   */
  public int getEventsPerMinute(int count, int eventTime) {
    long now = System.currentTimeMillis();
    int countChange = (count - lastCount) & 0xFFFF;

    if (lastCount < 0) {
      /*
       * eventTime for the initial count value is probably out of date, so not
       * updating the history.
       */
      lastCount = count;
      eventsPerMinute = 0;
      return 0;
    }

    lastCount = count;

    if (countChange != 0) {
      if (removeOldHistory(now)) {
        CadenceData lastCadenceData = history.getLast();
        int eventTimeChange = (eventTime - lastCadenceData.eventTime) & 0xFFFF;
        if (eventTimeChange != 0) {
          countChange = (count - lastCadenceData.count) & 0xFFFF;
          eventsPerMinute = countChange * EVENT_TIME_PER_MINUTE / eventTimeChange;
        }
      }
      history.addLast(new CadenceData(now, count, eventTime));
      return eventsPerMinute;
    } else {
      // The sensor has resent old data
      if (history.isEmpty()) {
        eventsPerMinute = 0;
        return 0;
      }

      CadenceData lastCadenceData = history.getLast();
      if ((now - lastCadenceData.systemTime) * eventsPerMinute < MILLIS_PER_MINUTE) {
        // The last eventsPerMinute is still valid
        return eventsPerMinute;
      }

      // Update the history
      if (!removeOldHistory(now)) {
        eventsPerMinute = 0;
        return 0;
      }

      /*
       * Too much time has passed since the last count change. A smaller value
       * than eventsPerMinute must be returned. The value is calculated from the
       * history.
       */
      return getValueFromHistory(now);
    }
  }

  /**
   * Gets the cadence value from the history when the sensor only resends the
   * old data.
   * 
   * @param now the current system time
   */
  private int getValueFromHistory(long now) {
    CadenceData firstCadenceData = history.getFirst();
    CadenceData lastCadenceData = history.getLast();
    int eventTimeChange = (lastCadenceData.eventTime - firstCadenceData.eventTime) & 0xFFFF;
    int countChange = (lastCount - firstCadenceData.count) & 0xFFFF;

    // (now - lastCadenceData) + (lastCadenceData - firstCadenceData)
    int systemTimeChange = (int) (now - lastCadenceData.systemTime
        + (eventTimeChange * MILLIS_PER_MINUTE) / EVENT_TIME_PER_MINUTE);

    /*
     * eventsPerMinute is not updated because it is still needed when a new
     * sensor event arrives.
     */
    if (systemTimeChange == 0) {
      return eventsPerMinute;
    }
    int value = (countChange * MILLIS_PER_MINUTE) / systemTimeChange;

    /*
     * Do not return a larger number than eventsPerMinute because this function
     * is only called when more time has passed after the last count change than
     * the interval from eventsPerMinute.
     */
    return value < eventsPerMinute ? value : eventsPerMinute;
  }

  /**
   * Removes old data from the history.
   * 
   * @param now the current system time
   * @return true if the remaining history is not empty.
   */
  private boolean removeOldHistory(long now) {
    CadenceData historyElement = history.peek();
    while (historyElement != null) {
      if (now - historyElement.systemTime <= MAX_HISTORY_TIME_IN_MILLIS
          && history.size() < MAX_HISTORY_SIZE) {
        return true;
      }
      history.removeFirst();
      historyElement = history.peek();
    }
    return false;
  }
}
