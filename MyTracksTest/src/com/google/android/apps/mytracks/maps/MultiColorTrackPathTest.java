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

import android.content.Context;
import android.test.AndroidTestCase;

/**
 * Tests for the {@link MultiColorTrackPath}.
 * 
 * @author Youtao Liu
 */
public class MultiColorTrackPathTest extends AndroidTestCase {

  MultiColorTrackPath multiColorTrackPath;
  Context context;

  @Override
  protected void setUp() throws Exception {
    context = getContext();
    super.setUp();
  }

  /**
   * Tests the {@link MultiColorTrackPath#getColor(int)} when use dynamic speed track path descriptor.
   */
  public void testGetColor_DynamicSpeedTrackPathDescriptor() {
    DynamicSpeedTrackPathDescriptor dynamicSpeedTrackPathDescriptor = new DynamicSpeedTrackPathDescriptor(
        context);
    dynamicSpeedTrackPathDescriptor.setAverageMovingSpeed(50);
    dynamicSpeedTrackPathDescriptor.setSpeedMargin(10);
    multiColorTrackPath = new MultiColorTrackPath(context, dynamicSpeedTrackPathDescriptor);
    
    assertEquals(multiColorTrackPath.getSlowColor(), multiColorTrackPath.getColor(5));
    assertEquals(multiColorTrackPath.getSlowColor(), multiColorTrackPath.getColor(44));
    assertEquals(multiColorTrackPath.getNormalColor(), multiColorTrackPath.getColor(50));
    assertEquals(multiColorTrackPath.getNormalColor(), multiColorTrackPath.getColor(54));
    assertEquals(multiColorTrackPath.getFastColor(), multiColorTrackPath.getColor(56));
    assertEquals(multiColorTrackPath.getFastColor(), multiColorTrackPath.getColor(100));

    assertNotSame(multiColorTrackPath.getSlowColor(), multiColorTrackPath.getNormalColor());
    assertNotSame(multiColorTrackPath.getSlowColor(), multiColorTrackPath.getFastColor());
  }
  
}
