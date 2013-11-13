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

  // Time

  // multiplication factor to convert seconds to milliseconds
  public static final double S_TO_MS = 1000.0;

  // multiplication factor to convert milliseconds to seconds
  public static final double MS_TO_S = 1 / S_TO_MS;

  // multiplication factor to convert minutes to seconds
  public static final double MIN_TO_S = 60.0;

  // multiplication factor to convert seconds to minutes
  public static final double S_TO_MIN = 1 / MIN_TO_S;

  // multiplication factor to convert hours to minutes
  public static final double HR_TO_MIN = 60.0;

  // multiplication factor to convert minutes to hours
  public static final double MIN_TO_HR = 1 / HR_TO_MIN;

  // Distance

  // multiplication factor to convert kilometers to miles
  public static final double KM_TO_MI = 0.621371192;

  // multiplication factor to convert miles to kilometers
  public static final double MI_TO_KM = 1 / KM_TO_MI;

  // multiplication factor to convert miles to feet
  public static final double MI_TO_FT = 5280.0;

  // multiplication factor to convert feet to miles
  public static final double FT_TO_MI = 1 / MI_TO_FT;

  // multiplication factor to covert kilometers to meters
  public static final double KM_TO_M = 1000.0;

  // multiplication factor to convert meters to kilometers
  public static final double M_TO_KM = 1 / KM_TO_M;

  // multiplication factor to convert meters to miles
  public static final double M_TO_MI = M_TO_KM * KM_TO_MI;

  // multiplication factor to convert meters to feet
  public static final double M_TO_FT = M_TO_MI * MI_TO_FT;

  // Weight

  // multiplication factor to convert kilograms to pounds
  public static final double KG_TO_LB = 2.2046;

  // multiplication factor to convert pounds to kilograms
  public static final double LB_TO_KG = 1 / KG_TO_LB;

  // Calories

  // multiplication factor to convert milliliters to liters
  public static final double ML_TO_L = 1 / 1000.0;

  // multiplication factor to convert liter to kcal
  public static final double L_TO_KCAL = 5.0;

  // multiplication factor to convert watt to kgm/min
  public static final double W_TO_KGM = 6.12;

  // multiplication factor to convert kgm to kcal
  public static final double KGM_TO_KCAL = 1 / 427.0;

  // Others

  // multiplication factor to convert meters per second to kilometers per hour
  public static final double MS_TO_KMH = M_TO_KM / (S_TO_MIN * MIN_TO_HR);

  // multiplication factor to convert degrees to radians
  public static final double DEG_TO_RAD = Math.PI / 180.0;
}
