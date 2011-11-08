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
package com.google.android.apps.mytracks;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.ChartView.Mode;
import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.TrackDataHub.ListenerDataType;
import com.google.android.apps.mytracks.content.TrackDataListener;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.services.tasks.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.stats.DoubleBuffer;
import com.google.android.apps.mytracks.stats.TripStatisticsBuilder;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ZoomControls;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * An activity that displays a chart from the track point provider.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class ChartActivity extends Activity implements TrackDataListener {

  private static final int CHART_SETTINGS_DIALOG = 1;
  private final DoubleBuffer elevationBuffer =
      new DoubleBuffer(Constants.ELEVATION_SMOOTHING_FACTOR);
  private final DoubleBuffer speedBuffer =
      new DoubleBuffer(Constants.SPEED_SMOOTHING_FACTOR);
  private final ArrayList<double[]> pendingPoints = new ArrayList<double[]>();

  private TrackDataHub dataHub;

  // Stats gathered from received data.
  private double profileLength = 0;
  private long startTime = -1;
  private Location lastLocation;
  private double trackMaxSpeed;

  // Modes of operation
  private boolean metricUnits;
  private boolean reportSpeed;

  /*
   * UI elements:
   */
  private ChartView chartView;
  private MenuItem chartSettingsMenuItem;
  private LinearLayout busyPane;
  private ZoomControls zoomControls;

  /**
   * A runnable that can be posted to the UI thread. It will remove the spinner
   * (if any), enable/disable zoom controls and orange pointer as appropriate
   * and redraw.
   */
  private final Runnable updateChart = new Runnable() {
    @Override
    public void run() {
      // Get a local reference in case it's set to null concurrently with this.
      TrackDataHub localDataHub = dataHub;
      if (localDataHub == null || isFinishing()) {
        return;
      }

      busyPane.setVisibility(View.GONE);
      zoomControls.setIsZoomInEnabled(chartView.canZoomIn());
      zoomControls.setIsZoomOutEnabled(chartView.canZoomOut());
      chartView.setShowPointer(localDataHub.isRecordingSelected());
      chartView.invalidate();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.w(TAG, "ChartActivity.onCreate");
    super.onCreate(savedInstanceState);

    // The volume we want to control is the Text-To-Speech volume
    int volumeStream =
        new StatusAnnouncerFactory(ApiFeatures.getInstance()).getVolumeStream();
    setVolumeControlStream(volumeStream);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.mytracks_charts);
    ViewGroup layout = (ViewGroup) findViewById(R.id.elevation_chart);
    chartView = new ChartView(this);
    LayoutParams params =
        new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    layout.addView(chartView, params);

    busyPane = (LinearLayout) findViewById(R.id.elevation_busypane);
    zoomControls = (ZoomControls) findViewById(R.id.elevation_zoom);
    zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        zoomIn();
      }
    });
    zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        zoomOut();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();

    dataHub = ((MyTracksApplication) getApplication()).getTrackDataHub();
    dataHub.registerTrackDataListener(this, EnumSet.of(
        ListenerDataType.SELECTED_TRACK_CHANGED,
        ListenerDataType.TRACK_UPDATES,
        ListenerDataType.POINT_UPDATES,
        ListenerDataType.SAMPLED_OUT_POINT_UPDATES,
        ListenerDataType.WAYPOINT_UPDATES,
        ListenerDataType.DISPLAY_PREFERENCES));
  }

  @Override
  protected void onPause() {
    dataHub.unregisterTrackDataListener(this);
    dataHub = null;

    super.onPause();
  }

  private void zoomIn() {
    chartView.zoomIn();
    zoomControls.setIsZoomInEnabled(chartView.canZoomIn());
    zoomControls.setIsZoomOutEnabled(chartView.canZoomOut());
  }

  private void zoomOut() {
    chartView.zoomOut();
    zoomControls.setIsZoomInEnabled(chartView.canZoomIn());
    zoomControls.setIsZoomOutEnabled(chartView.canZoomOut());
  }

  private void setMode(Mode newMode) {
    if (chartView.getMode() != newMode) {
      chartView.setMode(newMode);
      dataHub.reloadDataForListener(this);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    chartSettingsMenuItem =
        menu.add(0, Constants.MENU_CHART_SETTINGS, 0,
            R.string.chart_settings);
    chartSettingsMenuItem.setIcon(R.drawable.chart_settings);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case Constants.MENU_CHART_SETTINGS:
        showDialog(CHART_SETTINGS_DIALOG);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id == CHART_SETTINGS_DIALOG) {
      final ChartSettingsDialog settingsDialog = new ChartSettingsDialog(this);
      settingsDialog.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(DialogInterface arg0, int which) {
          if (which != DialogInterface.BUTTON_POSITIVE) {
            return;
          }

          for (int i = 0; i < ChartView.NUM_SERIES; i++) {
            chartView.setChartValueSeriesEnabled(i, settingsDialog.isSeriesEnabled(i));
          }
          setMode(settingsDialog.getMode());
          chartView.postInvalidate();
        }
      });
      return settingsDialog;
    }

    return super.onCreateDialog(id);
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    super.onPrepareDialog(id, dialog);

    if (id == CHART_SETTINGS_DIALOG) {
      prepareSettingsDialog((ChartSettingsDialog) dialog);
    }
  }

  private void prepareSettingsDialog(ChartSettingsDialog settingsDialog) {
    settingsDialog.setMode(chartView.getMode());
    for (int i = 0; i < ChartView.NUM_SERIES; i++) {
      settingsDialog.setSeriesEnabled(i, chartView.isChartValueSeriesEnabled(i));
    }
  }

  /**
   * Given a location, creates a new data point for the chart. A data point is
   * an array double[3 or 6], where:
   * data[0] = the time or distance
   * data[1] = the elevation
   * data[2] = the speed
   * and possibly:
   * data[3] = power
   * data[4] = cadence
   * data[5] = heart rate
   *
   * This must be called in order for each point.
   *
   * @param location the location to get data for (this method takes ownership of that location)
   * @param result the resulting point to fill out
   */
  private void fillDataPoint(Location location, double result[]) {
    double timeOrDistance = Double.NaN,
           elevation = Double.NaN,
           speed = Double.NaN,
           power = Double.NaN,
           cadence = Double.NaN,
           heartRate = Double.NaN;

    if (location instanceof MyTracksLocation &&
        ((MyTracksLocation) location).getSensorDataSet() != null) {
      SensorDataSet sensorData = ((MyTracksLocation) location).getSensorDataSet();
      if (sensorData.hasPower()
          && sensorData.getPower().getState() == Sensor.SensorState.SENDING
          && sensorData.getPower().hasValue()) {
        power = sensorData.getPower().getValue();
      }
      if (sensorData.hasCadence()
          && sensorData.getCadence().getState() == Sensor.SensorState.SENDING
          && sensorData.getCadence().hasValue()) {
        cadence = sensorData.getCadence().getValue();
      }
      if (sensorData.hasHeartRate()
          && sensorData.getHeartRate().getState() == Sensor.SensorState.SENDING
          && sensorData.getHeartRate().hasValue()) {
        heartRate = sensorData.getHeartRate().getValue();
      }
    }

    // TODO: Account for segment splits?
    Mode mode = chartView.getMode();
    switch (mode) {
      case BY_DISTANCE:
        timeOrDistance = profileLength / 1000.0;
        if (lastLocation != null) {
          double d = lastLocation.distanceTo(location);
          if (metricUnits) {
            profileLength += d;
          } else {
            profileLength += d * UnitConversions.KM_TO_MI;
          }
        }
        break;
      case BY_TIME:
        if (startTime == -1) {
          // Base case
          startTime = location.getTime();
        }
        timeOrDistance = (location.getTime() - startTime);
        break;
      default:
        Log.w(TAG, "ChartActivity unknown mode: " + mode);
    }

    elevationBuffer.setNext(metricUnits
        ? location.getAltitude()
        : location.getAltitude() * UnitConversions.M_TO_FT);
    elevation = elevationBuffer.getAverage();

    if (lastLocation == null) {
      if (Math.abs(location.getSpeed() - 128) > 1) {
        speedBuffer.setNext(location.getSpeed());
      }
    } else if (TripStatisticsBuilder.isValidSpeed(
        location.getTime(), location.getSpeed(), lastLocation.getTime(),
        lastLocation.getSpeed(), speedBuffer)
        && (location.getSpeed() <= trackMaxSpeed)) {
      speedBuffer.setNext(location.getSpeed());
    }
    speed = speedBuffer.getAverage() * 3.6;
    if (!metricUnits) {
      speed *= UnitConversions.KM_TO_MI;
    }
    if (!reportSpeed) {
      if (speed != 0) {
        // Format as hours per unit
        speed = (60.0 / speed);
      } else {
        speed = Double.NaN;
      }
    }

    // Keep a copy so the location can be reused.
    lastLocation = location;

    if (result != null) {
      result[0] = timeOrDistance;
      result[1] = elevation;
      result[2] = speed;
      result[3] = power;
      result[4] = cadence;
      result[5] = heartRate;
    }
  }

  @Override
  public void onProviderStateChange(ProviderState state) {
    // We don't care.
  }

  @Override
  public void onCurrentLocationChanged(Location loc) {
    // We don't care.
  }

  @Override
  public void onCurrentHeadingChanged(double heading) {
    // We don't care.
  }

  @Override
  public void onSelectedTrackChanged(Track track, boolean isRecording) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        busyPane.setVisibility(View.VISIBLE);
      }
    });
  }

  @Override
  public void onTrackUpdated(Track track) {
    if (track == null || track.getStatistics() == null) {
      trackMaxSpeed = 0.0;
      return;
    }

    trackMaxSpeed = track.getStatistics().getMaxSpeed();
  }

  @Override
  public void clearTrackPoints() {
    profileLength = 0;
    lastLocation = null;
    startTime = -1;
    elevationBuffer.reset();
    chartView.reset();
    speedBuffer.reset();
    pendingPoints.clear();

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        chartView.resetScroll();
      }
    });
  }

  @Override
  public void onNewTrackPoint(Location loc) {
    if (LocationUtils.isValidLocation(loc)) {
      double[] point = new double[6];
      fillDataPoint(loc, point);
      pendingPoints.add(point);
    }
  }

  @Override
  public void onSampledOutTrackPoint(Location loc) {
    // Still account for the point in the smoothing buffers.
    fillDataPoint(loc, null);
  }

  @Override
  public void onSegmentSplit() {
    // Do nothing.
  }

  @Override
  public void onNewTrackPointsDone() {
    chartView.addDataPoints(pendingPoints);
    pendingPoints.clear();
    runOnUiThread(updateChart);
  }

  @Override
  public void clearWaypoints() {
    chartView.clearWaypoints();
  }

  @Override
  public void onNewWaypoint(Waypoint wpt) {
    chartView.addWaypoint(wpt);
  }

  @Override
  public void onNewWaypointsDone() {
    runOnUiThread(updateChart);
  }

  @Override
  public boolean onUnitsChanged(boolean metric) {
    boolean changed = metric != this.metricUnits;
    if (!changed) return false;

    this.metricUnits = metric;

    chartView.setMetricUnits(metric);

    return true;  // Reload data
  }

  @SuppressWarnings("hiding")
  @Override
  public boolean onReportSpeedChanged(boolean reportSpeed) {
    boolean changed = reportSpeed != this.reportSpeed;
    if (!changed) return false;

    this.reportSpeed = reportSpeed;

    chartView.setReportSpeed(reportSpeed, this);

    return true;  // Reload data
  }
}
