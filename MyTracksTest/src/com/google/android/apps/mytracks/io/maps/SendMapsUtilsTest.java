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
package com.google.android.apps.mytracks.io.maps;

import com.google.android.apps.mytracks.io.gdata.maps.MapsFeature;

import android.location.Location;

import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Tests {@link SendMapsUtils}.
 *
 * @author Jimmy Shih
 */
public class SendMapsUtilsTest extends TestCase {

  /**
   * Test {@link SendMapsUtils#buildMapsMarkerFeature(String, String, String,
   * Location)} with a title.
   */
  public void testBuildMapsMarkerFeature_with_title() {
    Location location = new Location("test");
    location.setLatitude(123.0);
    location.setLongitude(456.0);
    MapsFeature mapFeature = SendMapsUtils.buildMapsMarkerFeature(
        "name", "this\nmap\ndescription", "url", location);
    assertEquals(MapsFeature.MARKER, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("name", mapFeature.getTitle());
    assertEquals("this<br>map<br>description", mapFeature.getDescription());
    assertEquals("url", mapFeature.getIconUrl());
    assertEquals(123.0, mapFeature.getPoint(0).getLatitude());
    assertEquals(456.0, mapFeature.getPoint(0).getLongitude());
  }

  /**
   * Test {@link SendMapsUtils#buildMapsMarkerFeature(String, String, String,
   * Location)} with an empty title.
   */
  public void testBuildMapsMarkerFeature_empty_title() {
    Location location = new Location("test");
    location.setLatitude(123.0);
    location.setLongitude(456.0);
    MapsFeature mapFeature = SendMapsUtils.buildMapsMarkerFeature(
        "", "description", "url", location);
    assertEquals(MapsFeature.MARKER, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("-", mapFeature.getTitle());
    assertEquals("description", mapFeature.getDescription());
    assertEquals("url", mapFeature.getIconUrl());
    assertEquals(123.0, mapFeature.getPoint(0).getLatitude());
    assertEquals(456.0, mapFeature.getPoint(0).getLongitude());
  }

  /**
   * Test {@link SendMapsUtils#buildMapsMarkerFeature(String, String, String,
   * Location)} with a null title.
   */
  public void testBuildMapsMarkerFeature_null_title() {
    Location location = new Location("test");
    location.setLatitude(123.0);
    location.setLongitude(456.0);
    MapsFeature mapFeature = SendMapsUtils.buildMapsMarkerFeature(
        null, "description", "url", location);
    assertEquals(MapsFeature.MARKER, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("-", mapFeature.getTitle());
    assertEquals("description", mapFeature.getDescription());
    assertEquals("url", mapFeature.getIconUrl());
    assertEquals(123.0, mapFeature.getPoint(0).getLatitude());
    assertEquals(456.0, mapFeature.getPoint(0).getLongitude());
  }

  /**
   * Test {@link SendMapsUtils#buildMapsLineFeature(String, ArrayList)} with a
   * title.
   */
  public void testBuildMapsLineFeature_with_title() {
    ArrayList<Location> locations = new ArrayList<Location>();
    Location location = new Location("test");
    location.setLatitude(50.0);
    location.setLongitude(100.0);
    locations.add(location);
    MapsFeature mapFeature = SendMapsUtils.buildMapsLineFeature("name", locations);

    assertEquals(MapsFeature.LINE, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("name", mapFeature.getTitle());
    assertEquals(0x80FF0000, mapFeature.getColor());
    assertEquals(50.0, mapFeature.getPoint(0).getLatitude());
    assertEquals(100.0, mapFeature.getPoint(0).getLongitude());
  }

  /**
   * Test {@link SendMapsUtils#buildMapsLineFeature(String, ArrayList)} with an
   * empty title.
   */
  public void testBuildMapsLineFeature_empty_title() {
    ArrayList<Location> locations = new ArrayList<Location>();
    Location location = new Location("test");
    location.setLatitude(50.0);
    location.setLongitude(100.0);
    locations.add(location);
    MapsFeature mapFeature = SendMapsUtils.buildMapsLineFeature("", locations);

    assertEquals(MapsFeature.LINE, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("-", mapFeature.getTitle());
    assertEquals(0x80FF0000, mapFeature.getColor());
    assertEquals(50.0, mapFeature.getPoint(0).getLatitude());
    assertEquals(100.0, mapFeature.getPoint(0).getLongitude());
  }

  /**
   * Test {@link SendMapsUtils#buildMapsLineFeature(String, ArrayList)} with a
   * null title.
   */
  public void testBuildMapsLineFeature_null_title() {
    ArrayList<Location> locations = new ArrayList<Location>();
    Location location = new Location("test");
    location.setLatitude(50.0);
    location.setLongitude(100.0);
    locations.add(location);
    MapsFeature mapFeature = SendMapsUtils.buildMapsLineFeature(null, locations);

    assertEquals(MapsFeature.LINE, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("-", mapFeature.getTitle());
    assertEquals(0x80FF0000, mapFeature.getColor());
    assertEquals(50.0, mapFeature.getPoint(0).getLatitude());
    assertEquals(100.0, mapFeature.getPoint(0).getLongitude());
  }
}