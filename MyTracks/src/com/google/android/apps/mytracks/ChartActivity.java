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

import com.google.android.apps.mytracks.ChartView.Mode;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackPointsColumns;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.stats.DoubleBuffer;
import com.google.android.apps.mytracks.stats.TripStatisticsBuilder;
import com.google.android.apps.mytracks.util.MyTracksUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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

/**
 * An activity that displays a chart from the track point provider.
 * 
 * @author Sandor Dornbush
 */
public class ChartActivity extends Activity implements
    SharedPreferences.OnSharedPreferenceChangeListener {

  private double profileLength = 0;

  private boolean metricUnits = true;
  private boolean reportSpeed = true;

  /**
   * The track id that is displayed.
   */
  private long selectedTrackId = -1;

  /**
   * Id of the last location that was seen when reading tracks from the
   * provider. This is used to determine which locations are new compared to the
   * last time the chart was updated.
   */
  private long lastSeenLocationId = -1;

  private long startTime = -1;

  private Location lastLocation;

  /**
   * The id of the track currently being recorded.
   */
  private long recordingTrackId = -1;

  private final DoubleBuffer elevationBuffer =
      new DoubleBuffer(MyTracksConstants.ELEVATION_SMOOTHING_FACTOR);
  private final DoubleBuffer speedBuffer =
      new DoubleBuffer(MyTracksConstants.SPEED_SMOOTHING_FACTOR);

  private Mode mode = Mode.BY_DISTANCE;

  /**
   * Utilities to deal with the database.
   */
  private MyTracksProviderUtils providerUtils;

  /*
   * UI elements:
   */
  private ChartView cv;
  private MenuItem chartSettingsMenuItem;
  private LinearLayout busyPane;
  private ZoomControls zoomControls;

  /** Handler for callbacks to the UI thread */
  private final Handler uiHandler = new Handler();

  /**
   * A runnable that can be posted to the UI thread. It will remove the spinner
   * (if any), enable/disable zoom controls and orange pointer as appropriate
   * and redraw.
   */
  private final Runnable updateChart = new Runnable() {
    @Override
    public void run() {
      busyPane.setVisibility(View.GONE);
      zoomControls.setIsZoomInEnabled(cv.canZoomIn());
      zoomControls.setIsZoomOutEnabled(cv.canZoomOut());
      cv.setShowPointer(selectedTrackIsRecording());
      cv.invalidate();
    }
  };

  /**
   * A runnable that can be posted to the UI thread. It will show the spinner.
   */
  private final Runnable showSpinner = new Runnable() {
    @Override
    public void run() {
      busyPane.setVisibility(View.VISIBLE);
    }
  };

  /**
   * An observer for the tracks provider. Will listen to new track points being
   * added and update the chart if necessary.
   */
  private ContentObserver observer;

  /**
   * An observer for the waypoints provider. Will listen to new way points being
   * added and update the chart if necessary.
   */
  private ContentObserver waypointObserver;

  /**
   * A thread with a looper. Post to updateTrackHandler to execute Runnables on
   * this thread.
   */
  private final HandlerThread updateTrackThread =
      new HandlerThread("updateTrackThread");

  /** Handler for updateTrackThread */
  private Handler updateTrackHandler;

  /**
   * A runnable that update the profile from the provider.
   */
  private final Runnable updateTrackRunnable = new Runnable() {
    @Override
    public void run() {
      Log.d(MyTracksConstants.TAG, "MyTracks: Updating chart.");
      Track track = providerUtils.getTrack(recordingTrackId);
      if (track == null) {
        Log.w(MyTracksConstants.TAG, "MyTracks: track not found");
        return;
      }
      Cursor cursor = null;
      try {
        cursor = providerUtils.getLocationsCursor(recordingTrackId,
            lastSeenLocationId + 1,
            MyTracksConstants.MAX_DISPLAYED_TRACK_POINTS - cv.getData().size(),
            true);
        if (cursor != null) {
          if (cursor.moveToLast()) {
            final int idColumnIdx =
                cursor.getColumnIndexOrThrow(TrackPointsColumns._ID);
            ArrayList<double[]> data = new ArrayList<double[]>();
            do {
              lastSeenLocationId = cursor.getLong(idColumnIdx);
              Location location = providerUtils.createLocation(cursor);
              if (location != null && MyTracksUtils.isValidLocation(location)) {
                data.add(getDataPoint(location, track));
              }
            } while (cursor.moveToPrevious());
            cv.addDataPoints(data);
          }
        }
      } catch (RuntimeException e) {
        Log.w(MyTracksConstants.TAG, "Caught an unexpected exception.", e);
      } finally {
        if (cursor != null) {
          cursor.close();
        }
        uiHandler.post(new Runnable() {
          public void run() {
            cv.invalidate();
          }
        });
      }
    }
  };

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key != null) {
      if (key.equals(MyTracksSettings.SELECTED_TRACK)) {
        selectedTrackId =
            sharedPreferences.getLong(MyTracksSettings.SELECTED_TRACK, -1);
        readProfileAsync();
      } else if (key.equals(MyTracksSettings.METRIC_UNITS)) {
        metricUnits =
            sharedPreferences.getBoolean(MyTracksSettings.METRIC_UNITS, true);
        cv.setMetricUnits(metricUnits);
        readProfileAsync();
      } else if (key.equals(MyTracksSettings.REPORT_SPEED)) {
        reportSpeed =
            sharedPreferences.getBoolean(MyTracksSettings.REPORT_SPEED, true);
        cv.setReportSpeed(reportSpeed, this);
        readProfileAsync();
      } else if (key.equals(MyTracksSettings.RECORDING_TRACK)) {
        recordingTrackId =
            sharedPreferences.getLong(MyTracksSettings.RECORDING_TRACK, -1);
        runOnUiThread(updateChart);
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.w(MyTracksConstants.TAG, "ChartActivity.onCreate");
    super.onCreate(savedInstanceState);
    MyTracks.getInstance().setChartActivity(this);
    providerUtils = MyTracksProviderUtils.Factory.get(this);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.mytracks_elevation);
    ViewGroup layout = (ViewGroup) findViewById(R.id.elevation_chart);
    cv = new ChartView(this);
    cv.setMode(this.mode);
    LayoutParams params =
        new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    layout.addView(cv, params);

    SharedPreferences preferences =
        getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (preferences != null) {
      selectedTrackId =
          preferences.getLong(MyTracksSettings.SELECTED_TRACK, -1);
      recordingTrackId =
          preferences.getLong(MyTracksSettings.RECORDING_TRACK, -1);
      metricUnits = preferences.getBoolean(MyTracksSettings.METRIC_UNITS, true);
      cv.setMetricUnits(metricUnits);
      reportSpeed = preferences.getBoolean(MyTracksSettings.REPORT_SPEED, true);
      cv.setReportSpeed(reportSpeed, this);
      preferences.registerOnSharedPreferenceChangeListener(this);
    }

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

    updateTrackThread.start();
    updateTrackHandler = new Handler(updateTrackThread.getLooper());

    // Register observer for the track point provider:
    Handler contentHandler = new Handler();
    observer = new ContentObserver(contentHandler) {
      @Override
      public void onChange(boolean selfChange) {
        Log.d(MyTracksConstants.TAG, "ChartActivity: ContentObserver.onChange");
        // Check for any new locations and append them to the currently
        // recording track:
        if (!MyTracks.getInstance().isRecording()) {
          // No track is being recorded. We should not be here.
          return;
        }
        if (selectedTrackId != recordingTrackId) {
          // no track, or one other than the recording track is selected, don't
          // bother.
          return;
        }
        // Update can potentially be lengthy, put it in its own thread:
        updateTrackHandler.post(updateTrackRunnable);
        super.onChange(selfChange);
      }
    };

    waypointObserver = new ContentObserver(contentHandler) {
      @Override
      public void onChange(boolean selfChange) {
        Log.d(MyTracksConstants.TAG,
            "MyTracksMap: ContentObserver.onChange waypoints");
        if (selectedTrackId < 0) {
          return;
        }
        Thread t = new Thread() {
          @Override
          public void run() {
            readWaypoints();
            ChartActivity.this.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                cv.invalidate();
              }
            });
          }
        };
        t.start();
        super.onChange(selfChange);
      }
    };

    readProfileAsync();
  }

  @Override
  protected void onPause() {
    super.onPause();
    unregisterContentObservers();
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Make sure any updates that might have happened are propagated to this
    // activity:
    observer.onChange(false);
    waypointObserver.onChange(false);
    registerContentObservers();
  }

  /**
   * Register the content observer for the map overlay.
   */
  private void registerContentObservers() {
    getContentResolver().registerContentObserver(TrackPointsColumns.CONTENT_URI,
        false/* notifyForDescendents */, observer);
    getContentResolver().registerContentObserver(WaypointsColumns.CONTENT_URI,
        false/* notifyForDescendents */, waypointObserver);
  }

  /**
   * Unregister the content observer for the map overlay.
   */
  private void unregisterContentObservers() {
    getContentResolver().unregisterContentObserver(observer);
    getContentResolver().unregisterContentObserver(waypointObserver);
  }

  private boolean selectedTrackIsRecording() {
    return selectedTrackId == recordingTrackId;
  }

  private void zoomIn() {
    cv.zoomIn();
    zoomControls.setIsZoomInEnabled(cv.canZoomIn());
    zoomControls.setIsZoomOutEnabled(cv.canZoomOut());
  }

  private void zoomOut() {
    cv.zoomOut();
    zoomControls.setIsZoomInEnabled(cv.canZoomIn());
    zoomControls.setIsZoomOutEnabled(cv.canZoomOut());
  }

  public void setMode(Mode newMode) {
    if (this.mode != newMode) {
      this.mode = newMode;
      cv.setMode(this.mode);
      readProfileAsync();
    }
  }

  public Mode getMode() {
    return mode;
  }

  public void setSeriesEnabled(int index, boolean enabled) {
    cv.getChartValueSeries(index).setEnabled(enabled);
    runOnUiThread(updateChart);
  }

  public boolean isSeriesEnabled(int index) {
    return cv.getChartValueSeries(index).isEnabled();
  }

  private void readWaypoints() {
    if (selectedTrackId < 0) {
      return;
    }
    Cursor cursor = null;
    cv.clearWaypoints();
    try {
      // We will silently drop extra waypoints to make the app responsive.
      cursor =
          providerUtils.getWaypointsCursor(selectedTrackId, 0,
              MyTracksConstants.MAX_DISPLAYED_TRACK_POINTS);
      if (cursor != null) {
        if (cursor.moveToFirst()) {
          do {
            Waypoint wpt = providerUtils.createWaypoint(cursor);
            cv.addWaypoint(wpt);
          } while (cursor.moveToNext());
        }
      }
    } catch (RuntimeException e) {
      Log.w(MyTracksConstants.TAG, "Caught an unexpected exception.", e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    chartSettingsMenuItem =
        menu.add(0, MyTracksConstants.MENU_CHART_SETTINGS, 0,
            R.string.chart_settings);
    chartSettingsMenuItem.setIcon(R.drawable.chart_settings);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case MyTracksConstants.MENU_CHART_SETTINGS:
        MyTracks.getInstance().showDialogSafely(MyTracks.DIALOG_CHART_SETTINGS);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Given a location, creates a new data point for the chart. A data point is
   * an array double[2], where:
   * data[0] = the time or distance
   * data[1] = the elevation
   * data[2] = the speed
   * 
   * @param location a location
   * @return the data point
   */
  public double[] getDataPoint(Location location, Track track) {
    double[] result = new double[3];
    switch (mode) {
      case BY_DISTANCE:
        result[0] = profileLength;
        if (lastLocation != null) {
          double d = lastLocation.distanceTo(location);
          if (metricUnits) {
            profileLength += d / 1000.0;
          } else {
            profileLength += d * UnitConversions.KM_TO_MI / 1000.0;
          }
        }
        break;
      case BY_TIME:
        if (startTime == -1) {
          // Base case
          startTime = location.getTime();
        }
        result[0] = (location.getTime() - startTime);
        break;
      default:
        Log.w(MyTracksConstants.TAG, "ChartActivity unknown mode: " + mode);
    }

    elevationBuffer.setNext(metricUnits
        ? location.getAltitude()
        : location.getAltitude() * UnitConversions.M_TO_FT);
    result[1] = elevationBuffer.getAverage();

    if (lastLocation == null) {
      if (Math.abs(location.getSpeed() - 128) > 1) {
        speedBuffer.setNext(location.getSpeed());
      }
    } else if (TripStatisticsBuilder.isValidSpeed(
        location.getTime(), location.getSpeed(), lastLocation.getTime(),
        lastLocation.getSpeed(), speedBuffer)
        && (location.getSpeed() <= track.getStatistics().getMaxSpeed())) {
      speedBuffer.setNext(location.getSpeed());
    }
    result[2] = speedBuffer.getAverage() * 3.6;
    if (!metricUnits) {
      result[2] *= UnitConversions.KM_TO_MI;
    }
    if (!reportSpeed && (result[2] != 0)) {
      // Format as hours per unit
      result[2] = (60.0 / result[2]);
    }
    lastLocation = location;
    return result;
  }

  /**
   * Sets the chart data points reading from the provider. This is non-blocking.
   */
  private void readProfileAsync() {
    cv.reset();
    updateTrackHandler.post(new Runnable() {
      public void run() {
        runOnUiThread(showSpinner);
        readProfile();
        readWaypoints();
        runOnUiThread(updateChart);
      }
    });
  }

  /**
   * Reads the track profile from the provider. This is a blocking function and
   * should not be run from the UI thread.
   */
  private void readProfile() {
    profileLength = 0;
    lastLocation = null;
    startTime = -1;
    Cursor cursor = null;
    if (selectedTrackId < 0) {
      return;
    }
    Track track = providerUtils.getTrack(selectedTrackId);
    if (track == null) {
      return;
    }
    long lastLocationRead = track.getStartId();
    long totalLocations = track.getStopId() - track.getStartId();

    // Limit the number of chart readings. Ideally we would want around 1024.
    int chartSamplingFrequency = Math.max(1, (int) (totalLocations / 1024.0));
    int bufferSize = 1024;
    try {
      final ArrayList<double[]> theData = new ArrayList<double[]>();
      int points = 0;
      while (lastLocationRead < track.getStopId()) {
        cursor = providerUtils.getLocationsCursor(
            selectedTrackId, lastLocationRead, bufferSize, false);
        if (cursor != null) {
          elevationBuffer.reset();
          speedBuffer.reset();
          if (cursor.moveToFirst()) {
            final int idColumnIdx =
                cursor.getColumnIndexOrThrow(TrackPointsColumns._ID);
            while (cursor.moveToNext()) {
              points++;
              Location location = providerUtils.createLocation(cursor);
              if (MyTracksUtils.isValidLocation(location)) {
                lastLocationRead = lastSeenLocationId =
                    cursor.getLong(idColumnIdx);
                double[] point = getDataPoint(location, track);
                if (points % chartSamplingFrequency == 0) {
                  theData.add(point);
                }
              }
            }
          } else {
            lastLocationRead += bufferSize;
          }
        } else {
          lastLocationRead += bufferSize;
        }
        cursor.close();
        cursor = null;
      }
      runOnUiThread(new Runnable() {
        public void run() {
          cv.setDataPoints(theData);
        }
      });
    } catch (RuntimeException e) {
      Log.w(MyTracksConstants.TAG, "Caught unexpected exception.", e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }
}
