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

package com.google.android.apps.mytracks.util;

import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Utilities for track name.
 *
 * @author Matthew Simmons
 */
public class TrackNameUtils {

  @VisibleForTesting
  static final String ISO_8601_FORMAT = "yyyy-MM-dd HH:mm";

  private TrackNameUtils() {
  }

  /**
   * Gets the track name.
   *
   * @param context the context
   * @param trackId the track id
   * @param startTime the track start time
   * @param location the track location
   */
  public static String getTrackName(
      Context context, long trackId, long startTime, Location location) {
    String trackName = PreferencesUtils.getString(context, R.string.track_name_key,
        PreferencesUtils.TRACK_NAME_DEFAULT);

    if (trackName.equals(
        context.getString(R.string.settings_recording_track_name_date_local_value))) {
      if (startTime == -1L) {
        return null;
      }
      return StringUtils.formatDateTime(context, startTime);
    } else if (trackName.equals(
        context.getString(R.string.settings_recording_track_name_date_iso_8601_value))) {
      if (startTime == -1L) {
        return null;
       }
      SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_FORMAT, Locale.US);
      return dateFormat.format(startTime);
    } else if (trackName.equals(
        context.getString(R.string.settings_recording_track_name_number_value))) {
      if (trackId == -1L) {
        return null;
      }
      return context.getString(R.string.track_name_format, trackId);
    } else {
      // trackName equals settings_recording_track_name_location_value or any
      // value
      if (location != null) {
        return getReverseGeoCoding(context, location);
      } else {
        // Use the startTime if available
        if (startTime != -1L) {
          return StringUtils.formatDateTime(context, startTime);
        }
        return null;
      }
    }
  }

  /**
   * Gets the reverse geo coding string for a location.
   * 
   * @param context the context
   * @param location the location
   */
  private static String getReverseGeoCoding(Context context, Location location) {
    if (location == null || !ApiAdapterFactory.getApiAdapter().isGeoCoderPresent()) {
      return null;
    }
    Geocoder geocoder = new Geocoder(context);
    try {
      List<Address> addresses = geocoder.getFromLocation(
          location.getLatitude(), location.getLongitude(), 1);
      if (addresses.size() > 0) {
        Address address = addresses.get(0);
        int lines = address.getMaxAddressLineIndex();
        if (lines > 0) {
          return address.getAddressLine(0);
        }
        String featureName = address.getFeatureName();
        if (featureName != null) {
          return featureName;
        }
        String thoroughfare = address.getThoroughfare();
        if (thoroughfare != null) {
          return thoroughfare;
        }
        String locality = address.getLocality();
        if (locality != null) {
          return locality;
        }
      }
    } catch (IOException e) {
      // Can safely ignore
    }
    return null;
  }
}
