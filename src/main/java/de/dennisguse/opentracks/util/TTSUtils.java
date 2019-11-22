package de.dennisguse.opentracks.util;

import android.speech.tts.TextToSpeech;

public class TTSUtils {

    private TTSUtils() {
    }

    public static int getTTSStream() {
        return TextToSpeech.Engine.DEFAULT_STREAM;
    }
}
