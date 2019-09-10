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

package de.dennisguse.opentracks.services;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.R;

/**
 * A service to control starting and stopping of a recording.
 * This service, through the AndroidManifest.xml, is configured to only allow components of the same application to invoke it.
 * This application delegates starting and stopping a recording to {@link TrackRecordingService} using RPC calls.
 *
 * @author Jimmy Shih
 */
public class ControlRecordingService extends IntentService implements ServiceConnection {

    private static final String TAG = ControlRecordingService.class.getSimpleName();

    private ITrackRecordingService trackRecordingService;
    private boolean connected = false;

    public ControlRecordingService() {
        super(ControlRecordingService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Intent newIntent = new Intent(this, TrackRecordingService.class);
        startService(newIntent);
        bindService(newIntent, this, 0);
    }

    @VisibleForTesting
    @Override
    public IBinder onBind(@Nullable Intent intent) {
        return new LocalBinder();
    }

    /**
     * Handles the intent to start or stop a recording.
     *
     * @param intent  to be handled
     * @param service the trackRecordingService
     */
    @VisibleForTesting
    void onHandleIntent(Intent intent, ITrackRecordingService service) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(getString(R.string.track_action_start))) {
                service.startNewTrack();
            } else if (action.equals(getString(R.string.track_action_end))) {
                service.endCurrentTrack();
            } else if (action.equals(getString(R.string.track_action_pause))) {
                service.pauseCurrentTrack();
            } else if (action.equals(getString(R.string.track_action_resume))) {
                service.resumeCurrentTrack();
            }
        }
        if (connected) {
            unbindService(this);
            connected = false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        trackRecordingService = (ITrackRecordingService) service;
        notifyConnected();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        connected = false;
    }

    /**
     * Notifies all threads that connection to {@link TrackRecordingService} is available.
     */
    private synchronized void notifyConnected() {
        connected = true;
        notifyAll();
    }

    /**
     * Waits until the connection to {@link TrackRecordingService} is available.
     */
    private synchronized void waitConnected() {
        while (!connected) {
            try {
                wait();
            } catch (InterruptedException e) {
                // can safely ignore
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        waitConnected();
        onHandleIntent(intent, trackRecordingService);
    }

    @VisibleForTesting
    class LocalBinder extends Binder {
        ControlRecordingService getService() {
            return ControlRecordingService.this;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (connected) {
            unbindService(this);
            connected = false;
        }
    }
}
