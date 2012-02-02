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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.dsi.ant.AntDefine;
import com.dsi.ant.AntMesg;
import com.dsi.ant.exception.AntInterfaceException;
import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * A sensor manager to connect ANT+ sensors.
 *
 * @author Sandor Dornbush
 * @author Laszlo Molnar
 */
public class AntDirectSensorManager extends AntSensorManager
    implements AntSensorDataCollector {

  // allocating one channel for each sensor type
  private static final byte HEART_RATE_CHANNEL = 0;
  private static final byte CADENCE_CHANNEL = 1;
  private static final byte CADENCE_SPEED_CHANNEL = 2;

  // ids for device number preferences
  private static final int sensorIdKeys[] = {
    R.string.ant_heart_rate_sensor_id_key,
    R.string.ant_cadence_sensor_id_key,
    R.string.ant_cadence_speed_sensor_id_key,
  };

  private AntSensorBase sensors[] = null;

  // current data to be sent for SensorDataSet
  private static final byte HEART_RATE_DATA_INDEX = 0;
  private static final byte CADENCE_DATA_INDEX = 1;
  private int currentSensorData[] = { -1, -1 };

  private long lastDataSentMillis = 0;
  private byte connectingChannelsBitmap = 0;

  public AntDirectSensorManager(Context context) {
    super(context);
  }

  @Override
  protected boolean handleMessage(byte messageId, byte[] messageData) {
    if (super.handleMessage(messageId, messageData)) {
      return true;
    }

    int channel = messageData[0] & AntDefine.CHANNEL_NUMBER_MASK;
    if (sensors == null || channel >= sensors.length) {
      Log.d(TAG, "Unknown channel in message: " + channel);
      return false;
    }

    AntSensorBase sensor = sensors[channel];
    switch (messageId) {
      case AntMesg.MESG_BROADCAST_DATA_ID:
        if (sensor.getDeviceNumber() == WILDCARD) {
          resolveWildcardDeviceNumber((byte) channel);
        }
        sensor.handleBroadcastData(messageData, this);
        break;
      case AntMesg.MESG_RESPONSE_EVENT_ID:
        handleMessageResponse(messageData);
        break;
      case AntMesg.MESG_CHANNEL_ID_ID:
        sensor.setDeviceNumber(handleChannelId(messageData));
        break;
      default:
        Log.e(TAG, "Unexpected ANT message id: " + messageId);
    }

    return true;
  }

  private short handleChannelId(byte[] rawMessage) {
    AntChannelIdMessage message = new AntChannelIdMessage(rawMessage);
    short deviceNumber = message.getDeviceNumber();
    byte channel = message.getChannelNumber();
    if (channel >= sensors.length) {
      Log.d(TAG, "Unknown channel in message: " + channel);
      return WILDCARD;
    }
    Log.i(TAG, "Found ANT device id: " + deviceNumber + " on channel: " + channel);

    SharedPreferences prefs = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(context.getString(sensorIdKeys[channel]), deviceNumber);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
    return deviceNumber;
  }

  private void channelOut(byte channel)
  {
    if (channel >= sensors.length) {
      Log.d(TAG, "Unknown channel in message: " + channel);
      return;
    }
    connectingChannelsBitmap &= ~(1 << channel);
    Log.i(TAG, "ANT channel " + channel + " disconnected.");

    if (sensors[channel].getDeviceNumber() != WILDCARD) {
      Log.i(TAG, "Retrying....");
      if (setupChannel(sensors[channel], channel)) {
        connectingChannelsBitmap |= 1 << channel;
      }
    }
    if (connectingChannelsBitmap == 0) {
      setSensorState(Sensor.SensorState.DISCONNECTED);
    }
  }

  private void handleMessageResponse(byte[] rawMessage) {
    AntChannelResponseMessage message = new AntChannelResponseMessage(rawMessage);
    byte channel = message.getChannelNumber();
    switch (message.getMessageId()) {
      case AntMesg.MESG_EVENT_ID:
        if (message.getMessageCode() == AntDefine.EVENT_RX_SEARCH_TIMEOUT) {
          // Search timeout
          Log.w(TAG, "ANT search timed out. Unassigning channel " + channel);
          try {
            getAntReceiver().ANTUnassignChannel(channel);
          } catch (AntInterfaceException e) {
            Log.e(TAG, "ANT error unassigning channel", e);
            channelOut(channel);
          }
        }
        break;

      case AntMesg.MESG_UNASSIGN_CHANNEL_ID:
        channelOut(channel);
        break;
    }
  }

  private void resolveWildcardDeviceNumber(byte channel) {
    try {
      getAntReceiver().ANTRequestMessage(channel, AntMesg.MESG_CHANNEL_ID_ID);
    } catch (AntInterfaceException e) {
      Log.e(TAG, "ANT error handling broadcast data", e);
    }
    Log.d(TAG, "Requesting channel id id on channel: " + channel);
  }

  @Override
  protected void setupAntSensorChannels() {
    short devIds[] = new short[sensorIdKeys.length];

    SharedPreferences prefs = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    if (prefs != null) {
      for (int i = 0; i < sensorIdKeys.length; ++i) {
        devIds[i] = (short) prefs.getInt(context.getString(sensorIdKeys[i]), WILDCARD);
      }
    }

    sensors = new AntSensorBase[] {
        new HeartRateSensor(devIds[HEART_RATE_CHANNEL]),
        new CadenceSensor(devIds[CADENCE_CHANNEL]),
        new CadenceSpeedSensor(devIds[CADENCE_SPEED_CHANNEL]),
    };

    connectingChannelsBitmap = 0;
    for (int i = 0; i < sensors.length; ++i) {
      if (setupChannel(sensors[i], (byte) i)) {
        connectingChannelsBitmap |= 1 << i;
      }
    }
    if (connectingChannelsBitmap == 0) {
      setSensorState(Sensor.SensorState.DISCONNECTED);
    }
  }

  protected boolean setupChannel(AntSensorBase sensor, byte channel) {
    Log.i(TAG, "setup channel=" + channel + " deviceType=" + sensor.getDeviceType());

    return setupAntSensorChannel(sensor.getNetworkNumber(),
        channel,
        sensor.getDeviceNumber(),
        sensor.getDeviceType(),
        (byte) 0x01,
        sensor.getChannelPeriod(),
        sensor.getFrequency(),
        (byte) 0);
  }

  private void sendSensorData(byte index, int value) {
    if (index >= currentSensorData.length) {
      Log.w(TAG, "invalid index in sendSensorData:" + index);
      return;
    }
    currentSensorData[index] = value;

    long now = System.currentTimeMillis();
    // data comes in at ~4Hz rate from the sensors, so after >300 msec
    // fresh data is here from all the connected sensors
    if (now < lastDataSentMillis + 300) {
      return;
    }
    lastDataSentMillis = now;
    setSensorState(Sensor.SensorState.CONNECTED);

    Sensor.SensorDataSet.Builder b = Sensor.SensorDataSet.newBuilder();
    if (currentSensorData[HEART_RATE_DATA_INDEX] >= 0) {
      b.setHeartRate(
          Sensor.SensorData.newBuilder()
              .setValue(currentSensorData[HEART_RATE_DATA_INDEX])
              .setState(Sensor.SensorState.SENDING));
    }

    if (currentSensorData[CADENCE_DATA_INDEX] >= 0) {
      b.setCadence(
          Sensor.SensorData.newBuilder()
              .setValue(currentSensorData[CADENCE_DATA_INDEX])
              .setState(Sensor.SensorState.SENDING));
    }
    sensorData = b.setCreationTime(now).build();
  }

  public void setCadence(int value) {
    sendSensorData(CADENCE_DATA_INDEX, value);
  }

  public void setHeartRate(int value) {
    sendSensorData(HEART_RATE_DATA_INDEX, value);
  }
}
