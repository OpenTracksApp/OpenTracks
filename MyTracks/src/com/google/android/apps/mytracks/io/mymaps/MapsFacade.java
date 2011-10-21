// Copyright 2011 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.mymaps;

import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.mytracks.R;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.parser.GDataParser;
import com.google.wireless.gdata.parser.ParseException;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.Collection;

import org.xmlpull.v1.XmlPullParserException;

/**
 * Single interface which abstracts all access to the Google Maps service.
 *
 * @author Rodrigo Damazio
 */
public class MapsFacade {

  /**
   * Interface for receiving data back from getMapsList.
   * All calls to the interface will happen before getMapsList returns.
   */
  public interface MapsListCallback {
    void onReceivedMapListing(String mapId, String title, String description,
        boolean isPublic);
  }

  private static final String END_ICON_URL =
    "http://maps.google.com/mapfiles/ms/micons/red-dot.png";
  private static final String START_ICON_URL =
    "http://maps.google.com/mapfiles/ms/micons/green-dot.png";
  
  private final Context context;
  private final MyMapsGDataWrapper wrapper;
  private final MyMapsGDataConverter gdataConverter;
  private final String authToken;

  public MapsFacade(Context context, AuthManager auth) {
    this.context = context;
    this.authToken = auth.getAuthToken();

    wrapper = new MyMapsGDataWrapper(context, auth);
    wrapper.setRetryOnAuthFailure(true);

    try {
      gdataConverter = new MyMapsGDataConverter();
    } catch (XmlPullParserException e) {
      throw new IllegalStateException("Unable to create maps data converter", e);
    }
  }

  public static String buildMapUrl(String mapId) {
    return MapsClient.buildMapUrl(mapId);
  }

  /**
   * Returns a list of all maps for the current user.
   *
   * @param callback callback to call for each map returned
   * @return true on success, false otherwise
   */
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
   * Creates a new map for the current user.
   *
   * @param title title of the map
   * @param category category of the map
   * @param description description for the map
   * @param isPublic whether the map should be public
   * @param mapIdBuilder builder to append the map ID to
   * @return true on success, false otherwise
   */
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
            + category + " - " + context.getString(R.string.new_map_description));
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

  /**
   * Uploads a single start or end marker to the given map.
   *
   * @param mapId ID of the map to upload to
   * @param trackName name of the track being started/ended
   * @param trackDescription description of the track being started/ended
   * @param location the location of the marker
   * @param isStart true to add a start marker, false to add an end marker
   * @return true on success, false otherwise
   */
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
        + (isStart ? context.getString(R.string.start)
                   : context.getString(R.string.end));
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
   * Uploads a series of waypoints to the given map.
   *
   * @param mapId ID of the map to upload to
   * @param waypoints the waypoints to upload
   * @return true on success, false otherwise
   */
  public boolean uploadWaypoints(
      final String mapId, final Iterable<Waypoint> waypoints) {
    return wrapper.runQuery(new MyMapsGDataWrapper.QueryFunction() {
      public void query(MapsClient client) {
        // TODO(rdamazio): Stream through the waypoints in chunks.
        // I am leaving the number of waypoints very high which should not be a
        // problem because we don't try to load them into objects all at the
        // same time.
        String featureFeed = MapsClient.getFeaturesFeed(mapId);

        try {
          for (Waypoint waypoint : waypoints) {
            MyMapsFeature feature = buildMyMapsPlacemarkFeature(
                waypoint.getName(), waypoint.getDescription(), waypoint.getIcon(),
                getGeoPoint(waypoint.getLocation()));
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
   * Uploads a series of points to the given map.
   *
   * @param mapId ID of the map to upload to
   * @param trackName the name of the track
   * @param locations the locations to upload
   * @return true on success, false otherwise
   */
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

  /**
   * Cleans up after a series of uploads.
   * This closes the connection to Maps and resets retry counters.
   */
  public void cleanUp() {
    wrapper.cleanUp();
  }

  private static GeoPoint getGeoPoint(Location location) {
    return new GeoPoint((int) (location.getLatitude() * 1E6),
                        (int) (location.getLongitude() * 1E6));
  }
}
