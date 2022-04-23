package de.dennisguse.opentracks.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackListActivity;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Manages the content of the notification shown by {@link TrackRecordingService}.
 */
class TrackRecordingServiceNotificationManager implements SharedPreferences.OnSharedPreferenceChangeListener {

    static final int NOTIFICATION_ID = 123;

    private static final String CHANNEL_ID = TrackRecordingServiceNotificationManager.class.getSimpleName();

    private final NotificationCompat.Builder notificationBuilder;

    private final NotificationManager notificationManager;

    private boolean previousLocationWasAccurate = true;

    private Boolean metricUnits = null;

    TrackRecordingServiceNotificationManager(Context context) {
        PreferencesUtils.registerOnSharedPreferenceChangeListener(this);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                notificationChannel.setAllowBubbles(true);
            }

            notificationManager.createNotificationChannel(notificationChannel);
        }

        notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationBuilder
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(context.getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_logo_color_24dp);
    }

    void stop() {
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(this);
    }

    @VisibleForTesting
    TrackRecordingServiceNotificationManager(NotificationManager notificationManager, NotificationCompat.Builder notificationBuilder) {
        this.notificationManager = notificationManager;
        this.notificationBuilder = notificationBuilder;
    }

    void updateContent(String content) {
        notificationBuilder.setSubText(content);
        updateNotification();
    }

    void updateTrackPoint(Context context, TrackStatistics trackStatistics, TrackPoint trackPoint, Distance recordingGpsAccuracy) {
        String formattedAccuracy = context.getString(R.string.value_none);

        DistanceFormatter formatter = DistanceFormatter.Builder().build(context);
        if (trackPoint.hasHorizontalAccuracy()) {
            formattedAccuracy = formatter.formatDistance(trackPoint.getHorizontalAccuracy(), metricUnits);

            boolean currentLocationWasAccurate = trackPoint.getHorizontalAccuracy().lessThan(recordingGpsAccuracy);
            boolean shouldAlert = !currentLocationWasAccurate && previousLocationWasAccurate;
            notificationBuilder.setOnlyAlertOnce(!shouldAlert);
            previousLocationWasAccurate = currentLocationWasAccurate;
        }

        notificationBuilder.setContentTitle(context.getString(R.string.track_distance_notification, formatter.formatDistance(trackStatistics.getTotalDistance(), metricUnits)));
        notificationBuilder.setContentText(context.getString(R.string.track_speed_notification, StringUtils.formatSpeed(context, trackPoint.getSpeed(), metricUnits, true)));
        notificationBuilder.setSubText(context.getString(R.string.track_recording_notification_accuracy, formattedAccuracy));
        updateNotification();

        notificationBuilder.setOnlyAlertOnce(true);
    }

    Notification setRecording(Context context) {
        Intent intent = IntentUtils.newIntent(context, TrackRecordingActivity.class);

        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(0, pendingIntentFlags);

        updateContent(context.getString(R.string.gps_starting));

        notificationBuilder.setContentIntent(pendingIntent);
        updateNotification();

        return getNotification();
    }

    Notification setGPSonlyStarted(Context context) {
        Intent intent = IntentUtils.newIntent(context, TrackListActivity.class);

        int pendingIntentFlags = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addParentStack(TrackListActivity.class)
                .addNextIntent(intent)
                .getPendingIntent(0, pendingIntentFlags);

        updateContent(context.getString(R.string.gps_starting));

        notificationBuilder.setContentIntent(pendingIntent);
        updateNotification();

        return getNotification();
    }

    void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    void setMetricUnits(boolean metricUnits) {
        this.metricUnits = metricUnits;
    }

    private void updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, getNotification());
    }

    private Notification getNotification() {
        return notificationBuilder.build();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PreferencesUtils.isKey(R.string.stats_units_key, key)) {
            setMetricUnits(PreferencesUtils.isMetricUnits());
        }
    }
}
