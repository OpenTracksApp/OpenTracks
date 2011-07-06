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
import com.google.android.apps.mytracks.services.sensors.SensorUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * A sensor manager that can connect to an SRM Power Control 7 head unit.
 *
 * @author Sandor Dornbush
 */
public class AntSRMSensorManager extends AntSensorManager {

  /*
   * These constants are defined by the SRM Headunit spec.
   */
  public static final byte NETWORK_NUMBER = (byte) 0;
  public static final byte DEVICE_TYPE = (byte) 12;
  public static final byte MANUFACTURER_ID = (byte) 1;
  public static final short CHANNEL_PERIOD = (short) 8192;
  public static final byte RF_FREQUENCY = (byte) 50;

  public static final int MESSAGE_TYPE_INDEX = 3;
  public static final int INITIALS_MESSAGE_TYPE = 5;
  public static final int SENSOR_DATA_MESSAGE_TYPE = 6;

  public static final int MESSAGE_ID_INDEX = 4;

  /**
   * This can be any value between 0..8
   */
  private byte channel = (byte) 5;

  /**
   * This is the id of this device.
   * The head unit must be programmed to expect this number.
   */
  private byte deviceId;

  /**
   * The id of the last message received from the SRM head unit.
   */
  private int lastMessageId;

  public AntSRMSensorManager(Context context) {
    super(context);

    // First read the the device id that we will be announcing.
    SharedPreferences prefs = context.getSharedPreferences(
        Constants.SETTINGS_NAME, 0);
    if (prefs != null) {
      deviceId = (byte) prefs.getInt(
          context.getString(R.string.ant_srm_bridge_sensor_id_key),
          WILDCARD);
    }
  }

  @Override
  public void handleMessage(byte[] antMessage) {
    // Parse channel number
    byte recievedChannel = (byte) (antMessage[AntMesg.MESG_DATA_OFFSET]
                               & AntDefine.CHANNEL_NUMBER_MASK);
    if (recievedChannel != channel) {
      Log.d(TAG, "Unexpected channel: " + recievedChannel);
      return;
    }
    switch (antMessage[AntMesg.MESG_ID_OFFSET]) {
      case AntMesg.MESG_BROADCAST_DATA_ID:
        handleBroadcastData(antMessage);
        break;
      case AntMesg.MESG_RESPONSE_EVENT_ID:
        handleMessageResponse(antMessage);
        break;
      case AntMesg.MESG_CHANNEL_ID_ID:
        handleChannelId(antMessage);
        break;
      default:
        Log.e(TAG, "Unexpected message id: " + antMessage[3]);
    }
  }

  private void handleBroadcastData(byte[] antMessage) {
    if (deviceId == WILDCARD) {
      try {
        Log.d(TAG, "Requesting channel id id.");
        getAntReceiver().ANTRequestMessage(channel, AntMesg.MESG_CHANNEL_ID_ID);
      } catch (AntInterfaceException e) {
        Log.e(TAG, "Failed to request channel id id", e);
      }
    }
    setSensorState(Sensor.SensorState.CONNECTED);
    switch(antMessage[MESSAGE_TYPE_INDEX]) {
      case INITIALS_MESSAGE_TYPE:
        // TODO handle initials message.
        break;
      case SENSOR_DATA_MESSAGE_TYPE:
        parseSensorData(antMessage);
        break;
      default:
        Log.e(TAG, "Unexpected message type: " + antMessage[MESSAGE_TYPE_INDEX]);
    }
  }

  private void handleChannelId(byte[] antMessage) {
    // Store the device id.
    deviceId = antMessage[3];
    Log.i(TAG, "Found device id: " + deviceId);

    SharedPreferences prefs = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(context.getString(R.string.ant_srm_bridge_sensor_id_key), deviceId);
    editor.commit();
  }

  private void handleMessageResponse(byte[] antMessage) {
    if (antMessage[3] == AntMesg.MESG_EVENT_ID
        && antMessage[4] == AntDefine.EVENT_RX_SEARCH_TIMEOUT) {
      // Search timeout
      Log.w(TAG, "Search timed out. Unassigning channel.");
      try {
        getAntReceiver().ANTUnassignChannel(channel);
      } catch (AntInterfaceException e) {
        Log.e(TAG, "Failed to unassign ANT channel", e);
      }
      setSensorState(Sensor.SensorState.DISCONNECTED);
    } else if (antMessage[3] == AntMesg.MESG_UNASSIGN_CHANNEL_ID) {
      setSensorState(Sensor.SensorState.DISCONNECTED);
      Log.i(TAG, "Disconnected from the sensor: " + getSensorState());
    }
  }

  private void parseSensorData(byte[] antMessage) {
    if (antMessage.length != 11) {
      Log.e(TAG, "Unexpected ant message length: " + antMessage.length);
      return;
    }

    int newMessageId = antMessage[MESSAGE_ID_INDEX] & 0xFF;
    if (lastMessageId == newMessageId) {
      // Repeated message.
      Log.i(TAG, String.format("SRM ignoring repeat: 0x%X", newMessageId));
      return;
    }
    if (newMessageId < lastMessageId) {
      if (!(newMessageId < 20 && lastMessageId > 200)) {
        Log.i(TAG, String.format("SRM ignoring repeat: 0x%X", newMessageId));
        return;
      } // else assume the byte overflowed to 0.
    }
    lastMessageId = newMessageId;

    Sensor.SensorDataSet.Builder builder = Sensor.SensorDataSet.newBuilder()
      .setCreationTime(System.currentTimeMillis());

    int power = SensorUtils.unsignedShortToInt(antMessage, 5);
    if (power >= 0) {
      Sensor.SensorData.Builder b = Sensor.SensorData.newBuilder()
        .setValue(power)
        .setState(Sensor.SensorState.SENDING);
      builder.setPower(b);
    }

    int speed = SensorUtils.unsignedShortToInt(antMessage, 7);
    if (speed >= 0) {
      // The speed from srm is in 1/10 km/hr.
      // Sensor data expects the speed as m/s.
      // TODO finish this.
      //sensorData.setWheelSpeed(UnitConversions.TENTH_KMH_TO_MPS * speed);
    }

    int cadence = antMessage[9] & 0xFF;
    if (cadence >= 0) {
      Sensor.SensorData.Builder b = Sensor.SensorData.newBuilder()
        .setValue(cadence)
        .setState(Sensor.SensorState.SENDING);
      builder.setCadence(b);
    }

    int bpm = antMessage[10] & 0xFF;
    if (bpm > 0) {
      Sensor.SensorData.Builder b = Sensor.SensorData.newBuilder()
        .setValue(bpm)
        .setState(Sensor.SensorState.SENDING);
      builder.setHeartRate(b);
    }
    sensorData = builder.build();
  }

  @Override
  protected void setupAntSensorChannels() {
    lastMessageId = 0;
    setupAntSensorChannel(NETWORK_NUMBER,
        channel,
        deviceId,
        DEVICE_TYPE,
        MANUFACTURER_ID,
        CHANNEL_PERIOD,
        RF_FREQUENCY,
        (byte) 0);
  }
}
