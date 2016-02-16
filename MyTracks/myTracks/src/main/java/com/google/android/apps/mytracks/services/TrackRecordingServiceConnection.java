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

import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.maps.mytracks.BuildConfig;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;

/**
 * Wrapper for the track recording service. This handles service
 * start/bind/unbind/stop. The service must be started before it can be bound.
 * Returns the service if it is started and bound.
 * 
 * @author Rodrigo Damazio
 */
public class TrackRecordingServiceConnection {

  private static final String TAG = TrackRecordingServiceConnection.class.getSimpleName();

  private final DeathRecipient deathRecipient = new DeathRecipient() {
      @Override
    public void binderDied() {
      Log.d(TAG, "Service died.");
      setTrackRecordingService(null);
    }
  };

  private final ServiceConnection serviceConnection = new ServiceConnection() {
      @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      Log.i(TAG, "Connected to the service.");
      try {
        service.linkToDeath(deathRecipient, 0);
      } catch (RemoteException e) {
        Log.e(TAG, "Failed to bind a death recipient.", e);
      }
      setTrackRecordingService(ITrackRecordingService.Stub.asInterface(service));
    }

      @Override
    public void onServiceDisconnected(ComponentName className) {
      Log.i(TAG, "Disconnected from the service.");
      setTrackRecordingService(null);
    }
  };

  private final Context context;
  private final Runnable callback;
  private ITrackRecordingService trackRecordingService;

  /**
   * Constructor.
   * 
   * @param context the context
   * @param callback the callback to invoke when the service binding changes
   */
  public TrackRecordingServiceConnection(Context context, Runnable callback) {
    this.context = context;
    this.callback = callback;
  }

  /**
   * Starts and binds the service.
   */
  public void startAndBind() {
    bindService(true);
  }

  /**
   * Binds the service if it is started.
   */
  public void bindIfStarted() {
    bindService(false);
  }

  /**
   * Unbinds and stops the service.
   */
  public void unbindAndStop() {
    unbind();
    context.stopService(new Intent(context, TrackRecordingService.class));
  }

  /**
   * Unbinds the service (but leave it running).
   */
  public void unbind() {
    try {
      context.unbindService(serviceConnection);
    } catch (IllegalArgumentException e) {
      // Means not bound to the service. OK to ignore.
    }
    setTrackRecordingService(null);
  }

  /**
   * Gets the track recording service if bound. Returns null otherwise
   */
  public ITrackRecordingService getServiceIfBound() {
    if (trackRecordingService != null && !trackRecordingService.asBinder().isBinderAlive()) {
      setTrackRecordingService(null);
      return null;
    }
    return trackRecordingService;
  }

  /**
   * Binds the service if it is started.
   * 
   * @param startIfNeeded start the service if needed
   */
  private void bindService(boolean startIfNeeded) {
    if (trackRecordingService != null) {
      // Service is already started and bound.
      return;
    }

    if (!startIfNeeded
        && !TrackRecordingServiceConnectionUtils.isRecordingServiceRunning(context)) {
      Log.d(TAG, "Service is not started. Not binding it.");
      return;
    }

    if (startIfNeeded) {
      Log.i(TAG, "Starting the service.");
      context.startService(new Intent(context, TrackRecordingService.class));
    }

    Log.i(TAG, "Binding the service.");
    int flags = BuildConfig.DEBUG ? Context.BIND_DEBUG_UNBIND : 0;
    context.bindService(new Intent(context, TrackRecordingService.class), serviceConnection, flags);
  }

  /**
   * Sets the trackRecordingService.
   * 
   * @param value the value
   */
  private void setTrackRecordingService(ITrackRecordingService value) {
    trackRecordingService = value;
    if (callback != null) {
      callback.run();
    }
  }
}
