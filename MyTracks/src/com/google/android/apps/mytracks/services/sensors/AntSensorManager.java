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
package com.google.android.apps.mytracks.services.sensors;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;

import com.dsi.ant.AntDefine;
import com.dsi.ant.AntInterface;
import com.dsi.ant.AntInterfaceIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * An abstract class for ANT+ based sensors.
 *
 * @author Sandor Dornbush
 */
public abstract class AntSensorManager extends SensorManager {

  // ANT Service
  public static final int EVENT_ANT_ENABLED = 1;
  public static final int EVENT_ANT_DISABLED = 2;

  // Pairing
  protected static final short WILDCARD = 0;

  private AntInterface antReceiver;

  private boolean status;

  // Flag to know if the ANT App was interrupted
  // TODO this code path is not used but probably should be.
  private boolean antInterrupted = false;

  /**
   * The data from the sensors.
   */
  protected SensorDataSet sensorData = null;

  protected Context context = null;
  
  private static final boolean DEBUGGING = false;

  /**
   * Receives all of the ANT intents and dispatches to the proper handler.
   */
  private final BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String antAction = intent.getAction();

      Log.i(MyTracksConstants.TAG, "enter onReceive" + antAction);
      if (antAction.equals(AntInterfaceIntent.ANT_ENABLED_ACTION)) {
        Log.i(MyTracksConstants.TAG, "enter onReceive ANT_ENABLED_ACTION "
            + antAction);
        handler.sendMessage(handler.obtainMessage(EVENT_ANT_ENABLED, 0));
      }
      if (antAction.equals(AntInterfaceIntent.ANT_DISABLED_ACTION)) {
        Log.i(MyTracksConstants.TAG, "enter onReceive ANT_DISABLED_ACTION "
            + antAction);
        handler.sendMessage(handler.obtainMessage(EVENT_ANT_DISABLED, 0));
      }

      if (antAction.equals(AntInterfaceIntent.ANT_RX_MESSAGE_ACTION)) {
        byte[] antMessage =
            intent.getByteArrayExtra(AntInterfaceIntent.ANT_MESSAGE);
        if (DEBUGGING) {
          StringBuilder sb = 
              new StringBuilder("ANT_RX_MESSAGE_ACTION Rx:");
  
          for (int i = 0; i < antMessage.length; i++)
            sb.append(String.format("[%X]]", (antMessage[i] & 0xFF)));
  
          Log.d(MyTracksConstants.TAG, sb.toString());
        }

        handleMessage(antMessage);
      }
    }
  };

  /**
   * Handler for all the ANT related events. The events will be handled only
   * when we are in the ANT application main menu
   **/
  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case EVENT_ANT_ENABLED:
          Log.i(MyTracksConstants.TAG,
              "enter handleMessage ----EVENT_ANT_ENABLED");
          break;
        case EVENT_ANT_DISABLED:
          Log.i(MyTracksConstants.TAG,
              "enter handleMessage ----EVENT_ANT_DISABLED");
          break;
      }
    }
  };

  public AntSensorManager(Context context) {
    this.context = context;

    // Need to enable the ANT if it is not enabled earlier
    antReceiver = AntInterface.getInstance(context, null);
    if (antReceiver == null) {
      return;
    }

    status = antReceiver.isEnabled();
    if (!status) {
      // Make sure not to call AntInterface.enable() again, if it has been
      // already called before
      if (antInterrupted == false) {
        status = antReceiver.enable();
        if (status == false) {
          Log.e(MyTracksConstants.TAG, "Can not enable ANT interface");
        } else {
          Log.i(MyTracksConstants.TAG, "Powering on Radio");
        }
      } else {
        Log.i(MyTracksConstants.TAG, "Radio already enabled");
      }
    }
  }

  @Override
  public void setupChannel() {
    Log.i(MyTracksConstants.TAG, "Registering for ant intents.");
    // Register for ANT intent broadcasts.
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(AntInterfaceIntent.ANT_ENABLED_ACTION);
    intentFilter.addAction(AntInterfaceIntent.ANT_DISABLED_ACTION);
    intentFilter.addAction(AntInterfaceIntent.ANT_RX_MESSAGE_ACTION);
    context.registerReceiver(receiver, intentFilter);
  }

  @Override
  public SensorDataSet getSensorDataSet() {
    return sensorData;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public abstract void handleMessage(byte[] antMessage);

  @Override
  public void onDestroy() {
    Log.i(MyTracksConstants.TAG, "Unregistering for ant intents.");
    context.unregisterReceiver(receiver);
  }

  /**
   * Set up an ANT+ channel.
   * 
   * @return True on success.
   */
  protected boolean antChannelSetup(byte networkNumber, byte channelNumber,
      short deviceNumber, byte deviceType, byte txType, short channelPeriod,
      byte radioFreq, byte proxSearch) {

    // Assign as slave channel on selected network (0 = public, 1 = ANT+, 2 =
    // ANTFS)
    if (!antReceiver.ANTAssignChannel(channelNumber,
        AntDefine.PARAMETER_RX_NOT_TX, networkNumber)) {
      return false;
    }

    if (!antReceiver.ANTSetChannelId(channelNumber, deviceNumber, deviceType,
        txType)) {
      return false;
    }

    if (!antReceiver.ANTSetChannelPeriod(channelNumber, channelPeriod)) {
      return false;
    }

    if (!antReceiver.ANTSetChannelRFFreq(channelNumber, radioFreq)) {
      return false;
    }

    // Disable high priority search
    if (!antReceiver.ANTSetChannelSearchTimeout(channelNumber, (byte) 0)) {
      return false;
    }

    // Set search timeout to 30 seconds (low priority search))
    if (!antReceiver.ANTSetLowPriorityChannelSearchTimeout(channelNumber,
        (byte) 12)) {
      return false;
    }

    if (deviceNumber == WILDCARD) {
      // Configure proximity search, if using wild card search
      if (!antReceiver.ANTSetProximitySearch(channelNumber, proxSearch)) {
        return false;
      }
    }

    return !antReceiver.ANTOpenChannel(channelNumber);
  }

  public AntInterface getAntReceiver() {
    return antReceiver;
  }
}
