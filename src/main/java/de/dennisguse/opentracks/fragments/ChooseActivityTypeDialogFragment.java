package de.dennisguse.opentracks.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
 */
public class ChooseActivityTypeDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener {

    private static final String CHOOSE_ACTIVITY_TYPE_DIALOG_TAG = "chooseActivityType";

    public static void showDialog(FragmentManager fragmentManager, String preselectedCategory) {
        new ChooseActivityTypeDialogFragment(preselectedCategory).show(fragmentManager, ChooseActivityTypeDialogFragment.CHOOSE_ACTIVITY_TYPE_DIALOG_TAG);
    }

    private static int getPosition(Context context, String category) {
        if (category == null) {
            return -1;
        }
        String iconValue = TrackIconUtils.getIconValue(context, category);

        return TrackIconUtils.getAllIconValues().indexOf(iconValue);
    }

    private String preselectedCategory;

    private ChooseActivityTypeCaller chooseActivityTypeCaller;

    private ChooseActivityTypeDialogFragment(String preselectedCategory) {
        this.preselectedCategory = preselectedCategory;
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

        gridView.setOnItemClickListener(this);
        return view;
    }

    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            chooseActivityTypeCaller = (ChooseActivityTypeCaller) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement " + ChooseActivityTypeCaller.class.getSimpleName());
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        chooseActivityTypeCaller.onChooseActivityTypeDone(TrackIconUtils.getAllIconValues().get(position));
        dismiss();
    }

    /**
     * Interface for chooseActivityTypeCaller of this dialog fragment.
     */
    public interface ChooseActivityTypeCaller {

        void onChooseActivityTypeDone(String iconValue);
    }
}
