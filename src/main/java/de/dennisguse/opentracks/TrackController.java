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
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import java.time.Duration;

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
public class TrackController implements View.OnTouchListener {

    private static final String TAG = TrackController.class.getSimpleName();

    private final Activity activity;
    private final TrackRecordingServiceConnection trackRecordingServiceConnection;
    private final Handler handlerUpdateTotalTime = new Handler();
    private final boolean alwaysShow;

    private Runnable buttonDelay;
    private final int buttonDelayDuration;

    private final TrackControllerBinding viewBinding;
    private final TransitionDrawable transition;

    private boolean isRecording;
    private boolean isRecordingPaused;
    private Duration totalTime;
    private boolean isResumed = false;

    private final Callback callback;

    // A runnable to update the total time.
    private final Runnable updateTotalTimeRunnable = new Runnable() {
        public void run() {
            if (isResumed && isRecording && !isRecordingPaused) {
                updateTotalTime();
                setTotalTime();
                handlerUpdateTotalTime.postDelayed(this, UnitConversions.ONE_SECOND_MS);
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    TrackController(Activity activity, TrackControllerBinding viewBinding, TrackRecordingServiceConnection trackRecordingServiceConnection, boolean alwaysShow, Callback callback) {
        this.activity = activity;
        this.viewBinding = viewBinding;
        this.trackRecordingServiceConnection = trackRecordingServiceConnection;
        this.alwaysShow = alwaysShow;
        this.callback = callback;

        buttonDelayDuration = activity.getResources().getInteger(R.integer.buttonDelayMillis);

        viewBinding.trackControllerRecord.setOnTouchListener(this);
        viewBinding.trackControllerRecord.setOnClickListener((view) -> {
            if (buttonDelay != null || (isRecording && !isRecordingPaused)) {
                return;
            }

            callback.recordStart();
        });

        viewBinding.trackControllerStop.setOnTouchListener(this);
        transition = (TransitionDrawable) viewBinding.trackControllerContainer.getBackground();
    }

    @SuppressLint("ClickableViewAccessibility") //We do accessibility manually.
    @Override
    public boolean onTouch(final View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return false;
        }

        if (buttonDelay != null && event.getAction() == MotionEvent.ACTION_UP) {
            handlerUpdateTotalTime.removeCallbacks(buttonDelay);
            buttonDelay = null;
            view.setPressed(false);

            transition.resetTransition();

            showStatusSetDefaultText();

            return false;
        }

        //Trigger button pressed animation
        view.setPressed(true);

        //To pause a recording
        if (viewBinding.trackControllerRecord.equals(view)
                && event.getAction() == MotionEvent.ACTION_DOWN
                && isRecording && !isRecordingPaused) {

            transition.startTransition(buttonDelayDuration);

            ActivityUtils.vibrate(activity, 150);
            viewBinding.trackControllerStatus.setText(R.string.hold_to_pause);

            buttonDelay = () -> {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                view.performClick();
                callback.recordStart(); //TODO Should be recordPause();

                transition.resetTransition();

                ActivityUtils.vibrate(activity, 1000);
            };
            handlerUpdateTotalTime.postDelayed(buttonDelay, buttonDelayDuration);
            return true;
        }

        //To stop a recording
        if (viewBinding.trackControllerStop.equals(view)
                && event.getAction() == MotionEvent.ACTION_DOWN
                && isRecording) {


            transition.startTransition(buttonDelayDuration);

            ActivityUtils.vibrate(activity, 150);
            viewBinding.trackControllerStatus.setText(R.string.hold_to_stop);

            buttonDelay = () -> {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                callback.recordStop();
                ActivityUtils.vibrate(activity, 1000);

                transition.resetTransition();
            };
            handlerUpdateTotalTime.postDelayed(buttonDelay, buttonDelayDuration);
            return true;
        }

        return false;
    }

    public void update(boolean recording, boolean paused) {
        if (!isResumed) {
            handlerUpdateTotalTime.removeCallbacks(updateTotalTimeRunnable);
            return;
        }
        isRecording = recording;
        isRecordingPaused = paused;
        boolean visible = alwaysShow || isRecording;
        viewBinding.trackControllerContainer.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (!visible) {
            handlerUpdateTotalTime.removeCallbacks(updateTotalTimeRunnable);
            return;
        }

        if (isRecording && !isRecordingPaused) {
            handlerUpdateTotalTime.postDelayed(updateTotalTimeRunnable, UnitConversions.ONE_SECOND_MS);
        }

        viewBinding.trackControllerRecord.setImageResource(isRecording && !isRecordingPaused ? R.drawable.ic_button_pause : R.drawable.button_record);
        viewBinding.trackControllerRecord.setContentDescription(activity.getString(isRecording && !isRecordingPaused ? R.string.image_pause : R.string.image_record));

        viewBinding.trackControllerStop.setEnabled(isRecording);

        viewBinding.trackControllerStatus.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);
        if (isRecording) {
            viewBinding.trackControllerStatus.setTextColor(activity.getResources().getColor(isRecordingPaused ? android.R.color.white : R.color.recording_text));
            showStatusSetDefaultText();
        }

        viewBinding.trackControllerTotalTime.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);

        if (isRecording) {
            updateTotalTime();
            setTotalTime();
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

    private void updateTotalTime() {
        TrackRecordingServiceInterface trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
        if (trackRecordingService != null) {
            totalTime = trackRecordingService.getTotalTime();
        }
    }

    private void setTotalTime() {
        if (totalTime != null) {
            viewBinding.trackControllerTotalTime.setText(StringUtils.formatElapsedTimeWithHour(totalTime));
        }
    }

    private void showStatusSetDefaultText() {
        viewBinding.trackControllerStatus.setText(isRecordingPaused ? R.string.generic_paused : R.string.generic_recording);
    }

    public interface Callback {
        void recordStart();

        void recordStop();
    }
}
