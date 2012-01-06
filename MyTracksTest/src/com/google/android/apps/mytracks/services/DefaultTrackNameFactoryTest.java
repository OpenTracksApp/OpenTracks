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

import com.google.android.apps.mytracks.util.StringUtils;

import android.content.Context;
import android.test.AndroidTestCase;

/**
 * Tests {@link DefaultTrackNameFactory}
 * 
 * @author Matthew Simmons
 */
public class DefaultTrackNameFactoryTest extends AndroidTestCase {
  /**
   * A version of the factory which allows us to supply our own answer as to
   * whether a timestamp-based track name should be used.
   */
  private static class MockDefaultTrackNameFactory
      extends DefaultTrackNameFactory {
    private final boolean useTimestamp;

    MockDefaultTrackNameFactory(Context context, boolean useTimestamp) {
      super(context);
      this.useTimestamp = useTimestamp;
    }
    
    @Override
    protected boolean useTimestampTrackName() {
      return useTimestamp;
    }
  }
  
  private static final long TIMESTAMP = 1288213406000L;

  public void testTimestampTrackName() {
    DefaultTrackNameFactory factory =
        new MockDefaultTrackNameFactory(getContext(), true);

    assertEquals(
        StringUtils.formatDateTime(getContext(), TIMESTAMP), factory.newTrackName(1, TIMESTAMP));
  }

  public void testIncrementingTrackName() {
    DefaultTrackNameFactory factory =
        new MockDefaultTrackNameFactory(getContext(), false);

    assertEquals("Track 1", factory.newTrackName(1, TIMESTAMP));
  }
}
