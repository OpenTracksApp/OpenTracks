package de.dennisguse.opentracks.viewmodels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import de.dennisguse.opentracks.services.RecordingData;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.ui.customRecordingLayout.DataField;

public abstract class StatisticViewHolder<T extends ViewBinding> {

    private Context context;

    private T binding;

    public void initialize(Context context, LayoutInflater inflater) {
        this.context = context;
        this.binding = createViewBinding(inflater);
    }

    protected abstract T createViewBinding(LayoutInflater inflater);

    public abstract void configureUI(DataField dataField);

    public abstract void onChanged(UnitSystem unitSystem, RecordingData data);

    public View getView() {
        return binding.getRoot();
    }

    T getBinding() {
        return binding;
    }

    Context getContext() {
        return context;
    }
}