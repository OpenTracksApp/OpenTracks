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
package com.google.android.apps.mytracks.signalstrength.test;

import com.google.android.apps.mytracks.signalstrength.SignalStrengthListener;
import com.google.android.apps.mytracks.signalstrength.SignalStrengthListener.SignalStrengthCallback;
import com.google.android.apps.mytracks.signalstrength.SignalStrengthListenerCupcake;
import com.google.android.apps.mytracks.signalstrength.SignalStrengthListenerEclair;
import com.google.android.apps.mytracks.signalstrength.SignalStrengthListenerFactory;
import com.google.android.testing.mocking.AndroidMock;

import android.test.AndroidTestCase;

/**
 * Tests for {@link SignalStrengthListenerFactory}.
 * These tests require Eclair+ (API level 7) to run.
 *
 * @author Rodrigo Damazio
 */
public class SignalStrengthListenerFactoryTest extends AndroidTestCase {
  private boolean hasModernSignalStrength;
  private TestableSignalStrengthListenerFactory factory;
  private SignalStrengthCallback callback;

  private class TestableSignalStrengthListenerFactory extends SignalStrengthListenerFactory {
    @Override
    protected boolean hasModernSignalStrength() {
      return hasModernSignalStrength;
    }
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    factory = new TestableSignalStrengthListenerFactory();
    callback = AndroidMock.createMock(SignalStrengthCallback.class);
  }

  public void testCreate_eclair() {
    hasModernSignalStrength = true;

    SignalStrengthListener listener = factory.create(getContext(), callback);
    assertTrue(listener.getClass().getName(),
        listener instanceof SignalStrengthListenerEclair);
  }

  public void testCreate_legacy() {
    hasModernSignalStrength = false;

    SignalStrengthListener listener = factory.create(getContext(), callback);
    assertTrue(listener.getClass().getName(),
        listener instanceof SignalStrengthListenerCupcake);
  }
}
