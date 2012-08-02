// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.gdata.maps;

import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.GDataParserFactory;
import com.google.wireless.gdata.client.GDataServiceClient;

import android.util.Log;

import java.util.Locale;

/**
 * Client to talk to Google Maps via GData.
 */
public class MapsClient extends GDataServiceClient {
  private static final boolean DEBUG = false;
  public static final boolean LOG_COMMUNICATION = false;

  private static final String MAPS_BASE_FEED_URL =
      "http://maps.google.com/maps/feeds/";
  private static final String MAPS_MAP_FEED_PATH = "maps/default/full";
  private static final String MAPS_FEATURE_FEED_PATH_BEFORE_MAPID = "features/";
  private static final String MAPS_FEATURE_FEED_PATH_AFTER_MAPID = "/full";
  private static final String MAPS_VERSION_FEED_PATH_FORMAT =
      "%smaps/%s/versions/%s/full/%s";

  private static final String MAP_ENTRY_ID_BEFORE_USER_ID = "maps/feeds/maps/";
  private static final String MAP_ENTRY_ID_BETWEEN_USER_ID_AND_MAP_ID = "/";
  private static final String V2_ONLY_PARAM = "?v=2.0";

  public MapsClient(GDataClient dataClient,
      GDataParserFactory dataParserFactory) {
    super(dataClient, dataParserFactory);
  }

  @Override
  public String getServiceName() {
    return MapsConstants.SERVICE_NAME;
  }

  public static String buildMapUrl(String mapId) {
    return MapsConstants.MAPSHOP_BASE_URL + "?msa=0&msid=" + mapId;
  }

  public static String getMapsFeed() {
    if (DEBUG) {
      Log.d("Maps Client", "Requesting map feed:");
    }
    return MAPS_BASE_FEED_URL + MAPS_MAP_FEED_PATH + V2_ONLY_PARAM;
  }

  public static String getFeaturesFeed(String mapid) {
    StringBuilder feed = new StringBuilder();
    feed.append(MAPS_BASE_FEED_URL);
    feed.append(MAPS_FEATURE_FEED_PATH_BEFORE_MAPID);
    feed.append(mapid);
    feed.append(MAPS_FEATURE_FEED_PATH_AFTER_MAPID);
    feed.append(V2_ONLY_PARAM);
    return feed.toString();
  }

  public static String getMapIdFromMapEntryId(String entryId) {
    String userId = null;
    String mapId = null;
    if (DEBUG) {
      Log.d("Maps GData Client", "Getting mapid from entry id: " + entryId);
    }
    int userIdStart =
        entryId.indexOf(MAP_ENTRY_ID_BEFORE_USER_ID)
            + MAP_ENTRY_ID_BEFORE_USER_ID.length();
    int userIdEnd =
        entryId.indexOf(MAP_ENTRY_ID_BETWEEN_USER_ID_AND_MAP_ID, userIdStart);
    if (userIdStart >= 0 && userIdEnd < entryId.length()
        && userIdStart <= userIdEnd) {
      userId = entryId.substring(userIdStart, userIdEnd);
    }
    int mapIdStart =
        entryId.indexOf(MAP_ENTRY_ID_BETWEEN_USER_ID_AND_MAP_ID, userIdEnd)
            + MAP_ENTRY_ID_BETWEEN_USER_ID_AND_MAP_ID.length();
    if (mapIdStart >= 0 && mapIdStart < entryId.length()) {
      mapId = entryId.substring(mapIdStart);
    }
    if (userId == null) {
      userId = "";
    }
    if (mapId == null) {
      mapId = "";
    }
    if (DEBUG) {
      Log.d("Maps GData Client", "Got user id: " + userId);
      Log.d("Maps GData Client", "Got map id: " + mapId);
    }
    return userId + "." + mapId;
  }

  public static String getVersionFeed(String versionUserId,
      String versionClient, String currentVersion) {
    return String.format(Locale.US, MAPS_VERSION_FEED_PATH_FORMAT, MAPS_BASE_FEED_URL,
        versionUserId, versionClient, currentVersion);
  }

}
