package de.dennisguse.opentracks.io.file.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Looper;

import androidx.annotation.NonNull;
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
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
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
import de.dennisguse.opentracks.content.sensor.SensorDataCycling;
import de.dennisguse.opentracks.content.sensor.SensorDataCyclingPower;
import de.dennisguse.opentracks.content.sensor.SensorDataHeartRate;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.services.sensors.AltitudeSumManager;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * Export a track to {@link TrackFileFormat} and verify that the import is identical.
 */
@RunWith(AndroidJUnit4.class)
public class ExportImportTest {

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
    public void FileTearDown() throws TimeoutException {
        tmpFile.deleteOnExit();
        tmpFileUri = null;

        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(new Intent(context, TrackRecordingService.class)))
                .getService();
        service.getTrackPointCreator().setClock(Clock.systemUTC());
    }

    public void setUp(boolean hasSensorDistance) throws TimeoutException {
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(new Intent(context, TrackRecordingService.class)))
                .getService();

        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:02Z"), ZoneId.of("CET")));
        trackId = service.startNewTrack();

        Distance sensorDistance = hasSensorDistance ? Distance.of(5) : null;

        sendLocation(trackPointCreator, Instant.parse("2020-02-02T02:02:03Z"), 3, 14, 10, 15, 10, 1, 66, 3, 50, sensorDistance);
        service.insertMarker("Marker 1", "Marker 1 category", "Marker 1 desc", null);
        sendLocation(trackPointCreator, Instant.parse("2020-02-02T02:02:04Z"), 3, 14.001, 10, 15, 10, 0, 66, 3, 50, sensorDistance);
        sendLocation(trackPointCreator, Instant.parse("2020-02-02T02:02:05Z"), 3, 14.002, 10, 15, 10, 0, 66, 3, 50, sensorDistance);
        service.insertMarker("Marker 2", "Marker 2 category", "Marker 2 desc", null);

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:06Z"), ZoneId.of("CET")));
        trackPointCreator.setRemoteSensorManager(new BluetoothRemoteSensorManager(context));
        service.pauseCurrentTrack();

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:20Z"), ZoneId.of("CET")));
        service.resumeCurrentTrack();

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:21Z"), ZoneId.of("CET")));
        sendLocation(trackPointCreator, Instant.parse("2020-02-02T02:02:21Z"), 3, 14.003, 10, 15, 10, 0, 66, 3, 50, sensorDistance);

        sendLocation(trackPointCreator, Instant.parse("2020-02-02T02:02:22Z"), 3, 16, 10, 15, 10, 0, 66, 3, 50, sensorDistance);
        sendLocation(trackPointCreator, Instant.parse("2020-02-02T02:02:23Z"), 3, 16.001, 10, 15, 10, 0, 66, 3, 50, sensorDistance);

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:24Z"), ZoneId.of("CET")));
        trackPointCreator.setRemoteSensorManager(new BluetoothRemoteSensorManager(context));
        service.endCurrentTrack();

        Track track = contentProviderUtils.getTrack(trackId);
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

        // 3. trackstatistics
        TrackStatistics importedTrackStatistics = importedTrack.getTrackStatistics();

        // Time
        assertEquals(Instant.parse("2020-02-02T02:02:02Z"), importedTrackStatistics.getStartTime());
        assertEquals(Instant.parse("2020-02-02T02:02:24Z"), importedTrackStatistics.getStopTime());

        assertEquals(track.getTrackStatistics().getTotalTime(), importedTrackStatistics.getTotalTime());
        assertEquals(Duration.ofSeconds(8), importedTrackStatistics.getTotalTime());
        assertEquals(Duration.ofSeconds(4), importedTrackStatistics.getMovingTime());

        // Distance
        assertEquals(Distance.of(30), importedTrackStatistics.getTotalDistance());

        // Speed
        assertEquals(Speed.of(15), importedTrackStatistics.getMaxSpeed());
        assertEquals(Speed.of(3.75), importedTrackStatistics.getAverageSpeed());
        assertEquals(Speed.of(7.5), importedTrackStatistics.getAverageMovingSpeed());

        // Altitude
        assertEquals(10, importedTrackStatistics.getMinAltitude(), 0.01);
        assertEquals(10, importedTrackStatistics.getMaxAltitude(), 0.01);
        assertEquals(1, importedTrackStatistics.getTotalAltitudeGain(), 0.01);
        assertEquals(1, importedTrackStatistics.getTotalAltitudeLoss(), 0.01);

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
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals(track.getCategory(), importedTrack.getCategory());
        assertEquals(track.getDescription(), importedTrack.getDescription());
        assertEquals(track.getName(), importedTrack.getName());

        //TODO exporting and importing a track icon is not yet supported by GpxTrackWriter.
        //assertEquals(track.getIcon(), importedTrack.getIcon());

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
        TrackStatistics trackStatistics = track.getTrackStatistics();
        TrackStatistics importedTrackStatistics = importedTrack.getTrackStatistics();

        // Time
        assertEquals(Instant.parse("2020-02-02T02:02:03Z"), importedTrackStatistics.getStartTime());
        assertEquals(Instant.parse("2020-02-02T02:02:23Z"), importedTrackStatistics.getStopTime());

        assertEquals(Duration.ofSeconds(20), importedTrackStatistics.getTotalTime());
        assertEquals(Duration.ofSeconds(19), importedTrackStatistics.getMovingTime());

        // Distance
        assertEquals(Distance.of(30), importedTrackStatistics.getTotalDistance());

        // Speed
        assertEquals(Speed.of(15), importedTrackStatistics.getMaxSpeed());
        assertEquals(Speed.of(1.5), importedTrackStatistics.getAverageSpeed());
        assertEquals(Speed.of(1.5789473684210527), importedTrackStatistics.getAverageMovingSpeed());

        // Altitude
        assertEquals(10, importedTrackStatistics.getMinAltitude(), 0.01);
        assertEquals(10, importedTrackStatistics.getMaxAltitude(), 0.01);
        assertEquals(1, importedTrackStatistics.getTotalAltitudeGain(), 0.01);
        assertEquals(1, importedTrackStatistics.getTotalAltitudeLoss(), 0.01);

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

    private void sendLocation(TrackPointCreator trackPointCreator, Instant time, double latitude, double longitude, float accuracy, float speed, float altitude, float altitudeGain, float heartRate, float cyclingCadence, float power, Distance distance) {
        Location location = new Location("mock");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        location.setSpeed(speed);
        location.setAltitude(altitude);

        trackPointCreator.setAltitudeSumManager(new AltitudeSumManager() {
            @Override
            public void fill(@NonNull TrackPoint trackPoint) {
                trackPoint.setAltitudeGain(altitudeGain);
                trackPoint.setAltitudeLoss(altitudeGain);
            }
        });

        trackPointCreator.setRemoteSensorManager(new BluetoothRemoteSensorManager(context) {
            @Override
            public SensorDataSet fill(@NonNull TrackPoint trackPoint) {
                SensorDataSet sensorDataSet = new SensorDataSet();
                sensorDataSet.set(new SensorDataCyclingPower("power", "power", power));
                sensorDataSet.set(new SensorDataHeartRate("heartRate", "heartRate", heartRate));

                SensorDataCycling.Cadence cadence = Mockito.mock(SensorDataCycling.Cadence.class);
                Mockito.when(cadence.hasValue()).thenReturn(true);
                Mockito.when(cadence.getValue()).thenReturn(cyclingCadence);
                sensorDataSet.set(cadence);

                SensorDataCycling.DistanceSpeed.Data distanceSpeedData = Mockito.mock(SensorDataCycling.DistanceSpeed.Data.class);
                Mockito.when(distanceSpeedData.getDistanceOverall()).thenReturn(distance);
                Mockito.when(distanceSpeedData.getSpeed()).thenReturn(Speed.of(speed));
                SensorDataCycling.DistanceSpeed distanceSpeed = Mockito.mock(SensorDataCycling.DistanceSpeed.class);
                Mockito.when(distanceSpeed.hasValue()).thenReturn(true);
                Mockito.when(distanceSpeed.getValue()).thenReturn(distanceSpeedData);

                sensorDataSet.set(distanceSpeed);

                sensorDataSet.fillTrackPoint(trackPoint);
                return sensorDataSet;
            }
        });

        trackPointCreator.setClock(Clock.fixed(time, ZoneId.of("CET")));
        trackPointCreator.getLocationHandler().onLocationChanged(location);
    }
}