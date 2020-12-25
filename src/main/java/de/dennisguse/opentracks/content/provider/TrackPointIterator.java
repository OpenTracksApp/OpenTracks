package de.dennisguse.opentracks.content.provider;

import android.database.Cursor;
import android.util.Log;

import java.util.Iterator;
import java.util.NoSuchElementException;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;

/**
 * A lightweight wrapper around the original {@link Cursor} with a method to clean up.
 */
public class TrackPointIterator implements Iterator<TrackPoint>, AutoCloseable {

    private static final String TAG = TrackPointIterator.class.getSimpleName();

    private final ContentProviderUtils contentProviderUtils;
    private final Track.Id trackId;
    private final CachedTrackPointsIndexes indexes;
    private TrackPoint.Id lastTrackPointId = null;
    private Cursor cursor;


    public TrackPointIterator(ContentProviderUtils contentProviderUtils, Track.Id trackId, TrackPoint.Id startTrackPointId) {
        this.contentProviderUtils = contentProviderUtils;
        this.trackId = trackId;

        cursor = getCursor(startTrackPointId);
        indexes = cursor != null ? new CachedTrackPointsIndexes(cursor)
                : null;
    }

    /**
     * Gets the track point cursor.
     *
     * @param trackPointId the starting track point id
     */
    private Cursor getCursor(TrackPoint.Id trackPointId) {
        return contentProviderUtils.getTrackPointCursor(trackId, trackPointId, contentProviderUtils.getDefaultCursorBatchSize());
    }

    /**
     * Advances the cursor to the next batch. Returns true if successful.
     */
    private boolean advanceCursorToNextBatch() {
        TrackPoint.Id trackPointId = lastTrackPointId == null ? null : new TrackPoint.Id(lastTrackPointId.getId() + 1);
        Log.d(TAG, "Advancing track point id: " + trackPointId);
        cursor.close();
        cursor = getCursor(trackPointId);
        return cursor != null;
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
    public TrackPoint next() {
        if (cursor == null) {
            throw new NoSuchElementException();
        }
        if (!cursor.moveToNext()) {
            if (!advanceCursorToNextBatch() || !cursor.moveToNext()) {
                throw new NoSuchElementException();
            }
        }
        lastTrackPointId = new TrackPoint.Id(cursor.getLong(indexes.idIndex));
        return ContentProviderUtils.fillTrackPoint(cursor, indexes);
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