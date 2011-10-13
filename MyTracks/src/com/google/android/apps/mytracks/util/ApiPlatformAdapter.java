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
import android.content.SharedPreferences;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * A set of methods that may be implemented in a platform specific way. 
 *  
 * @author Bartlomiej Niechwiej
 */
public interface ApiPlatformAdapter {
  
  /**
   * Puts the specified service into foreground.
   * 
   * @param service the service to be put in foreground.
   * @param notificationManager the notification manager used to post the given
   *        notification.
   * @param id the ID of the notification, unique within the application.
   * @param notification the notification to post.
   */
  void startForeground(Service service, NotificationManager notificationManager,
      int id, Notification notification);
  
  /**
   * Puts the given service into background.
   * @param service the service to put into background.
   * @param notificationManager the notification manager to user when removing
   *        notifications. 
   * @param id the ID of the notification to be remove, or -1 if the
   *        notification shouldn't be removed.
   */
  void stopForeground(Service service, NotificationManager notificationManager,
      int id);

  /**
   * Applies all changes done to the given preferences editor.
   * Changes may or may not be applied immediately.
   */
  void applyPreferenceChanges(SharedPreferences.Editor editor);
  
  /**
   * Enables strict mode where supported, only if this is a development build.
   */
  void enableStrictMode();
  
  /**
   * Copies elements from the input byte array into a new byte array, from
   * indexes start (inclusive) to end (exclusive). The end index must be less
   * than or equal to input.length.
   *
   * @param input the input byte array
   * @param start the start index
   * @param end the end index
   * @return a new array containing elements from the input byte array
   */
  byte[] copyByteArray(byte[] input, int start, int end);
  
  
  /**
   * Gets an instance of {@link DecimalFormatSymbols} for a locale.
   * 
   * @param locale the locale
   * @return a new instance of {@link DecimalFormatSymbols}
   */
  DecimalFormatSymbols getDecimalFormatSymbols(Locale locale);
}
