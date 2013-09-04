/*
 * Copyright 2008 Google Inc.
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
package com.google.android.apps.mytracks.util;

/**
 * Unit conversion constants.
 * 
 * @author Sandor Dornbush
 */
public class UnitConversions {

  private UnitConversions() {}

  // Milliliter to liter
  public static final double ML_TO_L = 1 / 1000.0;

  // Millisecond to second
  public static final double MS_TO_S = 1 / 1000.0;

  // Minute to second
  public static final double MIN_TO_S = 60.0;

  // Second to minute
  public static final double S_TO_MIN = 1 / MIN_TO_S;

  // Minute to hour
  public static final double HR_TO_MIN = 60.0;

  // Minute to hour
  public static final double MIN_TO_HR = 1 / HR_TO_MIN;

  // multiplication factor to convert kilometers to miles
  public static final double KM_TO_MI = 0.621371192;

  // multiplication factor to convert miles to kilometers
  public static final double MI_TO_KM = 1 / KM_TO_MI;

  // multiplication factor to convert miles to feet
  public static final double MI_TO_FT = 5280.0;

  // multiplication factor to convert feet to miles
  public static final double FT_TO_MI = 1 / MI_TO_FT;

  // multiplication factor to convert meters to kilometers
  public static final double M_TO_KM = 1 / 1000.0;

  public static final double KM_TO_M = 1 / M_TO_KM;

  // multiplication factor to convert meters per second to kilometers per hour
  public static final double MS_TO_KMH = 3.6;

  // multiplication factor to convert meters to miles
  public static final double M_TO_MI = M_TO_KM * KM_TO_MI;

  // multiplication factor to convert meters to feet
  public static final double M_TO_FT = M_TO_MI * MI_TO_FT;

  // multiplication factor to convert degrees to radians
  public static final double DEG_TO_RAD = Math.PI / 180.0;

  // Ratio of change kcal to joule.
  public static final double KCAL_TO_J = 4.184;
}
