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
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.util.ActivityUtils;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Track controller for record, pause, resume, and stop.
 *
 * @author Jimmy Shih
 */
//TODO Subscribe to TrackRecordingService.recordingStatus
//TODO Could be a fragment
public class TrackController implements View.OnTouchListener {

    private static final String TAG = TrackController.class.getSimpleName();

    private static final Duration UI_UPDATE_INTERVAL = Duration.ofSeconds(1);

    private final Activity activity;
    private final TrackRecordingServiceConnection trackRecordingServiceConnection;
    private final Handler handlerUpdateTotalTime = new Handler();
    private final boolean alwaysShow;

    private Runnable buttonDelay;
    private final int buttonDelayDuration;

    private final TrackControllerBinding viewBinding;
    private final TransitionDrawable transition;


    private TrackRecordingService.RecordingStatus recordingStatus;
    private Duration totalTime;

    private final Callback callback;

    // A runnable to update the total time.
    private final Runnable updateTotalTimeRunnable = new Runnable() {
        public void run() {
            if (isResumed() && recordingStatus.isRecordingAndNotPaused()) {
                updateTotalTime();
                setTotalTime();
                handlerUpdateTotalTime.postDelayed(this, UI_UPDATE_INTERVAL.toMillis());
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
            if (buttonDelay != null || recordingStatus.isRecordingAndNotPaused()) {
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
                && recordingStatus.isRecordingAndNotPaused()) {

            transition.startTransition(buttonDelayDuration);

            ActivityUtils.vibrate(activity, 150);
            viewBinding.trackControllerStatus.setText(R.string.hold_to_pause);

            buttonDelay = () -> {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                view.performClick();
                callback.recordPause();

                transition.resetTransition();

                ActivityUtils.vibrate(activity, 1000);
            };
            handlerUpdateTotalTime.postDelayed(buttonDelay, buttonDelayDuration);
            return true;
        }

        //To stop a recording
        if (viewBinding.trackControllerStop.equals(view)
                && event.getAction() == MotionEvent.ACTION_DOWN
                && recordingStatus.isRecording()) {


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

    public void update(TrackRecordingService.RecordingStatus recordingStatus) {
        if (!isResumed()) {
            handlerUpdateTotalTime.removeCallbacks(updateTotalTimeRunnable);
            return;
        }

        this.recordingStatus = recordingStatus;
        boolean isRecording = recordingStatus.isRecording();
        boolean isRecordingPaused = recordingStatus.isPaused();
        boolean visible = alwaysShow || isRecording;
        viewBinding.trackControllerContainer.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (!visible) {
            handlerUpdateTotalTime.removeCallbacks(updateTotalTimeRunnable);
            return;
        }

        if (isRecording && !isRecordingPaused) {
            handlerUpdateTotalTime.postDelayed(updateTotalTimeRunnable, UI_UPDATE_INTERVAL.toMillis());
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

        if (recordingStatus.isRecording()) {
            updateTotalTime();
            setTotalTime();
        }
    }

    void onResume(TrackRecordingService.RecordingStatus recordingStatus) {
        this.recordingStatus = recordingStatus;
        update(recordingStatus);
    }

    void onPause() {
        recordingStatus = null;
        handlerUpdateTotalTime.removeCallbacks(updateTotalTimeRunnable);
    }

    public void hide() {
        viewBinding.trackControllerContainer.setVisibility(View.GONE);
    }

    public void show() {
        viewBinding.trackControllerContainer.setVisibility(View.VISIBLE);
    }

    private void updateTotalTime() {
        TrackRecordingService trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
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
        viewBinding.trackControllerStatus.setText(recordingStatus.isPaused() ? R.string.generic_paused : R.string.generic_recording);
    }

    private boolean isResumed() {
        return recordingStatus != null;
    }

    public interface Callback {
        void recordStart();

        void recordPause();

        void recordStop();
    }
}
