// Copyright 2011 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.mymaps;

import com.google.android.apps.mytracks.io.AuthManager;
import com.google.wireless.gdata.client.GDataClient;

import android.app.Activity;

/**
 * Entry point for the maps service library.
 *
 * @author Rodrigo Damazio
 */
public class MapsService {
  private MapsService() {}

  /**
   * Creates a new façade to access Google Maps.
   *
   * @param context current context
   * @param gDataClient GData client to use for access
   * @param stringProvider interface to provide i18n'ed resources
   * @param authToken the authentication token to access the service with
   * @return the service façade
   */
  public static MapsFacade newClient(
      Activity context,
      GDataClient gDataClient,
      AuthManager auth) {
    return new MapsFacadeImpl(context, gDataClient, auth);
  }

  /** Returns the service name to authenticate to for providing the authentication token above. */
  public static String getServiceName() {
    return MyMapsConstants.SERVICE_NAME;
  }

  /** Builds and returns the user-visible URL for a map given its map ID. */
  public static String buildMapUrl(String mapId) {
    return MyMapsConstants.MAPSHOP_BASE_URL + "?msa=0&msid=" + mapId;
  }
}
