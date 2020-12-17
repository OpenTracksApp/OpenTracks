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
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.List;
import java.util.Locale;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.AnnouncementUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;

/**
 * This class will periodically announce the user's {@link TrackStatistics}.
 *
 * @author Sandor Dornbush
 */
public class AnnouncementPeriodicTask implements PeriodicTask {

    /**
     * The rate at which announcements are spoken.
     */
    private static final float TTS_SPEECH_RATE = 0.9f;

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

        if (tts == null) {
            tts = new TextToSpeech(context, status -> {
                Log.i(TAG, "TextToSpeech initialized with status " + status);
                ttsInitStatus = status;
            });
        }
    }

    @Override
    public void run(TrackRecordingService trackRecordingService) {
        if (trackRecordingService == null) {
            Log.e(TAG, "TrackRecordingService is null.");
            return;
        }
        announce(trackRecordingService.getTrackStatistics());
    }

    /**
     * Runs this task.
     *
     * @param trackStatistics the track statistics
     */
    private void announce(TrackStatistics trackStatistics) {
        if (trackStatistics == null) {
            Log.e(TAG, "TrackStatistics is null.");
            return;
        }

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

        if (audioManager.getMode() == AudioManager.MODE_IN_CALL || audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
            Log.i(TAG, "Speech is not allowed at this time.");
            return;
        }

        Track track = contentProviderUtils.getTrack(PreferencesUtils.getRecordingTrackId(context));
        String category = track != null ? track.getCategory() : "";

        List<TrackPoint> trackPoints = contentProviderUtils.getTrackPoints(track.getId());
        IntervalStatistics intervalStatistics = new IntervalStatistics(trackPoints, (float) (PreferencesUtils.isMetricUnits(context) ? 1d * UnitConversions.KM_TO_M : 1d * UnitConversions.MI_TO_M));
        IntervalStatistics.Interval lastInterval = intervalStatistics.getLastInterval();

        String announcement = AnnouncementUtils.getAnnouncement(context, trackStatistics, category, lastInterval);
        speakAnnouncement(announcement);
    }

    @Override
    public void shutdown() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
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

        // Slow down the speed just a bit as it is hard to hear when exercising.
        tts.setSpeechRate(TTS_SPEECH_RATE);

        tts.setOnUtteranceProgressListener(utteranceListener);
    }

    private void speakAnnouncement(String announcement) {
        // We don't care about the utterance id. It is supplied here to force onUtteranceCompleted to be called.
        tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "not used");
    }
}
