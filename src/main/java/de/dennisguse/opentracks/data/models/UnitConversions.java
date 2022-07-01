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
package de.dennisguse.opentracks.data.models;

/**
 * Unit conversion constants.
 *
 * @author Sandor Dornbush
 */
public class UnitConversions {

    // multiplication factor to convert kilometers to miles
    public static final double KM_TO_MI = 0.621371192;

    // Distance //TODO Make private to Distance class!
    // multiplication factor to convert miles to feet
    private static final double MI_TO_FT = 5280.0;
    // multiplication factor to covert kilometers to meters
    public static final double KM_TO_M = 1000.0;
    // multiplication factor to convert meters to kilometers
    public static final double M_TO_KM = 1 / KM_TO_M;
    // multiplication factor to convert meters to miles
    public static final double M_TO_MI = M_TO_KM * KM_TO_MI;
    // multiplication factor to convert meters to feet
    public static final double M_TO_FT = M_TO_MI * MI_TO_FT;
    public static final double NAUTICAL_MILE_TO_M = 1852.0;
    public static final double M_TO_NAUTICAL_MILE = 1 / NAUTICAL_MILE_TO_M;
    public static final double KM_TO_NAUTICAL_MILE = 1000 * M_TO_NAUTICAL_MILE;
    // multiplication factor to convert miles to km
    public static final double MI_TO_KM = 1 / KM_TO_MI;
    // multiplication factor to convert miles to m
    public static final double MI_TO_M = MI_TO_KM * KM_TO_M;

    private UnitConversions() {
    }
}
