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
package com.google.android.apps.mytracks.services.sensors.ant;

import com.dsi.ant.AntMesg;
import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class AntDirectSensorManagerTest extends AndroidTestCase {

  private SharedPreferences sharedPreferences;
  private AntDirectSensorManager manager;

  public void setUp() {
    sharedPreferences = getContext().getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    // Let's use default values.
    sharedPreferences.edit().clear().apply();
    manager = new AntDirectSensorManager(getContext());
  }

  @SuppressWarnings("deprecation")
  @SmallTest
  public void testBroadcastData() {
    manager.setDeviceNumberHRM((short) 42);
    byte[] buff = new byte[11];
    buff[0] = 9;
    buff[1] = AntMesg.MESG_BROADCAST_DATA_ID;
    buff[2] = 0;  // HRM CHANNEL
    buff[10] = (byte) 220;
    manager.handleMessage(buff);

    Sensor.SensorDataSet sds = manager.getSensorDataSet();
    assertNotNull(sds);
    assertTrue(sds.hasHeartRate());
    assertEquals(Sensor.SensorState.SENDING,
        sds.getHeartRate().getState());
    assertEquals(220, sds.getHeartRate().getValue());

    assertFalse(sds.hasCadence());
    assertFalse(sds.hasPower());
  }

  @SuppressWarnings("deprecation")
  @SmallTest
  public void testChannelId() {
    byte[] buff = new byte[11];
    buff[0] = 9;
    buff[1] = AntMesg.MESG_CHANNEL_ID_ID;
    buff[3] = 42;
    manager.handleMessage(buff);

    assertEquals(42, manager.getDeviceNumberHRM());
    assertEquals(42,
        sharedPreferences.getInt(
            getContext().getString(R.string.ant_heart_rate_sensor_id_key), -1));
    assertNull(manager.getSensorDataSet());
  }

  @SuppressWarnings("deprecation")
  @SmallTest
  public void testResponseEvent() {
    assertEquals(Sensor.SensorState.NONE, manager.getSensorState());
    byte[] buff = new byte[5];
    buff[0] = 3;  // length
    buff[1] = AntMesg.MESG_RESPONSE_EVENT_ID;
    buff[2] = 0;  // channel
    buff[3] = AntMesg.MESG_UNASSIGN_CHANNEL_ID;
    buff[4] = 0;  // code
    manager.handleMessage(buff);

    assertEquals(Sensor.SensorState.DISCONNECTED, manager.getSensorState());
    assertNull(manager.getSensorDataSet());
  }

  // TODO: Test timeout too.
}
