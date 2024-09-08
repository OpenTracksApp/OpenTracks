package de.dennisguse.opentracks.ui.aggregatedStatistics;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.TimeZone;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.FragmentFilterDialogBinding;
import de.dennisguse.opentracks.databinding.FragmentFilterDialogItemBinding;
import de.dennisguse.opentracks.util.StringUtils;

public class FilterDialogFragment extends DialogFragment {

    private static final String TAG = FilterDialogFragment.class.getSimpleName();
    public static final String KEY_FILTER_ITEMS = "filterItems";

    private FilterDialogListener filterDialogListener;
    private ArrayList<FilterItem> filterItems = new ArrayList<>();

    public static void showDialog(FragmentManager fragmentManager) {
        FilterDialogFragment filterDialogFragment = new FilterDialogFragment();
        filterDialogFragment.show(fragmentManager, TAG);
    }

    public static void showDialog(FragmentManager fragmentManager, ArrayList<FilterItem> items) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(KEY_FILTER_ITEMS, items);

        FilterDialogFragment filterDialogFragment = new FilterDialogFragment();
        filterDialogFragment.setArguments(bundle);
        filterDialogFragment.show(fragmentManager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        filterItems = getArguments().getParcelableArrayList(KEY_FILTER_ITEMS);

        FragmentFilterDialogBinding layout = FragmentFilterDialogBinding.inflate(getActivity().getLayoutInflater());

        for (FilterItem item : filterItems) {
            FragmentFilterDialogItemBinding view = FragmentFilterDialogItemBinding.inflate(getActivity().getLayoutInflater());

            view.filterDialogCheckButton.setText(item.value);
            view.filterDialogCheckButton.setChecked(item.isChecked);
            view.filterDialogCheckButton.setTag(item.id);
            view.filterDialogCheckButton.setOnClickListener(v -> item.isChecked = !item.isChecked);

            layout.filterItems.addView(view.getRoot());
        }


        // Interval is last 7 days.
        LocalDateTime initialToDate = LocalDate.now().atStartOfDay();
        LocalDateTime initialFromDate = initialToDate.minusDays(6);

        MaterialDatePicker<Pair<Long, Long>> dateRangePicker = MaterialDatePicker.Builder
                .dateRangePicker()
                .setSelection(new Pair<>(
                        initialFromDate.toInstant(ZoneOffset.UTC).toEpochMilli(),
                        initialToDate.toInstant(ZoneOffset.UTC).toEpochMilli())

                )
                .build();

        layout.filterDateEditTextFrom.setText(StringUtils.formatLocalDateTime(initialFromDate));
        layout.filterDateEditTextTo.setText(StringUtils.formatLocalDateTime(initialToDate));

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            Pair<LocalDateTime, LocalDateTime> javaxSelection = to(dateRangePicker);

            layout.filterDateEditTextFrom.setText(StringUtils.formatLocalDateTime(javaxSelection.first));
            layout.filterDateEditTextTo.setText(StringUtils.formatLocalDateTime(javaxSelection.second));
        });

        View.OnClickListener openDatePicker = v -> dateRangePicker.show(getChildFragmentManager(), "DATE_PICKER");
        layout.filterDateEditTextFrom.setOnClickListener(openDatePicker);
        layout.filterDateEditTextTo.setOnClickListener(openDatePicker);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.generic_filter));
        builder.setView(layout.getRoot());
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            Pair<LocalDateTime, LocalDateTime> a = to(dateRangePicker);
            filterDialogListener.onFilterDone(filterItems, a.first, a.second);
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    private static Pair<LocalDateTime, LocalDateTime> to(MaterialDatePicker<Pair<Long, Long>> dateRangePicker) {
        Pair<Long, Long> selection = dateRangePicker.getSelection();

        return new Pair<>(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(selection.first), TimeZone.getDefault().toZoneId()),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(selection.second), TimeZone.getDefault().toZoneId())
        );
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            filterDialogListener = (FilterDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement " + FilterDialogListener.class.getSimpleName());
        }
    }

    public interface FilterDialogListener {
        void onFilterDone(ArrayList<FilterItem> filters, LocalDateTime from, LocalDateTime to);
    }

    public static class FilterItem implements Parcelable {
        public final String id;
        public final String value;
        public boolean isChecked;

        public FilterItem(String id, String value) {
            this.id = id;
            this.value = value;
            this.isChecked = true;
        }

        public FilterItem(String id, String value, boolean isChecked) {
            this.id = id;
            this.value = value;
            this.isChecked = isChecked;
        }

        protected FilterItem(Parcel in) {
            id = in.readString();
            value = in.readString();
            isChecked = in.readByte() != 0;
        }

        public static final Creator<FilterItem> CREATOR = new Creator<>() {
            @Override
            public FilterItem createFromParcel(Parcel in) {
                return new FilterItem(in);
            }

            @Override
            public FilterItem[] newArray(int size) {
                return new FilterItem[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(id);
            dest.writeString(value);
            dest.writeByte((byte) (isChecked ? 1 : 0));
        }
    }
}
