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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import de.dennisguse.opentracks.BuildConfig;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Marker;

/**
 * Wrapper for the track recording service.
 * This handles service start/bind/unbind/stop.
 * The service must be started before it can be bound.
 * Returns the service if it is started and bound.
 *
 * @author Rodrigo Damazio
 */
public class TrackRecordingServiceConnection {

    private static final String TAG = TrackRecordingServiceConnection.class.getSimpleName();

    private final Callback callback;

    private TrackRecordingService trackRecordingService;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "Connected to the service: " + service);
            try {
                service.linkToDeath(deathRecipient, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to bind a death recipient.", e);
            }
            setTrackRecordingService(((TrackRecordingService.Binder) service).getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "Disconnected from the service.");
            setTrackRecordingService(null);
        }
    };

    private final DeathRecipient deathRecipient = () -> {
        Log.d(TAG, "Service died.");
        setTrackRecordingService(null);
    };

    public TrackRecordingServiceConnection(@NonNull Callback callback) {
        this.callback = callback;
    }

    public void bind(@NonNull Context context) {
        if (trackRecordingService != null) {
            callback.onConnected(trackRecordingService, this);
            return;
        }

        Log.i(TAG, "Binding the service.");
        int flags = BuildConfig.DEBUG ? Context.BIND_DEBUG_UNBIND : 0;
        context.bindService(new Intent(context, TrackRecordingService.class), serviceConnection, flags);
    }

    /**
     * Unbinds the service (but leave it running).
     */
    //TODO This is often called for one-shot operations and should be refactored as unbinding is required.
    public void unbind(Context context) {
        try {
            context.unbindService(serviceConnection);
        } catch (IllegalArgumentException e) {
            // Means not bound to the service. OK to ignore.
        }
        setTrackRecordingService(null);
    }

    public void stopService(Context context) {
        context.stopService(new Intent(context, TrackRecordingService.class));
    }

    public void unbindAndStop(Context context) {
        unbind(context);
        stopService(context);
    }

    private void setTrackRecordingService(TrackRecordingService value) {
        trackRecordingService = value;
        if (value != null) {
            callback.onConnected(value, this);
        }
    }

    //TODO Move to some other place; not needed here.
    @Nullable
    public Marker.Id addMarker(Context context, String name, String category, String description, String photoUrl) {
        if (trackRecordingService == null) {
            Log.d(TAG, "Unable to add marker, no track recording service");
            return null;
        }

        try {
            Marker.Id marker = trackRecordingService.insertMarker(name, category, description, photoUrl);
            if (marker != null) {
                Toast.makeText(context, R.string.marker_add_success, Toast.LENGTH_SHORT).show();
                return marker;
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Unable to add marker.", e);
        }

        Toast.makeText(context, R.string.marker_add_error, Toast.LENGTH_LONG).show();
        return null;
    }

    public void stopRecording(@NonNull Context context) {
        if (trackRecordingService == null) {
            Log.e(TAG, "TrackRecordingService not connected.");
        } else {
            trackRecordingService.endCurrentTrack();
        }
        unbindAndStop(context);
    }

    public interface Callback {
        void onConnected(TrackRecordingService service, TrackRecordingServiceConnection self);
    }

    public static void execute(Context context, Callback callback) {
        Callback withUnbind = (service, connection) -> {
            callback.onConnected(service, connection);
            connection.unbind(context);
        };
        new TrackRecordingServiceConnection(withUnbind)
                .bind(context);

        ContextCompat.startForegroundService(context, new Intent(context, TrackRecordingService.class));
    }
}
