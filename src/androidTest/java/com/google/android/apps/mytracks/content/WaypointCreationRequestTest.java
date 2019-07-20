/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks.content;

import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.apps.mytracks.content.Waypoint.WaypointType;

import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * Tests for the WaypointCreationRequest class.
 * {@link WaypointCreationRequest}
 *
 * @author Sandor Dornbush
 */
@RunWith(AndroidJUnit4.class)
public class WaypointCreationRequestTest {

  public void testTypeParceling() {
    WaypointCreationRequest original = WaypointCreationRequest.DEFAULT_WAYPOINT;
    Parcel p = Parcel.obtain();
    original.writeToParcel(p, 0);
    p.setDataPosition(0);
    WaypointCreationRequest copy = WaypointCreationRequest.CREATOR.createFromParcel(p);
    Assert.assertEquals(original.getType(), copy.getType());
    Assert.assertFalse(copy.isTrackStatistics());
    Assert.assertNull(copy.getName());
    Assert.assertNull(copy.getDescription());
    Assert.assertNull(copy.getIconUrl());
  }

  public void testAllAttributesParceling() {
    WaypointCreationRequest original = new WaypointCreationRequest(
        WaypointType.WAYPOINT, false, "name", "category", "description", "img.png", null);
    Parcel p = Parcel.obtain();
    original.writeToParcel(p, 0);
    p.setDataPosition(0);
    WaypointCreationRequest copy = WaypointCreationRequest.CREATOR.createFromParcel(p);
    Assert.assertEquals(original.getType(), copy.getType());
    Assert.assertFalse(copy.isTrackStatistics());
    Assert.assertEquals("name", copy.getName());
    Assert.assertEquals("category", copy.getCategory());
    Assert.assertEquals("description", copy.getDescription());
    Assert.assertEquals("img.png", copy.getIconUrl());
  }
}
