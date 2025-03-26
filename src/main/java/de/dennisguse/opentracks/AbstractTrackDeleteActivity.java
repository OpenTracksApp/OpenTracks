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

import android.os.Handler;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.services.TrackDeleteService;
import de.dennisguse.opentracks.ui.aggregatedStatistics.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.ui.aggregatedStatistics.ConfirmDeleteDialogFragment.ConfirmDeleteCaller;

/**
 * An abstract class for the following common tasks across
 * {@link TrackListActivity} and {@link TrackRecordedActivity}:
 * <p>
 * - delete tracks <br>
 *
 * @author Jimmy Shih
 */
//TODO Check if this class is still such a good idea; inheritance might limit maintainability
public abstract class AbstractTrackDeleteActivity extends AbstractActivity implements ConfirmDeleteCaller, TrackDeleteService.TrackDeleteResultReceiver.Receiver {

    protected void deleteTracks(Track.Id... trackIds) {
        ConfirmDeleteDialogFragment.showDialog(getSupportFragmentManager(), trackIds);
    }

    //TODO A callback is better.
    @Override
    public void onConfirmDeleteDone(Track.Id... trackIds) {
        ArrayList<Track.Id> trackIdList = Arrays.stream(trackIds)
                .filter(trackId -> !trackId.equals(getRecordingTrackId())).collect(Collectors.toCollection(ArrayList::new));

        onDeleteConfirmed();

        if (trackIds.length > trackIdList.size()) {
            Toast.makeText(this, getString(R.string.track_delete_not_recording), Toast.LENGTH_LONG).show();
        }

        TrackDeleteService.enqueue(this, new TrackDeleteService.TrackDeleteResultReceiver(new Handler(), this), trackIdList);
    }

    protected abstract void onDeleteConfirmed();

    protected abstract Track.Id getRecordingTrackId();
}
