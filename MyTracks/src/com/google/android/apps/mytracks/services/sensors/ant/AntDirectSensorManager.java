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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.maps.mytracks.R;

import com.dsi.ant.AntDefine;
import com.dsi.ant.AntMesg;
import com.dsi.ant.exception.AntInterfaceException;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * A sensor manager to connect ANT+ sensors.
 * This can include heart rate monitors.
 *
 * @author Sandor Dornbush
 */
public class AntDirectSensorManager extends AntSensorManager {

  /*
   * These constants are defined by the ANT+ heart rate monitor spec.
   */
  public static final byte HRM_CHANNEL = 0;
  public static final byte NETWORK_NUMBER = 1;
  public static final byte HEART_RATE_DEVICE_TYPE = 120;
  public static final byte POWER_DEVICE_TYPE = 11;
  public static final byte MANUFACTURER_ID = 1;
  public static final short CHANNEL_PERIOD = 8070;
  public static final byte RF_FREQUENCY = 57;

  private short deviceNumberHRM;

  public AntDirectSensorManager(Context context) {
    super(context);

    deviceNumberHRM = WILDCARD;

    // First read the the device id that we will be pairing with.
    SharedPreferences prefs = context.getSharedPreferences(
        Constants.SETTINGS_NAME, 0);
    if (prefs != null) {
      deviceNumberHRM =
        (short) prefs.getInt(context.getString(R.string.ant_heart_rate_sensor_id_key), 0);
    }
    Log.i(TAG, "Will pair with heart rate monitor: " + deviceNumberHRM);
  }

  @Override
  protected boolean handleMessage(byte messageId, byte[] messageData) {
    if (super.handleMessage(messageId, messageData)) {
      return true;
    }

    int channel = messageData[0] & AntDefine.CHANNEL_NUMBER_MASK;
    switch (channel) {
      case HRM_CHANNEL:
        antDecodeHRM(messageId, messageData);
        break;
      default:
        Log.d(TAG, "Unhandled message: " + channel);
    }

    return true;
  }

  /**
   * Decode an ant heart rate monitor message.
   * @param messageData The byte array received from the heart rate monitor.
   */
  private void antDecodeHRM(int messageId, byte[] messageData) {
    switch (messageId) {
      case AntMesg.MESG_BROADCAST_DATA_ID:
        handleBroadcastData(messageData);
        break;
      case AntMesg.MESG_RESPONSE_EVENT_ID:
        handleMessageResponse(messageData);
        break;
      case AntMesg.MESG_CHANNEL_ID_ID:
        handleChannelId(messageData);
        break;
      default:
        Log.e(TAG, "Unexpected message id: " + messageId);
    }
  }

  private void handleBroadcastData(byte[] antMessage) {
    if (deviceNumberHRM == WILDCARD) {
      try {
        getAntReceiver().ANTRequestMessage(HRM_CHANNEL,
            AntMesg.MESG_CHANNEL_ID_ID);
      } catch (AntInterfaceException e) {
        Log.e(TAG, "ANT error handling broadcast data", e);
      }
      Log.d(TAG, "Requesting channel id id.");
    }

    setSensorState(Sensor.SensorState.CONNECTED);
    int bpm = (int) antMessage[8] & 0xFF;
    Sensor.SensorData.Builder b = Sensor.SensorData.newBuilder()
      .setValue(bpm)
      .setState(Sensor.SensorState.SENDING);
    sensorData =
      Sensor.SensorDataSet.newBuilder()
      .setCreationTime(System.currentTimeMillis())
      .setHeartRate(b)
      .build();
  }

  void handleChannelId(byte[] rawMessage) {
    AntChannelIdMessage message = new AntChannelIdMessage(rawMessage);
    deviceNumberHRM = message.getDeviceNumber();
    Log.i(TAG, "Found device id: " + deviceNumberHRM);

    SharedPreferences prefs = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(context.getString(R.string.ant_heart_rate_sensor_id_key), deviceNumberHRM);
    editor.commit();
  }

  private void handleMessageResponse(byte[] rawMessage) {
    AntChannelResponseMessage message = new AntChannelResponseMessage(rawMessage);
    switch (message.getMessageId()) {
      case AntMesg.MESG_EVENT_ID:
        if (message.getMessageCode() == AntDefine.EVENT_RX_SEARCH_TIMEOUT) {
          // Search timeout
          Log.w(TAG, "Search timed out. Unassigning channel.");
          try {
            getAntReceiver().ANTUnassignChannel((byte) 0);
          } catch (AntInterfaceException e) {
            Log.e(TAG, "ANT error unassigning channel", e);
          }
          setSensorState(Sensor.SensorState.DISCONNECTED);
        }
        break;

      case AntMesg.MESG_UNASSIGN_CHANNEL_ID:
        setSensorState(Sensor.SensorState.DISCONNECTED);
        Log.i(TAG, "Disconnected from the sensor: " + getSensorState());
        break;
    }
  }

  @Override protected void setupAntSensorChannels() {
    setupAntSensorChannel(NETWORK_NUMBER,
        HRM_CHANNEL,
        deviceNumberHRM,
        HEART_RATE_DEVICE_TYPE,
        (byte) 0x01,
        CHANNEL_PERIOD,
        RF_FREQUENCY,
        (byte) 0);
  }

  public short getDeviceNumberHRM() {
    return deviceNumberHRM;
  }

  void setDeviceNumberHRM(short deviceNumberHRM) {
    this.deviceNumberHRM = deviceNumberHRM;
  }
}
