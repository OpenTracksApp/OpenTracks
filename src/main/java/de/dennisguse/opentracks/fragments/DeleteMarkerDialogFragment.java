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

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.util.DialogUtils;

/**
 * A DialogFragment to delete marker.
 *
 * @author Jimmy Shih
 */
public class DeleteMarkerDialogFragment extends DialogFragment {

    private static final String DELETE_MARKER_DIALOG_TAG = "deleteMarkerDialog";
    private static final String KEY_MARKER_IDS = "markerIds";

    private DeleteMarkerCaller caller;

    public static void showDialog(FragmentManager fragmentManager, long[] markerIds) {
        Bundle bundle = new Bundle();
        bundle.putLongArray(KEY_MARKER_IDS, markerIds);

        DeleteMarkerDialogFragment deleteMarkerDialogFragment = new DeleteMarkerDialogFragment();
        deleteMarkerDialogFragment.setArguments(bundle);
        deleteMarkerDialogFragment.show(fragmentManager, DELETE_MARKER_DIALOG_TAG);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            caller = (DeleteMarkerCaller) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement " + DeleteMarkerCaller.class.getSimpleName());
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final long[] markerIds = getArguments().getLongArray(KEY_MARKER_IDS);

        final FragmentActivity fragmentActivity = getActivity();
        int titleId;
        int messageId;
        if (markerIds.length == 1 && markerIds[0] == -1L) {
            titleId = R.string.generic_delete_all_confirm_title;
            messageId = R.string.marker_delete_all_confirm_message;
        } else {
            titleId = markerIds.length > 1 ? R.string.generic_delete_selected_confirm_title : R.string.marker_delete_one_confirm_title;
            messageId = markerIds.length > 1 ? R.string.marker_delete_multiple_confirm_message : R.string.marker_delete_one_confirm_message;
        }
        return DialogUtils.createConfirmationDialog(
                fragmentActivity, titleId, getString(messageId), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ContentProviderUtils contentProviderUtils = new ContentProviderUtils(fragmentActivity);
                                for (long markerId : markerIds) {
                                    contentProviderUtils.deleteWaypoint(markerId);
                                }
                                caller.onDeleteMarkerDone();
                            }
                        }).start();
                    }
                });
    }

    /**
     * Interface for caller of this dialog fragment.
     *
     * @author Jimmy Shih
     */
    public interface DeleteMarkerCaller {

        /**
         * Called when delete marker is done.
         */
        void onDeleteMarkerDone();
    }
}