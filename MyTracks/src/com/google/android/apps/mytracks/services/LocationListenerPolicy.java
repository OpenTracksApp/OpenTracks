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

/**
 * This is an interface for classes that will manage the location listener policy.
 * Different policy options are:
 *   Absolute
 *   Addaptive
 *
 * @author Sandor Dornbush
 */
public interface LocationListenerPolicy {

  /**
   * Returns the polling time this policy would like at this time.
   *
   * @return The polling that this policy dictates
   */
  public long getDesiredPollingInterval();

  /**
   * Returns the minimum distance between updates.
   */
  public int getMinDistance();

  /**
   * Notifies the amount of time the user has been idle at their current
   * location.
   *
   * @param idleTime The time that the user has been idle at this spot
   */
  public void updateIdleTime(long idleTime);
}
