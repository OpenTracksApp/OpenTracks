/*
 * Copyright 2012 Google Inc.
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

package de.dennisguse.opentracks;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.fragments.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.fragments.ConfirmDeleteDialogFragment.ConfirmDeleteCaller;
import de.dennisguse.opentracks.services.TrackDeleteService;
import de.dennisguse.opentracks.services.TrackDeleteServiceConnection;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;

/**
 * An abstract class for the following common tasks across
 * {@link TrackListActivity} and {@link TrackRecordedActivity}:
 * <p>
 * - delete tracks <br>
 *
 * @author Jimmy Shih
 */
//TODO Check if this class is still such a good idea; inheritance might limit maintainability
public abstract class AbstractTrackDeleteActivity extends AbstractActivity implements ConfirmDeleteCaller, TrackDeleteServiceConnection.Listener {

    private static final String TAG = AbstractTrackDeleteActivity.class.getSimpleName();

    private TrackDeleteServiceConnection trackDeleteServiceConnection;

    @Override
    protected void onStart() {
        super.onStart();
        trackDeleteServiceConnection = new TrackDeleteServiceConnection(this);
        trackDeleteServiceConnection.bind(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (trackDeleteServiceConnection != null) {
            trackDeleteServiceConnection.unbind(this);
            trackDeleteServiceConnection = null;
        }
    }

    /**
     * Delete tracks.
     *
     * @param trackIds the track ids
     */
    protected void deleteTracks(Track.Id... trackIds) {
        ConfirmDeleteDialogFragment.showDialog(getSupportFragmentManager(), trackIds);
    }

    @Override
    public void onConfirmDeleteDone(Track.Id... trackIds) {
        boolean stopRecording = false;

        onDeleteConfirmed();

        for (Track.Id trackId : trackIds) {
            if (trackId.equals(getRecordingTrackId())) {
                stopRecording = true;
                break;
            }
        }

        if (stopRecording) {
            getTrackRecordingServiceConnection().stopRecording(this);
        }

        trackDeleteServiceConnection = new TrackDeleteServiceConnection(this);
        trackDeleteServiceConnection.startAndBind(this, new ArrayList<>(Arrays.asList(trackIds)));
    }

    /**
     * Gets the track recording service connection.
     * For stopping the current recording if need to delete the current recording track.
     */
    protected TrackRecordingServiceConnection getTrackRecordingServiceConnection() {
        return null;
    }

    /**
     * Called every time a track is deleted.
     */
    protected void onTrackDeleteStatus(TrackDeleteService.DeleteStatus deleteStatus) {
        if (deleteStatus.isFinished() && trackDeleteServiceConnection != null) {
            trackDeleteServiceConnection.unbind(this);
            trackDeleteServiceConnection = null;
        }
    }

    protected abstract void onDeleteConfirmed();

    @Nullable
    protected Track.Id getRecordingTrackId() {
        return null;
    }

    @Override
    public void connected() {
        TrackDeleteService service = trackDeleteServiceConnection.getServiceIfBound();
        if (service == null) {
            return;
        }
        service.getDeletingStatusObservable().observe(AbstractTrackDeleteActivity.this, status -> onTrackDeleteStatus(status));
    }
}
