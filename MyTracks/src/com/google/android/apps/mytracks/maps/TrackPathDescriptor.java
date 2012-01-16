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
 * An interface for classes which describe how to draw a track path. 
 *
 * @author Vangelis S.
 */
public interface TrackPathDescriptor {
  /**
   * @return The maximum speed which is considered slow.
   */
  int getSlowSpeed();
  
  /**
   * @return The maximum speed which is considered normal.
   */
  int getNormalSpeed();
  
  /**
   * @return True if the path needs to be updated.
   */
  boolean needsRedraw();
}