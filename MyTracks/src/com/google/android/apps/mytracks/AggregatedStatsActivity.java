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
import com.google.android.apps.mytracks.util.StatsUtils;
import com.google.android.maps.mytracks.R;

import android.os.Bundle;

import java.util.List;

/**
 * An activity to view aggregated stats from all recorded tracks.
 *
 * @author Fergus Nelson
 */
public class AggregatedStatsActivity extends AbstractMyTracksActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    StatsUtils.setTripStatisticsValues(this, getTripStatistics());
    StatsUtils.setLocationValues(this, null, false);
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.stats;
  }

  /**
   * Gets the aggregated trip statistics for all the recorded tracks or null if
   * there is no track.
   */
  private TripStatistics getTripStatistics() {
    List<Track> tracks = MyTracksProviderUtils.Factory.get(this).getAllTracks();
    TripStatistics tripStatistics = null;
    if (!tracks.isEmpty()) {
      tripStatistics = new TripStatistics(tracks.iterator().next().getTripStatistics());
      for (int i = 1; i < tracks.size(); i++) {
        tripStatistics.merge(tracks.get(i).getTripStatistics());
      }
    }
    return tripStatistics;
  }
}
