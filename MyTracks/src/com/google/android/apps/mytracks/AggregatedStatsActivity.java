package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.content.MyTracksProviderUtilsFactory;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * Activity for viewing the combined statistics for all the recorded tracks.
 *
 * Other features to add - menu items to change setings.
 *
 * @author Fergus Nelson
 */
public class AggregatedStatsActivity extends Activity implements
        OnSharedPreferenceChangeListener {

  private final StatsUtilities utils;

  private MyTracksProviderUtils tracksProvider;

  private boolean metricUnits = true;

  public AggregatedStatsActivity() {
    this.utils = new StatsUtilities(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
          String key) {
    Log.d(Constants.TAG, "StatsActivity: onSharedPreferences changed "
            + key);
    if (key != null) {
      if (key.equals(getString(R.string.metric_units_key))) {
        metricUnits = sharedPreferences.getBoolean(
                getString(R.string.metric_units_key), true);
        utils.setMetricUnits(metricUnits);
        utils.updateUnits();
        loadAggregatedStats();
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.tracksProvider = MyTracksProviderUtilsFactory.get(this);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.stats);

    ScrollView sv = ((ScrollView) findViewById(R.id.scrolly));
    sv.setScrollBarStyle(ScrollView.SCROLLBARS_OUTSIDE_INSET);

    SharedPreferences preferences = getSharedPreferences(
            Constants.SETTINGS_NAME, 0);
    if (preferences != null) {
      metricUnits = preferences.getBoolean(getString(R.string.metric_units_key), true);
      preferences.registerOnSharedPreferenceChangeListener(this);
    }
    utils.setMetricUnits(metricUnits);
    utils.updateUnits();
    utils.setSpeedLabel(R.id.speed_label, R.string.speed, R.string.pace_label);
    utils.setSpeedLabels();

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);
    if (metrics.heightPixels > 600) {
      ((TextView) findViewById(R.id.speed_register)).setTextSize(80.0f);
    }
    loadAggregatedStats();
  }

  /**
   * 1. Reads tracks from the db
   * 2. Merges the trip stats from the tracks
   * 3. Updates the view
   */
  private void loadAggregatedStats() {
    List<Track> tracks = retrieveTracks();
    TripStatistics rollingStats = null;
    if (!tracks.isEmpty()) {
      rollingStats = new TripStatistics(tracks.iterator().next()
              .getStatistics());
      for (int i = 1; i < tracks.size(); i++) {
        rollingStats.merge(tracks.get(i).getStatistics());
      }
    }
    updateView(rollingStats);
  }

  private List<Track> retrieveTracks() {
    return tracksProvider.getAllTracks();
  }

  private void updateView(TripStatistics aggStats) {
    if (aggStats != null) {
      utils.setAllStats(aggStats);
    }
  }
}
