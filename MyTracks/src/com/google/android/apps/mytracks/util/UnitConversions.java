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
public abstract class UnitConversions {

  public static final double KM_TO_MI = 0.621371192;
  public static final double M_TO_FT = 3.2808399;
  public static final double MI_TO_M = 1609.344;
  public static final double MI_TO_FEET = 5280.0;
  public static final double KMH_TO_MPH = 1000 * M_TO_FT / MI_TO_FEET;
  public static final double TO_RADIANS = Math.PI / 180.0;
  public static final double MPH_TO_KMH = 1.609344;
  
  protected UnitConversions() {
  }
}
