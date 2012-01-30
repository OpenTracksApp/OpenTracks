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
  private AntSensorBase heartRateSensor;
  private static final byte HEART_RATE_CHANNEL = 0;

  private class MockAntDirectSensorManager extends AntDirectSensorManager {
    public MockAntDirectSensorManager(Context context) {
      super(context);
    }
    @Override
    protected boolean setupChannel(AntSensorBase sensor, byte channel) {
      if (channel == HEART_RATE_CHANNEL) {
        heartRateSensor = sensor;
        return true;
      }
      return false;
    }
  }
  private AntDirectSensorManager manager;

  public void setUp() {
    sharedPreferences = getContext().getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    // Let's use default values.
    sharedPreferences.edit().clear().apply();
    manager = new MockAntDirectSensorManager(getContext());
  }

  @SmallTest
  public void testBroadcastData() {
    manager.setupAntSensorChannels();
    assertNotNull(heartRateSensor);

    heartRateSensor.setDeviceNumber((short) 42);
    byte[] buff = new byte[9];
    buff[0] = HEART_RATE_CHANNEL;
    buff[8] = (byte) 220;
    manager.handleMessage(AntMesg.MESG_BROADCAST_DATA_ID, buff);

    Sensor.SensorDataSet sds = manager.getSensorDataSet();
    assertNotNull(sds);
    assertTrue(sds.hasHeartRate());
    assertEquals(Sensor.SensorState.SENDING,
        sds.getHeartRate().getState());
    assertEquals(220, sds.getHeartRate().getValue());

    assertFalse(sds.hasCadence());
    assertFalse(sds.hasPower());
    assertEquals(Sensor.SensorState.CONNECTED, manager.getSensorState());
  }

  @SmallTest
  public void testChannelId() {
    manager.setupAntSensorChannels();
    assertNotNull(heartRateSensor);

    byte[] buff = new byte[9];
    buff[1] = 43;
    manager.handleMessage(AntMesg.MESG_CHANNEL_ID_ID, buff);

    assertEquals(43, heartRateSensor.getDeviceNumber());
    assertEquals(43,
        sharedPreferences.getInt(
            getContext().getString(R.string.ant_heart_rate_sensor_id_key), -1));
    assertNull(manager.getSensorDataSet());
  }

  @SmallTest
  public void testResponseEvent() {
    manager.setupAntSensorChannels();
    assertNotNull(heartRateSensor);
    manager.setHeartRate(210);
    heartRateSensor.setDeviceNumber((short) 42);

    assertEquals(Sensor.SensorState.CONNECTED, manager.getSensorState());
    byte[] buff = new byte[3];
    buff[0] = HEART_RATE_CHANNEL;
    buff[1] = AntMesg.MESG_UNASSIGN_CHANNEL_ID;
    buff[2] = 0;  // code
    manager.handleMessage(AntMesg.MESG_RESPONSE_EVENT_ID, buff);
    assertEquals(Sensor.SensorState.CONNECTED, manager.getSensorState());

    heartRateSensor.setDeviceNumber((short) 0);
    manager.handleMessage(AntMesg.MESG_RESPONSE_EVENT_ID, buff);
    assertEquals(Sensor.SensorState.DISCONNECTED, manager.getSensorState());
  }

  // TODO: Test timeout too.
}
