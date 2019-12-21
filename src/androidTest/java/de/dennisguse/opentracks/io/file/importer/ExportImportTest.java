package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.location.Location;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.CustomContentProviderUtilsTest;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class ExportImportTest {

    private Context context = ApplicationProvider.getApplicationContext();

    private ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

    private static final String TRACK_ICON = "the track icon";
    private static final String TRACK_CATEGORY = "the category";
    private static final String TRACK_DESCRIPTION = "the description";
    private long exportTrackId = System.currentTimeMillis();
    private long importTrackId;

    @Before
    public void setUp() {
        Track track = CustomContentProviderUtilsTest.getTrack(exportTrackId, 150);
        track.setIcon(TRACK_ICON);
        track.setCategory(TRACK_CATEGORY);
        track.setDescription(TRACK_DESCRIPTION);
        contentProviderUtils.insertTrack(track);
        contentProviderUtils.bulkInsertTrackPoint(track.getLocations().toArray(new Location[0]), track.getLocations().size(), track.getId());
    }

    @After
    public void tearDown() {
        contentProviderUtils.deleteTrack(context, exportTrackId);
        contentProviderUtils.deleteTrack(context, importTrackId);
    }

    @Test
    public void kml_with_trackdetail() {
        // given
        Track track = contentProviderUtils.getTrack(exportTrackId);

        TrackFileFormat trackFileFormat = TrackFileFormat.KML_WITH_TRACKDETAIL;
        TrackExporter trackExporter = trackFileFormat.newTrackExporter(context, new Track[]{track}, null);

        // when

        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(context, outputStream);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new KmlFileTrackImporter(context, -1L);
        importTrackId = trackImporter.importFile(inputStream);

        // then
        Track trackImported = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(trackImported);
        assertEquals(track.getLocations(), trackImported.getLocations());
        assertEquals(track.getCategory(), trackImported.getCategory());
        assertEquals(track.getDescription(), trackImported.getDescription());
        assertEquals(track.getName(), trackImported.getName());
        assertEquals(track.getIcon(), trackImported.getIcon());

        //TODO Check (relative/absolute) time of trackpoints
        //TODO Check marker/waypoints
        //TODO Check tripstatistics
    }

    @Test
    public void gpx() {
        // given
        Track track = contentProviderUtils.getTrack(exportTrackId);

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
        Track trackImported = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(trackImported);
        assertEquals(track.getLocations(), trackImported.getLocations());
        assertEquals(track.getCategory(), trackImported.getCategory());
        assertEquals(track.getDescription(), trackImported.getDescription());
        assertEquals(track.getName(), trackImported.getName());

        //TODO exporting and importing a track icon is not yet supported by GpxTrackWriter.
        //assertEquals(track.getIcon(), trackImported.getIcon());
        //TODO Check (relative/absolute) time of trackpoints
        //TODO Check marker/waypoints
        //TODO Check tripstatistics
    }
}