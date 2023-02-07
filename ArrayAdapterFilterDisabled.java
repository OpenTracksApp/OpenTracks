package de.dennisguse.opentracks.ui.util;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;

// https://github.com/material-components/material-components-android/issues/1464
public class ArrayAdapterFilterDisabled<T> extends ArrayAdapter<T> {

    public ArrayAdapterFilterDisabled(@NonNull Context context, int resource, @NonNull T[] objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new NeverFilter();
    }

    private class NeverFilter extends Filter {
        protected FilterResults performFiltering(CharSequence prefix) {
            return new FilterResults();
        }

        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (ArrayAdapterFilterDisabled.this.getCount() > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
