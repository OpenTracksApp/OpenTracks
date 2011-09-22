// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.content.TrackDataHub;

import android.app.Application;

/**
 * MyTracksApplication for keeping global state.
 * 
 * @author jshih@google.com (Jimmy Shih)
 *
 */
public class MyTracksApplication extends Application {
  private TrackDataHub trackDataHub;

  /**
   * Gets the application's TrackDataHub.
   * 
   */
  public TrackDataHub getTrackDataHub() {
    if (trackDataHub == null) {     
      trackDataHub = TrackDataHub.newInstance(getApplicationContext());
    }
    return trackDataHub;
  }
  
  @Override
  public void onTerminate() {
    trackDataHub = null;
    super.onTerminate();
  }  
}
