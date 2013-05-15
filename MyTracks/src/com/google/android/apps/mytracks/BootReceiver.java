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
package com.google.android.apps.mytracks;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.services.RemoveTempFilesService;
import com.google.android.apps.mytracks.services.TrackRecordingService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class handles MyTracks related broadcast messages.
 *
 * One example of a broadcast message that this class is interested in,
 * is notification about the phone boot.  We may want to resume a previously
 * started tracking session if the phone crashed (hopefully not), or the user
 * decided to swap the battery or some external event occurred which forced
 * a phone reboot.
 *
 * This class simply delegates to {@link TrackRecordingService} to make a
 * decision whether to continue with the previous track (if any), or just
 * abandon it.
 * 
 * @author Bartlomiej Niechwiej
 */
public class BootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(TAG, "BootReceiver.onReceive: " + intent.getAction());
    if (ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
      Intent startIntent = new Intent(context, TrackRecordingService.class)
          .putExtra(TrackRecordingService.RESUME_TRACK_EXTRA_NAME, true);
      context.startService(startIntent);

      Intent removeTempFilesIntent = new Intent(context, RemoveTempFilesService.class);
      context.startService(removeTempFilesIntent);
    } else {
      Log.w(TAG, "BootReceiver: unsupported action");
    }
  }
}
