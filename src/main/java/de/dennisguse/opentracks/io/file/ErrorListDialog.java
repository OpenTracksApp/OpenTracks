package de.dennisguse.opentracks.io.file;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;

public class ErrorListDialog extends DialogFragment {

    public static final String TAG = ErrorListDialog.class.getSimpleName();

    private static final String EXTRA_TITLE = "extra_title";
    private static final String EXTRA_ERROR_LIST = "extra_error_list";

    private ArrayList<String> errorList;
    private String title;

    public static void showDialog(FragmentManager fragmentManager, String title, ArrayList<String> errorList) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_TITLE, title);
        bundle.putStringArrayList(EXTRA_ERROR_LIST, errorList);

        ErrorListDialog errorListDialog = new ErrorListDialog();
        errorListDialog.setArguments(bundle);
        errorListDialog.setRetainInstance(true);
        errorListDialog.show(fragmentManager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            title = getArguments().getString(EXTRA_TITLE);
            errorList = getArguments().getStringArrayList(EXTRA_ERROR_LIST);
        } else {
            title = savedInstanceState.getString(EXTRA_TITLE);
            errorList = savedInstanceState.getStringArrayList(EXTRA_ERROR_LIST);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_TITLE, title);
        outState.putStringArrayList(EXTRA_ERROR_LIST, errorList);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String[] tracks = errorList.toArray(new String[0]);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setItems(tracks, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dismiss());
        return alertDialogBuilder.create();
    }
}
