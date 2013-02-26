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

import com.google.android.apps.mytracks.services.sensors.ant.AntSensorManager.ChannelStates;

/**
 * Ant channel configuration.
 *
 * @author Jimmy Shih
 */
public abstract class ChannelConfiguration {

  public static final byte FREQUENCY = 57; // 2457Mhz (Ant+ frequency)
  public static final byte TRANSMISSION_TYPE = 0; // 0 for wild card search
  public static final byte PROXIMITY_SEARCH = 7;

  private short deviceNumber = AntSensorManager.WILDCARD;
  private ChannelStates channelState = ChannelStates.CLOSED;
  private boolean isInitializing = false;
  private boolean isDeinitializing = false;

  /**
   * Gets the My Tracks settings device id key.
   */
  public abstract int getDeviceIdKey();

  /**
   * Gets the device type.
   */
  public abstract byte getDeviceType();

  /**
   * Gets the message period.
   */
  public abstract short getMessagPeriod();

  /**
   * Decodes the message and places the value in antSensorValue.
   *
   * @param message the message
   * @param antSensorValue the ant sensor value
   */
  public abstract void decodeMessage(byte[] message, AntSensorValue antSensorValue);

  /**
   * Gets the device number.
   */
  public short getDeviceNumber() {
    return deviceNumber;
  }

  /**
   * Sets the device number.
   *
   * @param deviceNumber the device number to set
   */
  public void setDeviceNumber(short deviceNumber) {
    this.deviceNumber = deviceNumber;
  }

  /**
   * Gets the channel state.
   */
  public ChannelStates getChannelState() {
    return channelState;
  }

  /**
   * Sets the channel state.
   *
   * @param channelState the channelState to set
   */
  public void setChannelState(ChannelStates channelState) {
    this.channelState = channelState;
  }

  /**
   * Returns true if initializing.
   */
  public boolean isInitializing() {
    return isInitializing;
  }

  /**
   * Sets isInitializing.
   *
   * @param isInitializing true if isInitializing
   */
  public void setInitializing(boolean isInitializing) {
    this.isInitializing = isInitializing;
  }

  /**
   * Returns true if deinitializing.
   */
  public boolean isDeinitializing() {
    return isDeinitializing;
  }

  /**
   * Sets isDeinitializing.
   *
   * @param isDeinitializing true if isDeinitializing
   */
  public void setDeinitializing(boolean isDeinitializing) {
    this.isDeinitializing = isDeinitializing;
  }
}
