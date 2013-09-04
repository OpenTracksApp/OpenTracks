/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.mytracks.io.file.export;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Abstract class for {@link TrackExporter}.
 * 
 * @author Jimmy Shih
 */
abstract class AbstractTrackExporter implements TrackExporter {

  private static final String TAG = AbstractTrackExporter.class.getSimpleName();

  private Thread writeThread;
  private boolean success = false;

  @Override
  public void writeTrack(final OutputStream outputStream) {
    writeThread = new Thread() {
        @Override
      public void run() {
        try {
          performWrite(outputStream);
          success = true;
        } catch (InterruptedException e) {
          Log.e(TAG, "Unable to perform write", e);
          success = false;
        } catch (IOException e) {
          Log.e(TAG, "Unable to perform write", e);
          success = false;
        }
      }
    };
    writeThread.start();
    try {
      writeThread.join();
    } catch (InterruptedException e) {
      Log.e(TAG, "Interrupted while waiting for write thread to finish", e);
      success = false;
    }
  }

  @Override
  public boolean isSuccess() {
    return success;
  }

  abstract void performWrite(OutputStream outputStream) throws InterruptedException, IOException;
}
