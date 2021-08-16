package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * This class handle GPS status according to received locations` and some thresholds.
 */
//TODO should handle sharedpreference changes
class GpsStatus {

    private static final String TAG = GpsStatus.class.getSimpleName();

    // The duration that GpsStatus waits from minimal interval to consider GPS lost.
    private static final Duration SIGNAL_LOST_THRESHOLD = Duration.ofSeconds(10);

    private Distance thresholdHorizontalAccuracy;
    // Threshold for time without points.
    private Duration signalLostThreshold;

    private GpsStatusValue gpsStatus = GpsStatusValue.GPS_NONE;
    private GpsStatusListener client;
    private final Context context;

    @Nullable
    private TrackPoint lastTrackPoint = null;

    @Nullable
    // The last valid (not null) location. Null value means that there have not been any location yet.
    private TrackPoint lastValidTrackPoint = null;

    // Flag to prevent GpsStatus checks two or more locations at the same time.
    private boolean checking = false;

    private class GpsStatusRunner implements Runnable {
        private boolean stopped = false;

        @Override
        public void run() {
            if (gpsStatus != null && !stopped) {
                onLocationChanged(null);
                gpsStatusHandler.postDelayed(gpsStatusRunner, getIntervalThreshold().toMillis());
            }
        }

        public void stop() {
            stopped = true;
            sendStatus(gpsStatus, GpsStatusValue.GPS_NONE);
        }
    }

    private final Handler gpsStatusHandler;
    private GpsStatusRunner gpsStatusRunner = null;

    public GpsStatus(Context context, GpsStatusListener client) {
        this.client = client;
        this.context = context;

        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        thresholdHorizontalAccuracy = PreferencesUtils.getRecordingDistanceInterval(sharedPreferences, context);

        Duration minRecordingInterval = PreferencesUtils.getMinRecordingInterval(sharedPreferences, context);
        signalLostThreshold = SIGNAL_LOST_THRESHOLD.plus(minRecordingInterval);

        gpsStatusHandler = new Handler();
    }

    public void start() {
        client.onGpsStatusChanged(GpsStatusValue.GPS_NONE, GpsStatusValue.GPS_ENABLED);
    }

    /**
     * The client that uses GpsStatus has to call this method to stop the Runnable if needed.
     */
    public void stop() {
        client.onGpsStatusChanged(gpsStatus, GpsStatusValue.GPS_NONE);
        client = null;
        if (gpsStatusRunner != null) {
            gpsStatusRunner.stop();
            gpsStatusRunner = null;
        }
    }

    /**
     * Method to change the bad threshold from outside.
     *
     * @param value New preference value to signalBadThreshold.
     */
    public void onRecordingDistanceChanged(@NonNull Distance value) {
        thresholdHorizontalAccuracy = value;
    }

    public void onMinRecordingIntervalChanged(Duration value) {
        signalLostThreshold = SIGNAL_LOST_THRESHOLD.plus(value);
    }

    /**
     * This method must be called from the client every time a new trackPoint is received.
     * Receive new trackPoint and calculate the new status if needed.
     * It look for GPS changes in lastLocation if it's not null. If it's null then look for in lastValidLocation if any.
     */
    public void onLocationChanged(final TrackPoint trackPoint) {
        if (checking) {
            return;
        }

        checking = true;
        if (lastTrackPoint != null) {
            checkStatusFromLastLocation();
        } else if (lastValidTrackPoint != null) {
            checkStatusFromLastValidLocation();
        }

        if (trackPoint != null) {
            lastValidTrackPoint = trackPoint;
        }
        lastTrackPoint = trackPoint;
        checking = false;
    }

    /**
     * Checks if lastLocation has new GPS status looking up time and accuracy.
     * It depends of signalLostThreshold and signalBadThreshold.
     * If there is any change then it does the change.
     * Also, it'll run the runnable if signal is bad or stop it if the signal is lost.
     */
    private void checkStatusFromLastLocation() {
        if (Duration.between(lastTrackPoint.getTime(), Instant.now()).compareTo(signalLostThreshold) > 0 && gpsStatus != GpsStatusValue.GPS_SIGNAL_LOST) {
            // Too much time without receiving signal -> signal lost.
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_SIGNAL_LOST;
            sendStatus(oldStatus, gpsStatus);
            stopStatusRunner();
        } else if (lastTrackPoint.fulfillsAccuracy(thresholdHorizontalAccuracy) && gpsStatus != GpsStatusValue.GPS_SIGNAL_BAD) {
            // Too little accuracy -> bad signal.
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_SIGNAL_BAD;
            sendStatus(oldStatus, gpsStatus);
            startStatusRunner();
        } else if (lastTrackPoint.fulfillsAccuracy(thresholdHorizontalAccuracy) && gpsStatus != GpsStatusValue.GPS_SIGNAL_FIX) {
            // Gps okay.
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_SIGNAL_FIX;
            sendStatus(oldStatus, gpsStatus);
            startStatusRunner();
        }
    }

    /**
     * Checks if lastValidLocation has a new GPS status looking up time.
     * It depends on signalLostThreshold.
     * If there is any change then it does the change.
     */
    private void checkStatusFromLastValidLocation() {
        Duration elapsed = Duration.between(lastValidTrackPoint.getTime(), Instant.now());
        if (signalLostThreshold.minus(elapsed).isNegative()) {
            // Too much time without locations -> lost signal? (wait signalLostThreshold from last valid location).
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_SIGNAL_LOST;
            sendStatus(oldStatus, gpsStatus);
            stopStatusRunner();
            lastValidTrackPoint = null;
        }
    }

    /**
     * This method must be called from the client every time the GPS sensor is enabled.
     * Anyway, it checks that GPS is enabled because the client assumes that if it's on then GPS is enabled but user can disable GPS by hand.
     */
    public void onGpsEnabled() {
        if (gpsStatus != GpsStatusValue.GPS_ENABLED) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                GpsStatusValue oldStatus = gpsStatus;
                gpsStatus = GpsStatusValue.GPS_ENABLED;
                sendStatus(oldStatus, gpsStatus);
                startStatusRunner();
            } else {
                onGpsDisabled();
            }
        }
    }

    /**
     * This method must be called from service every time the GPS sensor is disabled.
     */
    public void onGpsDisabled() {
        if (gpsStatus != GpsStatusValue.GPS_DISABLED) {
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_DISABLED;
            sendStatus(oldStatus, gpsStatus);
            lastTrackPoint = null;
            lastValidTrackPoint = null;
            stopStatusRunner();
        }
    }

    private void sendStatus(GpsStatusValue prev, GpsStatusValue current) {
        if (client != null) {
            client.onGpsStatusChanged(prev, current);
        }
    }

    private void startStatusRunner() {
        if (gpsStatusRunner == null) {
            gpsStatusRunner = new GpsStatusRunner();
            gpsStatusRunner.run();
        }
    }

    private void stopStatusRunner() {
        if (gpsStatusRunner != null) {
            gpsStatusRunner.stop();
            gpsStatusRunner = null;
        }
    }

    public Duration getIntervalThreshold() {
        return signalLostThreshold;
    }

    public GpsStatusValue getGpsStatus() {
        return gpsStatus;
    }

    public interface GpsStatusListener {
        void onGpsStatusChanged(GpsStatusValue prevStatus, GpsStatusValue currentStatus);
    }
}
