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

package com.google.android.apps.mytracks.content;

import com.google.android.apps.mytracks.content.SearchEngine.ScoredResult;
import com.google.android.apps.mytracks.content.SearchEngine.SearchQuery;
import com.google.android.apps.mytracks.services.TrackRecordingServiceTest.MockContext;
import com.google.android.apps.mytracks.stats.TripStatistics;

import android.content.ContentUris;
import android.location.Location;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link SearchEngine}.
 * These are not meant to be quality tests, but instead feature-by-feature tests
 * (in other words, they don't test the mixing of different score boostings, just
 * each boosting separately)
 *
 * @author Rodrigo Damazio
 */
public class SearchEngineTest extends AndroidTestCase {

  private static final Location HERE = new Location("gps");
  private static final long NOW = 1234567890000L;  // After OLDEST_ALLOWED_TIMESTAMP
  private MyTracksProviderUtils providerUtils;
  private SearchEngine engine;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    MockContentResolver mockContentResolver = new MockContentResolver();
    RenamingDelegatingContext targetContext = new RenamingDelegatingContext(
        getContext(), getContext(), "test.");
    MockContext context = new MockContext(mockContentResolver, targetContext);
    MyTracksProvider provider = new MyTracksProvider();
    provider.attachInfo(context, null);
    mockContentResolver.addProvider(MyTracksProviderUtils.AUTHORITY, provider);
    setContext(context);

    providerUtils = MyTracksProviderUtils.Factory.get(context);
    engine = new SearchEngine(providerUtils);
  }

  @Override
  protected void tearDown() throws Exception {
    providerUtils.deleteAllTracks();

    super.tearDown();
  }

  private long insertTrack(String title, String description, String category, double distance, long hoursAgo) {
    Track track = new Track();
    track.setName(title);
    track.setDescription(description);
    track.setCategory(category);

    TripStatistics stats = track.getTripStatistics();
    if (hoursAgo > 0) {
      // Started twice hoursAgo, so the average time is hoursAgo.
      stats.setStartTime(NOW - hoursAgo * 1000L * 60L * 60L * 2);
      stats.setStopTime(NOW);
    }

    int latitude = (int) ((HERE.getLatitude() + distance) * 1E6);
    int longitude = (int) ((HERE.getLongitude() + distance) * 1E6);
    stats.setBounds(latitude, longitude, latitude, longitude);

    Uri uri = providerUtils.insertTrack(track);
    return ContentUris.parseId(uri);
  }

  private long insertTrack(String title, String description, String category) {
    return insertTrack(title, description, category, 0, -1);
  }

  private long insertTrack(String title, double distance) {
    return insertTrack(title, "", "", distance, -1);
  }

  private long insertTrack(String title, long hoursAgo) {
    return insertTrack(title, "", "", 0.0, hoursAgo);
  }

  private long insertWaypoint(String title, String description, String category, double distance, long hoursAgo, long trackId) {
    Waypoint waypoint = new Waypoint();
    waypoint.setName(title);
    waypoint.setDescription(description);
    waypoint.setCategory(category);
    waypoint.setTrackId(trackId);

    Location location = new Location(HERE);
    location.setLatitude(location.getLatitude() + distance);
    location.setLongitude(location.getLongitude() + distance);
    if (hoursAgo >= 0) {
      location.setTime(NOW - hoursAgo * 1000L * 60L * 60L);
    }
    waypoint.setLocation(location);

    Uri uri = providerUtils.insertWaypoint(waypoint);
    return ContentUris.parseId(uri);
  }

  private long insertWaypoint(String title, String description, String category) {
    return insertWaypoint(title, description, category, 0.0, -1, -1);
  }

  private long insertWaypoint(String title, double distance) {
    return insertWaypoint(title, "", "", distance, -1, -1);
  }

  private long insertWaypoint(String title, long hoursAgo) {
    return insertWaypoint(title, "", "", 0.0, hoursAgo, -1);
  }

  private long insertWaypoint(String title, long hoursAgo, long trackId) {
    return insertWaypoint(title, "", "", 0.0, hoursAgo, trackId);
  }

  public void testSearchText() {
    // Insert 7 tracks (purposefully out of result order):
    // - one which won't match
    // - one which will match the description
    // - one which will match the category
    // - one which will match the title
    // - one which will match in title and category
    // - one which will match in title and description
    // - one which will match in all fields
    insertTrack("bb", "cc", "dd");
    long descriptionMatchId = insertTrack("bb", "aa", "cc");
    long categoryMatchId = insertTrack("bb", "cc", "aa");
    long titleMatchId = insertTrack("aa", "bb", "cc");
    long titleCategoryMatchId = insertTrack("aa", "bb", "ca");
    long titleDescriptionMatchId = insertTrack("aa", "ba", "cc");
    long allMatchId = insertTrack("aa", "ba", "ca");

    SearchQuery query = new SearchQuery("a", null, -1, NOW);
    ArrayList<ScoredResult> results = new ArrayList<ScoredResult>(engine.search(query));

    // Title > Description > Category.
    assertTrackResults(results,
        allMatchId, titleDescriptionMatchId, titleCategoryMatchId, titleMatchId, descriptionMatchId,
        categoryMatchId);
  }

  public void testSearchWaypointText() {
    // Insert 7 waypoints (purposefully out of result order):
    // - one which won't match
    // - one which will match the description
    // - one which will match the category
    // - one which will match the title
    // - one which will match in title and category
    // - one which will match in title and description
    // - one which will match in all fields
    insertWaypoint("bb", "cc", "dd");
    long descriptionMatchId = insertWaypoint("bb", "aa", "cc");
    long categoryMatchId = insertWaypoint("bb", "cc", "aa");
    long titleMatchId = insertWaypoint("aa", "bb", "cc");
    long titleCategoryMatchId = insertWaypoint("aa", "bb", "ca");
    long titleDescriptionMatchId = insertWaypoint("aa", "ba", "cc");
    long allMatchId = insertWaypoint("aa", "ba", "ca");

    SearchQuery query = new SearchQuery("a", null, -1, NOW);
    ArrayList<ScoredResult> results = new ArrayList<ScoredResult>(engine.search(query));

    // Title > Description > Category.
    assertWaypointResults(results,
        allMatchId, titleDescriptionMatchId, titleCategoryMatchId, titleMatchId, descriptionMatchId,
        categoryMatchId);
  }

  public void testSearchMixedText() {
    // Insert 5 entries (purposefully out of result order):
    // - one waypoint which will match by description
    // - one waypoint which won't match
    // - one waypoint which will match by title
    // - one track which won't match
    // - one track which will match by title
    long descriptionWaypointId = insertWaypoint("bb", "aa", "cc");
    insertWaypoint("bb", "cc", "dd");
    long titleWaypointId = insertWaypoint("aa", "bb", "cc");
    insertTrack("bb", "cc", "dd");
    long trackId = insertTrack("aa", "bb", "cc");

    SearchQuery query = new SearchQuery("a", null, -1, NOW);
    ArrayList<ScoredResult> results = new ArrayList<ScoredResult>(engine.search(query));

    // Title > Description > Category.
    assertEquals(results.toString(), 3, results.size());
    assertTrackResult(trackId, results.get(0));
    assertWaypointResult(titleWaypointId, results.get(1));
    assertWaypointResult(descriptionWaypointId, results.get(2));
  }

  public void testSearchTrackDistance() {
    // All results match text, but they're at difference distances from the user.
    long farFarAwayId = insertTrack("aa", 0.3);
    long nearId = insertTrack("ab", 0.1);
    long farId = insertTrack("ac", 0.2);

    SearchQuery query = new SearchQuery("a", HERE, -1, NOW);
    ArrayList<ScoredResult> results = new ArrayList<ScoredResult>(engine.search(query));

    // Distance order.
    assertTrackResults(results, nearId, farId, farFarAwayId);
  }

  public void testSearchWaypointDistance() {
    // All results match text, but they're at difference distances from the user.
    long farFarAwayId = insertWaypoint("aa", 0.3);
    long nearId = insertWaypoint("ab", 0.1);
    long farId = insertWaypoint("ac", 0.2);

    SearchQuery query = new SearchQuery("a", HERE, -1, NOW);
    ArrayList<ScoredResult> results = new ArrayList<ScoredResult>(engine.search(query));

    // Distance order.
    assertWaypointResults(results, nearId, farId, farFarAwayId);
  }

  public void testSearchTrackRecent() {
    // All results match text, but they're were recorded at different times.
    long oldestId = insertTrack("aa", 3);
    long recentId = insertTrack("ab", 1);
    long oldId = insertTrack("ac", 2);

    SearchQuery query = new SearchQuery("a", null, -1, NOW);
    ArrayList<ScoredResult> results = new ArrayList<ScoredResult>(engine.search(query));

    // Reverse time order.
    assertTrackResults(results, recentId, oldId, oldestId);
  }

  public void testSearchWaypointRecent() {
    // All results match text, but they're were recorded at different times.
    long oldestId = insertWaypoint("aa", 2);
    long recentId = insertWaypoint("ab", 0);
    long oldId = insertWaypoint("ac", 1);

    SearchQuery query = new SearchQuery("a", null, -1, NOW);
    ArrayList<ScoredResult> results = new ArrayList<ScoredResult>(engine.search(query));

    // Reverse time order.
    assertWaypointResults(results, recentId, oldId, oldestId);
  }

  public void testSearchCurrentTrack() {
    // All results match text, but one of them is the current track.
    long currentId = insertTrack("ab", 1);
    long otherId = insertTrack("aa", 1);

    SearchQuery query = new SearchQuery("a", null, currentId, NOW);
    ArrayList<ScoredResult> results = new ArrayList<ScoredResult>(engine.search(query));

    // Current track should be demoted.
    assertTrackResults(results, otherId, currentId);
  }

  public void testSearchCurrentTrackWaypoint() {
    // All results match text, but one of them is in the current track.
    long otherId = insertWaypoint("aa", 1, 456);
    long currentId = insertWaypoint("ab", 1, 123);

    SearchQuery query = new SearchQuery("a", null, 123, NOW);
    ArrayList<ScoredResult> results = new ArrayList<ScoredResult>(engine.search(query));

    // Waypoint in current track should be promoted.
    assertWaypointResults(results, currentId, otherId);
  }

  private void assertTrackResult(long trackId, ScoredResult result) {
    assertNotNull("Not a track", result.track);
    assertNull("Ambiguous result", result.waypoint);
    assertEquals(trackId, result.track.getId());
  }

  private void assertTrackResults(List<ScoredResult> results, long... trackIds) {
    String errMsg = "Expected IDs=" + Arrays.toString(trackIds) + "; results=" + results;
    assertEquals(results.size(), trackIds.length);
    for (int i = 0; i < results.size(); i++) {
      ScoredResult result = results.get(i);
      assertNotNull(errMsg, result.track);
      assertNull(errMsg, result.waypoint);
      assertEquals(errMsg, trackIds[i], result.track.getId());
    }
  }

  private void assertWaypointResult(long waypointId, ScoredResult result) {
    assertNotNull("Not a waypoint", result.waypoint);
    assertNull("Ambiguous result", result.track);
    assertEquals(waypointId, result.waypoint.getId());
  }

  private void assertWaypointResults(List<ScoredResult> results, long... waypointIds) {
    String errMsg = "Expected IDs=" + Arrays.toString(waypointIds) + "; results=" + results;
    assertEquals(results.size(), waypointIds.length);
    for (int i = 0; i < results.size(); i++) {
      ScoredResult result = results.get(i);
      assertNotNull(errMsg, result.waypoint);
      assertNull(errMsg, result.track);
      assertEquals(errMsg, waypointIds[i], result.waypoint.getId());
    }
  }
}
