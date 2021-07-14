package de.dennisguse.opentracks.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;

public class TrackDeleteService extends Service {

    private static final String CHANNEL_ID = TrackDeleteService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 1;

    static final String EXTRA_TRACK_IDS = "extra_track_ids";

    private final Binder binder = new Binder();
    private ExecutorService serviceExecutor;
    private MutableLiveData<DeleteStatus> deleteResultObservable;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    @Override
    public void onCreate() {
        serviceExecutor = Executors.newSingleThreadExecutor();
        deleteResultObservable = new MutableLiveData<>();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serviceExecutor != null) {
            serviceExecutor.shutdownNow();
            serviceExecutor = null;
        }
        deleteResultObservable = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ArrayList<Track.Id> trackIds = intent.getParcelableArrayListExtra(EXTRA_TRACK_IDS);
        createAndShowNotification(trackIds.size());
        deleteTracks(trackIds);
        return START_NOT_STICKY;
    }

    private void deleteTracks(@NonNull ArrayList<Track.Id> trackIds) {
        sendResult(null,0, trackIds.size());
        serviceExecutor.execute(() -> {
            ContentProviderUtils contentProviderUtils = new ContentProviderUtils(this);
            for (int i = 0; i < trackIds.size(); i++) {
                updateNotification(i + 1, trackIds.size());
                contentProviderUtils.deleteTrack(this, trackIds.get(i));
                sendResult(trackIds.get(i), i + 1, trackIds.size());
            }
            stopSelf();
        });
    }

    private void sendResult(Track.Id trackId, int deletes, int total) {
        if (deleteResultObservable != null) {
            deleteResultObservable.postValue(new DeleteStatus(trackId, deletes, total));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public LiveData<DeleteStatus> getDeletingStatusObservable() {
        return deleteResultObservable;
    }

    /**
     * Starts and shows the notification.
     *
     * @param tracksToDelete number of tracks to be deleted.
     */
    private void createAndShowNotification(int tracksToDelete) {
        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, this.getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                notificationChannel.setAllowBubbles(true);
            }

            notificationManager.createNotificationChannel(notificationChannel);
        }

        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        notificationBuilder
                .setContentTitle(this.getString(R.string.track_delete_progress_message))
                .setContentText(this.getString(R.string.track_delete_progress, 0, tracksToDelete))
                .setSmallIcon(R.drawable.ic_logo_color_24dp)
                .setProgress(tracksToDelete, 0, false);

        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    /**
     * Updates notification progress.
     *
     * @param progress number of tracks already deleted.
     * @param total    total of tracks to be deleted.
     */
    private void updateNotification(int progress, int total) {
        notificationBuilder.setProgress(total, progress, false);
        notificationBuilder.setContentText(this.getString(R.string.track_delete_progress, progress, total));
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    public class Binder extends android.os.Binder {

        private Binder() {
            super();
        }

        public TrackDeleteService getService() {
            return TrackDeleteService.this;
        }
    }

    public static class DeleteStatus {
        private final int progress;
        private final int max;
        private final Track.Id trackId;

        /**
         * @param trackId  Track.Id just deleted if any.
         * @param progress number of deletes.
         * @param max      total of deletes to be done.
         */
        private DeleteStatus(@Nullable Track.Id trackId, int progress, int max) {
            this.trackId = trackId;
            this.progress = progress;
            this.max = max;
        }

        public boolean isFinished() {
            return progress == max;
        }

        public boolean isDeleted(Track.Id trackId) {
            return this.trackId != null && this.trackId.equals(trackId);
        }

        @NonNull
        @Override
        public String toString() {
            return "DeleteStatus{" +
                    "number of deletes=" + progress +
                    ", total=" + max +
                    '}';
        }
    }
}
