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
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.ServiceUtils;

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

    private ITrackRecordingService trackRecordingService;

    /**
     * Constructor.
     *
     * @param context  the context
     * @param callback the callback to invoke when the service binding changes
     */
    public TrackRecordingServiceConnection(Context context, Runnable callback) {
        this.callback = callback;
    }

    /**
     * Starts and binds the service.
     */
    public void startAndBind(Context context) {
        bindService(context, true);
    }

    /**
     * Binds the service if it is started.
     */
    public void bindIfStarted(Context context) {
        bindService(context, false);
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
    public ITrackRecordingService getServiceIfBound() {
        return trackRecordingService;
    }


    /**
     * Sets the trackRecordingService.
     *
     * @param value the value
     */
    private void setTrackRecordingService(ITrackRecordingService value) {
        trackRecordingService = value;
        if (callback != null) {
            callback.run();
        }
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

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        Log.i(TAG, "Connected to the service.");
        try {
            service.linkToDeath(this, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to bind a death recipient.", e);
        }
        setTrackRecordingService((ITrackRecordingService) service);
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
     * Binds the service if it is started.
     *
     * @param startIfNeeded start the service if needed
     */
    private void bindService(Context context, boolean startIfNeeded) {
        if (trackRecordingService != null) {
            // Service is already started and bound.
            return;
        }

        if (!startIfNeeded && !ServiceUtils.isTrackRecordingServiceRunning(context)) {
            Log.d(TAG, "Service is not started. Not binding it.");
            return;
        }

        if (startIfNeeded) {
            Log.i(TAG, "Starting the service.");
            context.startService(new Intent(context, TrackRecordingService.class));
        }

        Log.i(TAG, "Binding the service.");
        int flags = BuildConfig.DEBUG ? Context.BIND_DEBUG_UNBIND : 0;
        context.bindService(new Intent(context, TrackRecordingService.class), this, flags);
    }

    /**
     * Resumes the track recording service connection.
     *
     * @param context the context
     */
    public void startConnection(@NonNull Context context) {
        bindIfStarted(context);
        if (!ServiceUtils.isTrackRecordingServiceRunning(context)) {
            resetRecordingState(context);
        }
    }

    /**
     * Resumes the recording track.
     */
    public void resumeTrack() {
        ITrackRecordingService service = getServiceIfBound();
        if (service != null) {
            service.resumeCurrentTrack();
        }
    }

    /**
     * Pauses the recording track.
     */
    public void pauseTrack() {
        ITrackRecordingService service = getServiceIfBound();
        if (service != null) {
            service.pauseCurrentTrack();
        }
    }

    private static void resetRecordingState(Context context) {
        if (PreferencesUtils.isRecording(context)) {
            PreferencesUtils.setLong(context, R.string.recording_track_id_key, PreferencesUtils.RECORDING_TRACK_ID_DEFAULT);
        }
        boolean recordingTrackPaused = PreferencesUtils.isRecordingTrackPaused(context);
        if (!recordingTrackPaused) {
            PreferencesUtils.defaultRecordingTrackPaused(context);
        }
    }

    /**
     * Adds a marker.
     *
     * @return the id of the marker or -1L if none could be created.
     */
    public long addMarker(Context context, String name, String category, String description, String photoUrl) {
        ITrackRecordingService trackRecordingService = getServiceIfBound();
        if (trackRecordingService == null) {
            Log.d(TAG, "Unable to add marker, no track recording service");
        } else {
            try {
                long markerId = trackRecordingService.insertWaypoint(name, category, description, photoUrl);
                if (markerId != -1L) {
                    Toast.makeText(context, R.string.marker_add_success, Toast.LENGTH_SHORT).show();
                    return markerId;
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Unable to add marker.", e);
            }
        }

        Toast.makeText(context, R.string.marker_add_error, Toast.LENGTH_LONG).show();
        return -1L;
    }

    /**
     * Stops the recording.
     *
     * @param context    the context
     * @param showEditor true to show the editor
     */
    public void stopRecording(@NonNull Context context, boolean showEditor) {
        ITrackRecordingService trackRecordingService = getServiceIfBound();
        if (trackRecordingService == null) {
            resetRecordingState(context);
        } else {
            try {
                if (showEditor) {
                    // Need to remember the recordingTrackId before calling endCurrentTrack() as endCurrentTrack() sets the value to -1L.
                    long recordingTrackId = PreferencesUtils.getRecordingTrackId(context);
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
