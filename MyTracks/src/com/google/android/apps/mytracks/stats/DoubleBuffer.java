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

package com.google.android.apps.mytracks.stats;

/**
 * This class maintains a buffer of doubles. This buffer is a convenient class
 * for storing a series of doubles and calculating information about them. This
 * is a FIFO buffer.
 *
 * @author Sandor Dornbush
 */
public class DoubleBuffer {

  /**
   * The location that the next write will occur at.
   */
  private int index;

  /**
   * The sliding buffer of doubles.
   */
  private final double[] buffer;

  /**
   * Have all of the slots in the buffer been filled?
   */
  private boolean isFull;

  /**
   * Creates a buffer with size elements.
   *
   * @param size the number of elements in the buffer
   * @throws IllegalArgumentException if the size is not a positive value
   */
  public DoubleBuffer(int size) {
    if (size < 1) {
      throw new IllegalArgumentException("The buffer size must be positive.");
    }
    buffer = new double[size];
    reset();
  }

  /**
   * Adds a double to the buffer. If the buffer is full the oldest element is
   * overwritten.
   *
   * @param d the double to add
   */
  public void setNext(double d) {
    if (index == buffer.length) {
      index = 0;
    }
    buffer[index] = d;
    index++;
    if (index == buffer.length) {
      isFull = true;
    }
  }

  /**
   * Are all of the entries in the buffer used?
   */
  public boolean isFull() {
    return isFull;
  }

  /**
   * Resets the buffer to the initial state.
   */
  public void reset() {
    index = 0;
    isFull = false;
  }

  /**
   * Gets the average of values from the buffer.
   *
   * @return The average of the buffer
   */
  public double getAverage() {
    int numberOfEntries = isFull ? buffer.length : index;
    if (numberOfEntries == 0) {
      return 0;
    }

    double sum = 0;
    for (int i = 0; i < numberOfEntries; i++) {
      sum += buffer[i];
    }
    return sum / numberOfEntries;
  }

  /**
   * Gets the average and standard deviation of the buffer.
   *
   * @return An array of two elements - the first is the average, and the second
   *         is the variance
   */
  public double[] getAverageAndVariance() {
    int numberOfEntries = isFull ? buffer.length : index;
    if (numberOfEntries == 0) {
      return new double[]{0, 0};
    }

    double sum = 0;
    double sumSquares = 0;
    for (int i = 0; i < numberOfEntries; i++) {
      sum += buffer[i];
      sumSquares += Math.pow(buffer[i], 2);
    }

    double average = sum / numberOfEntries;
    return new double[]{average,
      sumSquares / numberOfEntries  - Math.pow(average, 2)};
  }

  @Override
  public String toString() {
    StringBuffer stringBuffer = new StringBuffer("Full: ");
    stringBuffer.append(isFull);
    stringBuffer.append("\n");
    for (int i = 0; i < buffer.length; i++) {
      stringBuffer.append((i == index) ? "<<" : "[");
      stringBuffer.append(buffer[i]);
      stringBuffer.append((i == index) ? ">> " : "] ");
    }
    return stringBuffer.toString();
  }
}
