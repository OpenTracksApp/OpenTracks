package de.dennisguse.opentracks.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.databinding.ChooseActivityTypeBinding;

public class ChooseActivityTypeDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener {

    private static final String CHOOSE_ACTIVITY_TYPE_DIALOG_TAG = "chooseActivityType";

    @Deprecated
    public static void showDialog(FragmentManager fragmentManager, Context context, String activityTypeLocalized) {
        ActivityType activityType = ActivityType.findByLocalizedString(context, activityTypeLocalized);
        showDialog(fragmentManager, activityType);
    }

    public static void showDialog(FragmentManager fragmentManager, ActivityType activityType) {
        new ChooseActivityTypeDialogFragment(activityType).show(fragmentManager, ChooseActivityTypeDialogFragment.CHOOSE_ACTIVITY_TYPE_DIALOG_TAG);
    }

    private static final List<ActivityType> activityTypes = List.of(
            ActivityType.UNKNOWN,
            ActivityType.AIRPLANE,
            ActivityType.BIKING,
            ActivityType.MOUNTAIN_BIKING,
            ActivityType.MOTOR_BIKE,
            ActivityType.KAYAKING,
            ActivityType.BOAT,
            ActivityType.SAILING,
            ActivityType.DRIVING,
            ActivityType.RUNNING,
            ActivityType.SNOW_BOARDING,
            ActivityType.SKIING,
            ActivityType.WALKING,
            ActivityType.ESCOOTER,
            ActivityType.INLINE_SKATING,
            ActivityType.SKATE_BOARDING,
            ActivityType.CLIMBING,
            ActivityType.SWIMMING,
            ActivityType.SWIMMING_OPEN,
            ActivityType.WORKOUT
    );

    private static int getPosition(Context context, ActivityType activityType) {
        if (activityType == null) {
            return -1;
        }

        return activityTypes.indexOf(activityType);
    }

    private ChooseActivityTypeBinding viewBinding;

    private final ActivityType preselectedActivityType;

    private ChooseActivityTypeCaller chooseActivityTypeCaller;

    private ChooseActivityTypeDialogFragment(ActivityType activityType) {
        this.preselectedActivityType = activityType;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.track_edit_activity_type_hint);
        return dialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = ChooseActivityTypeBinding.inflate(inflater, container, false);

        List<Integer> iconDrawableIds = new ArrayList<>();
        for (ActivityType activityType : activityTypes) {
            iconDrawableIds.add(activityType.getIconDrawableId());
        }

        final ChooseActivityTypeImageAdapter imageAdapter = new ChooseActivityTypeImageAdapter(iconDrawableIds);
        int position = getPosition(getContext(), preselectedActivityType);
        if (position != -1) {
            imageAdapter.setSelected(position);
        }

        viewBinding.chooseActivityTypeGridView.setAdapter(imageAdapter);
        viewBinding.chooseActivityTypeGridView.setOnItemClickListener(this);
        return viewBinding.getRoot();
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
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        chooseActivityTypeCaller.onChooseActivityTypeDone(activityTypes.get(position));
        dismiss();
    }

    /**
     * Interface for chooseActivityTypeCaller of this dialog fragment.
     */
    public interface ChooseActivityTypeCaller {

        void onChooseActivityTypeDone(ActivityType activityType);
    }
}
