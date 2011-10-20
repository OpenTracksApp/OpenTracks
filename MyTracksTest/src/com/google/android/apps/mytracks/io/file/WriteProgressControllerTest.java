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

import android.app.ProgressDialog;
import android.test.ActivityInstrumentationTestCase2;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link WriteProgressController}.
 *
 * @author Matthew Simmons
 */
public class WriteProgressControllerTest extends ActivityInstrumentationTestCase2<SaveActivity> {
  public WriteProgressControllerTest() {
    super(SaveActivity.class);
  }

  private static void assertProgress(ProgressDialog dialog, int expectedProgress,
      int expectedMax) {
    assertEquals(expectedProgress, dialog.getProgress());
    assertEquals(expectedMax, dialog.getMax());
  }

  public void testSimple() throws Exception {
    final AtomicReference<ProgressDialog> dialogRef = new AtomicReference<ProgressDialog>();
    final AtomicReference<Boolean> controllerDoneRef = new AtomicReference<Boolean>();
    final Semaphore writerDone = new Semaphore(0);

    TrackWriter mockWriter = new MockTrackWriter() {
      @Override
      public void writeTrackAsync() {
        onWriteListener.onWrite(1000, 10000);
        assertProgress(dialogRef.get(), 1000, 10000);

        onCompletionListener.onComplete();
        writerDone.release();
      }
    };

    final WriteProgressController controller = new WriteProgressController(
        getActivity(), mockWriter, SaveActivity.PROGRESS_DIALOG);
    controller.setOnCompletionListener(new WriteProgressController.OnCompletionListener() {
      @Override
      public void onComplete() {
        controllerDoneRef.set(true);
      }
    });

    /*
     * The WriteProgressController constructor calls the mockWriter's
     * setOnCompletionListener method with a listener that dismisses the
     * progress dialog. However, this unit test only tests the
     * WriteProgressController and doesn't actually show any progress dialog.
     * Thus after the WriteProgressController is setup, we need to call the
     * mockWriter's setOnCompletionListener method again with an listener that
     * doesn't dismiss dialog.
     */
    mockWriter.setOnCompletionListener(new TrackWriter.OnCompletionListener() {
      @Override
      public void onComplete() {
        controller.getOnCompletionListener().onComplete();
      }
    });

    dialogRef.set(controller.createProgressDialog());

    controller.startWrite();

    // wait for the writer to finish
    writerDone.acquire();

    assertTrue(controllerDoneRef.get());
  }
}
