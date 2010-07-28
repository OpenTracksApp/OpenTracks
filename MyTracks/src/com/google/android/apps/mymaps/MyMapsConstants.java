/*
 * Copyright 2009 Google Inc.
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
package com.google.android.apps.mymaps;

import com.google.wireless.gdata.maps.MapsClient;

/**
 * Constants for My Maps.
 */
public class MyMapsConstants {
  public static final String MAPSHOP_BASE_URL =
      "http://maps.google.com/maps/ms";
  public static final String MAPSHOP_SERVICE = MapsClient.SERVICE;
  /**
   * private constructor to prevent instanciation
   */
  private MyMapsConstants() { }
}
