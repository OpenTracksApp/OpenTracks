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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;

/**
 * The Eclair (API level 5) specific implementation of the
 * {@link ApiPlatformAdapter}.
 * 
 * @author Bartlomiej Niechwiej
 */
public class EclairPlatformAdapter extends CupcakePlatformAdapter {

  @Override
  public void startForeground(Service service,
      NotificationManager notificationManager, int id,
      Notification notification) {
    service.startForeground(id, notification);
  }

  @Override
  public void stopForeground(Service service,
      NotificationManager notificationManager, int id) {
    service.stopForeground(id != -1);
  }
}
