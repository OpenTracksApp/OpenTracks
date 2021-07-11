package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Looper;

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
import de.dennisguse.opentracks.content.data.Altitude;
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

    private TrackImporter trackImporter;

    @Before
    public void fileSetup() throws IOException {
        tmpFile = File.createTempFile("test", "test", context.getFilesDir());
        tmpFileUri = Uri.fromFile(tmpFile);

        trackImporter = new TrackImporter(context, contentProviderUtils, Distance.of(10), Distance.of(200), true);
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

        Distance sensorDistance = hasSensorDistance ? Distance.of(5) : null;

        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 14, 10, 15, 10, 1, 66, 3, 50, sensorDistance), 0);
        service.insertMarker("Marker 1", "Marker 1 category", "Marker 1 desc", null);
        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 14.001, 10, 15, 10, 0, 66, 3, 50, sensorDistance), 0);
        service.newTrackPoint(createTrackPoint(System.currentTimeMillis(), 3, 14.002, 10, 15, 10, 0, 66, 3, 50, sensorDistance), 0);
        service.insertMarker("Marker 2", "Marker 2 category", "Marker 2 desc", null);
        service.pauseCurrentTrack();

        service.resumeCurrentTrack();

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
        if (trackId != null) {
            contentProviderUtils.deleteTrack(context, trackId);
        }
        if (importTrackId != null) {
            contentProviderUtils.deleteTrack(context, importTrackId);
        }
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
        XMLImporter importer = new XMLImporter(new KmlTrackImporter(context, trackImporter));
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals(track.getCategory(), importedTrack.getCategory());
        assertEquals(track.getDescription(), importedTrack.getDescription());
        assertEquals(track.getName(), importedTrack.getName());
        assertEquals(track.getIcon(), importedTrack.getIcon());

        // 2. trackpoints
        TrackPointAssert a = new TrackPointAssert()
                .noAccuracy();
        a.assertEquals(trackPoints, TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId));

        // 2. trackstatistics
        assertTrackStatistics(false, true);

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
        KmzTrackImporter importer = new KmzTrackImporter(context, trackImporter);
        importTrackId = importer.importFile(tmpFileUri).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals(track.getCategory(), importedTrack.getCategory());
        assertEquals(track.getDescription(), importedTrack.getDescription());
        assertEquals(track.getName(), importedTrack.getName());
        assertEquals(track.getIcon(), importedTrack.getIcon());

        // 2. trackpoints
        TrackPointAssert a = new TrackPointAssert()
                .noAccuracy();
        a.assertEquals(trackPoints, TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId));

        // 2. trackstatistics
        assertTrackStatistics(false, true);

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
        XMLImporter importer = new XMLImporter(new KmlTrackImporter(context, trackImporter));
        importTrackId = importer.importFile(inputStream).get(0);

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
        XMLImporter importer = new XMLImporter(new GpxTrackImporter(context, trackImporter));
        importTrackId = importer.importFile(inputStream).get(0);

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

        TrackPointAssert a = new TrackPointAssert()
                .setDelta(0.05)
                .noAccuracy(); // speed is not fully
        a.assertEquals(trackPointsWithCoordinates, TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId));

        // 3. trackstatistics
        assertTrackStatistics(true, false);

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
        XMLImporter importer = new XMLImporter(new GpxTrackImporter(context, trackImporter));
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 1. track
        Track trackImported = contentProviderUtils.getTrack(importTrackId);
        assertNull(trackImported);
    }

    private void assertMarkers() {
        assertEquals(markers.size(), contentProviderUtils.getMarkers(importTrackId).size());

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
            assertEquals(marker.getLocation().getAltitude(), importMarker.getLocation().getAltitude(), 0.1);
        }
    }

    private void assertTrackStatistics(boolean isGpx, boolean verifyDistance) {
        double delta = isGpx ? 0.1 : 0.01;
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
        assertEquals(trackStatistics.getMinAltitude(), importedTrackStatistics.getMinAltitude(), delta);
        if (isGpx) {
            assertEquals(trackStatistics.getMaxAltitude(), importedTrackStatistics.getMaxAltitude(), 2);
        } else {
            assertEquals(trackStatistics.getMaxAltitude(), importedTrackStatistics.getMaxAltitude(), delta);
        }
        assertEquals(trackStatistics.getTotalAltitudeGain(), importedTrackStatistics.getTotalAltitudeGain(), delta);
        assertEquals(trackStatistics.getTotalAltitudeLoss(), importedTrackStatistics.getTotalAltitudeLoss(), delta);
    }

    private static TrackPoint createTrackPoint(long time, double latitude, double longitude, float accuracy, float speed, float altitude, float altitudeGain, float heartRate, float cyclingCadence, float power, Distance distance) {
        TrackPoint tp = new TrackPoint(latitude, longitude, Altitude.WGS84.of(altitude), Instant.ofEpochMilli(time));
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