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
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackDeleteService extends Service {

  private static final String CHANNEL_ID =
    TrackDeleteService.class.getSimpleName();
  private static final int NOTIFICATION_ID = 1;

  static final String EXTRA_TRACK_IDS = "extra_track_ids";

  private final Binder binder = new Binder();
  private ExecutorService serviceExecutor;
  private MutableLiveData<DeletionFinishedStatus> deleteResultObservable;
  private NotificationManager notificationManager;

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
    ArrayList<Track.Id> trackIds = intent.getParcelableArrayListExtra(
      EXTRA_TRACK_IDS
    );
    createAndShowNotification(trackIds.size());
    deleteTracks(trackIds);
    return START_NOT_STICKY;
  }

  private void deleteTracks(@NonNull List<Track.Id> trackIds) {
    serviceExecutor.execute(() -> {
      ContentProviderUtils contentProviderUtils = new ContentProviderUtils(
        this
      );
      contentProviderUtils.deleteTracks(this, trackIds);
      sendResult(trackIds);
      stopSelf();
    });
  }

  private void sendResult(List<Track.Id> trackIds) {
    if (deleteResultObservable != null) {
      deleteResultObservable.postValue(new DeletionFinishedStatus(trackIds));
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  public LiveData<DeletionFinishedStatus> getDeletingStatusObservable() {
    return deleteResultObservable;
  }

  /**
   * Starts and shows the notification.
   *
   * @param tracksToDelete number of tracks to be deleted.
   */
  private void createAndShowNotification(int tracksToDelete) {
    NotificationCompat.Builder notificationBuilder;
    notificationManager =
      (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel notificationChannel = new NotificationChannel(
        CHANNEL_ID,
        this.getString(R.string.app_name),
        NotificationManager.IMPORTANCE_LOW
      );
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        notificationChannel.setAllowBubbles(true);
      }

      notificationManager.createNotificationChannel(notificationChannel);
    }

    notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
    notificationBuilder
      .setContentTitle(this.getString(R.string.track_delete_progress_message))
      .setContentText(
        this.getString(R.string.track_delete_number_of_tracks, tracksToDelete)
      )
      .setSmallIcon(R.drawable.ic_logo_color_24dp)
      .setProgress(0, 0, true);

    startForeground(NOTIFICATION_ID, notificationBuilder.build());
  }

  public class Binder extends android.os.Binder {

    private Binder() {
      super();
    }

    public TrackDeleteService getService() {
      return TrackDeleteService.this;
    }
  }

  public static class DeletionFinishedStatus {

    private final List<Track.Id> trackIds;

    /**
     * @param trackIds List of deleted Track.Ids.
     */
    private DeletionFinishedStatus(@Nullable List<Track.Id> trackIds) {
      this.trackIds = trackIds;
    }

    public boolean isDeleted(Track.Id trackId) {
      return this.trackIds != null && this.trackIds.contains(trackId);
    }

    @NonNull
    @Override
    public String toString() {
      return "DeleteStatus{" + "trackIds=" + trackIds + '}';
    }
  }
}
