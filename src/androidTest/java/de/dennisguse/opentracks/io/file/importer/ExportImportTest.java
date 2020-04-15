package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.stats.TrackStatistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Export a track to {@link TrackFileFormat} and verify that the import is identical.
 * <p>
 * TODO: test ignores {@link TrackStatistics} for now.
 */
@RunWith(JUnit4.class)
public class ExportImportTest {

    private static final String TAG = ExportImportTest.class.getSimpleName();

    private final Context context = ApplicationProvider.getApplicationContext();

    private final ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

    private static final String TRACK_ICON = "the track icon";
    private static final String TRACK_CATEGORY = "the category";
    private static final String TRACK_DESCRIPTION = "the description";
    private final List<Waypoint> waypoints = new ArrayList<>();
    private long importTrackId;
    private final long trackId = System.currentTimeMillis();

    @Before
    public void setUp() {
        Pair<Track, TrackPoint[]> track = TestDataUtil.createTrack(trackId, 10);
        track.first.setIcon(TRACK_ICON);
        track.first.setCategory(TRACK_CATEGORY);
        track.first.setDescription(TRACK_DESCRIPTION);
        contentProviderUtils.insertTrack(track.first);
        contentProviderUtils.bulkInsertTrackPoint(track.second, track.first.getId());

        for (int i = 0; i < 3; i++) {
            Waypoint waypoint = new Waypoint(track.second[i].getLocation());
            waypoint.setName("the waypoint " + i);
            waypoint.setDescription("the waypoint description " + i);
            waypoint.setCategory("the waypoint category" + i);
            waypoint.setIcon("the waypoing icon" + i);
            waypoint.setPhotoUrl("the photo url" + i);
            waypoint.setTrackId(trackId);
            contentProviderUtils.insertWaypoint(waypoint);

            waypoints.add(waypoint);
        }

        assertEquals(waypoints.size(), contentProviderUtils.getWaypointCount(trackId));
    }

    @After
    public void tearDown() {
        contentProviderUtils.deleteTrack(context, trackId);
        contentProviderUtils.deleteTrack(context, importTrackId);
    }

    @LargeTest
    @Test
    public void kml_only_track() {
        // TODO
        Log.e(TAG, "Test not implemented.");
    }

    @LargeTest
    @Test
    public void kml_with_trackdetail() {
        // given
        Track track = contentProviderUtils.getTrack(trackId);

        TrackFileFormat trackFileFormat = TrackFileFormat.KML_WITH_TRACKDETAIL;
        TrackExporter trackExporter = trackFileFormat.newTrackExporter(context, new Track[]{track}, null);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(context, outputStream);

        System.out.println(outputStream.toString());

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new KmlFileTrackImporter(context, -1L);
        importTrackId = trackImporter.importFile(inputStream);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        //TODO assertEquals(track.getTrackPoints(), importedTrack.getTrackPoints());
        assertEquals(track.getCategory(), importedTrack.getCategory());
        assertEquals(track.getDescription(), importedTrack.getDescription());
        assertEquals(track.getName(), importedTrack.getName());
        assertEquals(track.getIcon(), importedTrack.getIcon());

        // 2. waypoints
        assertWaypoints();

        //TODO Check absolute time of trackpoints
    }

    @LargeTest
    @Test
    public void kml_with_trackdetail_and_sensordata() {
        // TODO
        Log.e(TAG, "Test not implemented.");
    }

    @LargeTest
    @Test
    public void kmz_only_track() {
        // TODO
        Log.e(TAG, "Test not implemented.");
    }

    @LargeTest
    @Test
    public void kmz_with_trackdetail() {
        // TODO
        Log.e(TAG, "Test not implemented.");
    }

    @LargeTest
    @Test
    public void kmz_with_trackdetail_and_sensordata() {
        // TODO
        Log.e(TAG, "Test not implemented.");
    }

    @LargeTest
    @Test
    public void kmz_with_trackdetail_and_sensordata_and_pictures() {
        // TODO
        Log.e(TAG, "Test not implemented.");
    }

    @LargeTest
    @Test
    public void gpx() {
        // given
        Track track = contentProviderUtils.getTrack(trackId);

        TrackFileFormat trackFileFormat = TrackFileFormat.GPX;
        TrackExporter trackExporter = trackFileFormat.newTrackExporter(context, new Track[]{track}, null);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(context, outputStream);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new GpxFileTrackImporter(context, contentProviderUtils);
        importTrackId = trackImporter.importFile(inputStream);

        // then
        // 1. track
        Track trackImported = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(trackImported);
        //TODO assertEquals(track.getTrackPoints(), trackImported.getTrackPoints());
        assertEquals(track.getCategory(), trackImported.getCategory());
        assertEquals(track.getDescription(), trackImported.getDescription());
        assertEquals(track.getName(), trackImported.getName());

        //TODO exporting and importing a track icon is not yet supported by GpxTrackWriter.
        //assertEquals(track.getIcon(), trackImported.getIcon());

        // 2. waypoints
        assertWaypoints();

        //TODO Check absolute time of trackpoints
    }

    private void assertWaypoints() {
        assertEquals(waypoints.size(), contentProviderUtils.getWaypointCount(importTrackId));

        List<Waypoint> importedWaypoints = contentProviderUtils.getWaypoints(importTrackId);
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint waypoint = waypoints.get(i);
            Waypoint importedWaypoint = importedWaypoints.get(i);
            assertEquals(waypoint.getCategory(), importedWaypoint.getCategory());
            assertEquals(waypoint.getDescription(), importedWaypoint.getDescription());
            // assertEquals(waypoint.getIcon(), importedWaypoint.getIcon()); // TODO for KML
            assertEquals(waypoint.getName(), importedWaypoint.getName());
            assertEquals("", importedWaypoint.getPhotoUrl());

            assertEquals(waypoint.getLocation().getLatitude(), importedWaypoint.getLocation().getLatitude(), 0.001);
            assertEquals(waypoint.getLocation().getLongitude(), importedWaypoint.getLocation().getLongitude(), 0.001);
            assertEquals(waypoint.getLocation().getAltitude(), importedWaypoint.getLocation().getAltitude(), 0.001);
        }
    }
}