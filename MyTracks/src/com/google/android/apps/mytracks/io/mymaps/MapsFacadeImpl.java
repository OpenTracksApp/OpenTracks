// Copyright 2011 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.mymaps;

import com.google.android.maps.GeoPoint;
import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.parser.GDataParser;
import com.google.wireless.gdata.parser.ParseException;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collection;

/**
 * Implementation of the Maps access abstraction.
 *
 * @author Rodrigo Damazio
 */
class MapsFacadeImpl implements MapsFacade {
  private static final String END_ICON_URL =
    "http://maps.google.com/mapfiles/ms/micons/red-dot.png";
  private static final String START_ICON_URL =
    "http://maps.google.com/mapfiles/ms/micons/green-dot.png";
  
  private final MyMapsGDataWrapper wrapper;
  private final MyMapsGDataConverter gdataConverter;
  private final MapsStringsProvider stringProvider;
  private final String authToken;

  public MapsFacadeImpl(Context context, GDataClient gdataClient,
      MapsStringsProvider stringProvider, String authToken) {
    this.stringProvider = stringProvider;
    this.authToken = authToken;

    wrapper = new MyMapsGDataWrapper(context, gdataClient);
    wrapper.setRetryOnAuthFailure(true);

    try {
      gdataConverter = new MyMapsGDataConverter();
    } catch (XmlPullParserException e) {
      throw new IllegalStateException("Unable to create maps data converter", e);
    }
  }

  @Override
  public void setAuthenticationRefresher(AuthenticationRefresher refresher) {
    wrapper.setAuthenticationRefresher(refresher);
  }

  @Override
  public boolean getMapsList(final MapsListCallback callback) {
    return wrapper.runQuery(new MyMapsGDataWrapper.QueryFunction() {
      @Override
      public void query(MapsClient client) throws IOException, Exception {
        GDataParser listParser = client.getParserForFeed(
            MapFeatureEntry.class, MapsClient.getMapsFeed(), authToken);
        listParser.init();
        while (listParser.hasMoreData()) {
          MapFeatureEntry entry =
              (MapFeatureEntry) listParser.readNextEntry(null);
          MyMapsMapMetadata metadata =
              MyMapsGDataConverter.getMapMetadataForEntry(entry);
          String mapId = MyMapsGDataConverter.getMapidForEntry(entry);

          callback.onReceivedMapListing(
              mapId, metadata.getTitle(), metadata.getDescription(), metadata.getSearchable());
        }
        listParser.close();
        listParser = null;
      }
    });
  }

  /**
   * Creates a new map for the given track.
   *
   * @param track The track that will be uploaded to this map
   * @return True on success
   */
  @Override
  public boolean createNewMap(
      final String title, final String category, final String description,
      final boolean isPublic, final StringBuilder mapIdBuilder) {
    if (mapIdBuilder.length() > 0) {
      throw new IllegalArgumentException("mapIdBuilder should be empty");
    }

    return wrapper.runQuery(new MyMapsGDataWrapper.QueryFunction() {
      @Override
      public void query(MapsClient client) throws IOException, Exception {
        Log.d(MyMapsConstants.TAG, "Creating a new map.");
        String mapFeed = MapsClient.getMapsFeed();
        Log.d(MyMapsConstants.TAG, "Map feed is " + mapFeed);
        MyMapsMapMetadata metaData = new MyMapsMapMetadata();
        metaData.setTitle(title);
        metaData.setDescription(description + " - "
            + category + " - " + stringProvider.getNewMapDescription());
        metaData.setSearchable(isPublic);
        Entry entry = MyMapsGDataConverter.getMapEntryForMetadata(metaData);
        Log.d(MyMapsConstants.TAG, "Title: " + entry.getTitle());
        Entry map = client.createEntry(mapFeed, authToken, entry);

        String mapId = MapsClient.getMapIdFromMapEntryId(map.getId());
        mapIdBuilder.append(mapId);
        Log.d(MyMapsConstants.TAG, "New map id is: " + mapId);
      }
    });
  }

  @Override
  public boolean uploadMarker(final String mapId, final String trackName,
      final String trackDescription, final Location loc, final boolean isStart) {
    return wrapper.runQuery(new MyMapsGDataWrapper.QueryFunction() {
      @Override
      public void query(MapsClient client)
          throws IOException, Exception {
        String featureFeed = MapsClient.getFeaturesFeed(mapId);
        GeoPoint geoPoint = getGeoPoint(loc);
        insertMarker(client, featureFeed, trackName, trackDescription, geoPoint, isStart);
      }
    });
  }

  /**
   * Inserts a place mark. Second try if 1st try fails. Will throw exception on
   * 2nd failure.
   */
  private void insertMarker(MapsClient client,
                            String featureFeed,
                            String trackName, String trackDescription,
                            GeoPoint geoPoint,
                            boolean isStart) throws IOException, Exception {
    MyMapsFeature feature =
        buildMyMapsPlacemarkFeature(trackName, trackDescription, geoPoint, isStart);
    Entry entry = gdataConverter.getEntryForFeature(feature);
    Log.d(MyMapsConstants.TAG, "SendToMyMaps: Creating placemark "
        + entry.getTitle());
    try {
      client.createEntry(featureFeed, authToken, entry);
      Log.d(MyMapsConstants.TAG, "SendToMyMaps: createEntry success!");
    } catch (IOException e) {
      Log.w(MyMapsConstants.TAG,
          "SendToMyMaps: createEntry 1st try failed. Trying again.");
      // Retry once (often IOException is thrown on a timeout):
      client.createEntry(featureFeed, authToken, entry);
      Log.d(MyMapsConstants.TAG,
          "SendToMyMaps: createEntry success on 2nd try!");
    }
  }

  /**
   * Builds a placemark MyMapsFeature from a track.
   *
   * @param track the track
   * @param isStart true if it's the start of the track, or false for end
   * @return a MyMapsFeature
   */
  private MyMapsFeature buildMyMapsPlacemarkFeature(
      String trackName, String trackDescription,
      GeoPoint geoPoint, boolean isStart) {
    String iconUrl;
    if (isStart) {
      iconUrl = START_ICON_URL;
    } else {
      iconUrl = END_ICON_URL;
    }
    String title = trackName + " "
        + (isStart ? stringProvider.getStart()
                   : stringProvider.getEnd());
    String description = isStart ? "" : trackDescription;
    return buildMyMapsPlacemarkFeature(title, description, iconUrl, geoPoint);
  }

  /**
   * Builds a MyMapsFeature from a track.
   *
   * @param wpt the waypoint
   * @return a MyMapsFeature
   */
  private static MyMapsFeature buildMyMapsPlacemarkFeature(
      String title, String description, String iconUrl, GeoPoint geoPoint) {
    MyMapsFeature myMapsFeature = new MyMapsFeature();
    myMapsFeature.generateAndroidId();
    myMapsFeature.setType(MyMapsFeature.MARKER);
    myMapsFeature.setIconUrl(iconUrl);
    myMapsFeature.setDescription(description);
    myMapsFeature.addPoint(geoPoint);
    if (TextUtils.isEmpty(title)) {
      // Features must have a name (otherwise GData upload may fail):
      myMapsFeature.setTitle("-");
    } else {
      myMapsFeature.setTitle(title);
    }
    myMapsFeature.setDescription(description.replaceAll("\n", "<br>"));
    return myMapsFeature;
  }

  /**
   * Uploads all of the waypoints associated with this track to map.
   *
   * @param track The track to upload waypoints for
   * @return True on success
   */
  @Override
  public boolean uploadWaypoints(
      final String mapId, final Iterable<WaypointData> waypoints) {
    return wrapper.runQuery(new MyMapsGDataWrapper.QueryFunction() {
      public void query(MapsClient client) {
        // TODO(rdamazio): Stream through the waypoints in chunks.
        // I am leaving the number of waypoints very high which should not be a
        // problem because we don't try to load them into objects all at the
        // same time.
        String featureFeed = MapsClient.getFeaturesFeed(mapId);

        try {
          for (WaypointData waypoint : waypoints) {
            MyMapsFeature feature = buildMyMapsPlacemarkFeature(
                waypoint.title, waypoint.description, waypoint.iconUrl,
                getGeoPoint(waypoint.location));
            Entry entry = gdataConverter.getEntryForFeature(feature);

            Log.d(MyMapsConstants.TAG,
                "SendToMyMaps: Creating waypoint.");
            try {
              client.createEntry(featureFeed, authToken, entry);
              Log.d(MyMapsConstants.TAG,
                  "SendToMyMaps: createEntry success!");
            } catch (IOException e) {
              Log.w(MyMapsConstants.TAG,
                  "SendToMyMaps: createEntry 1st try failed. Retrying.");
    
              // Retry once (often IOException is thrown on a timeout):
              client.createEntry(featureFeed, authToken, entry);
              Log.d(MyMapsConstants.TAG,
                  "SendToMyMaps: createEntry success on 2nd try!");
            }
          }
        } catch (ParseException e) {
          Log.w(MyMapsConstants.TAG, "ParseException caught.", e);
        } catch (HttpException e) {
          Log.w(MyMapsConstants.TAG, "HttpException caught.", e);
        } catch (IOException e) {
          Log.w(MyMapsConstants.TAG, "IOException caught.", e);
        }
      }
    });
  }

  /**
   * Uploads a given list of tracks to Google MyMaps using the maps GData feed.
   */
  @Override
  public boolean uploadTrackPoints(
      final String mapId, final String trackName, final Collection<Location> locations) {
    return wrapper.runQuery(new MyMapsGDataWrapper.QueryFunction() {
      @Override
      public void query(MapsClient client)
          throws IOException, Exception {
        String featureFeed = MapsClient.getFeaturesFeed(mapId);
        Log.d(MyMapsConstants.TAG, "Feature feed url: " + featureFeed);
        uploadTrackPoints(client, featureFeed, trackName, locations);
      }
    });
  }

  private boolean uploadTrackPoints(
      MapsClient client,
      String featureFeed,
      String trackName, Collection<Location> locations)
      throws IOException, Exception  {
    Entry entry = null;
    int numLocations = locations.size();
    if (numLocations < 2) {
      // Need at least two points for a polyline:
      Log.w(MyMapsConstants.TAG, "Not uploading too few points");
      return true;
    }
  
    // Put the line:
    entry = gdataConverter.getEntryForFeature(
        buildMyMapsLineFeature(trackName, locations));
    Log.d(MyMapsConstants.TAG,
        "SendToMyMaps: Creating line " + entry.getTitle());
    try {
      client.createEntry(featureFeed, authToken, entry);
      Log.d(MyMapsConstants.TAG, "SendToMyMaps: createEntry success!");
    } catch (IOException e) {
      Log.w(MyMapsConstants.TAG,
          "SendToMyMaps: createEntry 1st try failed. Trying again.");
      // Retry once (often IOException is thrown on a timeout):
      client.createEntry(featureFeed, authToken, entry);
      Log.d(MyMapsConstants.TAG,
          "SendToMyMaps: createEntry success on 2nd try!");
    }
    return true;
  }

  /**
   * Builds a MyMapsFeature from a track.
   *
   * @param track the track
   * @return a MyMapsFeature
   */
  private static MyMapsFeature buildMyMapsLineFeature(String trackName,
      Iterable<Location> locations) {
    MyMapsFeature myMapsFeature = new MyMapsFeature();
    myMapsFeature.generateAndroidId();
    myMapsFeature.setType(MyMapsFeature.LINE);
    if (TextUtils.isEmpty(trackName)) {
      // Features must have a name (otherwise GData upload may fail):
      myMapsFeature.setTitle("-");
    } else {
      myMapsFeature.setTitle(trackName);
    }
    myMapsFeature.setColor(0x80FF0000);
    for (Location loc : locations) {
      myMapsFeature.addPoint(getGeoPoint(loc));
    }
    return myMapsFeature;
  }

  @Override
  public void cleanUp() {
    wrapper.cleanUp();
  }

  private static GeoPoint getGeoPoint(Location location) {
    return new GeoPoint((int) (location.getLatitude() * 1E6),
                        (int) (location.getLongitude() * 1E6));
  }
}
