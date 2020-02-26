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

import android.content.Intent;

import de.dennisguse.opentracks.fragments.ConfirmDeleteDialogFragment;
import de.dennisguse.opentracks.fragments.ConfirmDeleteDialogFragment.ConfirmDeleteCaller;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * An abstract class for the following common tasks across
 * {@link TrackListActivity}, {@link TrackDetailActivity}, and
 * {@link SearchListActivity}:
 * <p>
 * - share track <br>
 * - delete tracks <br>
 *
 * @author Jimmy Shih
 */
public abstract class AbstractListActivity extends AbstractActivity implements ConfirmDeleteCaller {

    private static final String TAG = AbstractListActivity.class.getSimpleName();

    protected static final int GPS_REQUEST_CODE = 6;
    private static final int DELETE_REQUEST_CODE = 3;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DELETE_REQUEST_CODE) {
            onDeleted();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Delete tracks.
     *
     * @param trackIds the track ids
     */
    protected void deleteTracks(long[] trackIds) {
        ConfirmDeleteDialogFragment.showDialog(getSupportFragmentManager(), trackIds);
    }

    @Override
    public void onConfirmDeleteDone(long[] trackIds) {
        boolean stopRecording = false;
        if (trackIds.length == 1 && trackIds[0] == -1L) {
            stopRecording = true;
        } else {
            long recordingTrackId = PreferencesUtils.getRecordingTrackId(this);
            for (long trackId : trackIds) {
                if (trackId == recordingTrackId) {
                    stopRecording = true;
                    break;
                }
            }
        }
        if (stopRecording) {
            getTrackRecordingServiceConnection().stopRecording(this, false);
        }
        Intent intent = IntentUtils.newIntent(this, TrackDeleteActivity.class);
        intent.putExtra(TrackDeleteActivity.EXTRA_TRACK_IDS, trackIds);
        startActivityForResult(intent, DELETE_REQUEST_CODE);
    }

    /**
     * Gets the track recording service connection.
     * For stopping the current recording if need to delete the current recording track.
     */
    abstract protected TrackRecordingServiceConnection getTrackRecordingServiceConnection();

    /**
     * Called after {@link TrackDeleteActivity} returns its result.
     */
    abstract protected void onDeleted();
}
