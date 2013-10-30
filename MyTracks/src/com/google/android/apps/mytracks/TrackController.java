/*
 * Copyright 2012 Google Inc.
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

import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Track controller for record, pause, resume, and stop.
 * 
 * @author Jimmy Shih
 */
public class TrackController {

  private static final String TAG = TrackController.class.getSimpleName();
  private static final int ONE_SECOND = 1000;

  private final Activity activity;
  private final TrackRecordingServiceConnection trackRecordingServiceConnection;
  private final Handler handler;
  private final View containerView;
  private final TextView statusTextView;
  private final TextView totalTimeTextView;
  private final ImageButton recordImageButton;
  private final ImageButton stopImageButton;
  private final boolean alwaysShow;

  private boolean isRecording;
  private boolean isPaused;
  private long totalTime = 0;

  // the timestamp for the toal time
  private long totalTimeTimestamp = 0;

  private boolean isResumed = false;

  // A runnable to update the total time.
  private final Runnable updateTotalTimeRunnable = new Runnable() {
    public void run() {
      if (isResumed && isRecording && !isPaused) {
        totalTimeTextView.setText(StringUtils.formatElapsedTimeWithHour(
            System.currentTimeMillis() - totalTimeTimestamp + totalTime));
        handler.postDelayed(this, ONE_SECOND);
      }
    }
  };

  public TrackController(Activity activity,
      TrackRecordingServiceConnection trackRecordingServiceConnection, boolean alwaysShow,
      OnClickListener recordListener, OnClickListener stopListener) {
    this.activity = activity;
    this.trackRecordingServiceConnection = trackRecordingServiceConnection;
    this.alwaysShow = alwaysShow;
    handler = new Handler();
    containerView = (View) activity.findViewById(R.id.track_controler_container);
    statusTextView = (TextView) activity.findViewById(R.id.track_controller_status);
    totalTimeTextView = (TextView) activity.findViewById(R.id.track_controller_total_time);
    recordImageButton = (ImageButton) activity.findViewById(R.id.track_controller_record);
    recordImageButton.setOnClickListener(recordListener);
    stopImageButton = (ImageButton) activity.findViewById(R.id.track_controller_stop);
    stopImageButton.setOnClickListener(stopListener);
  }

  public void update(boolean recording, boolean paused) {
    if (!isResumed) {
      return;
    }
    isRecording = recording;
    isPaused = paused;
    boolean visible = alwaysShow || isRecording;
    containerView.setVisibility(visible ? View.VISIBLE : View.GONE);

    if (!visible) {
      handler.removeCallbacks(updateTotalTimeRunnable);
      return;
    }

    recordImageButton.setImageResource(
        isRecording && !isPaused ? R.drawable.button_pause : R.drawable.button_record);
    recordImageButton.setContentDescription(activity.getString(
        isRecording && !isPaused ? R.string.icon_pause_recording : R.string.icon_record_track));

    stopImageButton.setImageResource(
        isRecording ? R.drawable.button_stop : R.drawable.ic_button_stop_disabled);
    stopImageButton.setEnabled(isRecording);

    statusTextView.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);
    if (isRecording) {
      statusTextView.setTextColor(activity.getResources()
          .getColor(isPaused ? android.R.color.white : R.color.recording_text));
      statusTextView.setText(isPaused ? R.string.generic_paused : R.string.generic_recording);
    }

    handler.removeCallbacks(updateTotalTimeRunnable);
    totalTime = isRecording ? getTotalTime() : 0L;
    totalTimeTextView.setText(StringUtils.formatElapsedTimeWithHour(totalTime));
    if (isRecording && !isPaused) {
      totalTimeTimestamp = System.currentTimeMillis();
      handler.postDelayed(updateTotalTimeRunnable, ONE_SECOND);
    }
  }

  public void onResume(boolean recording, boolean paused) {
    isResumed = true;
    update(recording, paused);
  }

  public void onPause() {
    isResumed = false;
    handler.removeCallbacks(updateTotalTimeRunnable);
  }

  public void hide() {
    containerView.setVisibility(View.GONE);
  }

  public void show() {
    containerView.setVisibility(View.VISIBLE);
  }

  /**
   * Gets the total time for the current recording track.
   */
  private long getTotalTime() {
    ITrackRecordingService trackRecordingService = trackRecordingServiceConnection
        .getServiceIfBound();
    try {
      return trackRecordingService != null ? trackRecordingService.getTotalTime() : 0L;
    } catch (RemoteException e) {
      Log.e(TAG, "exception", e);
      return 0L;
    }
  }
}
