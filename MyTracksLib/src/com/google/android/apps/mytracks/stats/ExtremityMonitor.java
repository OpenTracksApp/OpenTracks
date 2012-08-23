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
 * A helper class that tracks a minimum and a maximum of a variable.
 * 
 * @author Sandor Dornbush
 */
public class ExtremityMonitor {

  // The smallest value seen so far.
  private double min;

  // The largest value seen so far.
  private double max;

  public ExtremityMonitor() {
    reset();
  }

  /**
   * Resets this object to it's initial state where the min and max are unknown.
   */
  public void reset() {
    min = Double.POSITIVE_INFINITY;
    max = Double.NEGATIVE_INFINITY;
  }

  /**
   * Gets the minimum value seen.
   */
  public double getMin() {
    return min;
  }

  /**
   * Gets the maximum value seen.
   */
  public double getMax() {
    return max;
  }

  /**
   * Updates the min and the max with a new value.
   * 
   * @param value the new value
   * @return true if an extremity was found
   */
  public boolean update(double value) {
    boolean changed = false;
    if (value < min) {
      min = value;
      changed = true;
    }
    if (value > max) {
      max = value;
      changed = true;
    }
    return changed;
  }

  /**
   * Sets the minimum and maximum values.
   * 
   * @param min the minimum value
   * @param max the maximum value
   */
  public void set(double min, double max) {
    this.min = min;
    this.max = max;
  }

  /**
   * Sets the minimum value.
   * 
   * @param min the minimum value
   */
  public void setMin(double min) {
    this.min = min;
  }

  /**
   * Sets the maximum value.
   * 
   * @param max the maximum value
   */
  public void setMax(double max) {
    this.max = max;
  }

  /**
   * Returns true if has data.
   */
  public boolean hasData() {
    return min != Double.POSITIVE_INFINITY && max != Double.NEGATIVE_INFINITY;
  }

  @Override
  public String toString() {
    return "Min: " + min + " Max: " + max;
  }
}
