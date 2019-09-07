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

package de.dennisguse.opentracks.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.DialogUtils;

/**
 * A DialogFragment to confirm delete.
 *
 * @author Jimmy Shih
 */
public class ConfirmDeleteDialogFragment extends AbstractDialogFragment {

    public static final String CONFIRM_DELETE_DIALOG_TAG = "confirmDeleteDialog";
    private static final String KEY_TRACK_IDS = "trackIds";
    private ConfirmDeleteCaller caller;

    /**
     * Create a new instance.
     *
     * @param trackIds list of track ids to delete. To delete all, set to size 1 with trackIds[0] == -1L
     */
    public static ConfirmDeleteDialogFragment newInstance(long[] trackIds) {
        Bundle bundle = new Bundle();
        bundle.putLongArray(KEY_TRACK_IDS, trackIds);

        ConfirmDeleteDialogFragment deleteTrackDialogFragment = new ConfirmDeleteDialogFragment();
        deleteTrackDialogFragment.setArguments(bundle);
        return deleteTrackDialogFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            caller = (ConfirmDeleteCaller) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement " + ConfirmDeleteCaller.class.getSimpleName());
        }
    }

    @Override
    protected Dialog createDialog() {
        final long[] trackIds = getArguments().getLongArray(KEY_TRACK_IDS);
        int titleId;
        int messageId;
        titleId = trackIds.length > 1 ? R.string.generic_delete_selected_confirm_title : R.string.track_delete_one_confirm_title;
        messageId = trackIds.length > 1 ? R.string.track_delete_multiple_confirm_message : R.string.track_delete_one_confirm_message;
        return DialogUtils.createConfirmationDialog(
                getActivity(), titleId, getString(messageId), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        caller.onConfirmDeleteDone(trackIds);
                    }
                });
    }

    /**
     * Interface for caller of this dialog fragment.
     *
     * @author Jimmy Shih
     */
    public interface ConfirmDeleteCaller {

        /**
         * Called when confirm delete is done.
         *
         * @param trackIds list of track ids to delete. To delete all, set to size 1
         *                 with trackIds[0] == -1L
         */
        void onConfirmDeleteDone(long[] trackIds);
    }
}