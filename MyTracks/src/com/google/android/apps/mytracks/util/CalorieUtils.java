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

import android.location.Location;

/**
 * Utils to calculate caloric expenditure.
 * 
 * @author youtaol
 */
public class CalorieUtils {

  /**
   * Resting VO2 is constant for everyone and is equal to 3.5 milliliters per
   * kilogram of body weight per minute.
   */
  private static final double RESTING_VO2 = 3.5;

  /**
   * Ratio of change vo2 to kcal/L
   */
  private static final double VO2H_TO_KCAL = 5;

  /**
   * Changes 4.5 miles per hour to meters per minutes.
   */
  private static final double CRTICAL_SPEED_RUNNING = 4.5 * UnitConversions.MI_TO_KM * 1000 / 60;

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
   * @return the VO2 value in ml/kg/min
   */
  private static double calculateWalkingVO2(double speed, double grade) {
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
   * @return the VO2 value in ml/kg/min
   */
  private static double calculateRunningVO2(double speed, double grade) {

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
   * @return the power value watts(Joule/second)
   */
  private static double calculateCyclingCalories(double speed, double grade, int weight,
      double timeUsed) {
    // Get the Power.
    double power = 9.8 * weight * speed * (0.0053 + grade) + 0.185 * (speed * speed * speed);
    // Get the calories.
    return power * timeUsed / UnitConversions.KCAL_TO_J;
  }

  /**
   * Calculates the calories expenditure by cycling.
   * 
   * @param start the start location
   * @param stop the stop location
   * @param grade the grade to calculate
   * @param weight the weight of user
   * @return the calories expenditure between the start and stop location
   */
  private static double calculateExpenditureCycling(Location start, Location stop, double grade,
      int weight) {
    // Seconds.
    double time = (double) (stop.getTime() - start.getTime()) / 1000;
    // Meter per second.
    double speed = (start.getSpeed() + stop.getSpeed()) / 2;
    if (grade < 0) {
      grade = 0;
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
   * @return the calories expenditure between the start and stop location
   */
  private static double calculateExpenditureFoot(Location start, Location stop, double grade,
      int weight) {
    // Seconds.
    double time = (double) (stop.getTime() - start.getTime()) / 1000;
    // Meter per minute.
    double speed = (start.getSpeed() + stop.getSpeed()) * 60 / 2;
    double VO2 = 0;
    if (grade < 0) {
      grade = 0;
    }

    if (speed > CRTICAL_SPEED_RUNNING) {
      VO2 = calculateRunningVO2(speed, grade);
    } else {
      VO2 = calculateWalkingVO2(speed, grade);
    }
    // Change mL/kg/min to mL/kg.
    double VO2All = VO2 * time / 60;
    // Change mL/kg to L/kg.
    VO2All = VO2All / 1000;
    // Get the calorie.
    return VO2All * weight * VO2H_TO_KCAL;
  }

  /**
   * Calculates the calories expenditure between two locations.
   * 
   * @param start the start location
   * @param stop the stop location
   * @param grade the grade to calculate
   * @param weight the weight of user
   * @param activityType can be foot or cycling
   * @return the calories expenditure between the start and stop location
   */
  public static double getCalories(Location start, Location stop, double grade, int weight,
      ActivityType activityType) {
    if (ActivityType.CYCLING == activityType) {
      return calculateExpenditureCycling(start, stop, grade, weight);
    } else {
      return calculateExpenditureFoot(start, stop, grade, weight);
    }
  }

  /**
   * Calculates the calorie expenditure of locations in Joule.
   * 
   * @param kcal
   * @return
   */
  public static double changeKcalToJ(double kcal) {
    return kcal * UnitConversions.KCAL_TO_J;
  }
}
