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
package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.io.file.MockTrackWriter;
import com.google.android.apps.mytracks.io.file.TrackWriter;

import android.app.ProgressDialog;
import android.test.ActivityInstrumentationTestCase2;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

public class WriteProgressControllerTest extends ActivityInstrumentationTestCase2<MyTracks> {
  public WriteProgressControllerTest() {
    super(MyTracks.class);
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

    WriteProgressController controller = new WriteProgressController(
        getActivity(), mockWriter);
    controller.setOnCompletionListener(new WriteProgressController.OnCompletionListener() {
      @Override
      public void onComplete(TrackWriter writer) {
        controllerDoneRef.set(true);
      }
    });

    dialogRef.set(controller.getDialog());

    controller.startWrite();

    // wait for the writer to finish
    writerDone.acquire();

    assertFalse(dialogRef.get().isShowing());
    assertTrue(controllerDoneRef.get());
  }
}
