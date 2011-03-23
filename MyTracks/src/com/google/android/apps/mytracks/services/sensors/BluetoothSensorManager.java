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
package com.google.android.apps.mytracks.services.sensors;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.maps.mytracks.R;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * Manage the connection to a bluetooth sensor.
 *
 * @author Sandor Dornbush
 */
public class BluetoothSensorManager extends SensorManager {

  // Local Bluetooth adapter
  private BluetoothAdapter bluetoothAdapter = null;

  // Member object for the sensor threads and connections.
  private BluetoothConnectionManager connectionManager = null;

  // Name of the connected device
  private String connectedDeviceName = null;

  private Context context = null;

  private Sensor.SensorDataSet sensorDataSet = null;

  private MessageParser parser;
  
  public BluetoothSensorManager(
      Context context, MessageParser parser) {
    this.context = context;
    this.parser = parser;
    // Get local Bluetooth adapter
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    // If BT is not available or not enabled quit.
    if (!isEnabled()) {
      return;
    }

    setupSensor();
  }

  private void setupSensor() {
    Log.d(Constants.TAG, "setupSensor()");

    // Initialize the BluetoothSensorAdapter to perform bluetooth connections.
    connectionManager = new BluetoothConnectionManager(messageHandler, parser);
  }

  public boolean isEnabled() {
    return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
  }

  public void setupChannel() {
    if (!isEnabled() || connectionManager == null) {
      Log.w(Constants.TAG, "Disabled manager onStartTrack");
      return;
    }
    SharedPreferences prefs =
        context.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    String address =
      prefs.getString(context.getString(R.string.bluetooth_sensor_key), null);
    if (address == null) {
      return;
    }
    Log.w(Constants.TAG, "Connecting to bluetooth sensor: " + address);
    // Get the BluetoothDevice object
    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
    // Attempt to connect to the device
    connectionManager.connect(device);

    // Performing this check in onResume() covers the case in which BT was
    // not enabled during onStart(), so we were paused to enable it...
    // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
    if (connectionManager != null) {
      // Only if the state is STATE_NONE, do we know that we haven't started
      // already
      if (connectionManager.getState() == Sensor.SensorState.NONE) {
        // Start the Bluetooth sensor services
        Log.w(Constants.TAG, "Disabled manager onStartTrack");
        connectionManager.start();
      }
    }
  }

  public void onDestroy() {
    // Stop the Bluetooth sensor services
    if (connectionManager != null) {
      connectionManager.stop();
    }
  }

  public Sensor.SensorDataSet getSensorDataSet() {
    return sensorDataSet;
  }

  public Sensor.SensorState getSensorState() {
    return (connectionManager == null)
        ? Sensor.SensorState.NONE
            : connectionManager.getState();
  }

  // The Handler that gets information back from the BluetoothSensorService
  private final Handler messageHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case BluetoothConnectionManager.MESSAGE_STATE_CHANGE:
          // TODO should we update the SensorManager state var?
          Log.i(Constants.TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
          break;
        case BluetoothConnectionManager.MESSAGE_WRITE:
          break;
        case BluetoothConnectionManager.MESSAGE_READ:
          byte[] readBuf = null;
          try {
            readBuf = (byte[]) msg.obj;
            sensorDataSet = parser.parseBuffer(readBuf);
            Log.d(Constants.TAG, "MESSAGE_READ: " + sensorDataSet.toString());
          } catch (IllegalArgumentException iae) {
            sensorDataSet = null;
            Log.i(Constants.TAG,
                "Got bad sensor data: " + new String(readBuf, 0, readBuf.length),
                iae);
          } catch (RuntimeException re) {
            sensorDataSet = null;
            Log.i(Constants.TAG, "Unexpected exception on read.", re);
          }
          break;
        case BluetoothConnectionManager.MESSAGE_DEVICE_NAME:
          // save the connected device's name
          connectedDeviceName =
              msg.getData().getString(BluetoothConnectionManager.DEVICE_NAME);
          Toast.makeText(context.getApplicationContext(),
              "Connected to " + connectedDeviceName, Toast.LENGTH_SHORT)
              .show();
          break;
      }
    }
  };
}
