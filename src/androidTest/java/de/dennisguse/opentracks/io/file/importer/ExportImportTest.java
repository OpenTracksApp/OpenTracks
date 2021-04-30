package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.stats.TrackStatistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Export a track to {@link TrackFileFormat} and verify that the import is identical.
 * <p>
 */
@RunWith(AndroidJUnit4.class)
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

    private File tmpFile;
    private Uri tmpFileUri;

    private Track track;
    private List<Marker> markers = new ArrayList<>();
    private List<TrackPoint> trackPoints = new ArrayList<>();

    private Track.Id trackId;
    private Track.Id importTrackId;

    @Before
    public void fileSetup() throws IOException {
        tmpFile = File.createTempFile("test", "test", context.getFilesDir());
        tmpFileUri = Uri.fromFile(tmpFile);
    }

    @After
    public void FileTearDown() {
        tmpFile.deleteOnExit();
        tmpFileUri = null;
    }

    public void setUp(boolean hasSensorDistance) throws TimeoutException {
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(new Intent(context, TrackRecordingService.class)))
                .getService();


        trackId = service.startNewTrack();
        //TODO Workaround as those managers overwrite input data; We need to refactor TrackRecordingService to make it actually testable
        service.setAltitudeSumManager(null);
        service.setRemoteSensorManager(null);

        Distance sensorDistance = hasSensorDistance ? Distance.of(5) : null;

        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 14, 10, 15, 10, 1, 66, 3, 50, sensorDistance), 0);
        service.insertMarker("Marker 1", "Marker 1 category", "Marker 1 desc", null);
        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 14.001, 10, 15, 10, 0, 66, 3, 50, sensorDistance), 0);
        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 14.002, 10, 15, 10, 0, 66, 3, 50, sensorDistance), 0);
        service.insertMarker("Marker 2", "Marker 2 category", "Marker 2 desc", null);
        service.pauseCurrentTrack();

        service.resumeCurrentTrack();
        //TODO Workaround as those managers overwrite input data; We need to refactor TrackRecordingService to make it actually testable
        service.setAltitudeSumManager(null);
        service.setRemoteSensorManager(null);

        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 14.003, 10, 15, 10, 0, 66, 3, 50, sensorDistance), 0);
        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 16, 10, 15, 10, 0, 66, 3, 50, sensorDistance), 0);
        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 16.001, 10, 15, 10, 0, 66, 3, 50, sensorDistance), 0);
        service.endCurrentTrack();

        track = contentProviderUtils.getTrack(trackId);
        track.setIcon(TRACK_ICON);
        track.setCategory(TRACK_CATEGORY);
        track.setDescription(TRACK_DESCRIPTION);
        contentProviderUtils.updateTrack(track);

        track = contentProviderUtils.getTrack(trackId);
        trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
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

    @LargeTest
    @Test
    public void kml_with_trackdetail() throws TimeoutException, IOException {
        setUp(false);

        // given
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.KML_WITH_TRACKDETAIL.createTrackExporter(context);

        // when
        // 1. export
        trackExporter.writeTrack(track, context.getContentResolver().openOutputStream(tmpFileUri));
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        InputStream inputStream = context.getContentResolver().openInputStream(tmpFileUri);
        XMLImporter trackImporter = new XMLImporter(new KmlFileTrackImporter(context));
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
        assertTrackpoints(trackPoints, false, false, false, false, false, false);

        // 3. trackstatistics
        assertTrackStatistics(false, false, true);

        // 4. markers
        assertMarkers();
    }

    @LargeTest
    @Test
    public void kml_with_trackdetail_and_sensordata() throws TimeoutException, IOException {
        setUp(true);

        // given
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.createTrackExporter(context);

        // when
        // 1. export
        trackExporter.writeTrack(track, context.getContentResolver().openOutputStream(tmpFileUri));
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        InputStream inputStream = context.getContentResolver().openInputStream(tmpFileUri);
        XMLImporter trackImporter = new XMLImporter(new KmlFileTrackImporter(context));
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
        assertTrackpoints(trackPoints, true, true, true, true, true, true);

        // 2. trackstatistics
        assertTrackStatistics(false, true, true);

        // 4. markers
        assertMarkers();
    }

    //TODO Does not test images
    @LargeTest
    @Test
    public void kmz_with_trackdetail_and_sensordata() throws TimeoutException, IOException {
        setUp(true);

        // given
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA.createTrackExporter(context);

        // when
        // 1. export
        trackExporter.writeTrack(track, context.getContentResolver().openOutputStream(tmpFileUri));
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        KmzTrackImporter trackImporter = new KmzTrackImporter();
        importTrackId = trackImporter.importFile(context, tmpFileUri).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals(track.getCategory(), importedTrack.getCategory());
        assertEquals(track.getDescription(), importedTrack.getDescription());
        assertEquals(track.getName(), importedTrack.getName());
        assertEquals(track.getIcon(), importedTrack.getIcon());

        // 2. trackpoints
        assertTrackpoints(trackPoints, true, true, true, true, true, true);

        // 2. trackstatistics
        assertTrackStatistics(false, true, true);

        // 4. markers
        assertMarkers();
    }

    @LargeTest
    @Test(expected = ImportAlreadyExistsException.class)
    public void kml_with_trackdetail_and_sensordata_duplicate_trackUUID() throws TimeoutException, IOException {
        setUp(false);

        // given
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(context.getString(R.string.import_prevent_reimport_key), true);
        editor.commit();
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.createTrackExporter(context);

        // when
        trackExporter.writeTrack(track, context.getContentResolver().openOutputStream(tmpFileUri));

        // 2. import
        InputStream inputStream = context.getContentResolver().openInputStream(tmpFileUri);
        XMLImporter trackImporter = new XMLImporter(new KmlFileTrackImporter(context));
        importTrackId = trackImporter.importFile(inputStream).get(0);

        // then
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNull(importedTrack);
    }

    @LargeTest
    @Test
    public void gpx() throws TimeoutException, IOException {
        setUp(true);

        // given
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.GPX.createTrackExporter(context);

        // when
        // 1. export
        trackExporter.writeTrack(track, context.getContentResolver().openOutputStream(tmpFileUri));
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        InputStream inputStream = context.getContentResolver().openInputStream(tmpFileUri);
        XMLImporter trackImporter = new XMLImporter(new GpxFileTrackImporter(context, contentProviderUtils));
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

        assertTrackpoints(trackPointsWithCoordinates, true, true, true, true, true, false);

        // 3. trackstatistics
        assertTrackStatistics(true, true, false);

        // 4. markers
        assertMarkers();
    }

    @LargeTest
    @Test(expected = ImportAlreadyExistsException.class)
    public void gpx_duplicate_trackUUID() throws TimeoutException, IOException {
        setUp(false);

        // given
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(context.getString(R.string.import_prevent_reimport_key), true);
        editor.commit();
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.GPX.createTrackExporter(context);

        // when
        // 1. export
        trackExporter.writeTrack(track, context.getContentResolver().openOutputStream(tmpFileUri));

        // 2. import
        InputStream inputStream = context.getContentResolver().openInputStream(tmpFileUri);
        XMLImporter trackImporter = new XMLImporter(new GpxFileTrackImporter(context, contentProviderUtils));
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

    private void assertTrackpoints(List<TrackPoint> trackPoints, boolean verifyPower, boolean verifyHeartrate, boolean verifyCadence, boolean verifyAltitudeGain, boolean verifyAltitudeLoss, boolean verifyDistance) {
        List<TrackPoint> importedTrackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
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
                assertEquals(trackPoint.getSpeed().toMPS(), importedTrackPoint.getSpeed().toMPS(), 0.001);
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
                assertEquals("" + trackPoint, trackPoint.getHeartRate_bpm(), importedTrackPoint.getHeartRate_bpm(), 0.01);
            }
            if (verifyCadence) {
                assertEquals("" + trackPoint, trackPoint.getCyclingCadence_rpm(), importedTrackPoint.getCyclingCadence_rpm(), 0.01);
            }
            if (verifyPower) {
                assertEquals("" + trackPoint, trackPoint.getPower(), importedTrackPoint.getPower(), 0.01);
            }
            if (verifyAltitudeGain) {
                assertEquals(trackPoint.getAltitudeGain(), importedTrackPoint.getAltitudeGain(), 0.01);
            }
            if (verifyAltitudeLoss) {
                assertEquals(trackPoint.getAltitudeLoss(), importedTrackPoint.getAltitudeLoss(), 0.01);
            }
            if (verifyDistance) {
                assertEquals(trackPoint.getSensorDistance(), importedTrackPoint.getSensorDistance());
            }
        }
    }

    private void assertTrackStatistics(boolean isGpx, boolean verifyAltitudeGainAndLoss, boolean verifyDistance) {
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
            if (verifyDistance) {
                assertEquals(trackStatistics.getTotalDistance(), importedTrackStatistics.getTotalDistance());
            }

            // Speed
            assertEquals(trackStatistics.getMaxSpeed(), importedTrackStatistics.getMaxSpeed());
            assertEquals(trackStatistics.getAverageSpeed(), importedTrackStatistics.getAverageSpeed());
            assertEquals(trackStatistics.getAverageMovingSpeed(), importedTrackStatistics.getAverageMovingSpeed());
        }

        // Altitude
        assertEquals(trackStatistics.getMinAltitude(), importedTrackStatistics.getMinAltitude(), 0.01);
        assertEquals(trackStatistics.getMaxAltitude(), importedTrackStatistics.getMaxAltitude(), 0.01);
        if (verifyAltitudeGainAndLoss) {
            assertEquals(trackStatistics.getTotalAltitudeGain(), importedTrackStatistics.getTotalAltitudeGain(), 0.01);
            assertEquals(trackStatistics.getTotalAltitudeLoss(), importedTrackStatistics.getTotalAltitudeLoss(), 0.01);
        } else {
            assertFalse(importedTrackStatistics.hasTotalAltitudeGain());
            assertFalse(importedTrackStatistics.hasTotalAltitudeLoss());
        }
    }

    private static TrackPoint createTrackPoint(long time, double latitude, double longitude, float accuracy, float speed, float altitude, float altitudeGain, float heartRate, float cyclingCadence, float power, Distance distance) {
        TrackPoint tp = new TrackPoint(latitude, longitude, (double) altitude, Instant.ofEpochMilli(time));
        tp.setAccuracy(accuracy);
        tp.setSpeed(Speed.of(speed));
        tp.setHeartRate_bpm(heartRate);
        tp.setCyclingCadence_rpm(cyclingCadence);
        tp.setPower(power);
        tp.setAltitudeGain(altitudeGain);
        tp.setAltitudeLoss(altitudeGain); //TODO
        tp.setSensorDistance(distance);
        return tp;
    }
}