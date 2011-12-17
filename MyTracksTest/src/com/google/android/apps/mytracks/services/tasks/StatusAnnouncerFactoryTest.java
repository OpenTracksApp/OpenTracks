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
package com.google.android.apps.mytracks.services.tasks;

import com.google.android.apps.mytracks.services.tasks.PeriodicTask;
import com.google.android.apps.mytracks.services.tasks.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.services.tasks.StatusAnnouncerTask;
import com.google.android.apps.mytracks.util.ApiFeatures;

import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.test.AndroidTestCase;

/**
 * Tests for {@link StatusAnnouncerFactory}.
 * These tests require Donut+ to run.
 *
 * @author Rodrigo Damazio
 */
public class StatusAnnouncerFactoryTest extends AndroidTestCase {
  /**
   * Mock version of the {@link ApiFeatures} class.
   */
  private class MockApiFeatures extends ApiFeatures {
    private boolean hasTts;

    public void setHasTextToSpeech(boolean hasTts) {
      this.hasTts = hasTts;
    }

    @Override
    public boolean hasTextToSpeech() {
      return hasTts;
    }
  }

  private MockApiFeatures apiFeatures;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    apiFeatures = new MockApiFeatures();
  }

  public void testCreate() {
    apiFeatures.setHasTextToSpeech(true);
    PeriodicTaskFactory factory =
        new StatusAnnouncerFactory(apiFeatures);
    PeriodicTask task = factory.create(getContext());
    assertTrue(task instanceof StatusAnnouncerTask);
  }

  public void testCreate_notAvailable() {
    apiFeatures.setHasTextToSpeech(false);
    PeriodicTaskFactory factory =
        new StatusAnnouncerFactory(apiFeatures);
    PeriodicTask task = factory.create(getContext());
    assertNull(task);
  }

  public void testGetVolumeStream() {
    apiFeatures.setHasTextToSpeech(true);
    StatusAnnouncerFactory factory =
        new StatusAnnouncerFactory(apiFeatures);
    assertEquals(
        TextToSpeech.Engine.DEFAULT_STREAM,
        factory.getVolumeStream());
  }

  public void testGetVolumeStream_notAvailable() {
    apiFeatures.setHasTextToSpeech(false);
    StatusAnnouncerFactory factory =
        new StatusAnnouncerFactory(apiFeatures);
    assertEquals(
        AudioManager.USE_DEFAULT_STREAM_TYPE,
        factory.getVolumeStream());
  }
}
