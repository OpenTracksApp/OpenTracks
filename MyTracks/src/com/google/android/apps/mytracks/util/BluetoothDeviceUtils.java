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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

import java.util.List;
import java.util.Set;

/**
 * Utilities for dealing with bluetooth devices.
 * 
 * @author Rodrigo Damazio
 */
public class BluetoothDeviceUtils {

  private BluetoothDeviceUtils() {}

  /**
   * Populates the device names and the device addresses with all the suitable
   * bluetooth devices.
   * 
   * @param bluetoothAdapter the bluetooth adapter
   * @param deviceNames list of device names
   * @param deviceAddresses list of device addresses
   */
  public static void populateDeviceLists(
      BluetoothAdapter bluetoothAdapter, List<String> deviceNames, List<String> deviceAddresses) {
    // Ensure the bluetooth adapter is not in discovery mode.
    bluetoothAdapter.cancelDiscovery();

    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
    for (BluetoothDevice device : pairedDevices) {
      BluetoothClass bluetoothClass = device.getBluetoothClass();
      if (bluetoothClass != null) {
        // Not really sure what we want, but I know what we don't want.
        switch (bluetoothClass.getMajorDeviceClass()) {
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
}
