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
package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.Constants;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.util.Log;

import java.util.List;
import java.util.Set;

/**
 * Utilities for dealing with bluetooth devices.
 * This can be used safely even in systems that don't support bluetooth,
 * in which case a dummy implementation will be used.
 *
 * @author Rodrigo Damazio
 */
public abstract class BluetoothDeviceUtils {
  public static final String ANY_DEVICE = "any";
  private static BluetoothDeviceUtils instance;

  /**
   * Dummy implementation, for systems that don't support bluetooth.
   */
  private static class DummyImpl extends BluetoothDeviceUtils {
    @Override
    public void populateDeviceLists(List<String> deviceNames, List<String> deviceAddresses) {
      // Do nothing - no devices to add
    }

    @Override
    public BluetoothDevice findDeviceMatching(String targetDeviceAddress) {
      return null;
    }
  }

  /**
   * Real implementation, for systems that DO support bluetooth.
   */
  private static class RealImpl extends BluetoothDeviceUtils {
    private final BluetoothAdapter bluetoothAdapter;

    public RealImpl() {
      bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

      if (bluetoothAdapter == null) {
        throw new IllegalStateException("Unable to get bluetooth adapter");
      }
    }

    @Override
    public void populateDeviceLists(List<String> deviceNames, List<String> deviceAddresses) {
      ensureNotDiscovering();

      Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
      for (BluetoothDevice device : pairedDevices) {
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass != null) {
          // Not really sure what we want, but I know what we don't want.
          switch(bluetoothClass.getMajorDeviceClass()) {
            case BluetoothClass.Device.Major.COMPUTER:
            case BluetoothClass.Device.Major.PHONE:
              break;
            default:
              deviceAddresses.add(device.getAddress());
              deviceNames.add(device.getName());
          }
        }
      }
    }

    @Override
    public BluetoothDevice findDeviceMatching(String targetDeviceAddress) {
      if (targetDeviceAddress.equals(ANY_DEVICE)) {
        return findAnyDevice();
      } else {
        return findDeviceByAddress(targetDeviceAddress);
      }
    }

    /**
     * Finds and returns the first suitable bluetooth sensor.
     */
    private BluetoothDevice findAnyDevice() {
      ensureNotDiscovering();

      Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
      for (BluetoothDevice device : pairedDevices) {
        // Look for the first paired computer device
        if (isSuitableDevice(device)) {
          return device;
        }
      }

      return null;
    }

    /**
     * Finds and returns a device with the given address, or null if it's not
     * a suitable sensor.
     */
    private BluetoothDevice findDeviceByAddress(String targetDeviceAddress) {
      ensureNotDiscovering();

      BluetoothDevice device = bluetoothAdapter.getRemoteDevice(targetDeviceAddress);
      if (isSuitableDevice(device)) {
        return device;
      }

      return null;
    }

    /**
     * Ensures the bluetooth adapter is not in discovery mode.
     */
    private void ensureNotDiscovering() {
      // If it's in discovery mode, cancel that for now.
      bluetoothAdapter.cancelDiscovery();
    }

    /**
     * Checks whether the given device is a suitable sensor.
     *
     * @param device the device to check
     * @return true if it's suitable, false otherwise
     */
    private boolean isSuitableDevice(BluetoothDevice device) {
      // Check that the device is bonded
      if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
        return false;
      }

      return true;
    }
  }

  /**
   * Returns the proper (singleton) instance of this class.
   */
  public static BluetoothDeviceUtils getInstance() {
    if (instance == null) {
      if (!isBluetoothMethodSupported()) {
        Log.d(Constants.TAG, "Using dummy bluetooth utils");
        instance = new DummyImpl();
      } else {
        Log.d(Constants.TAG, "Using real bluetooth utils");
        try {
          instance = new RealImpl();
        } catch (IllegalStateException ise) {
          Log.w(Constants.TAG, "Oops, I mean, using dummy bluetooth utils", ise);
          instance = new DummyImpl();
        }
      }
    }
    return instance;
  }

  /**
   * Populates the given lists with the names and addresses of all suitable
   * bluetooth devices.
   *
   * @param deviceNames the list to populate with user-visible names
   * @param deviceAddresses the list to populate with device addresses
   */
  public abstract void populateDeviceLists(List<String> deviceNames, List<String> deviceAddresses);

  /**
   * Finds the bluetooth device with the given address.
   *
   * @param targetDeviceAddress the address of the device, or
   *        {@link #ANY_DEVICE} for using the first suitable device
   * @return the device's descriptor, or null if not found
   */
  public abstract BluetoothDevice findDeviceMatching(String targetDeviceAddress);

  /**
   * @return whether the bluetooth method is supported on this device
   */
  public static boolean isBluetoothMethodSupported() {
    return Integer.parseInt(Build.VERSION.SDK) >= 5;
  }
}
