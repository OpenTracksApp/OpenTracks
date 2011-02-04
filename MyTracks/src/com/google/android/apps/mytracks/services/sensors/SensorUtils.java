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
package com.google.android.apps.mytracks.services.sensors;

import android.content.Context;

import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.maps.mytracks.R;

/**
 * A collection of methods for message parsers.
 *
 * @author Sandor Dornbush
 */
public class SensorUtils {

  private SensorUtils() {
  }

  /**
   * Extract one unsigned short from a big endian byte array.
   * @param buffer the buffer to extract the short from
   * @param index the first byte to be interpreted as part of the short
   * @return The unsigned short at the given index in the buffer
   */
  public static int unsignedShortToInt(byte[] buffer, int index) {
    int r = (buffer[index] & 0xFF) << 8;
    r |= buffer[index + 1] & 0xFF;
    return r;
  }

  public static String getStateAsString(Sensor.SensorState state, Context c) {
    switch (state) {
      case NONE:
        return c.getString(R.string.none);
      case CONNECTING:
        return c.getString(R.string.connecting);
      case CONNECTED:
        return c.getString(R.string.connected);
      case DISCONNECTED:
        return c.getString(R.string.disconnected);
      case SENDING:
        return c.getString(R.string.sending);
      default:
        return "";
    }
  }
}
