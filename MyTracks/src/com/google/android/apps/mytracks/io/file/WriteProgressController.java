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

import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;

/**
 * Given a {@link TrackWriter}, this class manages the process of writing the
 * data in the track represented by the writer.  This includes the display of
 * a progress dialog, updating the progress bar in said dialog, and notifying
 * interested parties when the write completes.
 *
 * @author Matthew Simmons
 */
class WriteProgressController {
  /**
   * This listener is used to notify interested parties when the write has
   * completed.
   */
  public interface OnCompletionListener {
    /**
     * When this method is invoked, the write has completed, and the progress
     * dialog has been dismissed.  Whether the write succeeded can be
     * determined by examining the {@link TrackWriter}.
     */
    public void onComplete();
  }

  private final Activity activity;
  private final TrackWriter writer;
  private ProgressDialog dialog;

  private OnCompletionListener onCompletionListener;
  private final int progressDialogId;

  /**
   * @param activity the activity associated with this write
   * @param writer the writer which writes the track to disk.  Note that this
   *     class will use the writer's completion listener.  If callers are
   *     interested in notification upon completion of the write, they should
   *     use {@link #setOnCompletionListener}.
   */
  public WriteProgressController(Activity activity, TrackWriter writer, int progressDialogId) {
    this.activity = activity;
    this.writer = writer;
    this.progressDialogId = progressDialogId;

    writer.setOnCompletionListener(writerCompleteListener);
    writer.setOnWriteListener(writerWriteListener);
  }

  /** Set a listener to be invoked when the write completes. */
  public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
    this.onCompletionListener = onCompletionListener;
  }
  
  // For testing purpose
  OnCompletionListener getOnCompletionListener() {
    return onCompletionListener;
  }

  public ProgressDialog createProgressDialog() {
    dialog = new ProgressDialog(activity);
    dialog.setIcon(android.R.drawable.ic_dialog_info);
    dialog.setTitle(activity.getString(R.string.progress_title));
    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    dialog.setMessage(activity.getString(R.string.write_progress_message));
    dialog.setIndeterminate(true);
    dialog.setOnCancelListener(dialogCancelListener);
    return dialog;
  }

  /** Initiate an asynchronous write. */
  public void startWrite() {
    activity.showDialog(progressDialogId);
    writer.writeTrackAsync();
  }

  /** VisibleForTesting */
  ProgressDialog getDialog() {
    return dialog;
  }

  private final DialogInterface.OnCancelListener dialogCancelListener =
    new DialogInterface.OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialogInterface) {
        writer.stopWriteTrack();
      }
    };

  private final TrackWriter.OnCompletionListener writerCompleteListener =
      new TrackWriter.OnCompletionListener() {
        @Override
        public void onComplete() {
          activity.dismissDialog(progressDialogId);

          if (onCompletionListener != null) {
            onCompletionListener.onComplete();
          }
        }
      };

  private final TrackWriter.OnWriteListener writerWriteListener =
      new TrackWriter.OnWriteListener() {
        @Override
        public void onWrite(int number, int max) {
          if (number % 500 == 0) {
            dialog.setIndeterminate(false);
            dialog.setMax(max);
            dialog.setProgress(Math.min(number, max));
          }
        }
      };
}
