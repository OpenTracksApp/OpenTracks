/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.stats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * This class maintains a ring buffer of doubles.
 * This buffer is a convenient class for storing a series of doubles and calculating information about them.
 * This is a FIFO buffer.
 *
 * @author Sandor Dornbush
 */
abstract class RingBuffer<T> {

    private final ArrayList<T> buffer;

    // The location that the next write will occur at.
    private int index;

    // True if the buffer is full
    private boolean isFull;

    /**
     * Creates a buffer with a certain size.
     *
     * @param size the size
     */
    RingBuffer(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("The buffer size must be greater than 1.");
        }

        buffer = new ArrayList<T>(size);
        reset();
    }

    RingBuffer(RingBuffer<T> toCopy) {
        this.buffer = new ArrayList<>(toCopy.buffer);
        this.index = toCopy.index;
        this.isFull = toCopy.isFull;
    }

    /**
     * Resets the buffer.
     */
    public void reset() {
        index = 0;
        isFull = false;
    }

    /**
     * Returns true if the buffer is full.
     */
    boolean isFull() {
        return isFull;
    }

    /**
     * Gets the average of the buffer.
     */
    public T getAverage() {
        int numberOfEntries = isFull ? buffer.size() : index;
        if (numberOfEntries == 0) {
            return null;
        }
        Double sum = null;
        int numberOfUsedEntries = 0;
        for (int i = 0; i < numberOfEntries; i++) {
            Number value = from(buffer.get(i));
            if (value != null) {
                if (sum == null) {
                    sum = 0.0;
                }
                sum += value.doubleValue();
                numberOfUsedEntries++;
            }
        }
        if (sum == null) {
            return null;
        } else {
            return to(sum / numberOfUsedEntries);
        }
    }

    @Nullable
    protected abstract Number from(T object);

    protected abstract T to(double object);

    /**
     * Adds a double to the buffer.
     * If the buffer is full the oldest element is overwritten.
     *
     * @param value the double to add
     */
    public void setNext(T value) {
        if (index == buffer.size()) {
            index = 0;
        }
        buffer.add(index, value);
        index++;
        if (index == buffer.size()) {
            isFull = true;
        }
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Full: ");
        builder.append(isFull);
        builder.append("\n");
        for (int i = 0; i < buffer.size(); i++) {
            builder.append((i == index) ? "<<" : "[");
            builder.append(buffer.get(i));
            builder.append((i == index) ? ">> " : "] ");
        }
        return builder.toString();
    }
}
