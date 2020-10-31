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

package de.dennisguse.opentracks;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import de.dennisguse.opentracks.databinding.TrackControllerBinding;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.services.TrackRecordingServiceInterface;
import de.dennisguse.opentracks.util.ActivityUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * Track controller for record, pause, resume, and stop.
 *
 * @author Jimmy Shih
 */
public class TrackController {

    private static final String TAG = TrackController.class.getSimpleName();

    private final Activity activity;
    private final TrackRecordingServiceConnection trackRecordingServiceConnection;
    private final Handler handlerUpdateTotalTime;
    private final boolean alwaysShow;
    private ButtonDelay buttonDelay;

    private final TrackControllerBinding viewBinding;

    private boolean isRecording;
    private boolean isPaused;
    private long totalTime = 0;

    private long totalTimeTimestamp = 0;

    private boolean isResumed = false;

    // A runnable to update the total time.
    private final Runnable updateTotalTimeRunnable = new Runnable() {
        public void run() {
            if (isResumed && isRecording && !isPaused) {
                viewBinding.trackControllerTotalTime.setText(StringUtils.formatElapsedTimeWithHour(System.currentTimeMillis() - totalTimeTimestamp + totalTime));
                handlerUpdateTotalTime.postDelayed(this, UnitConversions.ONE_SECOND_MS);
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    TrackController(Activity activity, TrackControllerBinding viewBinding, TrackRecordingServiceConnection trackRecordingServiceConnection, boolean alwaysShow, OnClickListener recordListener, OnClickListener stopListener) {
        this.activity = activity;
        this.viewBinding = viewBinding;
        this.trackRecordingServiceConnection = trackRecordingServiceConnection;
        this.alwaysShow = alwaysShow;
        handlerUpdateTotalTime = new Handler();
        viewBinding.trackControllerRecord.setOnTouchListener((view, motionEvent) -> onRecordTouch(activity, recordListener, motionEvent));

        viewBinding.trackControllerStop.setOnTouchListener((view, motionEvent) -> onStopTouch(activity, stopListener, motionEvent));
    }

    private boolean onRecordTouch(final Activity activity, final OnClickListener recordListener, final MotionEvent motionEvent) {
        if (isRecording && !isPaused) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                buttonDelay = new ButtonDelay(activity, viewBinding.trackControllerRecord, R.drawable.ic_button_pause_anim, R.string.hold_to_pause, recordListener);
                new Thread(buttonDelay).start();
                return true;
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                viewBinding.trackControllerRecord.setImageResource(R.drawable.ic_button_pause);
                if (buttonDelay != null) {
                    buttonDelay.canceled = true;
                }
                return true;
            }
        } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            recordListener.onClick(null);
            return true;
        }
        return false;
    }

    private boolean onStopTouch(final Activity activity, final OnClickListener stopListener, final MotionEvent motionEvent) {
        if (isRecording) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                buttonDelay = new ButtonDelay(activity, viewBinding.trackControllerStop, R.drawable.ic_button_stop_anim, R.string.hold_to_stop, stopListener);
                new Thread(buttonDelay).start();
                return true;
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                viewBinding.trackControllerStop.setImageResource(R.drawable.ic_button_stop);
                if (buttonDelay != null) {
                    buttonDelay.canceled = true;
                }
                return true;
            }
        }
        return false;
    }

    private static class ButtonDelay implements Runnable {

        private boolean canceled = false;

        private final ImageButton imageButton;
        private final Activity activity;
        private final OnClickListener clickListener;
        private final int delayMillis;
        private final Drawable drawable;
        private final int delayMessageId;

        private ButtonDelay(final Activity activity, final ImageButton imageButton, final int animDrawableId, final int delayMessageId, final OnClickListener clickListener) {
            this.activity = activity;
            this.clickListener = clickListener;
            this.delayMillis = activity.getResources().getInteger(R.integer.buttonDelayMillis);
            this.imageButton = imageButton;
            this.drawable = ContextCompat.getDrawable(activity, animDrawableId);
            this.delayMessageId = delayMessageId;
        }

        @Override
        public void run() {
            activity.runOnUiThread(() -> {
                imageButton.setImageDrawable(drawable);
                if (drawable instanceof AnimatedVectorDrawable) {
                    ((AnimatedVectorDrawable) drawable).start();
                }
                ActivityUtils.vibrate(activity, 150);
                ActivityUtils.toast(activity, delayMessageId, Toast.LENGTH_SHORT, Gravity.TOP);
            });

            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ignored) {
            }
            if (!canceled) {
                activity.runOnUiThread(() -> {
                    clickListener.onClick(null);
                    ActivityUtils.vibrate(activity, 1000);
                });
            }
        }
    }

    public void update(boolean recording, boolean paused) {
        if (!isResumed) {
            return;
        }
        isRecording = recording;
        isPaused = paused;
        boolean visible = alwaysShow || isRecording;
        viewBinding.trackControllerContainer.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (!visible) {
            handlerUpdateTotalTime.removeCallbacks(updateTotalTimeRunnable);
            return;
        }

        viewBinding.trackControllerRecord.setImageResource(isRecording && !isPaused ? R.drawable.ic_button_pause : R.drawable.button_record);
        viewBinding.trackControllerRecord.setContentDescription(activity.getString(isRecording && !isPaused ? R.string.image_pause : R.string.image_record));

        viewBinding.trackControllerStop.setImageResource(isRecording ? R.drawable.ic_button_stop : R.drawable.ic_button_stop_disabled);
        viewBinding.trackControllerStop.setEnabled(isRecording);

        viewBinding.trackControllerStatus.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);
        if (isRecording) {
            viewBinding.trackControllerStatus.setTextColor(activity.getResources().getColor(isPaused ? android.R.color.white : R.color.recording_text));
            viewBinding.trackControllerStatus.setText(isPaused ? R.string.generic_paused : R.string.generic_recording);
        }

        handlerUpdateTotalTime.removeCallbacks(updateTotalTimeRunnable);
        viewBinding.trackControllerTotalTime.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);
        if (isRecording) {
            totalTime = getTotalTime();
            viewBinding.trackControllerTotalTime.setText(StringUtils.formatElapsedTimeWithHour(totalTime));
            if (!isPaused) {
                totalTimeTimestamp = System.currentTimeMillis();
                handlerUpdateTotalTime.postDelayed(updateTotalTimeRunnable, UnitConversions.ONE_SECOND_MS);
            }
        }
    }

    void onResume(boolean recording, boolean paused) {
        isResumed = true;
        update(recording, paused);
    }

    void onPause() {
        isResumed = false;
        handlerUpdateTotalTime.removeCallbacks(updateTotalTimeRunnable);
    }

    public void hide() {
        viewBinding.trackControllerContainer.setVisibility(View.GONE);
    }

    public void show() {
        viewBinding.trackControllerContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Gets the total time for the current recording track.
     */
    private long getTotalTime() {
        TrackRecordingServiceInterface trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
        return trackRecordingService != null ? trackRecordingService.getTotalTime() : 0L;
    }
}
