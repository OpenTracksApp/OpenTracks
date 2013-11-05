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
import android.location.LocationManager;

import junit.framework.TestCase;

/**
 * A unit test for {@link CalorieUtils}.
 * 
 * @author youtaol
 */
public class CalorieUtilsTest extends TestCase {

  Location start = new Location(LocationManager.GPS_PROVIDER);
  Location stop = new Location(LocationManager.GPS_PROVIDER);
  private double grade = 0.07;
  private int weight = 80;
  private final long TIME_INTERVAL = 1000l;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Make the time interval is not 0
    stop.setTime(start.getTime() + TIME_INTERVAL);
  }

  /**
   * Checks using foot calculation equation.
   */
  public void testGetCalories_foot() {
    double actual = CalorieUtils.getCalorie(start, stop, grade, weight,
        CalorieUtils.ActivityType.FOOT);
    double expected = CalorieUtils.getFootCalorie(start, stop, grade, weight);
    assertEquals(expected, actual);
  }

  /**
   * Checks whether using foot calculation equation while grade is negative.
   */
  public void testGetCalories_footNegativeGrade() {
    double actualGrade = -5;
    double expectGrade = 0;
    double actual = CalorieUtils.getCalorie(start, stop, actualGrade, weight,
        CalorieUtils.ActivityType.FOOT);
    double expected = CalorieUtils.getFootCalorie(start, stop, expectGrade, weight);
    assertEquals(expected, actual);
  }

  /**
   * Checks using cycling calculation equation.
   */
  public void testGetCalories_cycling() {
    double actual = CalorieUtils.getCalorie(start, stop, grade, weight,
        CalorieUtils.ActivityType.CYCLING);
    double expected = CalorieUtils.getCyclingCalorie(start, stop, grade, weight);
    assertEquals(expected, actual);
  }

  /**
   * Checks using running VO2 equation.
   */
  public void testGetVO2_running() {
    double actual = CalorieUtils.getFootVo2(CalorieUtils.CRTICAL_SPEED_RUNNING * 2, grade);
    double expected = CalorieUtils.getHighSpeedFootVo2(CalorieUtils.CRTICAL_SPEED_RUNNING * 2,
        grade);
    assertEquals(expected, actual);
  }

  /**
   * Checks using walking VO2 equation.
   */
  public void testGetVO2_walking() {
    // Test at half the critical speed
    double footSpeed = CalorieUtils.CRTICAL_SPEED_RUNNING / 2.0;

    double actual = CalorieUtils.getFootVo2(footSpeed, grade);
    double expected = CalorieUtils.getLowSpeedFootVo2(footSpeed, grade);
    assertEquals(expected, actual);
  }

  /**
   * Checks calculating cycling calorie. 175 is the watt usage for a 90 kg bike
   * + rider to go 9 m/s (20 mph or 32 km/h) on the flat. <a href=
   * "http://www.ideafit.com/fitness-library/calculating-caloric-expenditure-0"
   * >Reference</a>
   */
  public void testCalculateCalorieCycling() {
    grade = 0;
    weight = 90;
    start.setSpeed(9);
    stop.setSpeed(9);
    double expected = 175.0 * (TIME_INTERVAL / 1000) * UnitConversions.J_TO_KCAL;
    double actual = CalorieUtils.getCyclingCalorie(start, stop, grade, weight);
    assertTrue((actual - expected) / expected < 0.02);
  }
}