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

  public TrackRecordingServiceConnection(Context context, Runnable bindChangedCallback) {
    this.context = context;
    this.bindChangedCallback = bindChangedCallback;
  }

  public void startAndBind() {
    bindService(true);
  }

  public void bindIfRunning() {
    bindService(false);
  }

  public void stop() {
    unbind();

    Log.d(TAG, "Stopping service");
    Intent intent = new Intent(context, TrackRecordingService.class);
    context.stopService(intent);
  }

  public void unbind() {
    Log.d(TAG, "Unbinding from the service");
    try {
      context.unbindService(serviceConnection);
    } catch (IllegalArgumentException e) {
      // Means we weren't bound, which is ok.
    }

    setBoundService(null);
  }

  public ITrackRecordingService getServiceIfBound() {
    return boundService;
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
