package de.dennisguse.opentracks.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Arrays;
import java.util.List;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Track;

public class TrackDeletionWorker extends Worker {

    public static final String TRACKIDS_KEY = "TRACKIDS_KEY";

    private final List<Track.Id> trackIds;

    public TrackDeletionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);

        trackIds = Arrays.stream(getInputData().getLongArray(TRACKIDS_KEY))
                .mapToObj(Track.Id::new)
                .toList();
    }

    @NonNull
    @Override
    public Result doWork() {
        new ContentProviderUtils(getApplicationContext()).deleteTracks(getApplicationContext(), trackIds);

        return Result.success();
    }
}
