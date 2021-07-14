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

import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.fragments.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.fragments.ConfirmDeleteDialogFragment.ConfirmDeleteCaller;
import de.dennisguse.opentracks.services.TrackDeleteService;
import de.dennisguse.opentracks.services.TrackDeleteServiceConnection;

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
        ArrayList<Track.Id> trackIdList = new ArrayList<>(Arrays.asList(trackIds))
                .stream().filter(trackId -> !trackId.equals(getRecordingTrackId())).collect(Collectors.toCollection(ArrayList::new));

        onDeleteConfirmed();

        if (trackIds.length > trackIdList.size()) {
            Toast.makeText(this, getString(R.string.track_delete_not_recording), Toast.LENGTH_LONG).show();
        }

        trackDeleteServiceConnection = new TrackDeleteServiceConnection(this);
        trackDeleteServiceConnection.startAndBind(this, trackIdList);
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

    protected abstract Track.Id getRecordingTrackId();

    @Override
    public void connected() {
        TrackDeleteService service = trackDeleteServiceConnection.getServiceIfBound();
        if (service == null) {
            return;
        }
        service.getDeletingStatusObservable().observe(AbstractTrackDeleteActivity.this, this::onTrackDeleteStatus);
    }
}
