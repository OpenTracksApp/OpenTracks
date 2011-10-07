/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks.maps;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtilsFactory;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;


/**
 * A dynamic speed path descriptor.
 *
 * @author Vangelis S.
 */
public class DynamicSpeedTrackPathDescriptor 
  implements TrackPathDescriptor, OnSharedPreferenceChangeListener {
  private int slowSpeed;
  private int normalSpeed;
  private int speedMargin;
  private double averageMovingSpeed;
  private final Context context;
  
  public DynamicSpeedTrackPathDescriptor(Context context){
    this.context = context;
    SharedPreferences prefs = context.getSharedPreferences(Constants.SETTINGS_NAME, 0);

    if (prefs == null) {
      speedMargin = 25;
      return;
    }
    prefs.registerOnSharedPreferenceChangeListener(this);

    speedMargin = Integer.parseInt(prefs.getString(
        context.getString(R.string.track_color_mode_dynamic_speed_variation_key), "25"));
  }
  
  /**
   * Get the slow speed calculated based on the % below the average speed.
   * @return The speed limit considered as slow.
   */
  public int getSlowSpeed()
  {
    slowSpeed = (int) (averageMovingSpeed - (averageMovingSpeed * speedMargin / 100)); 
    return slowSpeed;
  }
  
  /**
   * Get the medium speed calculated based on the % above the average speed.
   * @return The speed limit considered as normal.
   */
  public int getNormalSpeed()
  {
    normalSpeed = (int) (averageMovingSpeed + (averageMovingSpeed * speedMargin / 100)); 
    return normalSpeed;
  }
  
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    Log.d(TAG, "DynamicSpeedTrackPathDescriptor: onSharedPreferences changed " + key);
    if (key == null 
    	|| !key.equals(context.getString(R.string.track_color_mode_dynamic_speed_variation_key))) {
      return;
    }
    SharedPreferences prefs = context.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    	
    if (prefs == null) {
      speedMargin = 25;
      return;
    }
        
    speedMargin = 
        Integer.parseInt(
    	    prefs.getString(
    	        context.getString(R.string.track_color_mode_dynamic_speed_variation_key), "25"));
    }

  @Override
  public boolean needsRedraw() {
    SharedPreferences prefs = context.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    long currentTrackId = prefs.getLong(context.getString(R.string.selected_track_key), -1);
    if(currentTrackId == -1) {
      // Could not find track. 
      return false; 
    }
    Track track = MyTracksProviderUtilsFactory.get(context).getTrack(currentTrackId);
    TripStatistics stats = track.getStatistics();
    double newaverageSpeed = (int) Math.floor(stats.getAverageMovingSpeed() * 3.6);
    
    if(averageMovingSpeed == 0) {
      averageMovingSpeed = newaverageSpeed;
      return true;
    }
    
    double difference = Math.max(averageMovingSpeed, newaverageSpeed);

    if (difference == 0.0) {
      difference = 0.0;
    } else {
      difference = Math.abs(averageMovingSpeed - newaverageSpeed) / difference * 100;
    }
    if(difference >= 20) {
      averageMovingSpeed = newaverageSpeed;
      return true;
    }
    return false;
  }
}