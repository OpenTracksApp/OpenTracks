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

import android.content.Context;
import android.test.AndroidTestCase;

/**
 * Tests {@link DefaultCategoryFactory}
 * 
 * @author Rimas Trumpa
 */
public class DefaultCategoryFactoryTest extends AndroidTestCase {
  /**
   * A version of the factory which allows us to supply our own default activity
   * type.
   */
  private static class MockDefaultCategoryFactory extends DefaultCategoryFactory {
    private final String defaultCategory;

    MockDefaultCategoryFactory(Context context, String defaultCategory) {
      super(context);
      this.defaultCategory = defaultCategory;
    }

    @Override
    protected String getDefaultCategory() {
      return defaultCategory;
    }
  }

  public void testCategoryName() {
    DefaultCategoryFactory factory = new MockDefaultCategoryFactory(getContext(), "foo");
    assertEquals("foo", factory.newTrackCategory());
  }

  public void testNoDefaultCategoryName() {
    DefaultCategoryFactory factory = new MockDefaultCategoryFactory(getContext(), null);
    assertEquals("", factory.newTrackCategory());
  }
}
