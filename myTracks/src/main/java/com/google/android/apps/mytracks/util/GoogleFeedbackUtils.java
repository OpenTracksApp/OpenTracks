/*
 * Copyright 2013 Google Inc.
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

/**
 * Utilities for Google Feedback.
 * 
 * @author Jimmy Shih
 */
public class GoogleFeedbackUtils {

  private static final String TAG = GoogleFeedbackUtils.class.getSimpleName();

  private GoogleFeedbackUtils() {}

  /**
   * Binds the Google Feedback service.
   * 
   * @param context the context
   */
  public static void bindFeedback(Context context) {
    Intent intent = new Intent(Intent.ACTION_BUG_REPORT);
    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        try {
          service.transact(Binder.FIRST_CALL_TRANSACTION, Parcel.obtain(), null, 0);
        } catch (RemoteException e) {
          Log.e(TAG, "RemoteException", e);
        }
      }

        @Override
      public void onServiceDisconnected(ComponentName name) {}
    };
    // Bind to the service after creating it if necessary
    context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
  }
}
