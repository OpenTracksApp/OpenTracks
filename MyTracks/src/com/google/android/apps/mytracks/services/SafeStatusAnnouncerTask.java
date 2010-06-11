/*
 * Copyright 2009 Google Inc.
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

import android.content.Context;

/**
 * This class will periodically announce the user's trip statitics. This class
 * is a wrapper for cupcake devices.
 * 
 * @author Sandor Dornbush
 */
public class SafeStatusAnnouncerTask implements PeriodicTask {

  private StatusAnnouncerTask announcer;

  /* class initialization fails when this throws an exception */
  static {
    try {
      Class.forName(
          "com.google.android.apps.mytracks.services.StatusAnnouncerTask");
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    } catch (LinkageError er) {
      throw new RuntimeException(er);
    }
  }

  /* calling here forces class initialization */
  public static void checkAvailable() {
  }

  public SafeStatusAnnouncerTask(Context context) {
    announcer = new StatusAnnouncerTask(context);
  }

  public void run(TrackRecordingService service) {
    announcer.run(service);
  }

  public void shutdown() {
    announcer.shutdown();
  }

  @Override
  public void start() {
  }
}
