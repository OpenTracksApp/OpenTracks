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

package com.google.android.apps.mytracks.services;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.MyTracksSettings;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

import java.util.Locale;

/**
 * This class will periodically announce the user's trip statitics.
 *
 * @author Sandor Dornbush
 */
public class StatusAnnouncerTask implements PeriodicTask {

  /**
   * The rate at which announcements are spoken.
   */
  private static final float TTS_SPEECH_RATE = 0.9f;

  /**
   * A pointer to the service context.
   */
  private final Context context;

  /**
   * String utilities.
   */
  private final StringUtils stringUtils;

  /**
   * The interface to the text to speech engine.
   */
  private final TextToSpeech tts;

  /**
   * The response recieved from the TTS engine ater initialization.
   */
  private boolean ready = false;

  /**
   * Constructs the announcer and start the TTS engine.
   */
  public StatusAnnouncerTask(Context context) {
    this.context = context;
    this.stringUtils = new StringUtils(context);

    // We can't have this class also be the listener, otherwise it's unsafe to
    // reference it in Cupcake (even if we don't instantiate it).
    tts = new TextToSpeech(context, new OnInitListener() {
      @Override
      public void onInit(int status) {
        onTtsInit(status);
      }
    });
  }

  /**
   * Called when the TTS engine is initialized.
   */
  private void onTtsInit(int status) {
    Log.i(MyTracksConstants.TAG, "TrackRecordingService.TTS init: " + status);
    this.ready = (status == TextToSpeech.SUCCESS);

    if (ready) {
      // Force the language to be the same as the string we will be speaking,
      // if that's available.
      Locale speechLanguage = Locale.getDefault();
      int languageAvailability = tts.isLanguageAvailable(speechLanguage);
      if (languageAvailability == TextToSpeech.LANG_MISSING_DATA ||
          languageAvailability == TextToSpeech.LANG_NOT_SUPPORTED) {
        // English is probably supported
        // TODO: Somehow use announcement strings from English too
        Log.w(MyTracksConstants.TAG, "Default language not available, using English.");
        speechLanguage = Locale.ENGLISH;
      }
      tts.setLanguage(speechLanguage);

      // Slow down the speed just a bit as it is hard to hear when exercising.
      tts.setSpeechRate(TTS_SPEECH_RATE);
    }
  }

  /**
   * Announces the trip status.
   */
  @Override
  public void run(TrackRecordingService service) {
    if (!ready) {
      Log.e(MyTracksConstants.TAG, "StatusAnnouncer Tts not ready.");
      return;
    }

    if (service == null || service.getTripStatistics() == null) {
      Log.e(MyTracksConstants.TAG, "StatusAnnouncer stats not initialized.");
      return;
    }

    String announcement = getAnnouncement(service.getTripStatistics());
    Log.d(MyTracksConstants.TAG, "Announcement: " + announcement);
    tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null);
  }

  /**
   * Builds the announcement string.
   *
   * @return The string that will be read to the user
   */
  private String getAnnouncement(final TripStatistics stats) {
    SharedPreferences preferences =
        context.getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    boolean metricUnits = true;
    boolean reportSpeed = true;
    if (preferences != null) {
      metricUnits = 
          preferences.getBoolean(context.getString(R.string.metric_units_key),
              true);
      reportSpeed =
          preferences.getBoolean(context.getString(R.string.report_speed_key),
              true);
    }

    double d =  stats.getTotalDistance() / 1000;
    double s =  stats.getAverageMovingSpeed() * 3.6;
    if (d == 0) {
      return context.getString(R.string.announce_no_distance);
    }

    int speedLabel;
    if (metricUnits) {
      if (reportSpeed) {
        speedLabel = R.string.kilometer_per_hour_long;
      } else {
        speedLabel = R.string.per_kilometer;
      }
    } else {
      s *= UnitConversions.KMH_TO_MPH;
      d *= UnitConversions.KM_TO_MI;
      if (reportSpeed) {
        speedLabel = R.string.mile_per_hour_long;
      } else {
        speedLabel = R.string.per_mile;
      }
    }

    String speed = null;
    if ((s == 0) || Double.isNaN(s)) {
      speed = context.getString(R.string.unknown);
    } else {
      if (reportSpeed) {
        speed = String.format("%.1f", s);
      } else {
        double pace = 3600000.0 / s;
        Log.w(MyTracksConstants.TAG,
              "Converted speed: " + s + " to pace: " + pace);
        speed = stringUtils.formatTimeLong((long) pace);
      }
    }

    return context.getString(R.string.announce_template,
        context.getString(R.string.total_distance_label),
        d,
        context.getString(metricUnits
                          ? R.string.kilometers_long
                          : R.string.miles_long),
        stringUtils.formatTimeLong(stats.getMovingTime()),
        speed,
        context.getString(speedLabel));
  }

  public void shutdown() {
    tts.shutdown();
  }

  @Override
  public void start() {
  }

  public static int getVolumeStream() {
    return TextToSpeech.Engine.DEFAULT_STREAM;
  }
}
