package de.dennisguse.opentracks.content;

import android.database.Cursor;
import android.location.Location;

import java.util.Iterator;

/**
 * A lightweight wrapper around the original {@link Cursor} with a method to clean up.
 */
public interface LocationIterator extends Iterator<Location>, AutoCloseable {

    /**
     * Gets the most recently retrieved track point id by {@link #next()}.
     */
    long getLocationId();

    /**
     * Closes the iterator.
     */
    void close();
}