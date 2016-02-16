/*
 * Copyright (C) 2010 Google Inc.
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

import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorState;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Manages bluetooth connection. It has a thread for connecting with a bluetooth
 * device and a thread for performing data transmission when connected.
 * 
 * @author Sandor Dornbush
 */
public class BluetoothConnectionManager {

  // My Tracks UUID
  public static final UUID MY_TRACKS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  // Message types sent to hander
  public static final int MESSAGE_DEVICE_NAME = 1;
  public static final int MESSAGE_READ = 2;

  // Key for storing the device name
  public static final String KEY_DEVICE_NAME = "device_name";

  private static final String TAG = BluetoothConnectionManager.class.getSimpleName();

  private final BluetoothAdapter bluetoothAdapter;
  private final Handler handler;
  private final MessageParser messageParser;
  private SensorState sensorState;

  private ConnectThread connectThread;
  private ConnectedThread connectedThread;

  /**
   * Constructor.
   * 
   * @param bluetoothAdapter the bluetooth adapter
   * @param handler a hander for sending messages back to the UI activity
   * @param messageParser a message parser
   */
  public BluetoothConnectionManager(
      BluetoothAdapter bluetoothAdapter, Handler handler, MessageParser messageParser) {
    this.bluetoothAdapter = bluetoothAdapter;
    this.handler = handler;
    this.messageParser = messageParser;
    this.sensorState = SensorState.NONE;
  }

  /**
   * Gets the sensor state.
   */
  public synchronized SensorState getSensorState() {
    return sensorState;
  }

  /**
   * Sets the sensor state.
   * 
   * @param sensorState the sensor state
   */
  private synchronized void setState(Sensor.SensorState sensorState) {
    this.sensorState = sensorState;
  }

  /**
   * Resets the bluetooth connection manager.
   */
  public synchronized void reset() {
    cancelThreads();
    setState(Sensor.SensorState.NONE);
  }

  /**
   * Cancels all the threads.
   */
  private void cancelThreads() {
    if (connectThread != null) {
      connectThread.cancel();
      connectThread = null;
    }
    if (connectedThread != null) {
      connectedThread.cancel();
      connectedThread = null;
    }
  }

  /**
   * Connects to a bluetooth device.
   * 
   * @param bluetoothDevice the bluetooth device
   */
  public synchronized void connect(BluetoothDevice bluetoothDevice) {
    Log.d(TAG, "connect to: " + bluetoothDevice);
    cancelThreads();

    connectThread = new ConnectThread(bluetoothDevice);
    connectThread.start();
    setState(Sensor.SensorState.CONNECTING);
  }

  /**
   * Starts the ConnectedThread to read data.
   * 
   * @param bluetoothSocket the bluetooth socket
   * @param bluetoothDevice the bluetooth device
   */
  private synchronized void connected(
      BluetoothSocket bluetoothSocket, BluetoothDevice bluetoothDevice) {
    cancelThreads();

    connectedThread = new ConnectedThread(bluetoothSocket);
    connectedThread.start();

    // Send the device name to the handler
    Message message = handler.obtainMessage(MESSAGE_DEVICE_NAME);
    Bundle bundle = new Bundle();
    bundle.putString(KEY_DEVICE_NAME, bluetoothDevice.getName());
    message.setData(bundle);
    handler.sendMessage(message);

    setState(Sensor.SensorState.CONNECTED);
  }

  /**
   * A thread to connect to a bluetooth device.
   */
  private class ConnectThread extends Thread {
    private final BluetoothSocket bluetoothSocket;
    private final BluetoothDevice bluetoothDevice;

    public ConnectThread(BluetoothDevice device) {
      setName("ConnectThread-" + device.getName());
      this.bluetoothDevice = device;
      BluetoothSocket tmp = null;
      try {
        tmp = ApiAdapterFactory.getApiAdapter().getBluetoothSocket(device);
      } catch (IOException e) {
        Log.e(TAG, "Unable to get blueooth socket.", e);
      }
      bluetoothSocket = tmp;
    }

    @Override
    public void run() {
      if (bluetoothAdapter == null) {
        BluetoothConnectionManager.this.reset();
        return;
      }
      // Cancel discovery to prevent slow down
      bluetoothAdapter.cancelDiscovery();

      try {
        bluetoothSocket.connect();
      } catch (IOException connectException) {
        Log.i(TAG, "Unable to connect.", connectException);
        setState(Sensor.SensorState.DISCONNECTED);
        try {
          bluetoothSocket.close();
        } catch (IOException e) {
          Log.e(TAG, "Unable to close blueooth socket.", e);
        }
        // Reset the bluetooth connection manager
        BluetoothConnectionManager.this.reset();
        return;
      }

      // Reset the ConnectThread since we are done
      synchronized (BluetoothConnectionManager.this) {
        connectThread = null;
      }

      // Start the connected thread
      connected(bluetoothSocket, bluetoothDevice);
    }

    /**
     * Cancels this thread.
     */
    public void cancel() {
      try {
        bluetoothSocket.close();
      } catch (IOException e) {
        Log.e(TAG, "Unable to close bluetooth socket.", e);
      }
    }
  }

  /**
   * This thread handles data transmission when connected.
   */
  private class ConnectedThread extends Thread {
    private final BluetoothSocket bluetoothSSocket;
    private final InputStream inputStream;

    public ConnectedThread(BluetoothSocket bluetoothSocket) {
      this.bluetoothSSocket = bluetoothSocket;
      InputStream tmp = null;

      try {
        tmp = bluetoothSocket.getInputStream();
      } catch (IOException e) {
        Log.e(TAG, "Unable to get input stream.", e);
      }
      inputStream = tmp;
    }

    @Override
    public void run() {
      byte[] buffer = new byte[messageParser.getFrameSize()];
      int bytes; // bytes read
      int offset = 0;

      // Keep listening to the inputStream while connected
      while (true) {
        try {
          // Read from the inputStream
          bytes = inputStream.read(buffer, offset, messageParser.getFrameSize() - offset);

          if (bytes == -1) { throw new IOException("EOF reached."); }

          offset += bytes;

          if (offset != messageParser.getFrameSize()) {
            // Partial frame received. Call read again to receive the rest.
            continue;
          }

          if (!messageParser.isValid(buffer)) {
            int index = messageParser.findNextAlignment(buffer);
            if (index == -1) {
              Log.w(TAG, "Could not find any valid data. Drop data.");
              offset = 0;
              continue;
            }
            Log.w(TAG, "Misaligned data. Found new message at " + index + ". Recovering...");
            offset = messageParser.getFrameSize() - index;
            System.arraycopy(buffer, index, buffer, 0, offset);
            continue;
          }

          offset = 0;

          // Send a copy of the obtained bytes to the handler to avoid memory
          // inconsistency issues
          handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer.clone()).sendToTarget();
        } catch (IOException e) {
          Log.i(TAG, "Bluetooth connection lost.", e);
          setState(Sensor.SensorState.DISCONNECTED);
          break;
        }
      }
    }

    /**
     * Cancels this thread.
     */
    public void cancel() {
      try {
        bluetoothSSocket.close();
      } catch (IOException e) {
        Log.e(TAG, "Unable to close bluetooth socket.", e);
      }
    }
  }
}
