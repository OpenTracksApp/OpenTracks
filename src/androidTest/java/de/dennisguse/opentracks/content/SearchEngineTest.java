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

package de.dennisguse.opentracks.content;

import android.content.ContentUris;
import android.location.Location;
import android.net.Uri;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.provider.ProviderTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.dennisguse.opentracks.content.SearchEngine.ScoredResult;
import de.dennisguse.opentracks.content.SearchEngine.SearchQuery;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.CustomContentProvider;
import de.dennisguse.opentracks.content.provider.CustomSQLiteOpenHelper;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * Tests for {@link SearchEngine}.
 * These are not meant to be quality tests, but instead feature-by-feature tests.
 * In other words, they don't test the mixing of different score boostings, just each boosting separately.
 *
 * @author Rodrigo Damazio
 */
public class SearchEngineTest {

    private static final String TAG = SearchEngineTest.class.getSimpleName();

    private static final Location HERE = new Location("gps");
    private static final long NOW = SearchEngine.OLDEST_ALLOWED_TIMESTAMP + 1000;

    @Rule
    public ProviderTestRule sqliteContentProviderRule = new ProviderTestRule.Builder(CustomContentProvider.class, ContentProviderUtils.AUTHORITY_PACKAGE).setPrefix(TAG).build();

    private ContentProviderUtils providerUtils;
    private SearchEngine engine;

    @Before
    public void setUp() {
        providerUtils = new ContentProviderUtils(sqliteContentProviderRule.getResolver());

        engine = new SearchEngine(providerUtils);
    }

    @After
    public void tearDown() {
        InstrumentationRegistry.getInstrumentation().getTargetContext().deleteDatabase(TAG + CustomSQLiteOpenHelper.DATABASE_NAME);
    }

    private Track.Id insertTrack(String title, String description, String category, long hoursAgo) {
        Track track = new Track();
        track.setName(title);
        track.setDescription(description);
        track.setCategory(category);

        TrackStatistics stats = track.getTrackStatistics();
        if (hoursAgo > 0) {
            // Started twice hoursAgo, so the average time is hoursAgo.
            stats.setStartTime_ms(NOW - hoursAgo * 1000L * 60L * 60L * 2);
            stats.setStopTime_ms(NOW);
        }

        Uri uri = providerUtils.insertTrack(track);
        return new Track.Id(ContentUris.parseId(uri));
    }

    private Track.Id insertTrack(String title, String description, String category) {
        return insertTrack(title, description, category, -1);
    }

    private Track.Id insertTrack(String title, long hoursAgo) {
        return insertTrack(title, "", "", hoursAgo);
    }

    private Waypoint.Id insertWaypoint(String title, String description, String category, double distance, long hoursAgo, Track.Id trackId) {
        Location location = new Location(HERE);
        location.setLatitude(location.getLatitude() + distance);
        location.setLongitude(location.getLongitude() + distance);
        if (hoursAgo >= 0) {
            location.setTime(NOW - hoursAgo * 1000L * 60L * 60L);
        }
        Waypoint waypoint = new Waypoint(location);
        waypoint.setName(title);
        waypoint.setDescription(description);
        waypoint.setCategory(category);
        waypoint.setTrackId(trackId);

        Uri uri = providerUtils.insertWaypoint(waypoint);
        return new Waypoint.Id(ContentUris.parseId(uri));
    }

    private Waypoint.Id insertWaypoint(String title, String description, String category) {
        return insertWaypoint(title, description, category, 0.0, -1, null);
    }

    private Waypoint.Id insertWaypoint(String title, double distance) {
        return insertWaypoint(title, "", "", distance, -1, null);
    }

    private Waypoint.Id insertWaypoint(String title, long hoursAgo) {
        return insertWaypoint(title, "", "", 0.0, hoursAgo, null);
    }

    private Waypoint.Id insertWaypoint(String title, long hoursAgo, Track.Id trackId) {
        return insertWaypoint(title, "", "", 0.0, hoursAgo, trackId);
    }

    @Test
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
        Track.Id descriptionMatchId = insertTrack("bb", "aa", "cc");
        Track.Id categoryMatchId = insertTrack("bb", "cc", "aa");
        Track.Id titleMatchId = insertTrack("aa", "bb", "cc");
        Track.Id titleCategoryMatchId = insertTrack("aa", "bb", "ca");
        Track.Id titleDescriptionMatchId = insertTrack("aa", "ba", "cc");
        Track.Id allMatchId = insertTrack("aa", "ba", "ca");

        SearchQuery query = new SearchQuery("a", null, null, NOW);
        ArrayList<ScoredResult> results = new ArrayList<>(engine.search(query));

        // Title > Description > Category.
        assertTrackResults(results, allMatchId, titleDescriptionMatchId, titleCategoryMatchId, titleMatchId, descriptionMatchId, categoryMatchId);
    }

    @Test
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
        Waypoint.Id descriptionMatchId = insertWaypoint("bb", "aa", "cc");
        Waypoint.Id categoryMatchId = insertWaypoint("bb", "cc", "aa");
        Waypoint.Id titleMatchId = insertWaypoint("aa", "bb", "cc");
        Waypoint.Id titleCategoryMatchId = insertWaypoint("aa", "bb", "ca");
        Waypoint.Id titleDescriptionMatchId = insertWaypoint("aa", "ba", "cc");
        Waypoint.Id allMatchId = insertWaypoint("aa", "ba", "ca");

        SearchQuery query = new SearchQuery("a", null, null, NOW);
        ArrayList<ScoredResult> results = new ArrayList<>(engine.search(query));

        // Title > Description > Category.
        assertWaypointResults(results, allMatchId, titleDescriptionMatchId, titleCategoryMatchId, titleMatchId, descriptionMatchId, categoryMatchId);
    }

    @Test
    public void testSearchMixedText() {
        // Insert 5 entries (purposefully out of result order):
        // - one waypoint which will match by description
        // - one waypoint which won't match
        // - one waypoint which will match by title
        // - one track which won't match
        // - one track which will match by title
        Waypoint.Id descriptionWaypointId = insertWaypoint("bb", "aa", "cc");
        insertWaypoint("bb", "cc", "dd");
        Waypoint.Id titleWaypointId = insertWaypoint("aa", "bb", "cc");
        insertTrack("bb", "cc", "dd");
        Track.Id trackId = insertTrack("aa", "bb", "cc");

        SearchQuery query = new SearchQuery("a", null, null, NOW);
        ArrayList<ScoredResult> results = new ArrayList<>(engine.search(query));

        // Title > Description > Category.
        Assert.assertEquals(results.toString(), 3, results.size());
        assertTrackResult(trackId, results.get(0));
        assertWaypointResult(titleWaypointId, results.get(1));
        assertWaypointResult(descriptionWaypointId, results.get(2));
    }

    @Test
    public void testSearchWaypointDistance() {
        // All results match text, but they're at difference distances from the user.
        Waypoint.Id farFarAwayId = insertWaypoint("aa", 0.3);
        Waypoint.Id nearId = insertWaypoint("ab", 0.1);
        Waypoint.Id farId = insertWaypoint("ac", 0.2);

        SearchQuery query = new SearchQuery("a", HERE, null, NOW);
        ArrayList<ScoredResult> results = new ArrayList<>(engine.search(query));

        // Distance order.
        assertWaypointResults(results, nearId, farId, farFarAwayId);
    }

    public void testSearchTrackRecent() {
        // All results match text, but they're were recorded at different times.
        Track.Id oldestId = insertTrack("aa", 3);
        Track.Id recentId = insertTrack("ab", 1);
        Track.Id oldId = insertTrack("ac", 2);

        SearchQuery query = new SearchQuery("a", null, null, NOW);
        ArrayList<ScoredResult> results = new ArrayList<>(engine.search(query));

        // Reverse time order.
        assertTrackResults(results, recentId, oldId, oldestId);
    }

    @Test
    public void testSearchWaypointRecent() {
        // All results match text, but they're were recorded at different times.
        Waypoint.Id oldestId = insertWaypoint("aa", 2);
        Waypoint.Id recentId = insertWaypoint("ab", 0);
        Waypoint.Id oldId = insertWaypoint("ac", 1);

        SearchQuery query = new SearchQuery("a", null, null, NOW);
        ArrayList<ScoredResult> results = new ArrayList<>(engine.search(query));

        // Reverse time order.
        assertWaypointResults(results, recentId, oldId, oldestId);
    }

    @Test
    public void testSearchCurrentTrack() {
        // All results match text, but one of them is the current track.
        Track.Id currentId = insertTrack("ab", 1);
        Track.Id otherId = insertTrack("aa", 1);

        SearchQuery query = new SearchQuery("a", null, currentId, NOW);
        ArrayList<ScoredResult> results = new ArrayList<>(engine.search(query));

        // Current track should be demoted.
        assertTrackResults(results, otherId, currentId);
    }

    @Test
    public void testSearchCurrentTrackWaypoint() {
        // All results match text, but one of them is in the current track.
        Waypoint.Id otherId = insertWaypoint("aa", 1, new Track.Id(456));
        Waypoint.Id currentId = insertWaypoint("ab", 1, new Track.Id(123));

        SearchQuery query = new SearchQuery("a", null, new Track.Id(123), NOW);
        ArrayList<ScoredResult> results = new ArrayList<>(engine.search(query));

        // Waypoint in current track should be promoted.
        assertWaypointResults(results, currentId, otherId);
    }

    private void assertTrackResult(Track.Id trackId, ScoredResult result) {
        Assert.assertNotNull("Not a track", result.track);
        Assert.assertNull("Ambiguous result", result.waypoint);
        Assert.assertEquals(trackId, result.track.getId());
    }

    private void assertTrackResults(List<ScoredResult> results, Track.Id... trackIds) {
        String errMsg = "Expected IDs=" + Arrays.toString(trackIds) + "; results=" + results;
        Assert.assertEquals(results.size(), trackIds.length);
        for (int i = 0; i < results.size(); i++) {
            ScoredResult result = results.get(i);
            Assert.assertNotNull(errMsg, result.track);
            Assert.assertNull(errMsg, result.waypoint);
            Assert.assertEquals(errMsg, trackIds[i], result.track.getId());
        }
    }

    private void assertWaypointResult(Waypoint.Id waypointId, ScoredResult result) {
        Assert.assertNotNull("Not a waypoint", result.waypoint);
        Assert.assertNull("Ambiguous result", result.track);
        Assert.assertEquals(waypointId, result.waypoint.getId());
    }

    private void assertWaypointResults(List<ScoredResult> results, Waypoint.Id... waypointIds) {
        String errMsg = "Expected IDs=" + Arrays.toString(waypointIds) + "; results=" + results;
        Assert.assertEquals(results.size(), waypointIds.length);
        for (int i = 0; i < results.size(); i++) {
            ScoredResult result = results.get(i);
            Assert.assertNotNull(errMsg, result.waypoint);
            Assert.assertNull(errMsg, result.track);
            Assert.assertEquals(errMsg, waypointIds[i], result.waypoint.getId());
        }
    }
}
