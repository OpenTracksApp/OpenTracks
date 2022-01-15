package de.dennisguse.opentracks.data;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Iterator;
import java.util.NoSuchElementException;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;

/**
 * A lightweight wrapper around the original {@link Cursor}.
 */
public class TrackPointIterator implements Iterator<TrackPoint>, AutoCloseable {

    private static final String TAG = TrackPointIterator.class.getSimpleName();

    private final ContentProviderUtils contentProviderUtils;
    private final Track.Id trackId;
    private final CachedTrackPointsIndexes indexes;
    private Cursor cursor;

    public TrackPointIterator(ContentProviderUtils contentProviderUtils, Track.Id trackId, TrackPoint.Id startTrackPointId) {
        this.contentProviderUtils = contentProviderUtils;
        this.trackId = trackId;

        cursor = getCursor(startTrackPointId);
        indexes = new CachedTrackPointsIndexes(cursor);
    }

    private Cursor getCursor(TrackPoint.Id trackPointId) {
        return contentProviderUtils.getTrackPointCursor(trackId, trackPointId);
    }

    @Override
    public boolean hasNext() {
        if (cursor == null) {
            return false;
        }
        return !cursor.isLast() && !cursor.isAfterLast();
    }

    @Override
    @NonNull
    public TrackPoint next() {
        if (cursor == null || !cursor.moveToNext()) {
            throw new NoSuchElementException();
        }
        return ContentProviderUtils.fillTrackPoint(cursor, indexes);
    }

    @VisibleForTesting
    public int getCount() {
        return cursor.getCount();
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