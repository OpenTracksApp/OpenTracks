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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.BuildConfig;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackEditActivity;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * Wrapper for the track recording service.
 * This handles service start/bind/unbind/stop.
 * The service must be started before it can be bound.
 * Returns the service if it is started and bound.
 *
 * @author Rodrigo Damazio
 */
public class TrackRecordingServiceConnection implements ServiceConnection, DeathRecipient {

    private static final String TAG = TrackRecordingServiceConnection.class.getSimpleName();

    private final Runnable callback;

    private TrackRecordingServiceInterface trackRecordingService;

    public TrackRecordingServiceConnection() {
        callback = null;
    }

    /**
     * @param callback the callback to invoke when the service binding changes
     */
    public TrackRecordingServiceConnection(@NonNull Runnable callback) {
        this.callback = callback;
    }

    /**
     * Starts and binds the service.
     */
    public void startAndBind(Context context) {
        if (trackRecordingService != null) {
            // Service is already started and bound.
            return;
        }

        Log.i(TAG, "Starting the service.");
        context.startService(new Intent(context, TrackRecordingService.class));

        startConnection(context);
    }

    /**
     * Resumes the track recording service connection.
     *
     * @param context the context
     */
    public void startConnection(@NonNull Context context) {
        if (trackRecordingService != null) {
            // Service is already started and bound.
            return;
        }

        Log.i(TAG, "Binding the service.");
        int flags = BuildConfig.DEBUG ? Context.BIND_DEBUG_UNBIND : 0;
        context.bindService(new Intent(context, TrackRecordingService.class), this, flags);
    }

    /**
     * Unbinds the service (but leave it running).
     */
    public void unbind(Context context) {
        try {
            context.unbindService(this);
        } catch (IllegalArgumentException e) {
            // Means not bound to the service. OK to ignore.
        }
        setTrackRecordingService(null);
    }

    /**
     * Unbinds and stops the service.
     */
    public void unbindAndStop(Context context) {
        unbind(context);
        context.stopService(new Intent(context, TrackRecordingService.class));
    }

    /**
     * Gets the track recording service if bound. Returns null otherwise
     */
    public TrackRecordingServiceInterface getServiceIfBound() {
        return trackRecordingService;
    }

    /**
     * Sets the trackRecordingService.
     *
     * @param value the value
     */
    private void setTrackRecordingService(TrackRecordingServiceInterface value) {
        trackRecordingService = value;
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        Log.i(TAG, "Connected to the service.");
        try {
            service.linkToDeath(this, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to bind a death recipient.", e);
        }
        setTrackRecordingService((TrackRecordingServiceInterface) service);
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        Log.i(TAG, "Disconnected from the service.");
        setTrackRecordingService(null);
    }

    @Override
    public void binderDied() {
        Log.d(TAG, "Service died.");
        setTrackRecordingService(null);
    }

    /**
     * Resumes the recording track.
     */
    public void resumeTrack() {
        TrackRecordingServiceInterface service = getServiceIfBound();
        if (service != null) {
            service.resumeCurrentTrack();
        }
    }

    /**
     * Pauses the recording track.
     */
    public void pauseTrack() {
        TrackRecordingServiceInterface service = getServiceIfBound();
        if (service != null) {
            service.pauseCurrentTrack();
        }
    }

    /**
     * Adds a marker.
     *
     * @return the id of the marker or null if none could be created.
     */
    public Marker.Id addMarker(Context context, String name, String category, String description, String photoUrl) {
        TrackRecordingServiceInterface trackRecordingService = getServiceIfBound();
        if (trackRecordingService == null) {
            Log.d(TAG, "Unable to add marker, no track recording service");
        } else {
            try {
                Marker.Id waypoint = trackRecordingService.insertWaypoint(name, category, description, photoUrl);
                if (waypoint != null) {
                    Toast.makeText(context, R.string.marker_add_success, Toast.LENGTH_SHORT).show();
                    return waypoint;
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Unable to add marker.", e);
            }
        }

        Toast.makeText(context, R.string.marker_add_error, Toast.LENGTH_LONG).show();
        return null;
    }

    /**
     * Stops the recording.
     *
     * @param context    the context
     * @param showEditor true to show the editor
     */
    public void stopRecording(@NonNull Context context, boolean showEditor) {
        TrackRecordingServiceInterface trackRecordingService = getServiceIfBound();
        if (trackRecordingService == null) {
            Log.e(TAG, "TrackRecordingService not connected.");
        } else {
            try {
                if (showEditor) {
                    // Need to remember the recordingTrackId before calling endCurrentTrack() as endCurrentTrack() sets the value to -1L.
                    Track.Id recordingTrackId = PreferencesUtils.getRecordingTrackId(context);
                    trackRecordingService.endCurrentTrack();
                    if (PreferencesUtils.isRecording(context)) {
                        Intent intent = IntentUtils.newIntent(context, TrackEditActivity.class)
                                .putExtra(TrackEditActivity.EXTRA_TRACK_ID, recordingTrackId)
                                .putExtra(TrackEditActivity.EXTRA_NEW_TRACK, true);
                        context.startActivity(intent);
                    }
                } else {
                    trackRecordingService.endCurrentTrack();
                }
            } catch (Exception e) {
                //TODO What exception are we catching here? Should be removed...
                Log.e(TAG, "Unable to stop recording.", e);
            }
        }
        unbindAndStop(context);
    }
}
