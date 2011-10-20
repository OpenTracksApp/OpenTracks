/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks.signalstrength;

import static com.google.android.apps.mytracks.signalstrength.SignalStrengthConstants.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Broadcast listener which gets notified when we start or stop recording a track.
 *
 * @author Rodrigo Damazio
 */
public class RecordingStateReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context ctx, Intent intent) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);

    String action = intent.getAction();
    if (ctx.getString(R.string.track_started_broadcast_action).equals(action)) {
      boolean autoStart = preferences.getBoolean(
          ctx.getString(R.string.settings_auto_start_key), false);
      if (!autoStart) {
        Log.d(TAG, "Not auto-starting signal sampling");
        return;
      }

      startService(ctx);
    } else if (ctx.getString(R.string.track_stopped_broadcast_action).equals(action)) {
      boolean autoStop = preferences.getBoolean(
          ctx.getString(R.string.settings_auto_stop_key), true);
      if (!autoStop) {
        Log.d(TAG, "Not auto-stopping signal sampling");
        return;
      }

      stopService(ctx);
    } else {
      Log.e(TAG, "Unknown action received: " + action);
    }
  }

  // @VisibleForTesting
  protected void stopService(Context ctx) {
    SignalStrengthService.stopService(ctx);
  }

  // @VisibleForTesting
  protected void startService(Context ctx) {
    SignalStrengthService.startService(ctx);
  }
}
