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

package com.google.android.apps.mytracks.services.tasks;

import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.services.TrackRecordingService;

import android.content.Context;


/**
 * A simple task to insert statistics markers periodically.
 * @author Sandor Dornbush
 */
public class SplitTask implements PeriodicTask {

  private SplitTask() {
  }

  @Override
  public void run(TrackRecordingService service) {
    service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
  }

  @Override
  public void shutdown() {
  }

  @Override
  public void start() {
  }
  
  /**
   * Create new SplitTasks.
   */
  public static class Factory implements PeriodicTaskFactory {

    @Override
    public PeriodicTask create(Context context) {
      return new SplitTask();
    }
  }
}
