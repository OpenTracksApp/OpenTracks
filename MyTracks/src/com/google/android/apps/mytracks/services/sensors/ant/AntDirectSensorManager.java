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
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.LinkedList;

/**
 * A sensor manager to connect ANT+ sensors.
 * This can include heart rate monitors.
 *
 * @author Sandor Dornbush
 * @author Laszlo Molnar
 */
public class AntDirectSensorManager extends AntSensorManager {

  /*
   * These constants are defined by the ANT+ spec.
   */
  public static final byte NETWORK_NUMBER = 1;
  public static final byte RF_FREQUENCY = 57;

  public abstract class AntSensorBase {

    protected byte channel;
    protected short deviceNumber;
    private byte deviceType;
    private short channelPeriod;

    AntSensorBase(byte channel, short deviceNumber, byte deviceType,
              String deviceTypeString, short channelPeriod) {
      this.channel = channel;
      this.deviceNumber = deviceNumber;
      this.deviceType = deviceType;
      this.channelPeriod = channelPeriod;

      Log.i(TAG, "Will pair with " + deviceTypeString + " device: " + ((int) deviceNumber & 0xFFFF));
    }

    protected void resolveWildcardDeviceNumber() {
      if (!isPaired()) {
        try {
          getAntReceiver().ANTRequestMessage(channel, AntMesg.MESG_CHANNEL_ID_ID);
        } catch (AntInterfaceException e) {
          Log.e(TAG, "ANT error handling broadcast data", e);
        }
        Log.d(TAG, "Requesting channel id id on channel: " + channel);
      }
    }

    public abstract void handleBroadcastData(byte[] antMessage);

    public boolean setupChannel() {
      Log.i(TAG, "ant sensor setupChannel " + channel + " " + deviceType);

      return setupAntSensorChannel(NETWORK_NUMBER, channel, deviceNumber,
                                   deviceType, (byte) 0x01, channelPeriod,
                                   RF_FREQUENCY, (byte) 0);
    }

    public void setDeviceNumber(short dn) {
      deviceNumber = dn;
    }

    public boolean isPaired() {
      return deviceNumber != WILDCARD;
    }
  };

  // Heart reate monitor sensor
  public class HeartRateSensor extends AntSensorBase {
    /*
     * These constants are defined by the ANT+ heart rate monitor spec.
     */
    public static final byte HEART_RATE_DEVICE_TYPE = 120;
    public static final short HEART_RATE_CHANNEL_PERIOD = 8070;

    HeartRateSensor(byte channel, short devNum) {
      super(channel, devNum, HEART_RATE_DEVICE_TYPE,
            "heart rate monitor", HEART_RATE_CHANNEL_PERIOD);
    }

    /**
     * Decode an ANT+ heart rate monitor message.
     * @param antMessage The byte array received from the heart rate monitor.
     */
    public void handleBroadcastData(byte[] antMessage) {
      resolveWildcardDeviceNumber();

      int bpm = (int) antMessage[8] & 0xFF;
      Log.d(TAG, "now:" + System.currentTimeMillis() + " heart rate=" + bpm);
      sendHeartRate(bpm);
    }
  };

  /**
   * Processes an ANT sensor data + timestamp pair,
   * and returns the instantaneous value of the sensor
   */
  public class SensorDataProcessor {

    /**
     * History is used for looking back at the old data
     * when no new data is present, but an instantaneous value is needed
     */
    private class HistoryElement {
      public long sysTime;
      public int data;
      public int sensorTime;

      HistoryElement(long sys, int d, int sens) {
        sysTime = sys;
        data = d;
        sensorTime = sens;
      }
    };

    // Removes old data from the history.
    // Returns true if the remaining history is not empty.
    protected boolean removeOldHistory(long now) {
      HistoryElement h;
      while ((h = history.peek()) != null) {
        if (now - h.sysTime <= historyLengthMillis) {
          return true;
        }
        history.removeFirst();
      }
      return false;
    }

    private int counter = -1;  // the latest counter value reported by the sensor
    private int actValue = 0;
    public static final int historyLengthMillis = 5000; // 5 sec
    private LinkedList<HistoryElement> history = new LinkedList<HistoryElement>();

    public int getValue(int data, int sensorTime) {
      long now = System.currentTimeMillis();
      int dDelta = (data - counter) & 0xFFFF;

      Log.d(TAG, "now=" + now + " data=" + data + " sensortime=" + sensorTime);

      if (counter < 0) {
         // store the actual counter value from the sensor
        counter = data;
        return actValue = 0;
      }
      counter = data;

      if (dDelta != 0) {
        if (removeOldHistory(now)) {
          HistoryElement h = history.getLast();
          actValue = ((int) ((data - h.data) & 0xFFFF)) * 1024 * 60
                    / (int) ((sensorTime - h.sensorTime) & 0xFFFF);
        }
        history.addLast(new HistoryElement(now, data, sensorTime));
      } else if (!history.isEmpty()) {
        HistoryElement h = history.getLast();
        if (60000 < (now - h.sysTime) * actValue) {
          if (!removeOldHistory(now)) {
            actValue = 0;
          } else {
            HistoryElement f = history.getFirst();
            HistoryElement l = history.getLast();
            int sDelta = (l.sensorTime - f.sensorTime) & 0xFFFF;
            int cDelta = (data - f.data) & 0xFFFF;

            // the saved actValue is not overwritten by this
            // the returned value is computed from the history
            int v = (int) (cDelta * 60 * 1000 / (now - l.sysTime + (sDelta / 1024) * 1000));
            Log.d(TAG, "getValue returns (2):" + v);
            return v < actValue ? v : actValue;
          }
        } else {
          // the current actValue is still valid, nothing to do here
        }
      } else {
        actValue = 0;
      }

      Log.d(TAG, "getValue returns:" + actValue);
      return actValue;
    }
  }

  // Cadence sensor
  public class CadenseSensor extends AntSensorBase {
    /*
     * These constants are defined by the ANT+ bike speed and cadence sensor spec.
     */
    public static final byte CADENCE_DEVICE_TYPE = 122;
    public static final short CADENCE_CHANNEL_PERIOD = 8102;

    SensorDataProcessor cadence = new SensorDataProcessor();

    CadenseSensor(byte channel, short devNum) {
      super(channel, devNum, CADENCE_DEVICE_TYPE,
            "cadence sensor", CADENCE_CHANNEL_PERIOD);
    }

    /**
     * Decode an ANT+ cadence sensor message.
     * @param antMessage The byte array received from the cadence sensor.
     */
    public void handleBroadcastData(byte[] antMessage) {
      resolveWildcardDeviceNumber();

      int sensorTime = ((int) antMessage[5] & 0xFF) + ((int) antMessage[6] & 0xFF) * 256;
      int crankRevs = ((int) antMessage[7] & 0xFF) + ((int) antMessage[8] & 0xFF) * 256;
      sendCadence(cadence.getValue(crankRevs, sensorTime));
    }
  };

  // Combined cadence and speed sensor
  public class CadenceSpeedSensor extends AntSensorBase {
    /*
     * These constants are defined by the ANT+ bike speed and cadence sensor spec.
     */
    public static final byte CADENCE_SPEED_DEVICE_TYPE = 121;
    public static final short CADENCE_SPEED_CHANNEL_PERIOD = 8086;

    SensorDataProcessor cadence = new SensorDataProcessor();

    CadenceSpeedSensor(byte channel, short devNum) {
      super(channel, devNum, CADENCE_SPEED_DEVICE_TYPE,
            "speed&cadence sensor", CADENCE_SPEED_CHANNEL_PERIOD);
    }

    /**
     * Decode an ANT+ cadence&speed sensor message.
     * @param antMessage The byte array received from the sensor.
     */
    public void handleBroadcastData(byte[] antMessage) {
      resolveWildcardDeviceNumber();

      int sensorTime = ((int) antMessage[1] & 0xFF) + ((int) antMessage[2] & 0xFF) * 256;
      int crankRevs = ((int) antMessage[3] & 0xFF) + ((int) antMessage[4] & 0xFF) * 256;
      sendCadence(cadence.getValue(crankRevs, sensorTime));
    }
  };

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
        sensor.handleBroadcastData(messageData);
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

  short handleChannelId(byte[] rawMessage) {
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
    ApiFeatures.getInstance().getApiAdapter().applyPreferenceChanges(editor);
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

    if (sensors[channel].isPaired()) {
      Log.i(TAG, "Retrying....");
      if (sensors[channel].setupChannel()) {
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
        new HeartRateSensor(HEART_RATE_CHANNEL, devIds[HEART_RATE_CHANNEL]),
        new CadenseSensor(CADENCE_CHANNEL, devIds[CADENCE_CHANNEL]),
        new CadenceSpeedSensor(CADENCE_SPEED_CHANNEL, devIds[CADENCE_SPEED_CHANNEL]),
    };

    connectingChannelsBitmap = 0;
    for (int i = 0; i < sensors.length; ++i) {
      if (sensors[i].setupChannel()) {
        connectingChannelsBitmap |= 1 << i;
      }
    }
    if (connectingChannelsBitmap == 0) {
      setSensorState(Sensor.SensorState.DISCONNECTED);
    }
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

  public void sendCadence(int value) {
    sendSensorData(CADENCE_DATA_INDEX, value);
  }

  public void sendHeartRate(int value) {
    sendSensorData(HEART_RATE_DATA_INDEX, value);
  }
}
