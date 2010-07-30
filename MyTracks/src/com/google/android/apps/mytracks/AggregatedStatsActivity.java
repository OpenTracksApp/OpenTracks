package com.google.android.apps.mytracks;

import java.util.Collection;
import java.util.List;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Activity for viewing the combined statistics for all the recorded tracks.
 * 
 * Other features to add - menu items to change setings.
 * 
 * @author Fergus Nelson
 */
public class AggregatedStatsActivity extends Activity
	implements OnSharedPreferenceChangeListener {

	private final StatsUtilities utils;
	
	private MyTracksProviderUtils tracksProvider;
	
	private boolean metricUnits = true;
	
	public AggregatedStatsActivity() {
		this.utils = new StatsUtilities(this);
		this.tracksProvider = MyTracksProviderUtils.Factory.get(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(MyTracksConstants.TAG, "StatsActivity: onSharedPreferences changed "
		    + key);
		if (key != null) {
	    if (key.equals(MyTracksSettings.METRIC_UNITS)) {
				metricUnits = sharedPreferences.getBoolean(
				    MyTracksSettings.METRIC_UNITS, true);
				utils.setMetricUnits(metricUnits);
				utils.updateUnits();
			} 
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    //TODO - change to custom layout - without dynamic fields
    setContentView(R.layout.stats);

    ScrollView sv = ((ScrollView) findViewById(R.id.scrolly));
    sv.setScrollBarStyle(ScrollView.SCROLLBARS_OUTSIDE_INSET);

    SharedPreferences preferences =
        getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (preferences != null) {
      metricUnits = preferences.getBoolean(MyTracksSettings.METRIC_UNITS,
          true);
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
	}
	
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
		return tracksProvider.retrieveAllTracks();
	}
	
	private void updateView(TripStatistics aggStats) {
		if (aggStats != null) {
			
		}
	}
}
