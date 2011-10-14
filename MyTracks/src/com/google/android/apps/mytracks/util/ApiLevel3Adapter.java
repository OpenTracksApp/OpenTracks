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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.io.backup.BackupPreferencesListener;
import com.google.android.apps.mytracks.services.tasks.PeriodicTask;
import com.google.android.apps.mytracks.services.tasks.StatusAnnouncerTask;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * API level 3 specific implementation of the {@link ApiLevelAdapter}.
 *
 * @author Bartlomiej Niechwiej
 */
public class ApiLevel3Adapter implements ApiLevelAdapter {

  @Override
  public void startForeground(Service service,
      NotificationManager notificationManager, int id,
      Notification notification) {
    setServiceForeground(service, true);

    notificationManager.notify(id, notification);
  }

  @Override
  public void stopForeground(Service service,
      NotificationManager notificationManager, int id) {
    setServiceForeground(service, false);
    if (id != -1) {
      notificationManager.cancel(id);
    }
  }
  
  @Override
  public PeriodicTask getPeriodicTask(Context context) {
    return new StatusAnnouncerTask(context);
  }

  @Override
  public BackupPreferencesListener getBackupPreferencesListener(Context context) {
    return new BackupPreferencesListener() {
      @Override
      public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        
        // Do nothing
      }
    };
  }

  private void setServiceForeground(Service service, boolean foreground) {
    // setForeground has been completely removed in API level 11, so we use reflection.
    try {
      Method setForegroundMethod = Service.class.getMethod("setForeground", boolean.class);
      setForegroundMethod.invoke(service, foreground);
    } catch (SecurityException e) {
      Log.e(TAG, "Unable to set service foreground state", e);
    } catch (NoSuchMethodException e) {
      Log.e(TAG, "Unable to set service foreground state", e);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "Unable to set service foreground state", e);
    } catch (IllegalAccessException e) {
      Log.e(TAG, "Unable to set service foreground state", e);
    } catch (InvocationTargetException e) {
      Log.e(TAG, "Unable to set service foreground state", e);
    }
  }

  @Override
  public void applyPreferenceChanges(Editor editor) {
    editor.commit();
  }

  @Override
  public void enableStrictMode() {
    // Not supported
  }
  
  @Override
  public byte[] copyByteArray(byte[] input, int start, int end) {
    int length = end - start;
    byte[] output = new byte[length];
    System.arraycopy(input, start, output, 0, length);
    return output;
  }
  
  @Override
  public DecimalFormatSymbols getDecimalFormatSymbols(Locale locale) {
    return new DecimalFormatSymbols(locale);
  }

  @Override
  public HttpTransport getHttpTransport() {
    return new ApacheHttpTransport();
  }
}
