/*
 * Copyright 2009 Google Inc.
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

package de.dennisguse.opentracks.services.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Locale;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.AnnouncementUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;

/**
 * This class will periodically announce the user's {@link TrackStatistics}.
 *
 * @author Sandor Dornbush
 */
public class AnnouncementPeriodicTask implements PeriodicTask {

    private static final String TAG = AnnouncementPeriodicTask.class.getSimpleName();

    private final Context context;

    private final AudioManager audioManager;

    private final ContentProviderUtils contentProviderUtils;

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "Audio focus changed to " + focusChange);

            boolean stop = false;
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    stop = false;
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    stop = true;
                    break;
            }

            if (stop && tts != null && tts.isSpeaking()) {
                tts.stop();
                Log.i(TAG, "Aborting current tts due to focus change " + focusChange);
            }
        }
    };

    private final UtteranceProgressListener utteranceListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            int result = audioManager.requestAudioFocus(audioFocusChangeListener, TextToSpeech.Engine.DEFAULT_STREAM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                Log.w(TAG, "Failed to request audio focus.");
            }
        }

        @Override
        public void onDone(String utteranceId) {
            int result = audioManager.abandonAudioFocus(audioFocusChangeListener);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                Log.w(TAG, "Failed to relinquish audio focus.");
            }
        }

        @Override
        public void onError(String utteranceId) {
            Log.e(TAG, "An error occurred for utteranceId " + utteranceId);
        }
    };

    private SharedPreferences sharedPreferences;

    private TextToSpeech tts;
    // Response from TTS after its initialization
    private int ttsInitStatus = TextToSpeech.ERROR;

    private boolean ttsReady = false;

    AnnouncementPeriodicTask(Context context) {
        this.context = context;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        contentProviderUtils = new ContentProviderUtils(context);

    }

    @Override
    public void start() {
        Log.d(TAG, "Start");

        sharedPreferences = PreferencesUtils.getSharedPreferences(context);

        if (tts == null) {
            tts = new TextToSpeech(context, status -> {
                Log.i(TAG, "TextToSpeech initialized with status " + status);
                ttsInitStatus = status;
            });
        }
    }

    @Override
    public void run(@NonNull Track.Id trackId, @NonNull TrackStatistics trackStatistics) {
        synchronized (this) {
            if (!ttsReady) {
                ttsReady = ttsInitStatus == TextToSpeech.SUCCESS;
                if (ttsReady) {
                    onTtsReady();
                }
            }
            if (!ttsReady) {
                Log.i(TAG, "TTS not ready.");
                return;
            }
        }

        if (Arrays.asList(AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION)
                .contains(audioManager.getMode())) {
            Log.i(TAG, "Speech is not allowed at this time.");
            return;
        }

        Track track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            Log.i(TAG, "It doesn't exists a track with trackid = " + track);
            return;
        }
        String category = track.getCategory();

        boolean isMetricUnits = PreferencesUtils.isMetricUnits(sharedPreferences, context);
        boolean isReportSpeed = PreferencesUtils.isReportSpeed(sharedPreferences, context, category);
        Distance minGPSDistance = PreferencesUtils.getRecordingDistanceInterval(sharedPreferences, context);

        TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), null);
        IntervalStatistics intervalStatistics = new IntervalStatistics(Distance.one(isMetricUnits), minGPSDistance);
        intervalStatistics.addTrackPoints(trackPointIterator);
        IntervalStatistics.Interval lastInterval = intervalStatistics.getLastInterval();

        String announcement = AnnouncementUtils.getAnnouncement(context, trackStatistics, isMetricUnits, isReportSpeed, lastInterval);
        speakAnnouncement(announcement);
    }

    @Override
    public void shutdown() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }

        sharedPreferences = null;
    }

    private void onTtsReady() {
        Locale locale = Locale.getDefault();
        int languageAvailability = tts.isLanguageAvailable(locale);
        if (languageAvailability == TextToSpeech.LANG_MISSING_DATA || languageAvailability == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Default locale not available, use English.");
            locale = Locale.ENGLISH;
            /*
             * TODO: instead of using english, load the language if missing and show a toast if not supported.
             *  Not able to change the resource strings to English.
             */
        }
        tts.setLanguage(locale);
        tts.setSpeechRate(PreferencesUtils.getVoiceSpeedRate(PreferencesUtils.getSharedPreferences(context), context));
        tts.setOnUtteranceProgressListener(utteranceListener);
    }

    private void speakAnnouncement(String announcement) {
        // We don't care about the utterance id. It is supplied here to force onUtteranceCompleted to be called.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null);
        } else {
            tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "not used");
        }
    }

    /**
     * A {@link PeriodicTaskFactory} for text-to-speech announcement periodic task.
     *
     * @author Rodrigo Damazio
     */
    public static class Factory implements PeriodicTaskFactory {

        @Override
        @NonNull
        public PeriodicTask create(Context context) {
            return new AnnouncementPeriodicTask(context);
        }
    }
}
