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
 * Utils to calculate caloric expenditure.
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
   * Ratio of change vo2 to kcal/L
   */
  private static final double VO2H_TO_KCAL = 5;

  private static final double MILLISECOND_TO_SECOND = 1000.0;
  private static final double MILLILITER_TO_LITER = 1000.0;
  private static final double SECOND_TO_MINUTE = 60.0;

  /**
   * Changes 4.5 miles per hour to meters per minutes.
   */
  @VisibleForTesting
  static final double CRTICAL_SPEED_RUNNING = 4.5 * UnitConversions.MI_TO_KM * 1000
      / SECOND_TO_MINUTE;

  public enum ActivityType {
    CYCLING, FOOT
  }

  /**
   * Calculates the calorie expenditure of walking. This equation is appropriate
   * for fairly slow speed ranges—from 1.9 to approximately 4 miles per hour
   * (mph).
   * 
   * @param speed is calculated in meters per minute (m/min)
   * @param grade
   * @return the VO2 value in ml/kg/min.
   */
  @VisibleForTesting
  static double calculateWalkingVO2(double speed, double grade) {
    /*
     * 0.1 means oxygen cost per meter of moving each kilogram (kg) of body
     * weight while walking (horizontally). 1.8 means oxygen cost per meter of
     * moving total body mass against gravity (vertically).
     */
    return 0.1 * speed + 1.8 * speed * Math.abs(grade) + RESTING_VO2;
  }

  /**
   * Calculates the calorie expenditure of running. This equation is appropriate
   * for speeds greater than 5.0 mph (or 3.0 mph or greater if the subject is
   * truly jogging).
   * 
   * @param speed is calculated in meters per minute (m/min)
   * @param grade
   * @return the VO2 value in ml/kg/min.
   */
  @VisibleForTesting
  static double calculateRunningVO2(double speed, double grade) {

    /*
     * 0.2 means oxygen cost per meter of moving each kg of body weight while
     * running (horizontally). 0.9 means oxygen cost per meter of moving total
     * body mass against gravity (vertically).
     */
    return 0.2 * speed + 0.9 * speed * Math.abs(grade) + RESTING_VO2;
  }

  /**
   * Calculates the calorie expenditure of cycling. Use below equation: <br>
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
   * @param weight of rider plus bike
   * @param timeUsed how many times used in second
   * @return the power value watts(Joule/second).
   */
  @VisibleForTesting
  static double calculateCyclingCalories(double speed, double grade, int weight, double timeUsed) {
    // Get the Power
    double power = 9.8 * weight * speed * (0.0053 + grade) + 0.185 * (speed * speed * speed);
    // Get the calories
    return power * timeUsed / UnitConversions.KCAL_TO_J;
  }

  /**
   * Calculates the calories expenditure by cycling.
   * 
   * @param start the start location
   * @param stop the stop location
   * @param grade the grade to calculate
   * @param weight the weight of user
   * @return the calories expenditure between the start and stop location.
   */
  @VisibleForTesting
  static double calculateExpenditureCycling(Location start, Location stop, double grade, int weight) {
    // Gets time in seconds
    double time = (double) (stop.getTime() - start.getTime()) / MILLISECOND_TO_SECOND;
    // Meters per second
    double speed = (start.getSpeed() + stop.getSpeed()) / 2.0;
    if (grade < 0) {
      grade = 0.0;
    }

    return calculateCyclingCalories(speed, grade, weight, time);
  }

  /**
   * Calculates the calories expenditure by running or walking.
   * 
   * @param start the start location
   * @param stop the stop location
   * @param grade the grade to calculate
   * @param weight the weight of user
   * @return the calories expenditure between the start and stop location.
   */
  @VisibleForTesting
  static double calculateExpenditureFoot(Location start, Location stop, double grade, int weight) {
    // Meters per minute
    double averageSpeed = (start.getSpeed() + stop.getSpeed()) * SECOND_TO_MINUTE / 2.0;
    // Get VO2
    double VO2 = getVO2(averageSpeed, grade);
    // Seconds
    double time = (double) (stop.getTime() - start.getTime()) / MILLISECOND_TO_SECOND;
    // Change mL/kg/min to mL/kg
    double VO2All_mL = VO2 * time / SECOND_TO_MINUTE;
    // Change mL/kg to L/kg
    double VO2All_L = VO2All_mL / MILLILITER_TO_LITER;
    // Get the calorie
    return VO2All_L * weight * VO2H_TO_KCAL;
  }

  /**
   * Gets the VO2 value.
   * 
   * @param speed in meters per minute
   * @param grade the grade to calculate
   * @return the VO2 value.
   */
  @VisibleForTesting
  static double getVO2(double speed, double grade) {
    if (grade < 0) {
      grade = 0.0;
    }

    return speed > CRTICAL_SPEED_RUNNING ? calculateRunningVO2(speed, grade) : calculateWalkingVO2(
        speed, grade);
  }

  /**
   * Calculates the calories expenditure between two locations.
   * 
   * @param start the start location
   * @param stop the stop location
   * @param grade the grade to calculate
   * @param weight the weight of user
   * @param activityType can be foot or cycling
   * @return the calories expenditure between the start and stop location.
   */
  public static double getCalories(Location start, Location stop, double grade, int weight,
      ActivityType activityType) {
    if (ActivityType.CYCLING == activityType) {
      return calculateExpenditureCycling(start, stop, grade, weight);
    } else {
      return calculateExpenditureFoot(start, stop, grade, weight);
    }
  }
}
