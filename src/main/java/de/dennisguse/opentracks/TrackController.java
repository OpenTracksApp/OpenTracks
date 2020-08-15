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
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
    private final View containerView;
    private final TextView statusTextView;
    private final TextView totalTimeTextView;
    private final ImageButton recordImageButton;
    private final ImageButton stopImageButton;
    private final boolean alwaysShow;
    private ButtonDelay buttonDelay;

    private boolean isRecording;
    private boolean isPaused;
    private long totalTime = 0;

    private long totalTimeTimestamp = 0;

    private boolean isResumed = false;

    // A runnable to update the total time.
    private final Runnable updateTotalTimeRunnable = new Runnable() {
        public void run() {
            if (isResumed && isRecording && !isPaused) {
                totalTimeTextView.setText(StringUtils.formatElapsedTimeWithHour(System.currentTimeMillis() - totalTimeTimestamp + totalTime));
                handlerUpdateTotalTime.postDelayed(this, UnitConversions.ONE_SECOND_MS);
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    TrackController(Activity activity, TrackRecordingServiceConnection trackRecordingServiceConnection, boolean alwaysShow, OnClickListener recordListener, OnClickListener stopListener) {
        this.activity = activity;
        this.trackRecordingServiceConnection = trackRecordingServiceConnection;
        this.alwaysShow = alwaysShow;
        handlerUpdateTotalTime = new Handler();
        containerView = activity.findViewById(R.id.track_controller_container);
        statusTextView = activity.findViewById(R.id.track_controller_status);
        totalTimeTextView = activity.findViewById(R.id.track_controller_total_time);

        recordImageButton = activity.findViewById(R.id.track_controller_record);
        recordImageButton.setOnTouchListener((view, motionEvent) -> onRecordTouch(activity, recordListener, motionEvent));

        stopImageButton = activity.findViewById(R.id.track_controller_stop);
        stopImageButton.setOnTouchListener((view, motionEvent) -> onStopTouch(activity, stopListener, motionEvent));
    }

    private boolean onRecordTouch(final Activity activity, final OnClickListener recordListener, final MotionEvent motionEvent) {
        if (isRecording && !isPaused) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                buttonDelay = new ButtonDelay(activity, recordImageButton, R.drawable.ic_button_pause_anim, R.string.hold_to_pause, recordListener);
                new Thread(buttonDelay).start();
                return true;
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP ) {
                recordImageButton.setImageResource(R.drawable.ic_button_pause);
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
                buttonDelay = new ButtonDelay(activity, stopImageButton, R.drawable.ic_button_stop_anim, R.string.hold_to_stop, stopListener);
                new Thread(buttonDelay).start();
                return true;
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP ) {
                stopImageButton.setImageResource(R.drawable.ic_button_stop);
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
        private final AnimatedVectorDrawable animatedDrawable;
        private final int delayMessageId;

        private ButtonDelay(final Activity activity, final ImageButton imageButton, final int animDrawableId, final int delayMessageId, final OnClickListener clickListener) {
            this.activity = activity;
            this.clickListener = clickListener;
            this.delayMillis = activity.getResources().getInteger(R.integer.buttonDelayMillis);
            this.imageButton = imageButton;
            this.animatedDrawable = (AnimatedVectorDrawable) activity.getDrawable(animDrawableId);
            this.delayMessageId = delayMessageId;
        }

        @Override
        public void run() {
            imageButton.setImageDrawable(animatedDrawable);
            activity.runOnUiThread(animatedDrawable::start);

            ActivityUtils.vibrate(activity, 150);
            activity.runOnUiThread(()-> ActivityUtils.toast(activity, delayMessageId, Toast.LENGTH_SHORT, Gravity.TOP));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ignored) {
            }
            if (!canceled) {
                activity.runOnUiThread(()-> clickListener.onClick(null));
                ActivityUtils.vibrate(activity, 1000);
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
        containerView.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (!visible) {
            handlerUpdateTotalTime.removeCallbacks(updateTotalTimeRunnable);
            return;
        }

        recordImageButton.setImageResource(isRecording && !isPaused ? R.drawable.ic_button_pause : R.drawable.button_record);
        recordImageButton.setContentDescription(activity.getString(isRecording && !isPaused ? R.string.image_pause : R.string.image_record));

        stopImageButton.setImageResource(isRecording ? R.drawable.ic_button_stop : R.drawable.ic_button_stop_disabled);
        stopImageButton.setEnabled(isRecording);

        statusTextView.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);
        if (isRecording) {
            statusTextView.setTextColor(activity.getResources().getColor(isPaused ? android.R.color.white : R.color.recording_text));
            statusTextView.setText(isPaused ? R.string.generic_paused : R.string.generic_recording);
        }

        handlerUpdateTotalTime.removeCallbacks(updateTotalTimeRunnable);
        totalTimeTextView.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);
        if (isRecording) {
            totalTime = getTotalTime();
            totalTimeTextView.setText(StringUtils.formatElapsedTimeWithHour(totalTime));
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
        containerView.setVisibility(View.GONE);
    }

    public void show() {
        containerView.setVisibility(View.VISIBLE);
    }

    /**
     * Gets the total time for the current recording track.
     */
    private long getTotalTime() {
        TrackRecordingServiceInterface trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
        return trackRecordingService != null ? trackRecordingService.getTotalTime() : 0L;
    }
}
