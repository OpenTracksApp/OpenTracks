/*
 * Copyright 2013 Google Inc.
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.DialogUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;

/**
 * A DialogFragment to choose an activity type.
 *
 * @author apoorvn
 */
public class ChooseActivityTypeDialogFragment extends DialogFragment {

    public static final String CHOOSE_ACTIVITY_TYPE_DIALOG_TAG = "chooseActivityType";
    private static final String KEY_CATEGORY = "category";
    private ChooseActivityTypeCaller caller;

    public static ChooseActivityTypeDialogFragment newInstance(String category) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_CATEGORY, category);

        ChooseActivityTypeDialogFragment fragment = new ChooseActivityTypeDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static Dialog getDialog(final Context context, final String category, final ChooseActivityTypeCaller caller) {
        View view = LayoutInflater.from(context).inflate(R.layout.choose_activity_type, null);
        GridView gridView = view.findViewById(R.id.choose_activity_type_grid_view);

        List<Integer> imageIds = new ArrayList<>();
        for (String iconValue : TrackIconUtils.getAllIconValues()) {
            imageIds.add(TrackIconUtils.getIconDrawable(iconValue));
        }

        final ChooseActivityTypeImageAdapter imageAdapter = new ChooseActivityTypeImageAdapter(imageIds);
        gridView.setAdapter(imageAdapter);

        final AlertDialog alertDialog = new AlertDialog.Builder(context).setTitle(R.string.track_edit_activity_type_hint).setView(view).create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                int position = getPosition(context, category);
                if (position != -1) {
                    imageAdapter.setSelected(position);
                    imageAdapter.notifyDataSetChanged();
                }
                DialogUtils.setDialogTitleDivider(context, alertDialog);
            }
        });

        gridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                caller.onChooseActivityTypeDone(TrackIconUtils.getAllIconValues().get(position));
                alertDialog.dismiss();
            }
        });
        return alertDialog;
    }

    private static int getPosition(Context context, String category) {
        if (category == null) {
            return -1;
        }
        String iconValue = TrackIconUtils.getIconValue(context, category);

        return TrackIconUtils.getAllIconValues().indexOf(iconValue);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            caller = (ChooseActivityTypeCaller) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement " + ChooseActivityTypeCaller.class.getSimpleName());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return getDialog(getActivity(), getArguments().getString(KEY_CATEGORY), caller);
    }

    /**
     * Interface for caller of this dialog fragment.
     *
     * @author apoorvn
     */
    public interface ChooseActivityTypeCaller {

        /**
         * Called when choose activity type is done.
         */
        void onChooseActivityTypeDone(String iconValue);
    }
}
