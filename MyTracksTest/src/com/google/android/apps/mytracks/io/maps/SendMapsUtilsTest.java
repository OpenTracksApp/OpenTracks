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

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.gdata.maps.MapsFeature;
import com.google.android.maps.GeoPoint;

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
   * Tests {@link SendMapsUtils#getMapUrl(Track)} with null track.
   */
  public void testGetMapUrl_null_track() {
    assertEquals(null, SendMapsUtils.getMapUrl(null));
  }

  /**
   * Tests {@link SendMapsUtils#getMapUrl(Track)} with null map id.
   */
  public void testGetMapUrl_null_map_id() {
    Track track = new Track();
    track.setMapId(null);
    assertEquals(null, SendMapsUtils.getMapUrl(track));
  }

  /**
   * Tests {@link SendMapsUtils#getMapUrl(Track)} with a valid track.
   */
  public void testGetMapUrl_valid_track() {
    Track track = new Track();
    track.setMapId("123");
    assertEquals("https://maps.google.com/maps/ms?msa=0&msid=123", SendMapsUtils.getMapUrl(track));
  }

  /**
   * Test {@link SendMapsUtils#buildMapsMarkerFeature(String, String, String,
   * GeoPoint)} with a title.
   */
  public void testBuildMapsMarkerFeature_with_title() {
    MapsFeature mapFeature = SendMapsUtils.buildMapsMarkerFeature(
        "name", "this\nmap\ndescription", "url", new GeoPoint(123, 456));
    assertEquals(MapsFeature.MARKER, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("name", mapFeature.getTitle());
    assertEquals("this<br>map<br>description", mapFeature.getDescription());
    assertEquals("url", mapFeature.getIconUrl());
    assertEquals(123, mapFeature.getPoint(0).getLatitudeE6());
    assertEquals(456, mapFeature.getPoint(0).getLongitudeE6());
  }

  /**
   * Test {@link SendMapsUtils#buildMapsMarkerFeature(String, String, String,
   * GeoPoint)} with an empty title.
   */
  public void testBuildMapsMarkerFeature_empty_title() {
    MapsFeature mapFeature = SendMapsUtils.buildMapsMarkerFeature(
        "", "description", "url", new GeoPoint(123, 456));
    assertEquals(MapsFeature.MARKER, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("-", mapFeature.getTitle());
    assertEquals("description", mapFeature.getDescription());
    assertEquals("url", mapFeature.getIconUrl());
    assertEquals(123, mapFeature.getPoint(0).getLatitudeE6());
    assertEquals(456, mapFeature.getPoint(0).getLongitudeE6());
  }

  /**
   * Test {@link SendMapsUtils#buildMapsMarkerFeature(String, String, String,
   * GeoPoint)} with a null title.
   */
  public void testBuildMapsMarkerFeature_null_title() {
    MapsFeature mapFeature = SendMapsUtils.buildMapsMarkerFeature(
        null, "description", "url", new GeoPoint(123, 456));
    assertEquals(MapsFeature.MARKER, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("-", mapFeature.getTitle());
    assertEquals("description", mapFeature.getDescription());
    assertEquals("url", mapFeature.getIconUrl());
    assertEquals(123, mapFeature.getPoint(0).getLatitudeE6());
    assertEquals(456, mapFeature.getPoint(0).getLongitudeE6());
  }

  /**
   * Test {@link SendMapsUtils#buildMapsLineFeature(String, ArrayList)} with a
   * title.
   */
  public void testBuildMapsLineFeature_with_title() {
    ArrayList<Location> locations = new ArrayList<Location>();
    Location location = new Location("test");
    location.setLatitude(50);
    location.setLongitude(100);
    locations.add(location);
    MapsFeature mapFeature = SendMapsUtils.buildMapsLineFeature("name", locations);

    assertEquals(MapsFeature.LINE, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("name", mapFeature.getTitle());
    assertEquals(0x80FF0000, mapFeature.getColor());
    assertEquals(50000000, mapFeature.getPoint(0).getLatitudeE6());
    assertEquals(100000000, mapFeature.getPoint(0).getLongitudeE6());
  }

  /**
   * Test {@link SendMapsUtils#buildMapsLineFeature(String, ArrayList)} with an
   * empty title.
   */
  public void testBuildMapsLineFeature_empty_title() {
    ArrayList<Location> locations = new ArrayList<Location>();
    Location location = new Location("test");
    location.setLatitude(50);
    location.setLongitude(100);
    locations.add(location);
    MapsFeature mapFeature = SendMapsUtils.buildMapsLineFeature("", locations);

    assertEquals(MapsFeature.LINE, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("-", mapFeature.getTitle());
    assertEquals(0x80FF0000, mapFeature.getColor());
    assertEquals(50000000, mapFeature.getPoint(0).getLatitudeE6());
    assertEquals(100000000, mapFeature.getPoint(0).getLongitudeE6());
  }

  /**
   * Test {@link SendMapsUtils#buildMapsLineFeature(String, ArrayList)} with a
   * null title.
   */
  public void testBuildMapsLineFeature_null_title() {
    ArrayList<Location> locations = new ArrayList<Location>();
    Location location = new Location("test");
    location.setLatitude(50);
    location.setLongitude(100);
    locations.add(location);
    MapsFeature mapFeature = SendMapsUtils.buildMapsLineFeature(null, locations);

    assertEquals(MapsFeature.LINE, mapFeature.getType());
    assertNotNull(mapFeature.getAndroidId());
    assertEquals("-", mapFeature.getTitle());
    assertEquals(0x80FF0000, mapFeature.getColor());
    assertEquals(50000000, mapFeature.getPoint(0).getLatitudeE6());
    assertEquals(100000000, mapFeature.getPoint(0).getLongitudeE6());
  }

  /**
   * Test {@link SendMapsUtils#getGeoPoint(Location)}.
   */
  public void testGeoPoint() {
    Location location = new Location("test");
    location.setLatitude(50);
    location.setLongitude(100);
    GeoPoint geoPoint = SendMapsUtils.getGeoPoint(location);
    assertEquals(50000000, geoPoint.getLatitudeE6());
    assertEquals(100000000, geoPoint.getLongitudeE6());
  }
}