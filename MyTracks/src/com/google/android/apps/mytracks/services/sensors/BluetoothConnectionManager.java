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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.Sensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 *
 * @author Sandor Dornbush
 */
public class BluetoothConnectionManager {
  // Unique UUID for this application
  private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  private MessageParser parser;

  // Member fields
  private final BluetoothAdapter adapter;
  private final Handler handler;
  private ConnectThread connectThread;
  private ConnectedThread connectedThread;

  private Sensor.SensorState state;

  // Message types sent from the BluetoothSenorService Handler
  public static final int MESSAGE_STATE_CHANGE = 1;
  public static final int MESSAGE_READ = 2;
  public static final int MESSAGE_WRITE = 3;
  public static final int MESSAGE_DEVICE_NAME = 4;

  // Key names received from the BluetoothSenorService Handler
  public static final String DEVICE_NAME = "device_name";

  /**
   * Constructor. Prepares a new BluetoothSensor session.
   *
   * @param context The UI Activity Context
   * @param handler A Handler to send messages back to the UI Activity
   */
  public BluetoothConnectionManager(Handler handler, MessageParser parser) {
    this.adapter = BluetoothAdapter.getDefaultAdapter();
    this.state = Sensor.SensorState.NONE;
    this.handler = handler;
    this.parser = parser;
  }

  /**
   * Set the current state of the sensor connection
   *
   * @param state An integer defining the current connection state
   */
  private synchronized void setState(Sensor.SensorState state) {
    // TODO pretty print this.
    Log.d(Constants.TAG, "setState(" + state + ")");
    this.state = state;

    // Give the new state to the Handler so the UI Activity can update
    handler.obtainMessage(MESSAGE_STATE_CHANGE, state.getNumber(), -1).sendToTarget();
  }

  /**
   * Return the current connection state.
   */
  public synchronized Sensor.SensorState getState() {
    return state;
  }

  /**
   * Start the sensor service. Specifically start AcceptThread to begin a session
   * in listening (server) mode. Called by the Activity onResume()
   */
  public synchronized void start() {
    Log.d(Constants.TAG, "BluetoothConnectionManager.start()");

    // Cancel any thread attempting to make a connection
    if (connectThread != null) {
      connectThread.cancel();
      connectThread = null;
    }

    // Cancel any thread currently running a connection
    if (connectedThread != null) {
      connectedThread.cancel();
      connectedThread = null;
    }

    setState(Sensor.SensorState.NONE);
  }

  /**
   * Start the ConnectThread to initiate a connection to a remote device.
   *
   * @param device The BluetoothDevice to connect
   */
  public synchronized void connect(BluetoothDevice device) {
    Log.d(Constants.TAG, "connect to: " + device);

    // Cancel any thread attempting to make a connection
    if (state == Sensor.SensorState.CONNECTING) {
      if (connectThread != null) {
        connectThread.cancel();
        connectThread = null;
      }
    }

    // Cancel any thread currently running a connection
    if (connectedThread != null) {
      connectedThread.cancel();
      connectedThread = null;
    }

    // Start the thread to connect with the given device
    connectThread = new ConnectThread(device);
    connectThread.start();
    setState(Sensor.SensorState.CONNECTING);
  }

  /**
   * Start the ConnectedThread to begin managing a Bluetooth connection
   *
   * @param socket The BluetoothSocket on which the connection was made
   * @param device The BluetoothDevice that has been connected
   */
  public synchronized void connected(BluetoothSocket socket,
      BluetoothDevice device) {
    Log.d(Constants.TAG, "connected");

    // Cancel the thread that completed the connection
    if (connectThread != null) {
      connectThread.cancel();
      connectThread = null;
    }

    // Cancel any thread currently running a connection
    if (connectedThread != null) {
      connectedThread.cancel();
      connectedThread = null;
    }

    // Start the thread to manage the connection and perform transmissions
    connectedThread = new ConnectedThread(socket);
    connectedThread.start();

    // Send the name of the connected device back to the UI Activity
    Message msg = handler.obtainMessage(MESSAGE_DEVICE_NAME);
    Bundle bundle = new Bundle();
    bundle.putString(DEVICE_NAME, device.getName());
    msg.setData(bundle);
    handler.sendMessage(msg);

    setState(Sensor.SensorState.CONNECTED);
  }

  /**
   * Stop all threads
   */
  public synchronized void stop() {
    Log.d(Constants.TAG, "stop()");
    if (connectThread != null) {
      connectThread.cancel();
      connectThread = null;
    }
    if (connectedThread != null) {
      connectedThread.cancel();
      connectedThread = null;
    }
    setState(Sensor.SensorState.NONE);
  }

  /**
   * Write to the ConnectedThread in an unsynchronized manner
   *
   * @param out The bytes to write
   * @see ConnectedThread#write(byte[])
   */
  public void write(byte[] out) {
    // Create temporary object
    ConnectedThread r;
    // Synchronize a copy of the ConnectedThread
    synchronized (this) {
      if (state != Sensor.SensorState.CONNECTED) {
        return;
      }
      r = connectedThread;
    }
    // Perform the write unsynchronized
    r.write(out);
  }

  /**
   * Indicate that the connection attempt failed and notify the UI Activity.
   */
  private void connectionFailed() {
    setState(Sensor.SensorState.DISCONNECTED);
    Log.i(Constants.TAG, "Bluetooth connection failed.");
  }

  /**
   * Indicate that the connection was lost and notify the UI Activity.
   */
  private void connectionLost() {
    setState(Sensor.SensorState.DISCONNECTED);
    Log.i(Constants.TAG, "Bluetooth connection lost.");
  }

  /**
   * This thread runs while attempting to make an outgoing connection with a
   * device. It runs straight through; the connection either succeeds or fails.
   */
  private class ConnectThread extends Thread {
    private final BluetoothSocket socket;
    private final BluetoothDevice device;

    public ConnectThread(BluetoothDevice device) {
      setName("ConnectThread-" + device.getName());
      this.device = device;
      BluetoothSocket tmp = null;

      // Get a BluetoothSocket for a connection with the
      // given BluetoothDevice
      try {
        tmp = getSocket();
      } catch (IOException e) {
        Log.e(Constants.TAG, "create() failed", e);
      }
      socket = tmp;
    }

    private BluetoothSocket getSocket() throws IOException {
      try {
        return device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
      } catch (IOException e) {
        Log.e(Constants.TAG, "Unable to get insecure connect.", e);
      }
      return device.createRfcommSocketToServiceRecord(SPP_UUID);
    }

    @Override
    public void run() {
      Log.d(Constants.TAG, "BEGIN mConnectThread");

      // Always cancel discovery because it will slow down a connection
      adapter.cancelDiscovery();

      // Make a connection to the BluetoothSocket
      try {
        // This is a blocking call and will only return on a
        // successful connection or an exception
        socket.connect();
      } catch (IOException e) {
        connectionFailed();
        // Close the socket
        try {
          socket.close();
        } catch (IOException e2) {
          Log.e(Constants.TAG,
              "unable to close() socket during connection failure", e2);
        }
        // Start the service over to restart listening mode
        BluetoothConnectionManager.this.start();
        return;
      }

      // Reset the ConnectThread because we're done
      synchronized (BluetoothConnectionManager.this) {
        connectThread = null;
      }

      // Start the connected thread
      connected(socket, device);
    }

    public void cancel() {
      try {
        socket.close();
      } catch (IOException e) {
        Log.e(Constants.TAG, "close() of connect socket failed", e);
      }
    }
  }

  /**
   * This thread runs during a connection with a remote device. It handles all
   * incoming and outgoing transmissions.
   */
  private class ConnectedThread extends Thread {
    private final BluetoothSocket btSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket) {
      Log.d(Constants.TAG, "create ConnectedThread");
      btSocket = socket;
      InputStream tmpIn = null;
      OutputStream tmpOut = null;

      // Get the BluetoothSocket input and output streams
      try {
        tmpIn = socket.getInputStream();
        tmpOut = socket.getOutputStream();
      } catch (IOException e) {
        Log.e(Constants.TAG, "temp sockets not created", e);
      }

      mmInStream = tmpIn;
      mmOutStream = tmpOut;
    }

    @Override
    public void run() {
      Log.i(Constants.TAG, "BEGIN mConnectedThread");
      byte[] buffer = new byte[parser.getFrameSize()];
      int bytes;
      int offset = 0;

      // Keep listening to the InputStream while connected
      while (true) {
        try {
          // Read from the InputStream
          bytes = mmInStream.read(buffer, offset, parser.getFrameSize() - offset);

          if (bytes < 0) {
            throw new IOException("EOF reached");
          }

          offset += bytes;

          if (offset != parser.getFrameSize()) {
            // partial frame received, call read() again to receive the rest
            continue;
          }

          // check if its a valid frame
          if (!parser.isValid(buffer)) {
            int index = parser.findNextAlignment(buffer);
            if (index > 0) {
              // re-align
              offset = parser.getFrameSize() - index;
              System.arraycopy(buffer, index, buffer, 0, offset);
              Log.w(Constants.TAG, "Misaligned data, found new message at " +
                   index + " recovering...");
              continue;
            }
            Log.w(Constants.TAG, "Could not find valid data, dropping data");
            offset = 0;
            continue;
          }

          offset = 0;

          // Send copy of the obtained bytes to the UI Activity.
          // Avoids memory inconsistency issues.
          handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer.clone())
              .sendToTarget();
        } catch (IOException e) {
          Log.e(Constants.TAG, "disconnected", e);
          connectionLost();
          break;
        }
      }
    }

    /**
     * Write to the connected OutStream.
     *
     * @param buffer The bytes to write
     */
    public void write(byte[] buffer) {
      try {
        mmOutStream.write(buffer);

        // Share the sent message back to the UI Activity
        handler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
      } catch (IOException e) {
        Log.e(Constants.TAG, "Exception during write", e);
      }
    }

    public void cancel() {
      try {
        btSocket.close();
      } catch (IOException e) {
        Log.e(Constants.TAG, "close() of connect socket failed", e);
      }
    }
  }
}
