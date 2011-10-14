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

import junit.framework.TestCase;

/**
 * Tests the API feature detection code in {@link ApiFeatures}.
 * This test requires Froyo+ to run.
 *
 * @author Rodrigo Damazio
 */
public class ApiFeaturesTest extends TestCase {

  private TestableApiFeatures features;

  private class TestableApiFeatures extends ApiFeatures {
    private int apiLevel;

    public void setApiLevel(int apiLevel) {
      this.apiLevel = apiLevel;
    }

    @Override
    protected int getApiLevel() {
      return apiLevel;
    }
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    features = new TestableApiFeatures();
  }

  public void testHasBackup() {
    for (int i = 3; i <= 7; i++) {
      features.setApiLevel(i);
      assertFalse(features.hasBackup());
    }
    features.setApiLevel(8);
    assertTrue(features.hasBackup());
  }

  public void testHasTextToSpeech() {
    features.setApiLevel(3);
    assertFalse(features.hasTextToSpeech());
    for (int i = 4; i <= 8; i++) {
      features.setApiLevel(i);
      assertTrue(features.hasTextToSpeech());
    }
  }

  public void testGetApiAdapter() {
    assertNotNull(features.getApiAdapter());
  }
}
