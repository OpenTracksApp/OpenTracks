package de.dennisguse.opentracks.ui;


import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.time.ZoneOffset;
import java.util.stream.IntStream;

public class ZoneOffsetAdapter extends ArrayAdapter<ZoneOffset> {
    public ZoneOffsetAdapter(@NonNull Context context, int resource) {
        super(context, resource, IntStream.range(-12, +12)
                .boxed()
                .map(ZoneOffset::ofHours)
                .toList());
    }
}
