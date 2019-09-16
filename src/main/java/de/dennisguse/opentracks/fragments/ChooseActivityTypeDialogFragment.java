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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.TrackIconUtils;

/**
 * A DialogFragment to choose an activity type.
 *
 * @author apoorvn
 */
public class ChooseActivityTypeDialogFragment extends DialogFragment {

    private static final String CHOOSE_ACTIVITY_TYPE_DIALOG_TAG = "chooseActivityType";
    private final String preselectedCategory;

    private ChooseActivityTypeDialogFragment(String preselectedCategory) {
        this.preselectedCategory = preselectedCategory;
    }

    public static void showDialog(FragmentManager fragmentManager, String preselectedCategory) {
        new ChooseActivityTypeDialogFragment(preselectedCategory).show(fragmentManager, ChooseActivityTypeDialogFragment.CHOOSE_ACTIVITY_TYPE_DIALOG_TAG);
    }

    private ChooseActivityTypeCaller caller;

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
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.setTitle(R.string.track_edit_activity_type_hint);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_activity_type, container);
        GridView gridView = view.findViewById(R.id.choose_activity_type_grid_view);

        List<Integer> imageIds = new ArrayList<>();
        for (String iconValue : TrackIconUtils.getAllIconValues()) {
            imageIds.add(TrackIconUtils.getIconDrawable(iconValue));
        }

        final ChooseActivityTypeImageAdapter imageAdapter = new ChooseActivityTypeImageAdapter(imageIds);
        gridView.setAdapter(imageAdapter);

        int position = getPosition(getContext(), preselectedCategory);
        if (position != -1) {
            imageAdapter.setSelected(position);
            imageAdapter.notifyDataSetChanged();
        }

        gridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                caller.onChooseActivityTypeDone(TrackIconUtils.getAllIconValues().get(position));
                dismiss();
            }
        });
        return view;
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
