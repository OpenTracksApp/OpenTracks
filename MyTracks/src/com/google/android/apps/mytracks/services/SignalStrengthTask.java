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

package com.google.android.apps.mytracks.services;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.content.Waypoint;

import android.content.Context;
import android.location.Location;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

/**
 * A class to monitor the network signal strength.
 *
 * @author Sandor Dornbush
 */
public class SignalStrengthTask extends PhoneStateListener
    implements PeriodicTask {

  private final Context context;
  private TelephonyManager manager;
  private int signalStrength = -1;

  public SignalStrengthTask(Context c) {
    context = c;
  }

  @Override
  public void start() {
    manager =
      (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    if (manager == null) {
      Log.e(MyTracksConstants.TAG, "Cannot get telephony manager.");
    } else {
      manager.listen(this, getListenEvents());
    }
  }

  protected int getListenEvents() {
    return PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
  }

  @Override
  public void onSignalStrengthChanged(int asu) {
    this.signalStrength = asu;
    Log.d(MyTracksConstants.TAG, "Signal Strength: " + signalStrength);
  }

  @Override
  public void run(TrackRecordingService service) {
    Log.d(MyTracksConstants.TAG, "Adding signal marker");
    Location location = service.getLastLocation();
    if (manager == null) {
      Log.d(MyTracksConstants.TAG, "Adding signal marker: marker null");
      return;
    }
    if (location == null) {
      Log.d(MyTracksConstants.TAG, "Adding signal marker: location null");
      return;
    }
    Waypoint wpt = new Waypoint();
    wpt.setName("Signal Strength");
    wpt.setType(Waypoint.TYPE_WAYPOINT);
    wpt.setTrackId(service.getRecordingTrackId());
    int networkType = manager.getNetworkType();
    wpt.setIcon(getIcon(networkType));
    wpt.setLocation(location);
    wpt.setDescription(getDescription());
    long waypointId = service.insertWaypointMarker(wpt);
    if (waypointId >= 0) {
      Log.d(MyTracksConstants.TAG, "Added signal marker");
    } else {
      Log.e(MyTracksConstants.TAG, "Cannot insert waypoint marker?");
    }
  }

  /**
   * Gets a human readable description for the network type.
   *
   * @param type The integer constant for the network type
   * @return A human readable description of the network type
   */
  protected String getTypeAsString(int type) {
    switch (type) {
      case TelephonyManager.NETWORK_TYPE_EDGE:
        return "EDGE";
      case TelephonyManager.NETWORK_TYPE_GPRS:
        return "GPRS";
      case TelephonyManager.NETWORK_TYPE_UMTS:
        return "UMTS";
      case TelephonyManager.NETWORK_TYPE_UNKNOWN:
      default:
        return "UNKNOWN";
    }
  }

  /**
   * Builds a description for the current signal strength.
   * 
   * @return A human readable description of the network state
   */
  private String getDescription() {
    StringBuffer sb = new StringBuffer();
    sb.append(getStrengthAsString());

    sb.append("Network Type: ");
    sb.append(getTypeAsString(manager.getNetworkType()));
    sb.append('\n');

    sb.append("Operator: ");
    sb.append(manager.getNetworkOperatorName());
    sb.append(" / ");
    sb.append(manager.getNetworkOperator());
    sb.append('\n');

    sb.append("Roaming: ");
    sb.append(manager.isNetworkRoaming());
    sb.append('\n');

    List<NeighboringCellInfo> infos = manager.getNeighboringCellInfo();
    Log.i(MyTracksConstants.TAG, "Found " + infos.size() + " cells.");
    if (infos.size() > 0) {
      sb.append("Neighbors: ");
      for (NeighboringCellInfo info : infos) {
        sb.append(info.toString());
        sb.append(' ');
      }
      sb.append('\n');
    }

    CellLocation cell = manager.getCellLocation();
    if (cell != null) {
      sb.append("Cell: ");
      sb.append(cell.toString());
      sb.append('\n');
    }

    return sb.toString();
  }

  /**
   * Gets the url for the waypoint icon for the current network type.
   *
   * @param type The network type
   * @return A url to a image to use as the waypoint icon
   */
  protected String getIcon(int type) {
    switch (type) {
      case TelephonyManager.NETWORK_TYPE_GPRS:
      case TelephonyManager.NETWORK_TYPE_EDGE:
        return "http://maps.google.com/mapfiles/ms/micons/green.png";
      case TelephonyManager.NETWORK_TYPE_UMTS:
        return "http://maps.google.com/mapfiles/ms/micons/blue.png";
      case TelephonyManager.NETWORK_TYPE_UNKNOWN:
      default:
        return "http://maps.google.com/mapfiles/ms/micons/red.png";
    }
  }

  @Override
  public void shutdown() {
    if (manager != null) {
      manager.listen(this, PhoneStateListener.LISTEN_NONE);
      manager = null;
    }
  }

  public String getStrengthAsString() {
    return "Strength: " + signalStrength + "\n";
  }

  protected Context getContext() {
    return context;
  }
}
