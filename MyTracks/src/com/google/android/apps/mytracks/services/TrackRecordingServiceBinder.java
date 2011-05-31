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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.SystemUtils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.WeakHashMap;

/**
 * A helper for managing the binding to the track recording service.
 * This uses reference counting so multiple callers can share a binding.
 *
 * @author Rodrigo Damazio
 */
public class TrackRecordingServiceBinder {
  /** Singleton instance. */
  private static TrackRecordingServiceBinder instance;

  private final ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      Log.d(Constants.TAG, "Service now connected.");

      // Delay setting the service until we are done with initialization.
      ITrackRecordingService trackRecordingService =
          ITrackRecordingService.Stub.asInterface(service);
      synchronized (TrackRecordingServiceBinder.this) {
        TrackRecordingServiceBinder.this.trackRecordingService = trackRecordingService;
        try {
          for (Runnable callback : pendingBindCallbacks.keySet()) {
            if (callback != null) {
              callback.run();
            }
          }
        } finally {
          pendingBindCallbacks.clear();
        }
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      Log.d(TAG, "Service now disconnected.");
      synchronized (TrackRecordingServiceBinder.this) {
        trackRecordingService = null;
      }
    }
  };

  /** Pointer to the service, if bound. */
  private ITrackRecordingService trackRecordingService;

  /** Count of bindings to the service. */
  private int bindCount = 0;

  /** Set of callbacks to execute when the service connects. */
  private final WeakHashMap<Runnable, Object> pendingBindCallbacks =
      new WeakHashMap<Runnable, Object>();

  /** Application context. */
  private final Context applicationContext;

  /**
   * Starts the service with the default intent.
   *
   * This should only be called when the service will be put in recording mode,
   * as the service being started means it'll be listening to the GPS.
   * Notice that it is perfectly fine to call {@link #bindService} without calling
   * this at all.
   */
  public void startService() {
    Intent intent = new Intent(applicationContext, TrackRecordingService.class);
    applicationContext.startService(intent);
  }

  /**
   * Binds to the service, and calls the given callback afterwards.
   * Calls to this method should be balanced with calls to {@link #unbindService}.
   * 
   * @param onBindCallback the callback for when the service is connected
   */
  public void bindService(Runnable onBindCallback) {
    synchronized (this) {
      bindCount++;
      if (trackRecordingService != null) {
        if (onBindCallback != null) {
          onBindCallback.run();
        }
        return;
      }

      if (onBindCallback != null) {
        pendingBindCallbacks.put(onBindCallback, null);
      }

      if (bindCount == 1) {
        Intent intent = new Intent(applicationContext, TrackRecordingService.class);
        int flags = SystemUtils.isRelease(applicationContext) ? 0 : Context.BIND_DEBUG_UNBIND; 
        applicationContext.bindService(intent, serviceConnection, flags);
      }
    }
  }

  /**
   * Unbinds from the service.
   *
   * This will unbind from the service, unless another caller is still bound to it.
   * Calls to this method should be balanced with calls to {@link #bindService}.
   */
  public void unbindService() {
    synchronized (this) {
      bindCount--;
      if (bindCount > 0) {
        // Someone else may still be using it.
        return;
      } else if (bindCount < 0) {
        Log.e(TAG, "Unbalanced binding calls.");
        return;
      }

      try {
        applicationContext.unbindService(serviceConnection);
      } catch (IllegalArgumentException e) {
        Log.d(TAG, "Tried unbinding, but service was not registered.", e);
      }
    }
  }

  /**
   * Stops the service, and consequently its GPS listening.
   */
  public void stopService() {
    try {
      applicationContext.stopService(new Intent(applicationContext, TrackRecordingService.class));
    } catch (SecurityException e) {
      Log.e(TAG, "Encountered a security exception when trying to stop service.", e);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    if (bindCount > 0) {
      Log.e(TAG, "Leaked bindings: " + bindCount + " bindings left on finalize.");
    }

    super.finalize();
  }

  /**
   * Returns the connected service instance, or null if not connected.
   */
  public ITrackRecordingService getServiceIfBound() {
    return trackRecordingService;
  }

  /**
   * Returns the singleton instance of this class.
   * Notice that although a context is required, it is safe to call this from
   * multiple different contexts to obtain the same instance.
   *
   * @param context the current context
   * @return the singleton instance
   */
  public synchronized static TrackRecordingServiceBinder getInstance(Context context) {
    if (instance == null) {
      Context applicationContext = context.getApplicationContext();
      instance = new TrackRecordingServiceBinder(applicationContext);
    }
    return instance;
  }

  /**
   * Internal constructor.
   *
   * @param applicationContext the current application (not activity) context
   */
  private TrackRecordingServiceBinder(Context applicationContext) {
    this.applicationContext = applicationContext;
  }
}
