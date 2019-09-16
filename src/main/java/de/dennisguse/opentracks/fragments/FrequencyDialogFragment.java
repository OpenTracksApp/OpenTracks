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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * A DialogFragment to configure frequency.
 *
 * @author Jimmy Shih
 */
public class FrequencyDialogFragment extends DialogFragment {

    private static final String FREQUENCY_DIALOG_TAG = "frequencyDialog";
    private int preferenceId;
    private int defaultValue;
    private int titleId;

    public FrequencyDialogFragment(int preferenceId, int defaultValue, int titleId) {
        this.preferenceId = preferenceId;
        this.defaultValue = defaultValue;
        this.titleId = titleId;
    }

    public static void showDialog(FragmentManager fragmentManager, int preferenceId, int defaultValue, int titleId) {
        new FrequencyDialogFragment(preferenceId, defaultValue, titleId).show(fragmentManager, FREQUENCY_DIALOG_TAG);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity fragmentActivity = getActivity();

        int frequencyValue = PreferencesUtils.getInt(fragmentActivity, preferenceId, defaultValue);

        return new AlertDialog.Builder(fragmentActivity).setPositiveButton(
                R.string.generic_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int listIndex = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        PreferencesUtils.setInt(getActivity(), preferenceId, getFrequencyValue(listIndex));
                    }
                }).setSingleChoiceItems(
                getFrequencyDisplayOptions(fragmentActivity), getListIndex(frequencyValue), null)
                .setTitle(titleId).create();
    }

    /**
     * Gets the frequency display options.
     */
    private String[] getFrequencyDisplayOptions(FragmentActivity fragmentActivity) {
        boolean metricUnits = PreferencesUtils.isMetricUnits(fragmentActivity);
        return StringUtils.getFrequencyOptions(fragmentActivity, metricUnits);
    }

    /**
     * Gets the list index for a frequency value.
     * Returns 0 if the value is not on the list.
     */
    private int getListIndex(int frequencyValue) {
        String[] values = getResources().getStringArray(R.array.frequency_values);
        for (int i = 0; i < values.length; i++) {
            if (frequencyValue == Integer.parseInt(values[i])) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Gets the frequency value from a list index.
     *
     * @param listIndex the list index
     */
    private int getFrequencyValue(int listIndex) {
        String[] values = getResources().getStringArray(R.array.frequency_values);
        return Integer.parseInt(values[listIndex]);
    }
}