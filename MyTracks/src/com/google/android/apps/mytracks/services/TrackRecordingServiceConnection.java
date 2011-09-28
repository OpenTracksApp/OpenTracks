/*
 * Copyright 2011 Google Inc.
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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.util.SystemUtils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;

/**
 * Wrapper for the connection to the track recording service.
 * This handles connection/disconnection internally, only returning a real
 * service for use if one is available and connected.
 *
 * @author Rodrigo Damazio
 */
public class TrackRecordingServiceConnection {
  private ITrackRecordingService boundService;

  private final DeathRecipient deathRecipient = new DeathRecipient() {
    @Override
    public void binderDied() {
      Log.d(TAG, "Service died");
      setBoundService(null);
    }
  };

  private final ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      Log.i(TAG, "Connected to service");
      try {
        service.linkToDeath(deathRecipient, 0);
      } catch (RemoteException e) {
        Log.e(TAG, "Failed to bind a death recipient", e);
      }

      setBoundService(ITrackRecordingService.Stub.asInterface(service));
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      Log.i(TAG, "Disconnected from service");
      setBoundService(null);
    }
  };

  private final Context context;

  private final Runnable bindChangedCallback;

  /**
   * Constructor.
   *
   * @param context the current context
   * @param bindChangedCallback a callback to be executed when the state of the
   *        service binding changes
   */
  public TrackRecordingServiceConnection(Context context, Runnable bindChangedCallback) {
    this.context = context;
    this.bindChangedCallback = bindChangedCallback;
  }

  /**
   * Binds to the service, starting it if necessary.
   */
  public void startAndBind() {
    bindService(true);
  }

  /**
   * Binds to the service, only if it's already running.
   */
  public void bindIfRunning() {
    bindService(false);
  }

  /**
   * Unbinds from and stops the service.
   */
  public void stop() {
    unbind();

    Log.d(TAG, "Stopping service");
    Intent intent = new Intent(context, TrackRecordingService.class);
    context.stopService(intent);
  }

  /**
   * Unbinds from the service (but leaves it running).
   */
  public void unbind() {
    Log.d(TAG, "Unbinding from the service");
    try {
      context.unbindService(serviceConnection);
    } catch (IllegalArgumentException e) {
      // Means we weren't bound, which is ok.
    }

    setBoundService(null);
  }

  /**
   * Returns the service if connected to it, or null if not connected.
   */
  public ITrackRecordingService getServiceIfBound() {
    checkBindingAlive();

    return boundService;
  }

  private void checkBindingAlive() {
    if (boundService != null &&
        !boundService.asBinder().isBinderAlive()) {
      setBoundService(null);
    }
  }

  private void bindService(boolean startIfNeeded) {
    if (boundService != null) {
      // Already bound.
      return;
    }

    if (!startIfNeeded && !ServiceUtils.isServiceRunning(context)) {
      // Not running, start not requested.
      Log.d(TAG, "Service not running, not binding to it.");
      return;
    }

    if (startIfNeeded) {
      Log.i(TAG, "Starting the service");
      Intent intent = new Intent(context, TrackRecordingService.class);
      context.startService(intent);
    }

    Log.i(TAG, "Binding to the service");
    Intent intent = new Intent(context, TrackRecordingService.class);
    int flags = SystemUtils.isRelease(context) ? 0 : Context.BIND_DEBUG_UNBIND;
    context.bindService(intent, serviceConnection, flags);
  }

  private void setBoundService(ITrackRecordingService service) {
    boundService = service;
    if (bindChangedCallback != null) {
      bindChangedCallback.run();
    }
  }
}
