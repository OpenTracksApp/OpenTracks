package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceInterface;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Export a track to {@link TrackFileFormat} and verify that the import is identical.
 * <p>
 */
@RunWith(JUnit4.class)
public class ExportImportTest {

    private static final String TAG = ExportImportTest.class.getSimpleName();

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @BeforeClass
    public static void preSetUp() {
        // Prepare looper for Android's message queue
        if (Looper.myLooper() == null) Looper.prepare();
    }

    private final Context context = ApplicationProvider.getApplicationContext();

    private final ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

    private static final String TRACK_ICON = "the track icon";
    private static final String TRACK_CATEGORY = "the category";
    private static final String TRACK_DESCRIPTION = "the description";

    private Track track;
    private List<Marker> markers = new ArrayList<>();
    private List<TrackPoint> trackPoints = new ArrayList<>();

    private Track.Id trackId;
    private Track.Id importTrackId;

    @Before
    public void setUp() throws TimeoutException {
        TrackRecordingServiceInterface service = ((TrackRecordingServiceInterface) mServiceRule.bindService(new Intent(context, TrackRecordingService.class)));

        trackId = service.startNewTrack();

        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 14, 10, 15, 10, 0, 66, 3, 50), 0);
        service.insertMarker("Marker 1", "Marker 1 category", "Marker 1 desc", null);
        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 14.001, 10, 15, 10, 0, 66, 3, 50), 0);
        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 14.002, 10, 15, 10, 0, 66, 3, 50), 0);
        service.insertMarker("Marker 2", "Marker 2 category", "Marker 2 desc", null);
        service.pauseCurrentTrack();

        service.resumeCurrentTrack();
        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 14.003, 10, 15, 10, 0, 66, 3, 50), 0);
        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 16, 10, 15, 10, 0, 66, 3, 50), 0);
        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 16.001, 10, 15, 10, 0, 66, 3, 50), 0);
        service.endCurrentTrack();

        track = contentProviderUtils.getTrack(trackId);
        track.setIcon(TRACK_ICON);
        track.setCategory(TRACK_CATEGORY);
        track.setDescription(TRACK_DESCRIPTION);
        contentProviderUtils.updateTrack(track);

        track = contentProviderUtils.getTrack(trackId);
        trackPoints = contentProviderUtils.getTrackPoints(trackId);
        markers = contentProviderUtils.getMarkers(trackId);
        assertEquals(10, trackPoints.size());
        assertEquals(2, markers.size());
    }

    @After
    public void tearDown() {
        contentProviderUtils.deleteTrack(context, trackId);
        if (importTrackId != null) {
            contentProviderUtils.deleteTrack(context, importTrackId);
        }
    }

    @Ignore("Not implemented")
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

        TrackExporter trackExporter = TrackFileFormat.KML_WITH_TRACKDETAIL.createTrackExporter(context);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(track, outputStream);
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new KmlFileTrackImporter(context);
        importTrackId = trackImporter.importFile(inputStream).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals(track.getCategory(), importedTrack.getCategory());
        assertEquals(track.getDescription(), importedTrack.getDescription());
        assertEquals(track.getName(), importedTrack.getName());
        assertEquals(track.getIcon(), importedTrack.getIcon());
        assertEquals(track.getUuid(), importedTrack.getUuid());

        // 2. trackpoints
        assertTrackpoints(trackPoints, false, false, false, false, false);

        // 3. trackstatistics
        assertTrackStatistics(false, false);

        // 4. markers
        assertMarkers();
    }

    @LargeTest
    @Test
    public void kml_with_trackdetail_and_sensordata() {
        // given
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.createTrackExporter(context);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(track, outputStream);
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new KmlFileTrackImporter(context);
        importTrackId = trackImporter.importFile(inputStream).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals(track.getCategory(), importedTrack.getCategory());
        assertEquals(track.getDescription(), importedTrack.getDescription());
        assertEquals(track.getName(), importedTrack.getName());
        assertEquals(track.getIcon(), importedTrack.getIcon());

        // 2. trackpoints
        assertTrackpoints(trackPoints, true, true, true, true, true);

        // 2. trackstatistics
        assertTrackStatistics(false, true);

        // 4. markers
        assertMarkers();
    }

    @LargeTest
    @Test(expected = ImportAlreadyExistsException.class)
    public void kml_with_trackdetail_and_sensordata_duplicate_trackUUID() {
        // given
        PreferencesUtils.setBoolean(context, R.string.import_prevent_reimport_key, true);
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.createTrackExporter(context);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(track, outputStream);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new KmlFileTrackImporter(context);
        importTrackId = trackImporter.importFile(inputStream).get(0);

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

        TrackExporter trackExporter = TrackFileFormat.GPX.createTrackExporter(context);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(track, outputStream);
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new GpxFileTrackImporter(context, contentProviderUtils);
        importTrackId = trackImporter.importFile(inputStream).get(0);

        // then
        // 1. track
        Track trackImported = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(trackImported);
        assertEquals(track.getCategory(), trackImported.getCategory());
        assertEquals(track.getDescription(), trackImported.getDescription());
        assertEquals(track.getName(), trackImported.getName());

        //TODO exporting and importing a track icon is not yet supported by GpxTrackWriter.
        //assertEquals(track.getIcon(), trackImported.getIcon());

        // 2. trackpoints
        // The GPX exporter does not support exporting TrackPoints without lat/lng.
        // Therefore, the track segmentation is changes.
        List<TrackPoint> trackPointsWithCoordinates = trackPoints.stream().filter(it -> TrackPoint.Type.SEGMENT_START_AUTOMATIC.equals(it.getType()) || TrackPoint.Type.TRACKPOINT.equals(it.getType())).collect(Collectors.toList());
        trackPointsWithCoordinates.get(0).setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);
        trackPointsWithCoordinates.get(3).setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);

        assertTrackpoints(trackPointsWithCoordinates, false, true, true, true, true);

        // 3. trackstatistics
        assertTrackStatistics(true, true);

        // 4. markers
        assertMarkers();
    }

    @LargeTest
    @Test(expected = ImportAlreadyExistsException.class)
    public void gpx_duplicate_trackUUID() {
        // given
        PreferencesUtils.setBoolean(context, R.string.import_prevent_reimport_key, true);
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.GPX.createTrackExporter(context);

        // when
        // 1. export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trackExporter.writeTrack(track, outputStream);

        // 2. import
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        AbstractFileTrackImporter trackImporter = new GpxFileTrackImporter(context, contentProviderUtils);
        importTrackId = trackImporter.importFile(inputStream).get(0);

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

    private void assertTrackpoints(List<TrackPoint> trackPoints, boolean verifyPower, boolean verifyHeartrate, boolean verifyCadence, boolean verifyElevationGain, boolean verifyElevationLoss) {
        List<TrackPoint> importedTrackPoints = contentProviderUtils.getTrackPoints(importTrackId);

        assertEquals(trackPoints.size(), importedTrackPoints.size());

        for (int i = 0; i < trackPoints.size(); i++) {
            TrackPoint trackPoint = trackPoints.get(i);
            TrackPoint importedTrackPoint = importedTrackPoints.get(i);

            assertEquals(trackPoint.getTime(), importedTrackPoint.getTime());
            TrackPoint.Type type = trackPoint.getType();
            Log.e(TAG, "" + importedTrackPoint.getType().equals(type));
            assertEquals("" + i, type, importedTrackPoint.getType());

            // TODO Not exported for GPX/KML
            //   assertEquals(trackPoint.getAccuracy(), importedTrackPoint.getAccuracy(), 0.01);

            assertEquals(trackPoint.hasLocation(), importedTrackPoint.hasLocation());
            if (trackPoint.hasLocation()) {
                assertEquals(trackPoint.getLatitude(), importedTrackPoint.getLatitude(), 0.001);
                assertEquals(trackPoint.getLongitude(), importedTrackPoint.getLongitude(), 0.001);
            }
            assertEquals(trackPoint.hasSpeed(), importedTrackPoint.hasSpeed());
            if (trackPoint.hasSpeed()) {
                assertEquals(trackPoint.getSpeed(), importedTrackPoint.getSpeed(), 0.001);
            }
            assertEquals(trackPoint.hasAltitude(), importedTrackPoint.hasAltitude());
            if (trackPoint.hasAltitude()) {
                assertEquals(trackPoint.getAltitude(), importedTrackPoint.getAltitude(), 0.001);
            }

            if (type.equals(TrackPoint.Type.SEGMENT_START_MANUAL) || type.equals(TrackPoint.Type.SEGMENT_END_MANUAL)) {
                //TODO REMOVE
                continue;
            }

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

    private void assertTrackStatistics(boolean isGpx, boolean verifyElevationGainAndLoss) {
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);

        assertNotNull(importedTrack.getTrackStatistics());

        TrackStatistics trackStatistics = track.getTrackStatistics();
        TrackStatistics importedTrackStatistics = importedTrack.getTrackStatistics();

        // Time
        assertTrue(trackStatistics.getStartTime().isBefore(trackStatistics.getStopTime())); //Just to be sure.
        if (!isGpx) {
            assertEquals(trackStatistics.getStartTime(), importedTrackStatistics.getStartTime());
            assertEquals(trackStatistics.getStopTime(), importedTrackStatistics.getStopTime());

            assertEquals(trackStatistics.getTotalTime(), importedTrackStatistics.getTotalTime());
            assertEquals(trackStatistics.getMovingTime(), importedTrackStatistics.getMovingTime());

            // Distance
            assertEquals(trackStatistics.getTotalDistance(), importedTrackStatistics.getTotalDistance(), 0.01);

            // Speed
            assertEquals(trackStatistics.getMaxSpeed(), importedTrackStatistics.getMaxSpeed(), 0.01);
            assertEquals(trackStatistics.getAverageSpeed(), importedTrackStatistics.getAverageSpeed(), 0.01);
            assertEquals(trackStatistics.getAverageMovingSpeed(), importedTrackStatistics.getAverageMovingSpeed(), 0.01);
        }

        // Elevation
        assertEquals(trackStatistics.getMinElevation(), importedTrackStatistics.getMinElevation(), 0.01);
        assertEquals(trackStatistics.getMaxElevation(), importedTrackStatistics.getMaxElevation(), 0.01);
        if (verifyElevationGainAndLoss) {
            assertEquals(trackStatistics.getTotalElevationGain(), importedTrackStatistics.getTotalElevationGain(), 0.01);
            assertEquals(trackStatistics.getTotalElevationLoss(), importedTrackStatistics.getTotalElevationLoss(), 0.01);
        } else {
            assertFalse(importedTrackStatistics.hasTotalElevationGain());
            assertFalse(importedTrackStatistics.hasTotalElevationLoss());
        }
    }

    private static TrackPoint createTrackPoint(long time, double latitude, double longitude, float accuracy, long speed, long altitude, float elevationGain, float heartRate, float cyclingCadence, float power) {
        Location location = new Location("");
        location.setTime(time);
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        location.setAccuracy(accuracy);
        location.setAltitude(altitude);
        location.setSpeed(speed);

        TrackPoint tp = new TrackPoint(location);
        tp.setHeartRate_bpm(heartRate);
        tp.setCyclingCadence_rpm(cyclingCadence);
        tp.setPower(power);
        tp.setElevationGain(elevationGain);
        return tp;
    }
}