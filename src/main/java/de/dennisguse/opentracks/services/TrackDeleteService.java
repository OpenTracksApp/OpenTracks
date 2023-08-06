package de.dennisguse.opentracks.services;

import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.util.ArrayList;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Track;

public class TrackDeleteService extends JobIntentService {

    private static final int JOB_ID = 3;

    private static final String EXTRA_RECEIVER = "extra_receiver";

    private static final String EXTRA_TRACK_IDS = "extra_track_ids";

    public static void enqueue(Context context, TrackDeleteResultReceiver receiver, ArrayList<Track.Id> toBeDeleted) {
        Intent intent = new Intent(context, JobService.class);
        intent.putExtra(EXTRA_RECEIVER, receiver);
        intent.putParcelableArrayListExtra(EXTRA_TRACK_IDS, toBeDeleted);
        enqueueWork(context, TrackDeleteService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);
        ArrayList<Track.Id> trackIds = intent.getParcelableArrayListExtra(EXTRA_TRACK_IDS);

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(this);
        contentProviderUtils.deleteTracks(this, trackIds);

        resultReceiver.send(TrackDeleteResultReceiver.RESULT_CODE_SUCCESS, new Bundle());
    }

    public static class TrackDeleteResultReceiver extends ResultReceiver {

        public static final int RESULT_CODE_SUCCESS = 1;

        private final Receiver receiver;

        public TrackDeleteResultReceiver(Handler handler, @NonNull Receiver receiver) {
            super(handler);
            this.receiver = receiver;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch (resultCode) {
                case RESULT_CODE_SUCCESS -> receiver.onDeleteFinished();
                default -> throw new RuntimeException("Unknown resultCode.");
            }
        }

        public interface Receiver {
            void onDeleteFinished();
        }
    }
}
