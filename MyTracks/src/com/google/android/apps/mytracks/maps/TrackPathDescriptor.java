/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks.maps;

/**
 * A path descriptor interface as template to each type of path descriptor.
 *
 * @author Vangelis S.
 */
public interface TrackPathDescriptor {
  /**
   * @return The speed limit considered as slow.
   */
  int getSlowSpeed();
  
  /**
   * @return The speed limit considered as normal.
   */
  int getNormalSpeed();
  
  /**
   * @return If the path needs to be updated from scratch.
   */
  boolean needsRedraw();
}