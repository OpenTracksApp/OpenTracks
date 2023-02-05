package de.dennisguse.opentracks.data;

import android.database.Cursor;

import de.dennisguse.opentracks.data.tables.TrackPointsColumns;

/**
 * A cache of track points indexes.
 */
class CachedTrackPointsIndexes {
    final int idIndex;
    final int typeIndex;
    final int longitudeIndex;
    final int latitudeIndex;
    final int timeIndex;
    final int altitudeIndex;
    final int accuracyIndex;
    final int accuracyVerticalIndex;
    final int speedIndex;
    final int bearingIndex;
    final int sensorHeartRateIndex;
    final int sensorCadenceIndex;
    final int sensorDistanceIndex;
    final int sensorPowerIndex;
    final int altitudeGainIndex;
    final int altitudeLossIndex;

    CachedTrackPointsIndexes(Cursor cursor) {
        idIndex = cursor.getColumnIndex(TrackPointsColumns._ID);
        typeIndex = cursor.getColumnIndex(TrackPointsColumns.TYPE);
        longitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.LONGITUDE);
        latitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.LATITUDE);
        timeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.TIME);
        altitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALTITUDE);
        accuracyIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.HORIZONTAL_ACCURACY);
        accuracyVerticalIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.VERTICAL_ACCURACY);
        speedIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SPEED);
        bearingIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.BEARING);
        sensorHeartRateIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_HEARTRATE);
        sensorCadenceIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_CADENCE);
        sensorDistanceIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_DISTANCE);
        sensorPowerIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_POWER);
        altitudeGainIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALTITUDE_GAIN);
        altitudeLossIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALTITUDE_LOSS);
    }
}
