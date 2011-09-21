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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The Cupcake (API level 3) specific implementation of the
 * {@link ApiPlatformAdapter}.
 * 
 * @author Bartlomiej Niechwiej
 */
public class CupcakePlatformAdapter implements ApiPlatformAdapter {

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
}
