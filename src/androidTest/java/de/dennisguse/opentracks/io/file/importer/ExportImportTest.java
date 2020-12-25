package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

    private final List<Marker> markers = new ArrayList<>();
    private final List<TrackPoint> trackPoints = new ArrayList<>();

    private Track.Id importTrackId;
    private final Track.Id trackId = new Track.Id(System.currentTimeMillis());

    @Before
    public void setUp() {
        Pair<Track, List<TrackPoint>> track = TestDataUtil.createTrack(trackId, 10);
        track.first.setIcon(TRACK_ICON);
        track.first.setCategory(TRACK_CATEGORY);
        track.first.setDescription(TRACK_DESCRIPTION);
        contentProviderUtils.insertTrack(track.first);
        contentProviderUtils.bulkInsertTrackPoint(track.second, track.first.getId());

        trackPoints.clear();
        trackPoints.addAll(track.second);

        for (int i = 0; i < 3; i++) {
            Marker marker = new Marker(trackId, track.second.get(i).getLocation());
            marker.setName("the marker " + i);
            marker.setDescription("the marker description " + i);
            marker.setCategory("the marker category" + i);
            marker.setIcon("the marker icon" + i);
            marker.setPhotoUrl("the photo url" + i);
            contentProviderUtils.insertMarker(marker);

            markers.add(marker);
        }

        assertEquals(markers.size(), contentProviderUtils.getMarkerCount(trackId));
    }

    @After
    public void tearDown() {
        contentProviderUtils.deleteTrack(context, trackId);
        if (importTrackId != null) {
            contentProviderUtils.deleteTrack(context, importTrackId);
        }
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

        TrackExporter trackExporter = TrackFileFormat.KML_WITH_TRACKDETAIL.newTrackExporter(context);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(track, outputStream);
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new KmlFileTrackImporter(context);
        importTrackId = trackImporter.importFile(inputStream);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals(track.getCategory(), importedTrack.getCategory());
        assertEquals(track.getDescription(), importedTrack.getDescription());
        assertEquals(track.getName(), importedTrack.getName());
        assertEquals(track.getIcon(), importedTrack.getIcon());
        assertEquals(track.getUuid(), importedTrack.getUuid());

        // 2. markers
        assertMarkers();

        // 3. trackpoints
        assertTrackpoints(false, false, false, false, false);
    }

    @LargeTest
    @Test
    public void kml_with_trackdetail_and_sensordata() {
        // given
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.newTrackExporter(context);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(track, outputStream);
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new KmlFileTrackImporter(context);
        importTrackId = trackImporter.importFile(inputStream);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals(track.getCategory(), importedTrack.getCategory());
        assertEquals(track.getDescription(), importedTrack.getDescription());
        assertEquals(track.getName(), importedTrack.getName());
        assertEquals(track.getIcon(), importedTrack.getIcon());

        // 2. markers
        assertMarkers();

        // 3. trackpoints
        assertTrackpoints(true, true, true, true, true);
    }

    @LargeTest
    @Test(expected = ImportAlreadyExistsException.class)
    public void kml_with_trackdetail_and_sensordata_duplicate_trackUUID() {
        // given
        PreferencesUtils.setBoolean(context, R.string.import_prevent_reimport_key, true);
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.newTrackExporter(context);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(track, outputStream);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new KmlFileTrackImporter(context);
        importTrackId = trackImporter.importFile(inputStream);

        // then
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNull(importedTrack);
    }

    @Ignore
    @LargeTest
    @Test
    public void kmz_only_track() {
        // TODO
        Log.e(TAG, "Test not implemented.");
    }

    @Ignore
    @LargeTest
    @Test
    public void kmz_with_trackdetail() {
        // TODO
        Log.e(TAG, "Test not implemented.");
    }

    @Ignore
    @LargeTest
    @Test
    public void kmz_with_trackdetail_and_sensordata() {
        // TODO
        Log.e(TAG, "Test not implemented.");
    }

    @Ignore
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

        TrackExporter trackExporter = TrackFileFormat.GPX.newTrackExporter(context);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(track, outputStream);
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new GpxFileTrackImporter(context, contentProviderUtils);
        importTrackId = trackImporter.importFile(inputStream);

        // then
        // 1. track
        Track trackImported = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(trackImported);
        assertEquals(track.getCategory(), trackImported.getCategory());
        assertEquals(track.getDescription(), trackImported.getDescription());
        assertEquals(track.getName(), trackImported.getName());

        //TODO exporting and importing a track icon is not yet supported by GpxTrackWriter.
        //assertEquals(track.getIcon(), trackImported.getIcon());

        // 2. markers
        assertMarkers();

        // 3. trackpoints
        assertTrackpoints(false, true, true, true, true);
    }

    @LargeTest
    @Test(expected = ImportAlreadyExistsException.class)
    public void gpx_duplicate_trackUUID() {
        // given
        PreferencesUtils.setBoolean(context, R.string.import_prevent_reimport_key, true);
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.GPX.newTrackExporter(context);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(track, outputStream);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new GpxFileTrackImporter(context, contentProviderUtils);
        importTrackId = trackImporter.importFile(inputStream);

        // then
        // 1. track
        Track trackImported = contentProviderUtils.getTrack(importTrackId);
        assertNull(trackImported);
    }

    private void assertMarkers() {
        assertEquals(markers.size(), contentProviderUtils.getMarkerCount(importTrackId));

        List<Marker> importedMarkers = contentProviderUtils.getMarkers(importTrackId);
        for (int i = 0; i < markers.size(); i++) {
            Marker marker = markers.get(i);
            Marker importMarker = importedMarkers.get(i);
            assertEquals(marker.getCategory(), importMarker.getCategory());
            assertEquals(marker.getDescription(), importMarker.getDescription());
            // assertEquals(marker.getIcon(), importMarker.getIcon()); // TODO for KML
            assertEquals(marker.getName(), importMarker.getName());
            assertEquals("", importMarker.getPhotoUrl());

            assertEquals(marker.getLocation().getLatitude(), importMarker.getLocation().getLatitude(), 0.001);
            assertEquals(marker.getLocation().getLongitude(), importMarker.getLocation().getLongitude(), 0.001);
            assertEquals(marker.getLocation().getAltitude(), importMarker.getLocation().getAltitude(), 0.001);
        }
    }

    private void assertTrackpoints(boolean verifyPower, boolean verifyHeartrate, boolean verifyCadence, boolean verifyElevationGain, boolean verifyElevationLoss) {
        List<TrackPoint> importedTrackPoints = contentProviderUtils.getTrackPoints(importTrackId);
        assertEquals(trackPoints.size(), importedTrackPoints.size());

        for (int i = 0; i < trackPoints.size(); i++) {
            TrackPoint trackPoint = trackPoints.get(i);
            TrackPoint importedTrackPoint = importedTrackPoints.get(i);

            assertEquals(trackPoint.getTime(), importedTrackPoint.getTime(), 0.01);

            // TODO Not exported for GPX/KML
            //   assertEquals(trackPoint.getAccuracy(), importedTrackPoint.getAccuracy(), 0.01);

            assertEquals(trackPoint.getLatitude(), importedTrackPoint.getLatitude(), 0.001);
            assertEquals(trackPoint.getLongitude(), importedTrackPoint.getLongitude(), 0.001);
            assertEquals(trackPoint.getAltitude(), importedTrackPoint.getAltitude(), 0.001);
            assertEquals(trackPoint.getSpeed(), importedTrackPoint.getSpeed(), 0.001);
            if (verifyHeartrate) {
                assertEquals(trackPoint.getHeartRate_bpm(), importedTrackPoint.getHeartRate_bpm(), 0.01);
            }
            if (verifyCadence) {
                assertEquals(trackPoint.getCyclingCadence_rpm(), importedTrackPoint.getCyclingCadence_rpm(), 0.01);
            }
            if (verifyPower) {
                assertEquals(trackPoint.getPower(), importedTrackPoint.getPower(), 0.01);
            }
            if (verifyElevationGain) {
                assertEquals(trackPoint.getElevationGain(), importedTrackPoint.getElevationGain(), 0.01);
            }
            if (verifyElevationLoss) {
                assertEquals(trackPoint.getElevationLoss(), importedTrackPoint.getElevationLoss(), 0.01);
            }
        }
    }
}