// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.services;

import android.app.PendingIntent;
import android.app.Service;
import android.test.ServiceTestCase;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A {@link TrackRecordingService} that can be used with
 * {@link ServiceTestCase}. {@link ServiceTestCase} throws a null pointer
 * exception when the service calls
 * {@link Service#startForeground(int, android.app.Notification)} and
 * {@link Service#stopForeground(boolean)}.
 * <p>
 * See http://code.google.com/p/android/issues/detail?id=12122
 * <p>
 * Wrap these two methods in wrappers and override them.
 * 
 * @author Jimmy Shih
 */
public class TestRecordingService extends TrackRecordingService {

  private static final String TAG = TestRecordingService.class.getSimpleName();

  @Override
  protected void startForegroundService(PendingIntent pendingIntent, int messageId) {
    try {
      Method setForegroundMethod = Service.class.getMethod("setForeground", boolean.class);
      setForegroundMethod.invoke(this, true);
    } catch (SecurityException e) {
      Log.e(TAG, "Unable to start a service in foreground", e);
    } catch (NoSuchMethodException e) {
      Log.e(TAG, "Unable to start a service in foreground", e);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "Unable to start a service in foreground", e);
    } catch (IllegalAccessException e) {
      Log.e(TAG, "Unable to start a service in foreground", e);
    } catch (InvocationTargetException e) {
      Log.e(TAG, "Unable to start a service in foreground", e);
    }
  }

  @Override
  protected void stopForegroundService() {
    try {
      Method setForegroundMethod = Service.class.getMethod("setForeground", boolean.class);
      setForegroundMethod.invoke(this, false);
    } catch (SecurityException e) {
      Log.e(TAG, "Unable to start a service in foreground", e);
    } catch (NoSuchMethodException e) {
      Log.e(TAG, "Unable to start a service in foreground", e);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "Unable to start a service in foreground", e);
    } catch (IllegalAccessException e) {
      Log.e(TAG, "Unable to start a service in foreground", e);
    } catch (InvocationTargetException e) {
      Log.e(TAG, "Unable to start a service in foreground", e);
    }
  }
}
