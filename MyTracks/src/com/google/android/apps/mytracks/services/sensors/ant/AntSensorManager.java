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

import com.dsi.ant.AntDefine;
import com.dsi.ant.AntInterface;
import com.dsi.ant.AntInterfaceIntent;
import com.dsi.ant.AntMesg;
import com.dsi.ant.exception.AntInterfaceException;
import com.dsi.ant.exception.AntServiceNotConnectedException;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Sensor.SensorState;
import com.google.android.apps.mytracks.services.sensors.SensorManager;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;

/**
 * Ant Sensor Manager.
 * 
 * @author Jimmy Shih
 */
public class AntSensorManager extends SensorManager {

  /**
   * Channel States
   */
  public enum ChannelStates {
    CLOSED, // Channel is closed
    PENDING_OPEN, // User has requested opening the channel, waiting for a reset
    SEARCHING, // Channel is opened, but has not received data
    TRACKING_STATUS, // Channel is opened and has received status data recently
    TRACKING_DATA, // Channel is opened and has received measurement data
                   // recently
    OFFLINE // Channel is closed as the result of a search timeout
  }

  private static final String TAG = AntSensorManager.class.getSimpleName();

  public static final short WILDCARD = 0;

  private static final int CHANNELS = 2;
  private static final byte HRM_CHANNEL = (byte) 0;
  private static final byte SDM_CHANNEL = (byte) 1;

  private static final byte HRM_DEVICE_TYPE = 0x78;
  private static final byte SDM_DEVICE_TYPE = 0x7C;

  private static final short HRM_PERIOD = 8070;
  private static final short SDM_PERIOD = 8134;

  private static final byte DEFAULT_PROXIMITY_SEARCH_BIN = 7;

  private static final String RADIO_ANT = "ant";
  private static final byte ANT_NETWORK = (byte) 0x01;
  private static final int ANT_FREQUENCY = 57; // 2457Mhz (Ant+ frequency)
  private static final int ANT_TRANSMISSION_TYPE = 0; // 0 for wild card search

  private final Context context;
  private final ChannelConfiguration channelConfig[];
  private final IntentFilter statusIntentFilter;
  private final AntInterface antInterface;

  private boolean serviceConnected = false;
  private boolean hasClaimedInterface = false;
  private ChannelStates hrmState = ChannelStates.CLOSED;
  private ChannelStates sdmState = ChannelStates.CLOSED;
  private short hrmDeviceNumber = WILDCARD;
  private short sdmDeviceNumber = WILDCARD;

  private SensorDataSet sensorDataSet = null;
  private int lastHeartRate = -1;
  private int lastCadence = -1;
  private long lastSensorDataSetTime = 0;

  private boolean requestedReset = false;

  /**
   * Constructor.
   * 
   * @param context the context
   */
  public AntSensorManager(Context context) {
    this.context = context;

    channelConfig = new ChannelConfiguration[CHANNELS];
    channelConfig[HRM_CHANNEL] = new ChannelConfiguration();
    channelConfig[SDM_CHANNEL] = new ChannelConfiguration();

    statusIntentFilter = new IntentFilter();
    statusIntentFilter.addAction(AntInterfaceIntent.ANT_ENABLED_ACTION);
    statusIntentFilter.addAction(AntInterfaceIntent.ANT_ENABLING_ACTION);
    statusIntentFilter.addAction(AntInterfaceIntent.ANT_DISABLED_ACTION);
    statusIntentFilter.addAction(AntInterfaceIntent.ANT_DISABLING_ACTION);
    statusIntentFilter.addAction(AntInterfaceIntent.ANT_RESET_ACTION);
    statusIntentFilter.addAction(AntInterfaceIntent.ANT_INTERFACE_CLAIMED_ACTION);
    statusIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

    antInterface = new AntInterface();
  }

  @Override
  protected synchronized void setUpChannel() {
    tearDownChannel();
    if (AntInterface.hasAntSupport(context)) {
      context.registerReceiver(statusReceiver, statusIntentFilter);
      if (!antInterface.initService(context, serviceListener)) {
        AntInterface.goToMarket(context);
      } else {
        setSensorState(Sensor.SensorState.CONNECTING);
        handleServiceConnected();
      }
    }
  }

  /**
   * Handles service connected. Needs to be synchronized.
   */
  private synchronized void handleServiceConnected() {
    serviceConnected = antInterface.isServiceConnected();
    if (serviceConnected) {
      try {
        hasClaimedInterface = antInterface.hasClaimedInterface();
        if (hasClaimedInterface) {
          enableDataMessage(true);
        } else {
          // Need to claim the ant interface if it is available
          hasClaimedInterface = antInterface.claimInterface();
        }
      } catch (AntInterfaceException e) {
        handleAntError();
      }
    }
  }
  
  @Override
  protected void tearDownChannel() {
    try {
      context.unregisterReceiver(statusReceiver);
    } catch (IllegalArgumentException e) {
      // Can safely ignore
    }
    enableDataMessage(false);
    if (serviceConnected) {
      try {
        if (hasClaimedInterface) {
          antInterface.releaseInterface();
        }
        hasClaimedInterface = false;
        antInterface.stopRequestForceClaimInterface();
      } catch (AntServiceNotConnectedException e) {
        // Can safely ignore
      } catch (AntInterfaceException e) {
        Log.w(TAG, "Exception in tearDonwChannel.", e);
      }
      antInterface.releaseService();
      serviceConnected = false;
    }
  }

  @Override
  public SensorDataSet getSensorDataSet() {
    return sensorDataSet;
  }

  /**
   * Tries to claim the ant interface.
   */
  public void tryClaimAnt() {
    try {
      antInterface.requestForceClaimInterface(context.getString(R.string.my_tracks_app_name));
    } catch (AntInterfaceException e) {
      handleAntError();
    }
  }

  private AntInterface.ServiceListener serviceListener = new AntInterface.ServiceListener() {
      @Override
    public void onServiceConnected() {
      handleServiceConnected();
    }

      @Override
    public void onServiceDisconnected() {
      serviceConnected = false;
      if (hasClaimedInterface) {
        enableDataMessage(false);
      }
    }
  };

  /**
   * Configures ant radio.
   */
  private void configureAntRadio() {
    try {
      if (serviceConnected && hasClaimedInterface && antInterface.isEnabled()) {
        try {
          antInterface.ANTDisableEventBuffering();
        } catch (AntInterfaceException e) {
          Log.e(TAG, "Cannot disable event buffering.", e);
        }
      } else {
        Log.i(TAG, "Cannot disable event buffering now.");
      }
    } catch (AntInterfaceException e) {
      Log.e(TAG, "Unable to check enabled state.", e);
    }
  }

  /**
   * Handles ant error.
   */
  private void handleAntError() {
    clearAllChannels();
  }

  /**
   * Opens a channel.
   * 
   * @param channel the channel
   */
  private void openChannel(byte channel) {
    channelConfig[channel].deviceNumber = 0;
    channelConfig[channel].deviceType = 0;
    channelConfig[channel].TransmissionType = ANT_TRANSMISSION_TYPE;
    channelConfig[channel].period = 0;
    channelConfig[channel].freq = ANT_FREQUENCY;
    channelConfig[channel].proxSearch = DEFAULT_PROXIMITY_SEARCH_BIN;
    switch (channel) {
      case HRM_CHANNEL:
        hrmDeviceNumber = (short) PreferencesUtils.getInt(
            context, R.string.ant_heart_rate_monitor_id_key, WILDCARD);
        channelConfig[channel].deviceNumber = hrmDeviceNumber;
        channelConfig[channel].deviceType = HRM_DEVICE_TYPE;
        channelConfig[channel].period = HRM_PERIOD;
        hrmState = ChannelStates.PENDING_OPEN;
        break;
      case SDM_CHANNEL:
        sdmDeviceNumber = (short) PreferencesUtils.getInt(
            context, R.string.ant_speed_distance_monitor_id_key, WILDCARD);
        channelConfig[channel].deviceNumber = sdmDeviceNumber;
        channelConfig[channel].deviceType = SDM_DEVICE_TYPE;
        channelConfig[channel].period = SDM_PERIOD;
        sdmState = ChannelStates.PENDING_OPEN;
        break;
      default:
        break;
    }
    setupAntChannel(ANT_NETWORK, channel);
  }

  /**
   * Closes a channel.
   * 
   * @param channel the channel
   */
  private void closeChannel(byte channel) {
    channelConfig[channel].isInitializing = false;
    channelConfig[channel].isDeinitializing = true;
    switch (channel) {
      case HRM_CHANNEL:
        hrmState = ChannelStates.CLOSED;
        break;
      case SDM_CHANNEL:
        sdmState = ChannelStates.CLOSED;
        break;
      default:
        break;
    }
    setSensorState(SensorState.DISCONNECTED);
    try {
      antInterface.ANTCloseChannel(channel);
      // Note, unassign channel after getting channel closed event
    } catch (AntInterfaceException e) {
      Log.w(TAG, "Unable to close channel: " + channel, e);
      handleAntError();
    }
  }

  /**
   * Clears all channels.
   */
  private void clearAllChannels() {
    hrmState = ChannelStates.CLOSED;
    sdmState = ChannelStates.CLOSED;
    setSensorState(SensorState.DISCONNECTED);
  }

  /**
   * Requests reset.
   */
  public void requestReset() {
    try {
      requestedReset = true;
      antInterface.ANTResetSystem();
      configureAntRadio();
    } catch (AntInterfaceException e) {
      Log.e(TAG, "Unable to reset ant.", e);
      requestedReset = false;
    }
  }

  @Override
  public boolean isEnabled() {
    if (antInterface == null || !antInterface.isServiceConnected()) {
      return false;
    }
    try {
      return antInterface.isEnabled();
    } catch (AntInterfaceException e) {
      Log.w(TAG, "Unable to check enabled.", e);
      return false;
    }
  }

  private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
    public void onReceive(Context c, Intent intent) {
      String ANTAction = intent.getAction();
      if (ANTAction.equals(AntInterfaceIntent.ANT_DISABLED_ACTION)) {
        clearAllChannels();
      } else if (ANTAction.equals(AntInterfaceIntent.ANT_RESET_ACTION)) {
        if (!requestedReset) {
          // Someone else triggered an ant reset
          clearAllChannels();
        } else {
          requestedReset = false;
          configureAntRadio();
        }
      } else if (ANTAction.equals(AntInterfaceIntent.ANT_INTERFACE_CLAIMED_ACTION)) {
        boolean wasClaimed = hasClaimedInterface;

        // Could also read ANT_INTERFACE_CLAIMED_PID from intent and see if it
        // matches the current process PID.
        try {
          hasClaimedInterface = antInterface.hasClaimedInterface();
          if (hasClaimedInterface) {
            enableDataMessage(true);
          } else {
            if (wasClaimed) {
              // Claimed by another application
              enableDataMessage(false);
            }
          }
        } catch (AntInterfaceException e) {
          handleAntError();
        }
      } else if (ANTAction.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
        if (isAirPlaneMode()) {
          clearAllChannels();
        }
      }
    }
  };

  private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
      @Override
    public void onReceive(Context c, Intent intent) {
      if (intent.getAction().equals(AntInterfaceIntent.ANT_RX_MESSAGE_ACTION)) {
        byte[] ANTRxMessage = intent.getByteArrayExtra(AntInterfaceIntent.ANT_MESSAGE);
        switch (ANTRxMessage[AntMesg.MESG_ID_OFFSET]) {
          case AntMesg.MESG_BROADCAST_DATA_ID:
          case AntMesg.MESG_ACKNOWLEDGED_DATA_ID:
            // Switch on channel number
            switch (ANTRxMessage[AntMesg.MESG_DATA_OFFSET]) {
              case HRM_CHANNEL:
                decodeHrmMessage(ANTRxMessage);
                break;
              case SDM_CHANNEL:
                decodeSdmMessage(ANTRxMessage);
                break;
              default:
                break;
            }
            break;
          case AntMesg.MESG_RESPONSE_EVENT_ID:
            handleResponseEventMessage(ANTRxMessage);
            break;
          case AntMesg.MESG_CHANNEL_ID_ID:
            short deviceNum = (short) ((ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 1] & 0xFF
                | ((ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 2] & 0xFF) << 8)) & 0xFFFF);
            // Switch on channel number
            switch (ANTRxMessage[AntMesg.MESG_DATA_OFFSET]) {
              case HRM_CHANNEL:
                hrmDeviceNumber = deviceNum;
                PreferencesUtils.setInt(
                    context, R.string.ant_heart_rate_monitor_id_key, hrmDeviceNumber);
                Toast.makeText(context,
                    context.getString(R.string.settings_sensor_connected, deviceNum),
                    Toast.LENGTH_SHORT).show();
                setSensorState(SensorState.CONNECTED);
                break;
              case SDM_CHANNEL:
                sdmDeviceNumber = deviceNum;
                PreferencesUtils.setInt(
                    context, R.string.ant_speed_distance_monitor_id_key, sdmDeviceNumber);
                Toast.makeText(context,
                    context.getString(R.string.settings_sensor_connected, deviceNum),
                    Toast.LENGTH_SHORT).show();
                setSensorState(SensorState.CONNECTED);
                break;
              default:
                break;
            }
            break;
          default:
            break;
        }
      }
    }

    /**
     * Handles response event message.
     * 
     * @param message the message
     */
    private void handleResponseEventMessage(byte[] message) {
      // For a list of possible message codes see ANT Message Protocol and Usage
      // section 9.5.6.1 available from thisisant.com
      byte channelNumber = message[AntMesg.MESG_DATA_OFFSET];
      if ((message[AntMesg.MESG_DATA_OFFSET + 1] == AntMesg.MESG_EVENT_ID)
          && (message[AntMesg.MESG_DATA_OFFSET + 2] == AntDefine.EVENT_RX_SEARCH_TIMEOUT)) {
        // A channel timed out searching, unassign it
        channelConfig[channelNumber].isInitializing = false;
        channelConfig[channelNumber].isDeinitializing = false;
        switch (channelNumber) {
          case HRM_CHANNEL:
            try {
              hrmState = ChannelStates.OFFLINE;
              antInterface.ANTUnassignChannel(HRM_CHANNEL);
              setSensorState(SensorState.DISCONNECTED);
            } catch (AntInterfaceException e) {
              handleAntError();
            }
            break;
          case SDM_CHANNEL:
            try {
              sdmState = ChannelStates.OFFLINE;
              antInterface.ANTUnassignChannel(SDM_CHANNEL);
              setSensorState(SensorState.DISCONNECTED);
            } catch (AntInterfaceException e) {
              handleAntError();
            }
            break;
          default:
            break;
        }
      }

      if (channelConfig[channelNumber].isInitializing) {
        if (message[AntMesg.MESG_DATA_OFFSET + 2] != 0) {
          // Error response
          Log.e(TAG, String.format("Error code(%#02x) on message ID(%#02x) on channel %d",
              message[AntMesg.MESG_DATA_OFFSET + 2],
              message[AntMesg.MESG_DATA_OFFSET + 1], channelNumber));
        } else {
          // Switch on message id
          switch (message[AntMesg.MESG_DATA_OFFSET + 1]) {
            case AntMesg.MESG_ASSIGN_CHANNEL_ID:
              try {
                antInterface.ANTSetChannelId(channelNumber,
                    channelConfig[channelNumber].deviceNumber,
                    channelConfig[channelNumber].deviceType,
                    channelConfig[channelNumber].TransmissionType);
              } catch (AntInterfaceException e) {
                handleAntError();
              }
              break;
            case AntMesg.MESG_CHANNEL_ID_ID:
              try {
                antInterface.ANTSetChannelPeriod(
                    channelNumber, channelConfig[channelNumber].period);
              } catch (AntInterfaceException e) {
                handleAntError();
              }
              break;
            case AntMesg.MESG_CHANNEL_MESG_PERIOD_ID:
              try {
                antInterface.ANTSetChannelRFFreq(channelNumber, channelConfig[channelNumber].freq);
              } catch (AntInterfaceException e) {
                handleAntError();
              }
              break;
            case AntMesg.MESG_CHANNEL_RADIO_FREQ_ID:
              try {
                // Disable high priority search
                antInterface.ANTSetChannelSearchTimeout(channelNumber, (byte) 0);
              } catch (AntInterfaceException e) {
                handleAntError();
              }
              break;
            case AntMesg.MESG_CHANNEL_SEARCH_TIMEOUT_ID:
              try {
                // Set search timeout to 30 seconds (low priority search)
                antInterface.ANTSetLowPriorityChannelSearchTimeout(channelNumber, (byte) 12);
              } catch (AntInterfaceException e) {
                handleAntError();
              }
              break;
            case AntMesg.MESG_SET_LP_SEARCH_TIMEOUT_ID:
              if (channelConfig[channelNumber].deviceNumber == WILDCARD) {
                try {
                  // Configure proximity search, if using wild card search
                  antInterface.ANTSetProximitySearch(
                      channelNumber, channelConfig[channelNumber].proxSearch);
                } catch (AntInterfaceException e) {
                  handleAntError();
                }
              } else {
                try {
                  antInterface.ANTOpenChannel(channelNumber);
                } catch (AntInterfaceException e) {
                  handleAntError();
                }
              }
              break;
            case AntMesg.MESG_PROX_SEARCH_CONFIG_ID:
              try {
                antInterface.ANTOpenChannel(channelNumber);
              } catch (AntInterfaceException e) {
                handleAntError();
              }
              break;
            case AntMesg.MESG_OPEN_CHANNEL_ID:
              channelConfig[channelNumber].isInitializing = false;
              switch (channelNumber) {
                case HRM_CHANNEL:
                  hrmState = ChannelStates.SEARCHING;
                  break;
                case SDM_CHANNEL:
                  sdmState = ChannelStates.SEARCHING;
                  break;
                default:
                  break;
              }
              break;
            default:
              break;
          }
        }
      } else if (channelConfig[channelNumber].isDeinitializing) {
        if ((message[AntMesg.MESG_DATA_OFFSET + 1] == AntMesg.MESG_EVENT_ID)
            && (message[AntMesg.MESG_DATA_OFFSET + 2] == AntDefine.EVENT_CHANNEL_CLOSED)) {
          try {
            antInterface.ANTUnassignChannel(channelNumber);
          } catch (AntInterfaceException e) {
            handleAntError();
          }
        } else if ((message[AntMesg.MESG_DATA_OFFSET + 1] == AntMesg.MESG_UNASSIGN_CHANNEL_ID)
            && (message[AntMesg.MESG_DATA_OFFSET + 2] == AntDefine.RESPONSE_NO_ERROR)) {
          channelConfig[channelNumber].isDeinitializing = false;
        }
      }
    }

    /**
     * Decodes HRM message.
     * 
     * @param message the message
     */
    private void decodeHrmMessage(byte[] message) {
      if (hrmState != ChannelStates.CLOSED) {
        hrmState = ChannelStates.TRACKING_DATA;
      }

      if (hrmDeviceNumber == WILDCARD) {
        try {
          antInterface.ANTRequestMessage(HRM_CHANNEL, AntMesg.MESG_CHANNEL_ID_ID);
        } catch (AntInterfaceException e) {
          handleAntError();
        }
      }

      lastHeartRate = message[10] & 0xFF;
      setSenorDataSet();
    }

    /**
     * Decodes SDM message.
     * 
     * @param message the message
     */
    private void decodeSdmMessage(byte[] message) {
      if (sdmState != ChannelStates.CLOSED) {
        sdmState = ChannelStates.TRACKING_DATA;
      }

      if (sdmDeviceNumber == WILDCARD) {
        try {
          antInterface.ANTRequestMessage(SDM_CHANNEL, AntMesg.MESG_CHANNEL_ID_ID);
        } catch (AntInterfaceException e) {
          handleAntError();
        }
      }

      // Check page 2 data
      if (message[3] == 0x02) {
        lastCadence = (int) ((message[6] & 0xFF) + (((message[7] >>> 4) & 0x0F) / 16.0f));
        setSenorDataSet();
      }
    }
  };

  /**
   * Sets sensor data set.
   */
  private void setSenorDataSet() {
    long now = System.currentTimeMillis();
    // Data comes in at ~4Hz rate from the sensors, so after >300 msec fresh
    // data is here from all the connected sensors
    if (now < lastSensorDataSetTime + 300) { return; }
    lastSensorDataSetTime = now;

    SensorDataSet.Builder builder = Sensor.SensorDataSet.newBuilder();
    if (lastHeartRate != -1) {
      builder.setHeartRate(Sensor.SensorData.newBuilder()
          .setValue(lastHeartRate).setState(Sensor.SensorState.SENDING));
    }
    if (lastCadence != -1) {
      builder.setCadence(Sensor.SensorData.newBuilder()
          .setValue(lastCadence).setState(Sensor.SensorState.SENDING));
    }
    sensorDataSet = builder.setCreationTime(now).build();
    setSensorState(SensorState.SENDING);
  }

  /**
   * Sets up ant channel.
   * 
   * @param networkNumber the network number
   * @param channel the channel
   */
  private void setupAntChannel(byte networkNumber, byte channel) {
    try {
      channelConfig[channel].isInitializing = true;
      channelConfig[channel].isDeinitializing = false;

      // Assign as slave channel on selected network
      antInterface.ANTAssignChannel(channel, AntDefine.PARAMETER_RX_NOT_TX, networkNumber);

      // The rest of the channel configuration will occur after the response is
      // received in handleResponseEventMessage
    } catch (AntInterfaceException aie) {
      handleAntError();
    }
  }

  /**
   * Enables data message.
   * 
   * @param enabled true to enable
   */
  private void enableDataMessage(boolean enabled) {
    if (enabled) {
      context.registerReceiver(
          dataReceiver, new IntentFilter(AntInterfaceIntent.ANT_RX_MESSAGE_ACTION));
      openChannel(HRM_CHANNEL);
      openChannel(SDM_CHANNEL);
    } else {
      try {
        context.unregisterReceiver(dataReceiver);
        closeChannel(HRM_CHANNEL);
        closeChannel(SDM_CHANNEL);
        setSensorState(SensorState.DISCONNECTED);
      } catch (IllegalArgumentException e) {
        // Can safely ignore
      }
    }
  }

  /**
   * Returns true if in airplane mode.
   */
  private boolean isAirPlaneMode() {
    if (!Settings.System.getString(
        context.getContentResolver(), Settings.System.AIRPLANE_MODE_RADIOS)
        .contains(RADIO_ANT)) {
      return false;
    }
    if (Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0)
        == 0) {
      return false;
    }

    try {
      Field field = Settings.System.class.getField("AIRPLANE_MODE_TOGGLEABLE_RADIOS");
      return !Settings.System.getString(context.getContentResolver(), (String) field.get(null))
          .contains(RADIO_ANT);
    } catch (Exception e) {
      // This is expected if the list does not yet exist, so just return true
      return true;
    }
  }
}
