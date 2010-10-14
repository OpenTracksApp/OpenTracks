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
package com.google.android.apps.mytracks.services;

import com.google.android.apps.mytracks.util.ApiFeatures;

import android.test.AndroidTestCase;

/**
 * Tests for {@link SignalStrengthTaskFactoryTest}.
 * These tests require Eclair+ (API level 7) to run.
 *
 * @author Rodrigo Damazio
 */
public class SignalStrengthTaskFactoryTest extends AndroidTestCase {
  /**
   * Mock version of the {@link ApiFeatures} class.
   */
  private static class MockApiFeatures extends ApiFeatures {
    private boolean hasModernStrength;

    public void setHasModernSignalStrength(boolean hasModernStrength) {
      this.hasModernStrength = hasModernStrength;
    }

    @Override
    public boolean hasModernSignalStrength() {
      return hasModernStrength;
    }
  }

  private MockApiFeatures apiFeatures;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    apiFeatures = new MockApiFeatures();
  }

  public void testCreate() {
    apiFeatures.setHasModernSignalStrength(true);
    SignalStrengthTaskFactory factory =
        new SignalStrengthTaskFactory(apiFeatures);

    PeriodicTask task = factory.create(getContext());
    assertTrue(task.getClass().getName(),
        task instanceof SignalStrengthTaskModern);
  }

  public void testCreate_legacy() {
    apiFeatures.setHasModernSignalStrength(false);
    SignalStrengthTaskFactory factory =
        new SignalStrengthTaskFactory(apiFeatures);

    PeriodicTask task = factory.create(getContext());
    assertTrue(task.getClass().getName(), task instanceof SignalStrengthTask);
  }
}
