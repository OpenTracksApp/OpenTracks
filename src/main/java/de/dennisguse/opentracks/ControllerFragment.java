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
import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.dennisguse.opentracks.databinding.ControllerFragmentBinding;
import de.dennisguse.opentracks.services.RecordingData;
import de.dennisguse.opentracks.services.RecordingStatus;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.ui.util.ActivityUtils;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Track controller for record, pause, resume, and stop.
 *
 * @author Jimmy Shih
 */
//TODO Move buttons into separate components
public class ControllerFragment extends Fragment implements View.OnTouchListener {

    private static final String TAG = ControllerFragment.class.getSimpleName();

    private Handler handlerUpdateTotalTime;

    private Runnable buttonDelay;
    private int buttonDelayDuration;

    private ControllerFragmentBinding viewBinding;
    private TransitionDrawable transition;

    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private RecordingStatus recordingStatus;
    private RecordingData recordingData;

    private final TrackRecordingServiceConnection.Callback bindChangedCallback = service -> {
        service.getRecordingStatusObservable()
                .observe(ControllerFragment.this, this::onRecordingStatusChanged);
        service.getRecordingDataObservable()
                .observe(ControllerFragment.this, this::onTotalTimeChanged);
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buttonDelayDuration = getResources().getInteger(R.integer.buttonDelayMillis);

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindChangedCallback);
        handlerUpdateTotalTime = new Handler();
        recordingStatus = TrackRecordingService.STATUS_DEFAULT;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = ControllerFragmentBinding.inflate(inflater, container, false);

        viewBinding.controllerRecord.setOnTouchListener(this);
        viewBinding.controllerRecord.setOnClickListener((view) -> {
            if (buttonDelay != null || recordingStatus.isRecordingAndNotPaused()) {
                return;
            }

            Callback callback = (Callback) getContext();
            if (callback != null) {
                callback.recordStart();
            }
        });

        viewBinding.controllerStop.setOnTouchListener(this);
        transition = (TransitionDrawable) viewBinding.controllerContainer.getBackground();

        onRecordingStatusChanged(recordingStatus);

        return viewBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        trackRecordingServiceConnection.startConnection(getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        handlerUpdateTotalTime.removeCallbacksAndMessages(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        trackRecordingServiceConnection.unbind(getContext());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
        transition = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        trackRecordingServiceConnection = null;
        handlerUpdateTotalTime = null;
        recordingStatus = null;
    }

    //TODO Move into a separate object
    @SuppressLint("ClickableViewAccessibility") //We do accessibility manually.
    @Override
    public boolean onTouch(final View view, MotionEvent event) {
        if (!isResumed()) {
            Log.w(TAG, "The UI is already gone, no need to update anything; seen on Android 7.1.2");
            return false;
        }
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
        if (viewBinding.controllerRecord.equals(view)
                && event.getAction() == MotionEvent.ACTION_DOWN
                && recordingStatus.isRecordingAndNotPaused()) {

            transition.startTransition(buttonDelayDuration);

            ActivityUtils.vibrate(getContext(), 150);
            viewBinding.controllerStatus.setText(R.string.hold_to_pause);

            buttonDelay = () -> {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                view.performClick();
                Callback callback = (Callback) getContext();
                if (callback != null) {
                    callback.recordPause();
                }

                transition.resetTransition();

                ActivityUtils.vibrate(getContext(), 1000);
            };
            handlerUpdateTotalTime.postDelayed(buttonDelay, buttonDelayDuration);
            return true;
        }

        //To stop a recording
        if (viewBinding.controllerStop.equals(view)
                && event.getAction() == MotionEvent.ACTION_DOWN
                && recordingStatus.isRecording()) {


            transition.startTransition(buttonDelayDuration);

            ActivityUtils.vibrate(getContext(), 150);
            viewBinding.controllerStatus.setText(R.string.hold_to_stop);

            buttonDelay = () -> {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                Context context = getContext();
                Callback callback = (Callback) context;
                if (callback != null) {
                    callback.recordStop();
                    ActivityUtils.vibrate(context, 1000);
                }

                transition.resetTransition();
            };
            handlerUpdateTotalTime.postDelayed(buttonDelay, buttonDelayDuration);
            return true;
        }

        return false;
    }

    public void hide() {
        viewBinding.controllerContainer.setVisibility(View.GONE);
    }

    public void show() {
        viewBinding.controllerContainer.setVisibility(View.VISIBLE);
    }

    private void showStatusSetDefaultText() {
        viewBinding.controllerStatus.setText(recordingStatus.isPaused() ? R.string.generic_paused : R.string.generic_recording);
    }

    public interface Callback {
        void recordStart();

        void recordPause();

        void recordStop();
    }

    private void onRecordingStatusChanged(RecordingStatus status) {
        recordingStatus = status;

        viewBinding.controllerRecord.setImageResource(recordingStatus.isRecordingAndNotPaused() ? R.drawable.ic_button_pause : R.drawable.button_record);
        viewBinding.controllerRecord.setContentDescription(getContext().getString(recordingStatus.isRecordingAndNotPaused() ? R.string.image_pause : R.string.image_record));

        viewBinding.controllerStop.setEnabled(recordingStatus.isRecording());

        viewBinding.controllerStatus.setVisibility(recordingStatus.isRecording() ? View.VISIBLE : View.INVISIBLE);
        if (recordingStatus.isRecording()) {
            viewBinding.controllerStatus.setTextColor(getContext().getResources().getColor(recordingStatus.isPaused() ? android.R.color.white : R.color.recording_text));
            showStatusSetDefaultText();
        }

        viewBinding.controllerTotalTime.setVisibility(recordingStatus.isRecording() ? View.VISIBLE : View.INVISIBLE);
    }

    private void onTotalTimeChanged(RecordingData recordingData) {
        this.recordingData = recordingData;

        viewBinding.controllerTotalTime.setText(StringUtils.formatElapsedTimeWithHour(this.recordingData.getTrackStatistics().getTotalTime()));
    }
}
