package de.dennisguse.opentracks.content;

import android.database.Cursor;
import android.location.Location;
import android.util.Log;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A lightweight wrapper around the original {@link Cursor} with a method to clean up.
 */
public class LocationIterator implements Iterator<Location>, AutoCloseable {

    private static final String TAG = LocationIterator.class.getSimpleName();

    private final ContentProviderUtils contentProviderUtils;
    private final long trackId;
    private final boolean descending;
    private final LocationFactory locationFactory;
    private final ContentProviderUtils.CachedTrackPointsIndexes indexes;
    private long lastTrackPointId = -1L;
    private Cursor cursor;


    public LocationIterator(ContentProviderUtils contentProviderUtils, long trackId, long startTrackPointId, boolean descending, LocationFactory locationFactory) {
        this.contentProviderUtils = contentProviderUtils;
        this.trackId = trackId;
        this.descending = descending;
        this.locationFactory = locationFactory;

        cursor = getCursor(startTrackPointId);
        indexes = cursor != null ? new ContentProviderUtils.CachedTrackPointsIndexes(cursor)
                : null;
    }

    /**
     * Gets the track point cursor.
     *
     * @param trackPointId the starting track point id
     */
    private Cursor getCursor(long trackPointId) {
        return contentProviderUtils.getTrackPointCursor(trackId, trackPointId, contentProviderUtils.getDefaultCursorBatchSize(), descending);
    }

    /**
     * Advances the cursor to the next batch. Returns true if successful.
     */
    private boolean advanceCursorToNextBatch() {
        long trackPointId = lastTrackPointId == -1L ? -1L : lastTrackPointId + (descending ? -1 : 1);
        Log.d(TAG, "Advancing track point id: " + trackPointId);
        cursor.close();
        cursor = getCursor(trackPointId);
        return cursor != null;
    }

    public long getLocationId() {
        return lastTrackPointId;
    }

    @Override
    public boolean hasNext() {
        if (cursor == null) {
            return false;
        }
        if (cursor.isAfterLast()) {
            return false;
        }
        if (cursor.isLast()) {
            if (cursor.getCount() != contentProviderUtils.getDefaultCursorBatchSize()) {
                return false;
            }
            return advanceCursorToNextBatch() && !cursor.isAfterLast();
        }
        return true;
    }

    @Override
    public Location next() {
        if (cursor == null) {
            throw new NoSuchElementException();
        }
        if (!cursor.moveToNext()) {
            if (!advanceCursorToNextBatch() || !cursor.moveToNext()) {
                throw new NoSuchElementException();
            }
        }
        lastTrackPointId = cursor.getLong(indexes.idIndex);
        Location location = locationFactory.createLocation();
        ContentProviderUtils.fillTrackPoint(cursor, indexes, location);
        return location;
    }

    @Override
    public void close() {
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}