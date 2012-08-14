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

package com.google.android.apps.mytracks.services.tasks;

import com.google.android.apps.mytracks.services.TrackRecordingService;

/**
 * This is interface for a task that will be executed on some schedule.
 * 
 * @author Sandor Dornbush
 */
public interface PeriodicTask {
  /**
   * Sets up this task for subsequent calls to the run method.
   */
  public void start();

  /**
   * This method will be called periodically.
   * 
   * @param trackRecordingService the track recording service
   */
  public void run(TrackRecordingService trackRecordingService);

  /**
   * Shuts down this task and clean up resources.
   */
  public void shutdown();
}
