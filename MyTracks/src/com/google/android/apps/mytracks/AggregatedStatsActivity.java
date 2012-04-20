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

package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.StatsUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.MenuItem;

import java.util.List;

/**
 * An activity to view aggregated stats from all recorded tracks.
 *
 * @author Fergus Nelson
 */
public class AggregatedStatsActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);
    ApiAdapterFactory.getApiAdapter().configureActionBarHomeAsUp(this);
    setContentView(R.layout.aggregated_stats);

    SharedPreferences preferences = getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    boolean metricUnits = preferences.getBoolean(getString(R.string.metric_units_key), true);
    boolean reportSpeed = preferences.getBoolean(getString(R.string.report_speed_key), true);

    StatsUtils.setSpeedLabels(this, reportSpeed, false);
    StatsUtils.setTripStatisticsValues(this, getTripStatistics(), metricUnits, reportSpeed);
    StatsUtils.setLocationElevationValue(this, Double.NaN, metricUnits);    
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() != android.R.id.home) {
      return false;
    }
    Intent intent = IntentUtils.newIntent(this, TrackListActivity.class);
    startActivity(intent);
    return true;
  }

  /**
   * Gets the aggregated trip statistics for all the recorded tracks or null if
   * there is no track.
   */
  private TripStatistics getTripStatistics() {
    List<Track> tracks = MyTracksProviderUtils.Factory.get(this).getAllTracks();
    TripStatistics tripStatistics = null;
    if (!tracks.isEmpty()) {
      tripStatistics = new TripStatistics(tracks.iterator().next().getStatistics());
      for (int i = 1; i < tracks.size(); i++) {
        tripStatistics.merge(tracks.get(i).getStatistics());
      }
    }
    return tripStatistics;
  }
}
