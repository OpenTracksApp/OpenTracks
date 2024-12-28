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

package de.dennisguse.opentracks.services.announcement;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.PreferencesUtils;


public class TTSManager {

    public final static int AUDIO_STREAM = TextToSpeech.Engine.DEFAULT_STREAM;

    private static final String TAG = TTSManager.class.getSimpleName();

    private final Context context;

    private final AudioManager audioManager;

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "Audio focus changed to " + focusChange);

            boolean stop = List.of(AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
                    .contains(focusChange);

            if (stop && tts != null && tts.isSpeaking()) {
                tts.stop();
                Log.i(TAG, "Aborting current tts due to focus change " + focusChange);
            }
        }
    };

    private final UtteranceProgressListener utteranceListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            int result = audioManager.requestAudioFocus(audioFocusChangeListener, AUDIO_STREAM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
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

    private MediaPlayer ttsFallback;

    TTSManager(Context context) {
        this.context = context;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void start() {
        Log.d(TAG, "Start");

        if (tts == null) {
            tts = new TextToSpeech(context, status -> {
                Log.i(TAG, "TextToSpeech initialized with status " + status);
                ttsInitStatus = status;
            });
        }
        if (ttsFallback == null) {
            ttsFallback = MediaPlayer.create(context, R.raw.tts_fallback);
            if (ttsFallback == null) {
                Log.w(TAG, "MediaPlayer for ttsFallback could not be created.");
            } else {
                ttsFallback.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build());
                ttsFallback.setLooping(false);
            }
        }
    }

    public void speak(@NonNull CharSequence announcement) {
        synchronized (this) {
            if (!ttsReady) {
                ttsReady = ttsInitStatus == TextToSpeech.SUCCESS;
                if (ttsReady) {
                    onTtsReady();
                }
            }
        }

        if (List.of(AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION)
                .contains(audioManager.getMode())) {
            Log.i(TAG, "Announcement is not allowed at this time.");
            return;
        }

        if (!ttsReady) {
            if (ttsFallback == null) {
                Log.w(TAG, "MediaPlayer for ttsFallback was not created.");
            } else {
                Log.i(TAG, "TTS not ready/available, just generating a tone.");
                ttsFallback.seekTo(0);
                ttsFallback.start();
            }
            return;
        }

        if (announcement.length() > 0) {
            // We don't care about the utterance id. It is supplied here to force onUtteranceCompleted to be called.
            tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "not used");
        }
    }

    public void stop() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }

        if (ttsFallback != null) {
            ttsFallback.release();
            ttsFallback = null;
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
        tts.setSpeechRate(PreferencesUtils.getVoiceSpeedRate());
        tts.setOnUtteranceProgressListener(utteranceListener);
    }
}
