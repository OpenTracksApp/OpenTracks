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

import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Sensor.SensorState;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Bluetooth sensor manager.
 * 
 * @author Sandor Dornbush
 */
public class BluetoothSensorManager extends SensorManager {

  private static final BluetoothAdapter bluetoothAdapter = getDefaultBluetoothAdapter();
  private static final String TAG = BluetoothConnectionManager.class.getSimpleName();

  /**
   * Gets the default bluetooth adapter.
   */
  private static BluetoothAdapter getDefaultBluetoothAdapter() {
    // If from the main application thread, return directly
    if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
      return BluetoothAdapter.getDefaultAdapter();
    }
  
    // Get the default adapter from the main application thread
    final ArrayList<BluetoothAdapter> adapters = new ArrayList<BluetoothAdapter>(1);
    final Object mutex = new Object();
  
    Handler handler = new Handler(Looper.getMainLooper());
    handler.post(new Runnable() {
        @Override
      public void run() {
        adapters.add(BluetoothAdapter.getDefaultAdapter());
        synchronized (mutex) {
          mutex.notify();
        }
      }
    });
  
    while (adapters.isEmpty()) {
      synchronized (mutex) {
        try {
          mutex.wait(1000L);
        } catch (InterruptedException e) {
          Log.e(TAG, "Interrupted while waiting for default bluetooth adapter", e);
        }
      }
    }
  
    if (adapters.get(0) == null) {
      Log.w(TAG, "No bluetooth adapter found.");
      return null;
    }
    return adapters.get(0);
  }

  private final Context context;
  private final MessageParser messageParser;
  private final BluetoothConnectionManager bluetoothConnectionManager;
  private SensorDataSet sensorDataSet = null;

  // Handler that gets information back from the bluetoothConnectionManager
  private final Handler messageHandler = new Handler(Looper.getMainLooper()) {
      @Override
    public void handleMessage(Message message) {
      switch (message.what) {
        case BluetoothConnectionManager.MESSAGE_DEVICE_NAME:
          String deviceName = message.getData()
              .getString(BluetoothConnectionManager.KEY_DEVICE_NAME);
          Toast.makeText(context, context.getString(R.string.settings_sensor_connected, deviceName),
              Toast.LENGTH_SHORT).show();
          break;
        case BluetoothConnectionManager.MESSAGE_READ:
          try {
            byte[] readBuf = (byte[]) message.obj;
            sensorDataSet = messageParser.parseBuffer(readBuf);
            Log.d(TAG, "MESSAGE_READ: " + sensorDataSet);
          } catch (IllegalArgumentException e) {
            sensorDataSet = null;
            Log.i(TAG, "Unexpected exception on read", e);
          } catch (RuntimeException e) {
            sensorDataSet = null;
            Log.i(TAG, "Unexpected exception on read.", e);
          }
          break;
        default:
          break;
      }
    }
  };

  /**
   * Constructor.
   * 
   * @param context the context
   * @param messageParser the message parser
   */
  public BluetoothSensorManager(Context context, MessageParser messageParser) {
    this.context = context;
    this.messageParser = messageParser;
    bluetoothConnectionManager = new BluetoothConnectionManager(
        bluetoothAdapter, messageHandler, messageParser);
  }

  @Override
  public boolean isEnabled() {
    return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
  }

  @Override
  protected void setUpChannel() {
    if (!isEnabled()) {
      Log.w(TAG, "Bluetooth not enabled.");
      return;
    }
    String address = PreferencesUtils.getString(
        context, R.string.bluetooth_sensor_key, PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT);
    if (PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT.equals(address)) {
      Log.w(TAG, "No blueooth address.");
      return;
    }
    Log.w(TAG, "Connecting to bluetooth address: " + address);

    BluetoothDevice device;
    try {
      device = bluetoothAdapter.getRemoteDevice(address);
    } catch (IllegalArgumentException e) {
      Log.d(TAG, "Unable to get remote device for: " + address, e);
      return;
    }
    bluetoothConnectionManager.connect(device);
  }

  @Override
  protected void tearDownChannel() {
    bluetoothConnectionManager.reset();
  }

  @Override
  public SensorState getSensorState() {
    return bluetoothConnectionManager.getSensorState();
  }

  @Override
  public SensorDataSet getSensorDataSet() {
    return sensorDataSet;
  }
}
