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
package com.google.android.apps.mytracks.signalstrength;

/**
 * Interface for the service that reads signal strength.
 *
 * @author Rodrigo Damazio
 */
public interface SignalStrengthListener {
  /**
   * Interface for getting notified about a new sampled signal strength.
   */
  public interface SignalStrengthCallback {
    void onSignalStrengthSampled(
        String description,
        String icon);
  }

  /**
   * Starts listening to signal strength.
   */
  void register();

  /**
   * Stops listening to signal strength.
   */
  void unregister();
}
