package de.dennisguse.opentracks.io.file.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TimezoneRule;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.sensors.AltitudeSumManager;
import de.dennisguse.opentracks.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCycling;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingPower;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataHeartRate;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceTestUtils;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * Export a track to {@link TrackFileFormat} and verify that the import is identical.
 */
@RunWith(AndroidJUnit4.class)
public class ExportImportTest {

    private static final String TAG = ExportImportTest.class.getSimpleName();

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    //For csv_export_only() as we the timezone is hardcoded in the expectation.
    @Rule
    public TimezoneRule timezoneRule = new TimezoneRule(TimeZone.getTimeZone("Europe/Berlin"));

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
    public void fileSetup() throws IOException, TimeoutException {
        TrackRecordingServiceTestUtils.resetService(mServiceRule, context);

        tmpFile = File.createTempFile("test", "test", context.getFilesDir());
        tmpFileUri = Uri.fromFile(tmpFile);

        trackImporter = new TrackImporter(context, contentProviderUtils, Distance.of(200), true);

        TrackRecordingServiceTestUtils.resetService(mServiceRule, context);
    }

    @After
    public void tearDown() throws TimeoutException {
        tmpFile.deleteOnExit();
        tmpFileUri = null;

        // Ensure that the database is empty after every test
        contentProviderUtils.deleteAllTracks(context);

        TrackRecordingServiceTestUtils.resetService(mServiceRule, context);
    }

    public void setUp() throws TimeoutException {
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(new Intent(context, TrackRecordingService.class)))
                .getService();

        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock("2020-02-02T02:02:02Z");
        trackId = service.startNewTrack();

        Distance sensorDistance = Distance.of(10); // recording distance interval

        sendLocation(trackPointCreator, "2020-02-02T02:02:03Z", 3, 14, 10, 13, 15, 10, 1);
        service.insertMarker("Marker 1", "Marker 1 category", "Marker 1 desc", null);

        // A sensor-only TrackPoint
        trackPointCreator.setClock("2020-02-02T02:02:04Z");
        mockAltitudeChange(trackPointCreator, 1);
        mockBLESensorData(trackPointCreator, 15f, sensorDistance, 66f, 3f, 50f);

        trackPointCreator.setClock("2020-02-02T02:02:05Z");
        mockBLESensorData(trackPointCreator, 5f, Distance.of(2), 66f, 3f, 50f); // Distance will be added to next TrackPoint

        sendLocation(trackPointCreator, "2020-02-02T02:02:05Z", 3, 14.001, 10, 13, 15, 10, 0);
        service.insertMarker("Marker 2", "Marker 2 category", "Marker 2 desc", null);

        trackPointCreator.setClock("2020-02-02T02:02:06Z");
        trackPointCreator.setRemoteSensorManager(new BluetoothRemoteSensorManager(context, null, trackPointCreator));
        service.endCurrentTrack();

        trackPointCreator.setClock("2020-02-02T02:02:20Z");
        service.resumeTrack(trackId);

        sendLocation(trackPointCreator, "2020-02-02T02:02:21Z", 3, 14.002, 10, 13, 15, 10, 0);

        sendLocation(trackPointCreator, "2020-02-02T02:02:22Z", 3, 16, 10, 13, 15, 10, 0);

        sendLocation(trackPointCreator, "2020-02-02T02:02:23Z", 3, 16.001, 10, 27, 15, 10, 0);

        trackPointCreator.setClock("2020-02-02T02:02:24Z");
        trackPointCreator.setRemoteSensorManager(new BluetoothRemoteSensorManager(context, null, trackPointCreator));
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

    //TODO Does not test images
    @LargeTest
    @Test
    public void kmz_with_trackdetail_and_sensordata() throws TimeoutException, IOException {
        setUp();

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
        TrackPointAssert a = new TrackPointAssert();
        List<TrackPoint> actual = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
        a.assertEquals(trackPoints, actual);

        // 3. trackstatistics
        TrackStatistics importedTrackStatistics = importedTrack.getTrackStatistics();

        // Time
        assertEquals(track.getZoneOffset(), importedTrack.getZoneOffset());
        assertEquals(Instant.parse("2020-02-02T02:02:02Z"), importedTrackStatistics.getStartTime());
        assertEquals(Instant.parse("2020-02-02T02:02:24Z"), importedTrackStatistics.getStopTime());

        TrackStatistics originalTrackStatistics = track.getTrackStatistics();

        assertEquals(originalTrackStatistics.getTotalTime(), importedTrackStatistics.getTotalTime());
        assertEquals(Duration.ofSeconds(7), importedTrackStatistics.getTotalTime());

        assertEquals(originalTrackStatistics.getMovingTime(), importedTrackStatistics.getMovingTime());
        assertEquals(Duration.ofSeconds(3), importedTrackStatistics.getMovingTime());

        // Distance
        assertEquals(originalTrackStatistics.getTotalDistance(), importedTrackStatistics.getTotalDistance());
        assertEquals(123.16, importedTrackStatistics.getTotalDistance().toM(), 0.01);

        // Speed
        assertEquals(originalTrackStatistics.getMaxSpeed(), importedTrackStatistics.getMaxSpeed());
        assertEquals(41.05, importedTrackStatistics.getMaxSpeed().toMPS(), 0.01);

        assertEquals(originalTrackStatistics.getAverageSpeed(), importedTrackStatistics.getAverageSpeed());
        assertEquals(17.59, importedTrackStatistics.getAverageSpeed().toMPS(), 0.01);

        assertEquals(originalTrackStatistics.getAverageMovingSpeed(), importedTrackStatistics.getAverageMovingSpeed());
        assertEquals(41.05, importedTrackStatistics.getMaxSpeed().toMPS(), 0.01);

        // Altitude
        assertEquals(originalTrackStatistics.getMinAltitude(), importedTrackStatistics.getMinAltitude(), 0.01);
        assertEquals(10, importedTrackStatistics.getMinAltitude(), 0.01);

        assertEquals(originalTrackStatistics.getMaxAltitude(), importedTrackStatistics.getMaxAltitude(), 0.01);
        assertEquals(10, importedTrackStatistics.getMaxAltitude(), 0.01);

        assertEquals(originalTrackStatistics.getTotalAltitudeGain(), importedTrackStatistics.getTotalAltitudeGain(), 0.01);
        assertEquals(2, importedTrackStatistics.getTotalAltitudeGain(), 0.01);

        assertEquals(originalTrackStatistics.getTotalAltitudeLoss(), importedTrackStatistics.getTotalAltitudeLoss(), 0.01);
        assertEquals(2, importedTrackStatistics.getTotalAltitudeLoss(), 0.01);

        // 4. markers
        assertMarkers();
    }

    @LargeTest
    @Test(expected = ImportAlreadyExistsException.class)
    public void kml_with_trackdetail_and_sensordata_duplicate_trackUUID() throws TimeoutException, IOException {
        setUp();

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
        setUp();

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

        TrackPointAssert a = new TrackPointAssert()
                .setDelta(0.05); // speed is not fully
        List<TrackPoint> actual = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC, Instant.parse("2020-02-02T02:02:03Z"))
                        .setLatitude(3)
                        .setLongitude(14)
                        .setAltitude(10)
                        .setSpeed(Speed.of(15))
                        .setAltitudeLoss(1f)
                        .setAltitudeGain(1f)
                        .setHorizontalAccuracy(Distance.of(10)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2020-02-02T02:02:05Z"))
                        .setLatitude(3)
                        .setLongitude(14.001)
                        .setAltitude(10)
                        .setSpeed(Speed.of(5))
                        .setAltitudeLoss(1f)
                        .setAltitudeGain(1f)
                        .setSensorDistance(Distance.of(12))
                        .setHeartRate(66f)
                        .setPower(50f)
                        .setCadence(3f)
                        .setHorizontalAccuracy(Distance.of(10)),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC, Instant.parse("2020-02-02T02:02:21Z"))
                        .setLatitude(3)
                        .setLongitude(14.002)
                        .setAltitude(10)
                        .setAltitudeLoss(0f)
                        .setAltitudeGain(0f)
                        .setSpeed(Speed.of(15))
                        .setHorizontalAccuracy(Distance.of(10)),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC, Instant.parse("2020-02-02T02:02:22Z"))
                        .setLatitude(3)
                        .setLongitude(16)
                        .setAltitude(10)
                        .setAltitudeLoss(0f)
                        .setAltitudeGain(0f)
                        .setSpeed(Speed.of(15))
                        .setHorizontalAccuracy(Distance.of(10)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2020-02-02T02:02:23Z"))
                        .setLatitude(3)
                        .setLongitude(16.001)
                        .setAltitude(10)
                        .setAltitudeLoss(0f)
                        .setAltitudeGain(0f)
                        .setSpeed(Speed.of(15))
                        .setHorizontalAccuracy(Distance.of(10))
        ), actual);

        // 3. trackstatistics
        TrackStatistics trackStatistics = track.getTrackStatistics();
        TrackStatistics importedTrackStatistics = importedTrack.getTrackStatistics();

        // Time
        assertEquals(track.getZoneOffset(), importedTrack.getZoneOffset());
        assertEquals(Instant.parse("2020-02-02T02:02:03Z"), importedTrackStatistics.getStartTime());
        assertEquals(Instant.parse("2020-02-02T02:02:23Z"), importedTrackStatistics.getStopTime());

        assertEquals(Duration.ofSeconds(3), importedTrackStatistics.getTotalTime());
        assertEquals(Duration.ofSeconds(3), importedTrackStatistics.getMovingTime());

        // Distance
        assertEquals(123.16, importedTrackStatistics.getTotalDistance().toM(), 0.01);

        // Speed
        assertEquals(41.05, importedTrackStatistics.getMaxSpeed().toMPS(), 0.01);
        assertEquals(41.05, importedTrackStatistics.getAverageSpeed().toMPS(), 0.01);
        assertEquals(41.05, importedTrackStatistics.getAverageMovingSpeed().toMPS(), 0.01);

        // Altitude
        assertEquals(10, importedTrackStatistics.getMinAltitude(), 0.01);
        assertEquals(10, importedTrackStatistics.getMaxAltitude(), 0.01);
        assertEquals(2, importedTrackStatistics.getTotalAltitudeGain(), 0.01);
        assertEquals(2, importedTrackStatistics.getTotalAltitudeLoss(), 0.01);

        // 4. markers
        assertMarkers();
    }

    @LargeTest
    @Test(expected = ImportAlreadyExistsException.class)
    public void gpx_duplicate_trackUUID() throws TimeoutException, IOException {
        setUp();

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

    @Ignore(value = "TODO Fails on CI; works on API24 and API30 locally")
    @LargeTest
    @Test
    public void csv_export_only() throws TimeoutException, IOException {
        setUp();

        // given
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.CSV.createTrackExporter(context);

        // when
        // 1. export
        trackExporter.writeTrack(track, context.getContentResolver().openOutputStream(tmpFileUri));
        contentProviderUtils.deleteTrack(context, trackId);

        // then
        InputStream expected = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.debug.test.R.raw.csv_export);
        String expectedText = new BufferedReader(new InputStreamReader(expected, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));


        InputStream actual = context.getContentResolver().openInputStream(tmpFileUri);
        String actualText = new BufferedReader(new InputStreamReader(actual, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        assertEquals(expectedText, actualText);
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

    private void mockBLESensorData(TrackPointCreator trackPointCreator, Float speed, Distance distance, float heartRate, float cadence, Float power) {
        trackPointCreator.setRemoteSensorManager(new BluetoothRemoteSensorManager(context, null, trackPointCreator) {
            @Override
            public SensorDataSet fill(@NonNull TrackPoint trackPoint) {
                SensorDataSet sensorDataSet = new SensorDataSet();
                sensorDataSet.set(new SensorDataCyclingPower("power", "power", Power.of(power)));
                sensorDataSet.set(new SensorDataHeartRate("heartRate", "heartRate", HeartRate.of(heartRate)));

                SensorDataCycling.CyclingCadence cyclingCadence = Mockito.mock(SensorDataCycling.CyclingCadence.class);
                Mockito.when(cyclingCadence.hasValue()).thenReturn(true);
                Mockito.when(cyclingCadence.getValue()).thenReturn(Cadence.of(cadence));
                sensorDataSet.set(cyclingCadence);

                if (distance != null && speed != null) {
                    SensorDataCycling.DistanceSpeed.Data distanceSpeedData = Mockito.mock(SensorDataCycling.DistanceSpeed.Data.class);
                    Mockito.when(distanceSpeedData.getDistanceOverall()).thenReturn(distance);
                    Mockito.when(distanceSpeedData.getSpeed()).thenReturn(Speed.of(speed));
                    SensorDataCycling.DistanceSpeed distanceSpeed = Mockito.mock(SensorDataCycling.DistanceSpeed.class);
                    Mockito.when(distanceSpeed.hasValue()).thenReturn(true);
                    Mockito.when(distanceSpeed.getValue()).thenReturn(distanceSpeedData);
                    sensorDataSet.set(distanceSpeed);
                }

                sensorDataSet.fillTrackPoint(trackPoint);
                return sensorDataSet;
            }
        });
        trackPointCreator.onChange(new SensorDataSet());
    }

    private void mockAltitudeChange(TrackPointCreator trackPointCreator, float altitudeGain) {
        AltitudeSumManager altitudeSumManager = trackPointCreator.getAltitudeSumManager();
        altitudeSumManager.setAltitudeGain_m(altitudeGain);
        altitudeSumManager.setAltitudeLoss_m(altitudeGain);
    }

    private void sendLocation(TrackPointCreator trackPointCreator, String time, double latitude, double longitude, float accuracy, float verticalAccuracy, float speed, float altitude, float altitudeGain) {
        Location location = new Location("mock");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            location.setVerticalAccuracyMeters(verticalAccuracy);
        }
        location.setSpeed(speed);
        location.setAltitude(altitude);

        mockAltitudeChange(trackPointCreator, altitudeGain);

        trackPointCreator.setClock(time);
        trackPointCreator.getGpsHandler().onLocationChanged(location);
    }
}