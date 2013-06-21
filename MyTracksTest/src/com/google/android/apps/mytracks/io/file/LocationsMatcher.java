/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.mytracks.io.file;

import com.google.android.testing.mocking.AndroidMock;

import android.location.Location;

import java.util.Arrays;

import org.easymock.IArgumentMatcher;

/**
 * Locations matcher. Workaround because of capture bug 2617107 in easymock:
 * http://sourceforge.net/tracker/?func=detail&aid=2617107&group_id=82958&atid=567837
 * 
 * @author Jimmy Shih
 */
public class LocationsMatcher implements IArgumentMatcher {

  public static Location[] eqLoc(Location expected) {
    return eqLoc(new Location[] { expected });
  }

  public static Location[] eqLoc(Location[] expected) {
    IArgumentMatcher matcher = new LocationsMatcher(expected);
    AndroidMock.reportMatcher(matcher);
    return null;
  }

  private final Location[] expectedLocations;

  private LocationsMatcher(Location[] expected) {
    this.expectedLocations = expected;
  }

  @Override
  public void appendTo(StringBuffer buf) {
    buf.append("eqLoc(").append(Arrays.toString(expectedLocations)).append(")");
  }

  @Override
  public boolean matches(Object obj) {
    if (!(obj instanceof Location[])) {
      return false;
    }
    Location[] locations = (Location[]) obj;
    if (locations.length < expectedLocations.length) {
      return false;
    }

    // Only check the first elements (those that will be taken into account)
    for (int i = 0; i < expectedLocations.length; i++) {
      if (!matchLocation(locations[i], expectedLocations[i])) {
        return false;
      }
    }
    return true;
  }

  private boolean matchLocation(Location location1, Location location2) {
    return (location1.getTime() == location2.getTime())
        && (location1.getLatitude() == location2.getLatitude())
        && (location1.getLongitude() == location2.getLongitude())
        && (location1.getAltitude() == location2.getAltitude());
  }
}
