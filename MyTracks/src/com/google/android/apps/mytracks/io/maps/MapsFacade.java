// Copyright 2011 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.maps;

import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.util.StringUtils;
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
  private final MapsGDataWrapper wrapper;
  private final MapsGDataConverter gdataConverter;
  private final String authToken;

  public MapsFacade(Context context, AuthManager auth) {
    this.context = context;
    this.authToken = auth.getAuthToken();

    wrapper = new MapsGDataWrapper(context, auth);
    wrapper.setRetryOnAuthFailure(true);

    try {
      gdataConverter = new MapsGDataConverter();
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
    return wrapper.runQuery(new MapsGDataWrapper.QueryFunction() {
      @Override
      public void query(MapsClient client) throws IOException, Exception {
        GDataParser listParser = client.getParserForFeed(
            MapFeatureEntry.class, MapsClient.getMapsFeed(), authToken);
        listParser.init();
        while (listParser.hasMoreData()) {
          MapFeatureEntry entry =
              (MapFeatureEntry) listParser.readNextEntry(null);
          MapsMapMetadata metadata =
              MapsGDataConverter.getMapMetadataForEntry(entry);
          String mapId = MapsGDataConverter.getMapidForEntry(entry);

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

    return wrapper.runQuery(new MapsGDataWrapper.QueryFunction() {
      @Override
      public void query(MapsClient client) throws IOException, Exception {
        Log.d(MapsConstants.TAG, "Creating a new map.");
        String mapFeed = MapsClient.getMapsFeed();
        Log.d(MapsConstants.TAG, "Map feed is " + mapFeed);
        MapsMapMetadata metaData = new MapsMapMetadata();
        metaData.setTitle(title);
        metaData.setDescription(description + " - "
            + category + " - " + StringUtils.getCreatedByMyTracks(context, false));
        metaData.setSearchable(isPublic);
        Entry entry = MapsGDataConverter.getMapEntryForMetadata(metaData);
        Log.d(MapsConstants.TAG, "Title: " + entry.getTitle());
        Entry map = client.createEntry(mapFeed, authToken, entry);

        String mapId = MapsClient.getMapIdFromMapEntryId(map.getId());
        mapIdBuilder.append(mapId);
        Log.d(MapsConstants.TAG, "New map id is: " + mapId);
      }
    });
  }

  /**
   * Uploads a single start or end marker to the given map.
   *
   * @param mapId ID of the map to upload to
   * @param trackName name of the track being started/ended
   * @param trackDescription description of the track being started/ended
   * @param loc the location of the marker
   * @param isStart true to add a start marker, false to add an end marker
   * @return true on success, false otherwise
   */
  public boolean uploadMarker(final String mapId, final String trackName,
      final String trackDescription, final Location loc, final boolean isStart) {
    return wrapper.runQuery(new MapsGDataWrapper.QueryFunction() {
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
    MapsFeature feature =
        buildMapsPlacemarkFeature(trackName, trackDescription, geoPoint, isStart);
    Entry entry = gdataConverter.getEntryForFeature(feature);
    Log.d(MapsConstants.TAG, "SendToMaps: Creating placemark "
        + entry.getTitle());
    try {
      client.createEntry(featureFeed, authToken, entry);
      Log.d(MapsConstants.TAG, "SendToMaps: createEntry success!");
    } catch (IOException e) {
      Log.w(MapsConstants.TAG,
          "SendToMaps: createEntry 1st try failed. Trying again.");
      // Retry once (often IOException is thrown on a timeout):
      client.createEntry(featureFeed, authToken, entry);
      Log.d(MapsConstants.TAG,
          "SendToMaps: createEntry success on 2nd try!");
    }
  }

  /**
   * Builds a placemark MapsFeature from a track.
   *
   * @param trackName the track
   * @param trackDescription the track description
   * @param geoPoint the geo point
   * @param isStart true if it's the start of the track, or false for end
   * @return a MapsFeature
   */
  private MapsFeature buildMapsPlacemarkFeature(
      String trackName, String trackDescription,
      GeoPoint geoPoint, boolean isStart) {
    String iconUrl;
    if (isStart) {
      iconUrl = START_ICON_URL;
    } else {
      iconUrl = END_ICON_URL;
    }
    String title = isStart ? context.getString(R.string.marker_label_start, trackName) :
      context.getString(R.string.marker_label_end, trackName);
    String description = isStart ? "" : trackDescription;
    return buildMapsPlacemarkFeature(title, description, iconUrl, geoPoint);
  }

  /**
   * Builds a MapsFeature from a waypoint.
   * 
   * @param title the title
   * @param description the description
   * @param iconUrl the icon url
   * @param geoPoint the waypoint
   * @return a MapsFeature
   */
  private static MapsFeature buildMapsPlacemarkFeature(
      String title, String description, String iconUrl, GeoPoint geoPoint) {
    MapsFeature mapsFeature = new MapsFeature();
    mapsFeature.generateAndroidId();
    mapsFeature.setType(MapsFeature.MARKER);
    mapsFeature.setIconUrl(iconUrl);
    mapsFeature.setDescription(description);
    mapsFeature.addPoint(geoPoint);
    if (TextUtils.isEmpty(title)) {
      // Features must have a name (otherwise GData upload may fail):
      mapsFeature.setTitle("-");
    } else {
      mapsFeature.setTitle(title);
    }
    mapsFeature.setDescription(description.replaceAll("\n", "<br>"));
    return mapsFeature;
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
    return wrapper.runQuery(new MapsGDataWrapper.QueryFunction() {
      public void query(MapsClient client) {
        // TODO(rdamazio): Stream through the waypoints in chunks.
        // I am leaving the number of waypoints very high which should not be a
        // problem because we don't try to load them into objects all at the
        // same time.
        String featureFeed = MapsClient.getFeaturesFeed(mapId);

        try {
          for (Waypoint waypoint : waypoints) {
            MapsFeature feature = buildMapsPlacemarkFeature(
                waypoint.getName(), waypoint.getDescription(), waypoint.getIcon(),
                getGeoPoint(waypoint.getLocation()));
            Entry entry = gdataConverter.getEntryForFeature(feature);

            Log.d(MapsConstants.TAG,
                "SendToMaps: Creating waypoint.");
            try {
              client.createEntry(featureFeed, authToken, entry);
              Log.d(MapsConstants.TAG,
                  "SendToMaps: createEntry success!");
            } catch (IOException e) {
              Log.w(MapsConstants.TAG,
                  "SendToMaps: createEntry 1st try failed. Retrying.");
    
              // Retry once (often IOException is thrown on a timeout):
              client.createEntry(featureFeed, authToken, entry);
              Log.d(MapsConstants.TAG,
                  "SendToMaps: createEntry success on 2nd try!");
            }
          }
        } catch (ParseException e) {
          Log.w(MapsConstants.TAG, "ParseException caught.", e);
        } catch (HttpException e) {
          Log.w(MapsConstants.TAG, "HttpException caught.", e);
        } catch (IOException e) {
          Log.w(MapsConstants.TAG, "IOException caught.", e);
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
    return wrapper.runQuery(new MapsGDataWrapper.QueryFunction() {
      @Override
      public void query(MapsClient client)
          throws IOException, Exception {
        String featureFeed = MapsClient.getFeaturesFeed(mapId);
        Log.d(MapsConstants.TAG, "Feature feed url: " + featureFeed);
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
      Log.w(MapsConstants.TAG, "Not uploading too few points");
      return true;
    }
  
    // Put the line:
    entry = gdataConverter.getEntryForFeature(
        buildMapsLineFeature(trackName, locations));
    Log.d(MapsConstants.TAG,
        "SendToMaps: Creating line " + entry.getTitle());
    try {
      client.createEntry(featureFeed, authToken, entry);
      Log.d(MapsConstants.TAG, "SendToMaps: createEntry success!");
    } catch (IOException e) {
      Log.w(MapsConstants.TAG,
          "SendToMaps: createEntry 1st try failed. Trying again.");
      // Retry once (often IOException is thrown on a timeout):
      client.createEntry(featureFeed, authToken, entry);
      Log.d(MapsConstants.TAG,
          "SendToMaps: createEntry success on 2nd try!");
    }
    return true;
  }

  /**
   * Builds a MapsFeature from a track.
   *
   * @param trackName the track name
   * @param locations locations on the track
   * @return a MapsFeature
   */
  private static MapsFeature buildMapsLineFeature(String trackName,
      Iterable<Location> locations) {
    MapsFeature mapsFeature = new MapsFeature();
    mapsFeature.generateAndroidId();
    mapsFeature.setType(MapsFeature.LINE);
    if (TextUtils.isEmpty(trackName)) {
      // Features must have a name (otherwise GData upload may fail):
      mapsFeature.setTitle("-");
    } else {
      mapsFeature.setTitle(trackName);
    }
    mapsFeature.setColor(0x80FF0000);
    for (Location loc : locations) {
      mapsFeature.addPoint(getGeoPoint(loc));
    }
    return mapsFeature;
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
