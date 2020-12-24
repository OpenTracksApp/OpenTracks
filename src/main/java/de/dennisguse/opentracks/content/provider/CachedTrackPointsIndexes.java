package de.dennisguse.opentracks.content.provider;

import android.database.Cursor;

import de.dennisguse.opentracks.content.data.TrackPointsColumns;

/**
 * A cache of track points indexes.
 */
class CachedTrackPointsIndexes {
    final int idIndex;
    final int longitudeIndex;
    final int latitudeIndex;
    final int timeIndex;
    final int altitudeIndex;
    final int accuracyIndex;
    final int speedIndex;
    final int bearingIndex;
    final int sensorHeartRateIndex;
    final int sensorCadenceIndex;
    final int sensorPowerIndex;
    final int elevationGainIndex;
    final int elevationLossIndex;

    CachedTrackPointsIndexes(Cursor cursor) {
        idIndex = cursor.getColumnIndex(TrackPointsColumns._ID);
        longitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.LONGITUDE);
        latitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.LATITUDE);
        timeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.TIME);
        altitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALTITUDE);
        accuracyIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.ACCURACY);
        speedIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SPEED);
        bearingIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.BEARING);
        sensorHeartRateIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_HEARTRATE);
        sensorCadenceIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_CADENCE);
        sensorPowerIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_POWER);
        elevationGainIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.ELEVATION_GAIN);
        elevationLossIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.ELEVATION_LOSS);
    }
}
