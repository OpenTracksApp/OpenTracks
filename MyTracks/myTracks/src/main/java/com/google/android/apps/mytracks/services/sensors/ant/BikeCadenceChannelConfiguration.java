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

import com.google.android.maps.mytracks.R;

/**
 * Bike cadence sensor channel configuration.
 *
 * @author Jimmy Shih
 */
public class BikeCadenceChannelConfiguration extends ChannelConfiguration {

  private static final int DEVICE_ID_KEY = R.string.ant_bike_cadence_sensor_id_key;
  private static final byte DEVICE_TYPE = 0x7A;
  private static final short MESSAGE_PERIOD = 8102;

  private CadenceCounter cadenceCounter = new CadenceCounter();

  @Override
  public int getDeviceIdKey() {
    return DEVICE_ID_KEY;
  }

  @Override
  public byte getDeviceType() {
    return DEVICE_TYPE;
  }

  @Override
  public short getMessagPeriod() {
    return MESSAGE_PERIOD;
  }

  @Override
  public void decodeMessage(byte[] message, AntSensorValue antSensorValue) {
    int revolutionCount = ((int) message[9] & 0xFF) + ((int) message[10] & 0xFF) * 256;
    int eventTime = ((int) message[7] & 0xFF) + ((int) message[8] & 0xFF) * 256;
    antSensorValue.setCadence(cadenceCounter.getEventsPerMinute(revolutionCount, eventTime));
  }
}
