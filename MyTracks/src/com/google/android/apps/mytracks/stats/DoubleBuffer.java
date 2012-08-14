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

  // The location that the next write will occur at.
  private int index;

  // The sliding buffer of doubles.
  private final double[] buffer;

  // True if the buffer is full
  private boolean isFull;

  /**
   * Creates a buffer with a certain size.
   * 
   * @param size the size
   */
  public DoubleBuffer(int size) {
    if (size < 1) {
      throw new IllegalArgumentException("The buffer size must be greater than 1.");
    }
    buffer = new double[size];
    reset();
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
  public boolean isFull() {
    return isFull;
  }

  /**
   * Gets the average of the buffer.
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
   * Gets the average and the variance of the buffer.
   * 
   * @return an array of two elements - the first is the average, the second is
   *         the variance
   */
  public double[] getAverageAndVariance() {
    int numberOfEntries = isFull ? buffer.length : index;
    if (numberOfEntries == 0) {
      return new double[] { 0, 0 };
    }

    double sum = 0;
    double sumSquares = 0;
    for (int i = 0; i < numberOfEntries; i++) {
      sum += buffer[i];
      sumSquares += Math.pow(buffer[i], 2);
    }

    double average = sum / numberOfEntries;
    double variance = sumSquares / numberOfEntries - Math.pow(average, 2);
    return new double[] { average, variance };
  }

  /**
   * Adds a double to the buffer. If the buffer is full the oldest element is
   * overwritten.
   * 
   * @param value the double to add
   */
  public void setNext(double value) {
    if (index == buffer.length) {
      index = 0;
    }
    buffer[index] = value;
    index++;
    if (index == buffer.length) {
      isFull = true;
    }
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
