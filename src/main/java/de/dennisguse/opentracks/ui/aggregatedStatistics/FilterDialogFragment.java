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
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Locale;

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

        LocalDateTime firstDayThisWeek = LocalDate.now().with(WeekFields.of(Locale.getDefault()).getFirstDayOfWeek()).atStartOfDay();
        layout.filterDateEditTextFrom.setText(StringUtils.formatLocalDateTime(firstDayThisWeek));
        layout.filterDatePickerFrom.init(firstDayThisWeek.getYear(), firstDayThisWeek.getMonthValue() - 1, firstDayThisWeek.getDayOfMonth(), (view, year, monthOfYear, dayOfMonth) -> {
            LocalDateTime localDateTime = LocalDateTime.of(year, monthOfYear + 1, dayOfMonth, 0, 0, 0);
            layout.filterDateEditTextFrom.setText(StringUtils.formatLocalDateTime(localDateTime));
            layout.filterDatePickerFrom.setVisibility(View.GONE);
            layout.filterDatePickerTo.setMinDate(localDateTime.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli());
            if (localDateTime.isAfter(LocalDateTime.of(layout.filterDatePickerTo.getYear(), layout.filterDatePickerTo.getMonth() + 1, layout.filterDatePickerTo.getDayOfMonth(), 23, 59, 59))) {
                layout.filterDatePickerTo.updateDate(year, monthOfYear, dayOfMonth);
            }
        });

        LocalDateTime lastDayThisWeek = firstDayThisWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59);
        layout.filterDateEditTextTo.setText(StringUtils.formatLocalDateTime(lastDayThisWeek));
        layout.filterDatePickerTo.init(lastDayThisWeek.getYear(), lastDayThisWeek.getMonthValue() - 1, lastDayThisWeek.getDayOfMonth(), (view, year, monthOfYear, dayOfMonth) -> {
            LocalDateTime localDateTime = LocalDateTime.of(year, monthOfYear + 1, dayOfMonth, 23, 59, 59);
            layout.filterDateEditTextTo.setText(StringUtils.formatLocalDateTime(localDateTime));
            layout.filterDatePickerTo.setVisibility(View.GONE);
        });

        layout.filterDateEditTextFrom.setOnClickListener(v -> {
            layout.filterDatePickerFrom.setVisibility(View.VISIBLE);
            layout.filterDatePickerTo.setVisibility(View.GONE);
        });

        layout.filterDateEditTextTo.setOnClickListener(v -> {
            layout.filterDatePickerFrom.setVisibility(View.GONE);
            layout.filterDatePickerTo.setVisibility(View.VISIBLE);
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.generic_filter));
        builder.setView(layout.getRoot());
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> filterDialogListener.onFilterDone(
                filterItems,
                LocalDateTime.of(layout.filterDatePickerFrom.getYear(), layout.filterDatePickerFrom.getMonth() + 1, layout.filterDatePickerFrom.getDayOfMonth(), 0, 0, 0),
                LocalDateTime.of(layout.filterDatePickerTo.getYear(), layout.filterDatePickerTo.getMonth() + 1, layout.filterDatePickerTo.getDayOfMonth(), 23, 59, 59)
        ));
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
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
