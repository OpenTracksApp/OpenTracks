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
package com.google.android.apps.mytracks.io.file;

import java.io.File;

/**
 * A simple, fake {@link TrackWriter} subclass with all methods mocked out.
 * Tests are expected to override {@link #writeTrack}.
 *
 * @author Matthew Simmons
 *
 */
public class MockTrackWriter implements TrackWriter {
  public OnWriteListener onWriteListener;

  @Override
  public void setOnWriteListener(OnWriteListener onWriteListener) {
    this.onWriteListener = onWriteListener;
  }

  @Override
  public void setDirectory(File directory) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public String getAbsolutePath() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void writeTrack() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void stopWriteTrack() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public boolean wasSuccess() {
    return false;
  }

  @Override
  public int getErrorMessage() {
    return 0;
  }
}
