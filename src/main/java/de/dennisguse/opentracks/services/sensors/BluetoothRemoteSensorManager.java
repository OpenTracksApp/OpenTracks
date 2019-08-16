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

package de.dennisguse.opentracks.services.sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.content.sensor.SensorState;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * Bluetooth sensor manager.
 * <p>
 * TODO: Handle a BluetoothGatt.STATE_DISCONNECTED
 *
 * @author Sandor Dornbush
 */
public class BluetoothRemoteSensorManager extends RemoteSensorManager {

    private static final String TAG = BluetoothConnectionManager.class.getSimpleName();
    private static final BluetoothAdapter bluetoothAdapter = getDefaultBluetoothAdapter();
    private final Context context;
    private final BluetoothConnectionManager bluetoothConnectionManager;
    private SensorDataSet sensorDataSet = null;
    // Handler that gets information back from the bluetoothConnectionManager
    private final Handler messageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case BluetoothConnectionManager.MESSAGE_DEVICE_NAME:
                    String deviceName = message.getData().getString(BluetoothConnectionManager.KEY_DEVICE_NAME);
                    break;
                case BluetoothConnectionManager.MESSAGE_READ:
                    if (!(message.obj instanceof SensorDataSet)) {
                        Log.e(TAG, "Received message did not contain a SensorDataSet.");
                        sensorDataSet = null;
                    } else {
                        sensorDataSet = (SensorDataSet) message.obj;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * @param context the context
     */
    public BluetoothRemoteSensorManager(Context context) {
        this.context = context;
        bluetoothConnectionManager = new BluetoothConnectionManager(context, messageHandler);
    }

    private static BluetoothAdapter getDefaultBluetoothAdapter() {
        // If from the main application thread, return directly
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            return BluetoothAdapter.getDefaultAdapter();
        }

        // Get the default adapter from the main application thread
        final ArrayList<BluetoothAdapter> adapters = new ArrayList<>(1);
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
                    mutex.wait(UnitConversions.ONE_SECOND);
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
        String address = PreferencesUtils.getString(context, R.string.bluetooth_sensor_key, PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT);
        if (PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT.equals(address)) {
            Log.w(TAG, "No bluetooth address.");
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
