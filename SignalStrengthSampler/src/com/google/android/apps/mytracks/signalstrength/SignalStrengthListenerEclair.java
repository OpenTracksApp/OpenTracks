/*
 * Copyright 2010 Google Inc.
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

package com.google.android.apps.mytracks.signalstrength;

import static com.google.android.apps.mytracks.signalstrength.SignalStrengthConstants.*;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * A class to monitor the network signal strength.
 *
 * TODO: i18n
 *
 * @author Sandor Dornbush
 */
public class SignalStrengthListenerEclair extends SignalStrengthListenerCupcake {

  private SignalStrength signalStrength = null;

  public SignalStrengthListenerEclair(Context ctx, SignalStrengthCallback callback) {
    super(ctx, callback);
  }

  @Override
  protected int getListenEvents() {
    return PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
  }

  @SuppressWarnings("hiding")
  @Override
  public void onSignalStrengthsChanged(SignalStrength signalStrength) {
    Log.d(TAG, "Signal Strength Modern: " + signalStrength);
    this.signalStrength = signalStrength;
    notifySignalSampled();
  }

  /**
   * Gets a human readable description for the network type.
   *
   * @param type The integer constant for the network type
   * @return A human readable description of the network type
   */
  @Override
  protected String getTypeAsString(int type) {
    switch (type) {
      case TelephonyManager.NETWORK_TYPE_1xRTT:
        return "1xRTT";
      case TelephonyManager.NETWORK_TYPE_CDMA:
        return "CDMA";
      case TelephonyManager.NETWORK_TYPE_EDGE:
        return "EDGE";
      case TelephonyManager.NETWORK_TYPE_EVDO_0:
        return "EVDO 0";
      case TelephonyManager.NETWORK_TYPE_EVDO_A:
        return "EVDO A";
      case TelephonyManager.NETWORK_TYPE_GPRS:
        return "GPRS";
      case TelephonyManager.NETWORK_TYPE_HSDPA:
        return "HSDPA";
      case TelephonyManager.NETWORK_TYPE_HSPA:
        return "HSPA";
      case TelephonyManager.NETWORK_TYPE_HSUPA:
        return "HSUPA";
      case TelephonyManager.NETWORK_TYPE_UMTS:
        return "UMTS";
      case TelephonyManager.NETWORK_TYPE_UNKNOWN:
      default:
        return "UNKNOWN";
    }
  }

  /**
   * Gets the url for the waypoint icon for the current network type.
   *
   * @param type The network type
   * @return A url to a image to use as the waypoint icon
   */
  @Override
  protected String getIcon(int type) {
    switch (type) {
      case TelephonyManager.NETWORK_TYPE_1xRTT:
      case TelephonyManager.NETWORK_TYPE_CDMA:
      case TelephonyManager.NETWORK_TYPE_GPRS:
      case TelephonyManager.NETWORK_TYPE_EDGE:
        return "http://maps.google.com/mapfiles/ms/micons/green.png";
      case TelephonyManager.NETWORK_TYPE_EVDO_0:
      case TelephonyManager.NETWORK_TYPE_EVDO_A:
      case TelephonyManager.NETWORK_TYPE_HSDPA:
      case TelephonyManager.NETWORK_TYPE_HSPA:
      case TelephonyManager.NETWORK_TYPE_HSUPA:
      case TelephonyManager.NETWORK_TYPE_UMTS:
        return "http://maps.google.com/mapfiles/ms/micons/blue.png";
      case TelephonyManager.NETWORK_TYPE_UNKNOWN:
      default:
        return "http://maps.google.com/mapfiles/ms/micons/red.png";
    }
  }

  @Override
  public String getStrengthAsString() {
    if (signalStrength == null) {
      return "Strength: " + getContext().getString(R.string.unknown) + "\n";
    }
    StringBuffer sb = new StringBuffer();
    if (signalStrength.isGsm()) {
      appendSignal(signalStrength.getGsmSignalStrength(),
                   R.string.gsm_strength,
                   sb);
      maybeAppendSignal(signalStrength.getGsmBitErrorRate(),
                        R.string.error_rate,
                        sb);
    } else {
      appendSignal(signalStrength.getCdmaDbm(), R.string.cdma_strength, sb);
      appendSignal(signalStrength.getCdmaEcio() / 10.0, R.string.ecio, sb);
      appendSignal(signalStrength.getEvdoDbm(), R.string.evdo_strength, sb);
      appendSignal(signalStrength.getEvdoEcio() / 10.0, R.string.ecio, sb);
      appendSignal(signalStrength.getEvdoSnr(),
                   R.string.signal_to_noise_ratio,
                   sb);
    }
    return sb.toString();
  }

  private void maybeAppendSignal(
      int signal, int signalFormat, StringBuffer sb) {
    if (signal > 0) {
      sb.append(getContext().getString(signalFormat, signal));
    }
  }

  private void appendSignal(int signal, int signalFormat, StringBuffer sb) {
    sb.append(getContext().getString(signalFormat, signal));
    sb.append("\n");
  }

  private void appendSignal(double signal, int signalFormat, StringBuffer sb) {
    sb.append(getContext().getString(signalFormat, signal));
    sb.append("\n");
  }
}
