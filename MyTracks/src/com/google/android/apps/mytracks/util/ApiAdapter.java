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

import com.google.android.apps.mytracks.io.backup.BackupPreferencesListener;
import com.google.android.apps.mytracks.services.tasks.PeriodicTask;
import com.google.api.client.http.HttpTransport;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

/**
 * A set of methods that may be implemented differently depending on the Android
 * API level.
 *
 * @author Bartlomiej Niechwiej
 */
public interface ApiAdapter {

  /**
   * Gets a status announcer task.
   * <p>
   * Due to changes in API level 8.
   * 
   * @param context the context
   */
  public PeriodicTask getStatusAnnouncerTask(Context context);

  /**
   * Gets a {@link BackupPreferencesListener}.
   * <p>
   * Due to changes in API level 8.
   * 
   * @param context the context
   */
  public BackupPreferencesListener getBackupPreferencesListener(Context context);

  /**
   * Applies all the changes done to a given preferences editor. Changes may or
   * may not be applied immediately.
   * <p>
   * Due to changes in API level 9.
   * 
   * @param editor the editor
   */
  public void applyPreferenceChanges(SharedPreferences.Editor editor);

  /**
   * Enables strict mode where supported, only if this is a development build.
   * <p>
   * Due to changes in API level 9.
   */
  public void enableStrictMode();

  /**
   * Copies elements from an input byte array into a new byte array, from
   * indexes start (inclusive) to end (exclusive). The end index must be less
   * than or equal to the input length.
   * <p>
   * Due to changes in API level 9.
   *
   * @param input the input byte array
   * @param start the start index
   * @param end the end index
   * @return a new array containing elements from the input byte array.
   */
  public byte[] copyByteArray(byte[] input, int start, int end);

  /**
   * Gets a {@link HttpTransport}.
   * <p>
   * Due to changes in API level 9.
   */
  public HttpTransport getHttpTransport();

  /**
   * Gets a {@link BluetoothSocket}.
   * <p>
   * Due to changes in API level 10.
   *
   * @param bluetoothDevice
   */
  public BluetoothSocket getBluetoothSocket(BluetoothDevice bluetoothDevice) throws IOException;
}
