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

import com.google.android.apps.mytracks.MockMyTracksOverlay;
import com.google.android.maps.MapView;

import android.graphics.Canvas;
import android.test.AndroidTestCase;

/**
 * Tests for the MyTracks track path descriptors and painters.
 * 
 * @author Vangelis S.
 */
public class TrackPathPainterTestCase extends AndroidTestCase {
  protected Canvas canvas;
  protected MockMyTracksOverlay myTracksOverlay;
  protected MapView mockView;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    canvas = new Canvas();
    myTracksOverlay = new MockMyTracksOverlay(getContext());
    // Enable drawing.
    myTracksOverlay.setTrackDrawingEnabled(true);
    mockView = null;
  }
}
