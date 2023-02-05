package de.dennisguse.opentracks.ui.aggregatedStatistics;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Locale;

import de.dennisguse.opentracks.R;
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.generic_filter));

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View layout = inflater.inflate(R.layout.fragment_filter_dialog, null, false);
        MaterialButtonToggleGroup itemsLayout = layout.findViewById(R.id.filter_items);
        builder.setView(layout);

        for (FilterItem item : filterItems) {
            View view = inflater.inflate(R.layout.fragment_filter_dialog_item, null);

            MaterialButton button = view.findViewById(R.id.filter_dialog_check_button);
            button.setText(item.value);
            button.setChecked(item.isChecked);
            button.setTag(item.id);
            button.setOnClickListener(v -> item.isChecked = !item.isChecked);

            itemsLayout.addView(view);
        }

        DatePicker datePickerFrom = layout.findViewById(R.id.filter_date_picker_from);
        DatePicker datePickerTo = layout.findViewById(R.id.filter_date_picker_to);
        TextInputEditText dateFrom = layout.findViewById(R.id.filter_date_edit_text_from);
        TextInputEditText dateTo = layout.findViewById(R.id.filter_date_edit_text_to);

        LocalDateTime firstDayThisWeek = LocalDate.now().with(WeekFields.of(Locale.getDefault()).getFirstDayOfWeek()).atStartOfDay();
        dateFrom.setText(StringUtils.formatLocalDateTime(firstDayThisWeek));
        datePickerFrom.init(firstDayThisWeek.getYear(), firstDayThisWeek.getMonthValue() - 1, firstDayThisWeek.getDayOfMonth(), (view, year, monthOfYear, dayOfMonth) -> {
            LocalDateTime localDateTime = LocalDateTime.of(year, monthOfYear + 1, dayOfMonth, 0, 0, 0);
            dateFrom.setText(StringUtils.formatLocalDateTime(localDateTime));
            datePickerFrom.setVisibility(View.GONE);
            datePickerTo.setMinDate(localDateTime.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli());
            if (localDateTime.isAfter(LocalDateTime.of(datePickerTo.getYear(), datePickerTo.getMonth() + 1, datePickerTo.getDayOfMonth(), 23, 59, 59))) {
                datePickerTo.updateDate(year, monthOfYear, dayOfMonth);
            }
        });

        LocalDateTime lastDayThisWeek = firstDayThisWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59);
        dateTo.setText(StringUtils.formatLocalDateTime(lastDayThisWeek));
        datePickerTo.init(lastDayThisWeek.getYear(), lastDayThisWeek.getMonthValue() - 1, lastDayThisWeek.getDayOfMonth(), (view, year, monthOfYear, dayOfMonth) -> {
            LocalDateTime localDateTime = LocalDateTime.of(year, monthOfYear + 1, dayOfMonth, 23, 59, 59);
            dateTo.setText(StringUtils.formatLocalDateTime(localDateTime));
            datePickerTo.setVisibility(View.GONE);
        });

        dateFrom.setOnClickListener(v -> {
            datePickerFrom.setVisibility(View.VISIBLE);
            datePickerTo.setVisibility(View.GONE);
        });

        dateTo.setOnClickListener(v -> {
            datePickerFrom.setVisibility(View.GONE);
            datePickerTo.setVisibility(View.VISIBLE);
        });

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> filterDialogListener.onFilterDone(
                filterItems,
                LocalDateTime.of(datePickerFrom.getYear(), datePickerFrom.getMonth() + 1, datePickerFrom.getDayOfMonth(), 0, 0, 0),
                LocalDateTime.of(datePickerTo.getYear(), datePickerTo.getMonth() + 1, datePickerTo.getDayOfMonth(), 23, 59, 59)
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
        protected boolean isChecked;

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
