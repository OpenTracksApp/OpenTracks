/*
 * Copyright 2013 Google Inc.
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

import com.google.common.annotations.VisibleForTesting;

import android.location.Location;

/**
 * Utils to calculate calories.
 * 
 * @author youtaol
 */
public class CalorieUtils {

  private CalorieUtils() {}

  /**
   * Resting VO2 is constant for everyone and is equal to 3.5 milliliters per
   * kilogram of body weight per minute.
   */
  private static final double RESTING_VO2 = 3.5;

  /**
   * Ratio of change Liter to kcal. In kcal per Liter.
   */
  private static final double L_TO_KCAL = 5;

  private static final double earthGravity = 9.8;

  /**
   * Lumped constant for all frictional losses (tires, bearings, chain)
   */
  private static final double K1 = 0.0053;

  /**
   * Lumped constant for aerodynamic drag
   */
  private static final double K2 = 0.185;

  /**
   * Critical speed running in meters per minute. Converts 4.5 miles per hour to
   * meters per minute. 4 miles per hour is regarded as the max speed of walking
   * and 5 miles per hour is regarded as the minimal speed of running, so we use
   * the 4.5 miles per hour as the critical seed. <a href=
   * "http://www.ideafit.com/fitness-library/calculating-caloric-expenditure-0"
   * >Reference</a>
   */
  @VisibleForTesting
  static final double CRTICAL_SPEED_RUNNING = 4.5 * UnitConversions.MI_TO_KM
      * UnitConversions.KM_TO_M / UnitConversions.HR_TO_MIN;

  public enum ActivityType {
    CYCLING, FOOT
  }

  /**
   * Calculates the calorie of walking. This equation is appropriate for fairly
   * slow speed ranges—from 1.9 to approximately 4 miles per hour (mph).
   * 
   * @param speed is calculated in meters per second (m/s)
   * @param grade
   * @return the VO2 value in ml/kg/min.
   */
  @VisibleForTesting
  static double calculateWalkingVo2(double speed, double grade) {
    // Change meters per second to meters per minute
    speed = speed / UnitConversions.S_TO_MIN;
    /*
     * 0.1 means oxygen cost per meter of moving each kilogram (kg) of body
     * weight while walking (horizontally). 1.8 means oxygen cost per meter of
     * moving total body mass against gravity (vertically).
     */
    return 0.1 * speed + 1.8 * speed * Math.abs(grade) + RESTING_VO2;
  }

  /**
   * Calculates the calorie of running. This equation is appropriate for speeds
   * greater than 5.0 mph (or 3.0 mph or greater if the subject is truly
   * jogging).
   * 
   * @param speed is calculated in meters per second (m/s)
   * @param grade
   * @return the VO2 value in ml/kg/min.
   */
  @VisibleForTesting
  static double calculateRunningVo2(double speed, double grade) {
    // Change meters per second to meters per minute
    speed = speed / UnitConversions.S_TO_MIN;
    /*
     * 0.2 means oxygen cost per meter of moving each kg of body weight while
     * running (horizontally). 0.9 means oxygen cost per meter of moving total
     * body mass against gravity (vertically).
     */
    return 0.2 * speed + 0.9 * speed * Math.abs(grade) + RESTING_VO2;
  }

  /**
   * Calculates the calorie of cycling. Use below equation: <br>
   * P = g * m * Vg * (K1 + s) + K2 * (Va)^2 * Vg
   * <ul>
   * <li>P - Power in watts(Joule/second)</li>
   * <li>g - gravity, using 9.8 m / s^2</li>
   * <li>m - mass of the rider plus bike, in kg</li>
   * <li>s - grade of surface</li>
   * <li>Vg - ground relative speed m/s</li>
   * <li>Va - air relative speed. We assume the air speed is zero, so Va is
   * equal with Vg.</li>
   * <li>K1 - a constant which represents frictional losses, using 0.0053.</li>
   * <li>K2 - a constant representing aerodynamic drag, using 0.185 kg/m.</li>
   * <li></li>
   * </ul>
   * 
   * @param speed is calculated in meters per second (m/s)
   * @param grade the grade to calculate
   * @param weight of rider plus bike, in kilogram
   * @param timeUsed how many times used in second
   * @return the power value watts(Joule/second).
   */
  @VisibleForTesting
  static double calculateCyclingCalories(double speed, double grade, int weight, double timeUsed) {
    // Get the Power, the unit is Watt (Joule/second)
    double power = earthGravity * weight * speed * (K1 + grade) + K2 * (speed * speed * speed);

    // Get the calories
    return power * timeUsed / UnitConversions.KCAL_TO_J;
  }

  /**
   * Calculates the calories by cycling.
   * 
   * @param start the start location
   * @param stop the stop location
   * @param grade the grade to calculate
   * @param weight the weight of user, in kilogram
   * @return the calories between the start and stop location.
   */
  @VisibleForTesting
  static double calculateCaloriesCycling(Location start, Location stop, double grade, int weight) {
    // Gets duration in seconds
    double duration = (double) (stop.getTime() - start.getTime()) * UnitConversions.MS_TO_S;
    // Get speed in meters per second
    double speed = (start.getSpeed() + stop.getSpeed()) / 2.0;
    if (grade < 0) {
      grade = 0.0;
    }

    return calculateCyclingCalories(speed, grade, weight, duration);
  }

  /**
   * Calculates the calories by running or walking.
   * 
   * @param start the start location
   * @param stop the stop location
   * @param grade the grade to calculate
   * @param weight the weight of user, in kilogram
   * @return the calories between the start and stop location, in
   *         calories.
   */
  @VisibleForTesting
  static double calculateCaloriesFoot(Location start, Location stop, double grade, int weight) {
    // Get speed in meters per second
    double averageSpeed = (start.getSpeed() + stop.getSpeed()) / 2.0;
    // Get VO2 in mL/kg/min
    double vo2 = getVo2(averageSpeed, grade);
    // Minutes
    double time = (double) (stop.getTime() - start.getTime()) * UnitConversions.MS_TO_S
        * UnitConversions.S_TO_MIN;
    // Get the calorie. The unit of calorie is kcal which is came from mL/kg/min
    // * min * kg * L/mL * kcal/L
    return vo2 * time * weight * UnitConversions.ML_TO_L * L_TO_KCAL;
  }

  /**
   * Gets the VO2 value.
   * 
   * @param speed in meters per second
   * @param grade the grade to calculate
   * @return the VO2 value.
   */
  @VisibleForTesting
  static double getVo2(double speed, double grade) {
    if (grade < 0) {
      grade = 0.0;
    }

    return speed > CRTICAL_SPEED_RUNNING ? calculateRunningVo2(speed, grade) : calculateWalkingVo2(
        speed, grade);
  }

  /**
   * Calculates the calories between two locations.
   * 
   * @param start the start location
   * @param stop the stop location
   * @param grade the grade to calculate
   * @param weight the weight of user
   * @param activityType can be foot or cycling
   * @return the calories between the start and stop location.
   */
  public static double getCalories(Location start, Location stop, double grade, int weight,
      ActivityType activityType) {
    if (ActivityType.CYCLING == activityType) {
      return calculateCaloriesCycling(start, stop, grade, weight);
    } else {
      return calculateCaloriesFoot(start, stop, grade, weight);
    }
  }
}
