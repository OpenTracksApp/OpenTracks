package de.dennisguse.opentracks.io.file.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.TimezoneRule;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.AltitudeGainLoss;
import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.sensors.BluetoothHandlerManagerCyclingPower;
import de.dennisguse.opentracks.sensors.sensorData.Aggregator;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorBarometer;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorCyclingCadence;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorCyclingPower;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorHeartRate;
import de.dennisguse.opentracks.sensors.sensorData.Raw;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * Export a track to {@link TrackFileFormat} and verify that the import is identical.
 * <p>
 * Note: those tests are affected by {@link Aggregator}.isOutdated().
 * If the test device is too slow (like in a CI) these are likely to fail as the sensor data will be omitted from actual.
 */
@RunWith(AndroidJUnit4.class)
public class ExportImportTest {

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    @Rule
    public GrantPermissionRule mGrantPermissionRule = TestUtil.createGrantPermissionRule();

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

    private static final ActivityType TRACK_ACTIVITY_TYPE = ActivityType.MOUNTAIN_BIKING;
    private static final String TRACK_ACTIVITY_TYPE_LOCALIZED = "the activity type";
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

        trackImporter = new TrackImporter(context, contentProviderUtils, Distance.of(200), true);
    }

    @After
    public void tearDown() {
        tmpFile.deleteOnExit();
        tmpFileUri = null;

        // Ensure that the database is empty after every test
        contentProviderUtils.deleteAllTracks(context);
    }

    public void setUp() throws TimeoutException {
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(new Intent(context, TrackRecordingService.class)))
                .getService();

        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock("2020-02-02T02:02:02Z");
        trackId = service.startNewTrack();

        Distance sensorDistance = Distance.of(10); // recording distance interval

        sendLocation(trackPointCreator, "2020-02-02T02:02:03Z", 3.1234567, 14.0014567, 10, 13, 15, 1020.25, 1f);
        contentProviderUtils.insertMarker(new Marker(trackId, service.getLastStoredTrackPointWithLocation(), "Marker 1", "Marker 1 desc", "Marker 1 category", null, null));

        // A sensor-only TrackPoint
        trackPointCreator.setClock("2020-02-02T02:02:04Z");
        mockSensorData(trackPointCreator, 15f, sensorDistance, 66f, 3f, 50f, 1f);

        trackPointCreator.setClock("2020-02-02T02:02:14Z");
        mockSensorData(trackPointCreator, 15f, null, 67f, 3f, 50f, null);
        trackPointCreator.setClock("2020-02-02T02:02:15Z");
        mockSensorData(trackPointCreator, null, null, 68f, 3f, 50f, null);
        trackPointCreator.setClock("2020-02-02T02:02:16Z");
        mockSensorData(trackPointCreator, 5f, Distance.of(2), 69f, 3f, 50f, null); // Distance will be added to next TrackPoint

        sendLocation(trackPointCreator, "2020-02-02T02:02:17Z", 3.1234567, 14.0014567, 10, 13, 15, 1020.25, 0f);
        contentProviderUtils.insertMarker(new Marker(trackId, service.getLastStoredTrackPointWithLocation(), "Marker 2", "Marker 2 desc", "Marker 2 category", null, null));

        trackPointCreator.setClock("2020-02-02T02:02:18Z");
        trackPointCreator.getSensorManager().sensorDataSet = new SensorDataSet(trackPointCreator);
        service.endCurrentTrack();

        trackPointCreator.setClock("2020-02-02T02:03:20Z");
        service.resumeTrack(trackId);

        sendLocation(trackPointCreator, "2020-02-02T02:03:21Z", 3.1234567, 14.0024567, 10, 13, 15, 999.123, 0f);

        sendLocation(trackPointCreator, "2020-02-02T02:03:22Z", 3.1234567, 16, 10, 13, 15, 999.123, 0f);

        trackPointCreator.setClock("2020-02-02T02:03:30Z");
        service.getTrackRecordingManager().onIdle();

        sendLocation(trackPointCreator, "2020-02-02T02:03:50Z", 3.1234567, 16.001, 10, 27, 15, 999.123, 0f);

        trackPointCreator.getSensorManager().sensorDataSet = new SensorDataSet(trackPointCreator);
        trackPointCreator.setClock("2020-02-02T02:04:00Z");
        service.endCurrentTrack();

        Track track = contentProviderUtils.getTrack(trackId);
        track.setActivityType(TRACK_ACTIVITY_TYPE);
        track.setActivityTypeLocalized(TRACK_ACTIVITY_TYPE_LOCALIZED);
        track.setDescription(TRACK_DESCRIPTION);
        contentProviderUtils.updateTrack(track);

        track = contentProviderUtils.getTrack(trackId);
        trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        markers = contentProviderUtils.getMarkers(trackId);
    }

    @LargeTest
    @Test
    public void track() throws TimeoutException {
        setUp();

        Track track = contentProviderUtils.getTrack(trackId);
        TrackStatistics trackStatistics = track.getTrackStatistics();

        assertEquals(ZoneOffset.of("+01:00"), track.getZoneOffset());
        assertEquals(Instant.parse("2020-02-02T02:02:02Z"), trackStatistics.getStartTime());
        assertEquals(Instant.parse("2020-02-02T02:04:00Z"), trackStatistics.getStopTime());

        assertEquals(Duration.ofSeconds(56), trackStatistics.getTotalTime());
        assertEquals(Duration.ofSeconds(26), trackStatistics.getMovingTime()); //TODO Likely too low

        // Distance
        assertEquals(222049.34375, trackStatistics.getTotalDistance().toM(), 0.01); //TODO Too low

        // Speed
        assertEquals(8540.359, trackStatistics.getMaxSpeed().toMPS(), 0.01);
        assertEquals(3965.166, trackStatistics.getAverageSpeed().toMPS(), 0.01);
        assertEquals(8540.359, trackStatistics.getAverageMovingSpeed().toMPS(), 0.01);

        // Altitude
        assertEquals(999.122, trackStatistics.getMinAltitude(), 0.01);
        assertEquals(1020.25, trackStatistics.getMaxAltitude(), 0.01);

        assertEquals(2, trackStatistics.getTotalAltitudeGain(), 0.01);
        assertEquals(2, trackStatistics.getTotalAltitudeLoss(), 0.01);

        List<TrackPoint> actual = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse("2020-02-02T02:02:02Z")),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2020-02-02T02:02:03Z"),
                                3.123456, 14.001456, Distance.of(10),
                                Altitude.WGS84.of(1020.25), null,
                                null,
                                Speed.of(15)))
                        .setAltitudeLoss(1f)
                        .setAltitudeGain(1f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        Instant.parse("2020-02-02T02:02:04Z"))
                        .setSensorDistance(Distance.of(10))
                        .setSpeed(Speed.of(15))
                        .setHeartRate(HeartRate.of(66))
                        .setCadence(3)
                        .setPower(50)
                        .setAltitudeLoss(1f)
                        .setAltitudeGain(1f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2020-02-02T02:02:15Z"))
                        .setHeartRate(HeartRate.of(68))
                        .setCadence(3)
                        .setPower(50),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2020-02-02T02:02:17Z"),
                                3.123456, 14.001456, Distance.of(10),
                                Altitude.WGS84.of(1020.25), null,
                                null,
                                Speed.of(5)))
                        .setSensorDistance(Distance.of(2))
                        .setAltitudeLoss(0f)
                        .setAltitudeGain(0f)
                        .setHeartRate(HeartRate.of(69))
                        .setCadence(3)
                        .setPower(50),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse("2020-02-02T02:02:18Z")),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse("2020-02-02T02:03:20Z")),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2020-02-02T02:03:21Z"),
                                3.123456, 14.002456, Distance.of(10),
                                Altitude.WGS84.of(999.1229858398438), null,
                                null,
                                Speed.of(15)))
                        .setAltitudeLoss(0f)
                        .setAltitudeGain(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC,
                        new Position(Instant.parse("2020-02-02T02:03:22Z"),
                                3.123456, 16d, Distance.of(10),
                                Altitude.WGS84.of(999.1229858398438), null,
                                null,
                                Speed.of(15)))
                        .setAltitudeLoss(0f)
                        .setAltitudeGain(0f),
                new TrackPoint(TrackPoint.Type.IDLE, Instant.parse("2020-02-02T02:03:30Z"))
                        .setAltitudeLoss(0f)
                        .setAltitudeGain(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2020-02-02T02:03:50Z"),
                                3.123456, 16.001, Distance.of(10),
                                Altitude.WGS84.of(999.1229858398438), null,
                                null, Speed.of(15)))
                        .setAltitudeLoss(0f)
                        .setAltitudeGain(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse("2020-02-02T02:04:00Z"))
        ), actual);
    }

    //TODO Does not test marker images
    @LargeTest
    @Test
    public void kmz_with_trackdetail_and_sensordata() throws TimeoutException, IOException {
        setUp();

        // given
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES.createTrackExporter(context, contentProviderUtils);

        // when
        // 1. export
        trackExporter.writeTrack(List.of(track), context.getContentResolver().openOutputStream(tmpFileUri));
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        KMZTrackImporter importer = new KMZTrackImporter(context, trackImporter);
        importTrackId = importer.importFile(tmpFileUri).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals(track.getActivityType(), importedTrack.getActivityType());
        assertEquals(track.getActivityTypeLocalized(), importedTrack.getActivityTypeLocalized());
        assertEquals(track.getDescription(), importedTrack.getDescription());
        assertEquals(track.getName(), importedTrack.getName());

        // 2. trackpoints
        TrackPointAssert a = new TrackPointAssert();
        List<TrackPoint> actual = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
        a.assertEquals(trackPoints, actual);

        // 3. trackstatistics
        TrackStatistics importedTrackStatistics = importedTrack.getTrackStatistics();

        // Time
        assertEquals(track.getZoneOffset(), importedTrack.getZoneOffset());
        assertEquals(Instant.parse("2020-02-02T02:02:02Z"), importedTrackStatistics.getStartTime());
        assertEquals(Instant.parse("2020-02-02T02:04:00Z"), importedTrackStatistics.getStopTime());

        assertEquals(Duration.ofSeconds(56), importedTrackStatistics.getTotalTime());
        assertEquals(Duration.ofSeconds(26), importedTrackStatistics.getMovingTime());

        // Distance
        assertEquals(222049.421, importedTrackStatistics.getTotalDistance().toM(), 0.01);

        // Speed
        assertEquals(8540.362, importedTrackStatistics.getMaxSpeed().toMPS(), 0.01);
        assertEquals(3965.168, importedTrackStatistics.getAverageSpeed().toMPS(), 0.01);
        assertEquals(8540.362, importedTrackStatistics.getAverageMovingSpeed().toMPS(), 0.01);

        // Altitude
        assertEquals(999.122, importedTrackStatistics.getMinAltitude(), 0.01);
        assertEquals(1020.25, importedTrackStatistics.getMaxAltitude(), 0.01);
        assertEquals(2, importedTrackStatistics.getTotalAltitudeGain(), 0.01);
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

        TrackExporter trackExporter = TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.createTrackExporter(context, contentProviderUtils);

        // when
        trackExporter.writeTrack(List.of(track), context.getContentResolver().openOutputStream(tmpFileUri));

        // 2. import
        InputStream inputStream = context.getContentResolver().openInputStream(tmpFileUri);
        XMLImporter importer = new XMLImporter(new KMLTrackImporter(context, trackImporter));
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

        TrackExporter trackExporter = TrackFileFormat.GPX.createTrackExporter(context, contentProviderUtils);

        // when
        // 1. export
        trackExporter.writeTrack(List.of(track), context.getContentResolver().openOutputStream(tmpFileUri));
        contentProviderUtils.deleteTrack(context, trackId);

        // 2. import
        InputStream inputStream = context.getContentResolver().openInputStream(tmpFileUri);
        XMLImporter importer = new XMLImporter(new GPXTrackImporter(context, trackImporter));
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals(track.getActivityType(), importedTrack.getActivityType());
        assertEquals(track.getActivityTypeLocalized(), importedTrack.getActivityTypeLocalized());
        assertEquals(track.getDescription(), importedTrack.getDescription());
        assertEquals(track.getName(), importedTrack.getName());

        // 2. trackpoints
        // The GPX exporter does not support exporting TrackPoints without lat/lng.
        // Therefore, the track segmentation is changes.

        TrackPointAssert a = new TrackPointAssert()
                .setDelta(0.05); // speed is not fully
        List<TrackPoint> actual = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC,
                        new Position(
                                Instant.parse("2020-02-02T02:02:03Z"),
                                3.123456, 14.001456d, Distance.of(10),
                                Altitude.WGS84.of(1020.2), null,
                                null,
                                Speed.of(15)))
                        .setAltitudeLoss(1f)
                        .setAltitudeGain(1f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2020-02-02T02:02:17Z"),
                                3.123456, 14.001456, Distance.of(10),
                                Altitude.WGS84.of(1020.2), null,
                                null,
                                Speed.of(5)))
                        .setAltitudeLoss(1f)
                        .setAltitudeGain(1f)
                        .setSensorDistance(Distance.of(12))
                        .setHeartRate(69)
                        .setPower(50f)
                        .setCadence(3f),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC,
                        new Position(
                                Instant.parse("2020-02-02T02:03:21Z"),
                                3.123456, 14.002456, Distance.of(10),
                                Altitude.WGS84.of(999.0999755859375), null,
                                null,
                                Speed.of(15)))
                        .setAltitudeLoss(0f)
                        .setAltitudeGain(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC,
                        new Position(
                                Instant.parse("2020-02-02T02:03:22Z"),
                                3.123456, 16d, Distance.of(10),
                                Altitude.WGS84.of(999.0999755859375), null,
                                null,
                                Speed.of(15)))
                        .setAltitudeLoss(0f)
                        .setAltitudeGain(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2020-02-02T02:03:50Z"),
                                3.123456, 16.001, Distance.of(10),
                                Altitude.WGS84.of(999.0999755859375), null,
                                null,
                                Speed.of(10)))
                        .setAltitudeLoss(0f)
                        .setAltitudeGain(0f)
                        .setSpeed(Speed.of(15))
        ), actual);

        // 3. trackstatistics
        TrackStatistics importedTrackStatistics = importedTrack.getTrackStatistics();

        // Time
        assertEquals(track.getZoneOffset(), importedTrack.getZoneOffset());
        assertEquals(Instant.parse("2020-02-02T02:02:03Z"), importedTrackStatistics.getStartTime());
        assertEquals(Instant.parse("2020-02-02T02:03:50Z"), importedTrackStatistics.getStopTime());

        assertEquals(Duration.ofSeconds(107), importedTrackStatistics.getTotalTime());
        assertEquals(Duration.ofSeconds(107), importedTrackStatistics.getMovingTime());

        // Distance
        assertEquals(222271.734, importedTrackStatistics.getTotalDistance().toM(), 0.01);

        // Speed
        assertEquals(2077.305, importedTrackStatistics.getMaxSpeed().toMPS(), 0.01);
        assertEquals(2077.305, importedTrackStatistics.getAverageSpeed().toMPS(), 0.01);
        assertEquals(2077.305, importedTrackStatistics.getAverageMovingSpeed().toMPS(), 0.01);

        // Altitude
        assertEquals(999.099, importedTrackStatistics.getMinAltitude(), 0.01);
        assertEquals(1020.2, importedTrackStatistics.getMaxAltitude(), 0.01);
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

        TrackExporter trackExporter = TrackFileFormat.GPX.createTrackExporter(context, contentProviderUtils);

        // when
        // 1. export
        trackExporter.writeTrack(List.of(track), context.getContentResolver().openOutputStream(tmpFileUri));

        // 2. import
        InputStream inputStream = context.getContentResolver().openInputStream(tmpFileUri);
        XMLImporter importer = new XMLImporter(new GPXTrackImporter(context, trackImporter));
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 1. track
        Track trackImported = contentProviderUtils.getTrack(importTrackId);
        assertNull(trackImported);
    }

    @LargeTest
    @Test
    public void csv_export_only() throws TimeoutException, IOException {
        setUp();

        // given
        Track track = contentProviderUtils.getTrack(trackId);

        TrackExporter trackExporter = TrackFileFormat.CSV.createTrackExporter(context, contentProviderUtils);

        // when
        // 1. export
        trackExporter.writeTrack(List.of(track), context.getContentResolver().openOutputStream(tmpFileUri));
        contentProviderUtils.deleteTrack(context, trackId);

        // then
        InputStream expected = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.test.R.raw.csv_export);
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
            assertFalse(importMarker.hasPhoto());

            assertEquals(marker.getPosition().latitude(), importMarker.getPosition().latitude(), 0.001);
            assertEquals(marker.getPosition().longitude(), importMarker.getPosition().longitude(), 0.001);
            assertEquals(marker.getPosition().altitude().toM(), importMarker.getPosition().altitude().toM(), 0.1);
        }
    }

    private void mockSensorData(TrackPointCreator trackPointCreator, Float speed, Distance distance, float heartRate, float cadence, Float power, Float altitudeGain) {
        SensorDataSet sensorDataSet = trackPointCreator.getSensorManager().sensorDataSet;

        AggregatorCyclingPower cyclingPower = new AggregatorCyclingPower("", "");
        cyclingPower.add(new Raw<>(trackPointCreator.createNow(), new BluetoothHandlerManagerCyclingPower.Data(Power.of(power), null)));
        sensorDataSet.add(cyclingPower);


        AggregatorHeartRate avgHeartRate = new AggregatorHeartRate("", "");
        avgHeartRate.add(new Raw<>(trackPointCreator.createNow(), HeartRate.of(heartRate)));
        sensorDataSet.add(avgHeartRate);

        AggregatorCyclingCadence cyclingCadence = new AggregatorCyclingCadence("", "") {
            @NonNull
            @Override
            public Cadence getAggregatedValue(Instant now) {
                return Cadence.of(cadence);
            }

            @Override
            public boolean hasReceivedData() {
                return true;
            }
        };
        sensorDataSet.add(cyclingCadence);

        if (distance != null && speed != null) {
            AggregatorCyclingDistanceSpeed distanceSpeed = Mockito.mock(AggregatorCyclingDistanceSpeed.class);
            Mockito.when(distanceSpeed.hasReceivedData()).thenReturn(true);
            Mockito.when(distanceSpeed.getAggregatedValue(Mockito.any())).thenReturn(new AggregatorCyclingDistanceSpeed.Data(null, distance, Speed.of(speed)));
            sensorDataSet.add(distanceSpeed);
        } else {
            sensorDataSet.add(new AggregatorCyclingDistanceSpeed("", ""));
        }

        mockAltitudeChange(trackPointCreator, altitudeGain);

        trackPointCreator.onChange(sensorDataSet);
    }

    private void mockAltitudeChange(TrackPointCreator trackPointCreator, Float altitudeGain) {
        SensorDataSet sensorDataSet = trackPointCreator.getSensorManager().sensorDataSet;

        if (altitudeGain != null) {
            AggregatorBarometer barometer = Mockito.mock(AggregatorBarometer.class);
            Mockito.when(barometer.hasReceivedData()).thenReturn(true);
            Mockito.when(barometer.getAggregatedValue(Mockito.any())).thenReturn(new AltitudeGainLoss(altitudeGain, altitudeGain));
            sensorDataSet.add(barometer);
        } else {
            sensorDataSet.add(new AggregatorBarometer("test", null));
        }
    }

    private void sendLocation(TrackPointCreator trackPointCreator, String time, double latitude, double longitude, float accuracy, float verticalAccuracy, float speed, double altitude, Float altitudeGain) {
        Location location = new Location("mock");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        location.setVerticalAccuracyMeters(verticalAccuracy);
        location.setSpeed(speed);
        location.setAltitude(altitude);

        mockAltitudeChange(trackPointCreator, altitudeGain);

        trackPointCreator.setClock(time);
        trackPointCreator.getSensorManager().getGpsManager().onLocationChanged(location);
    }
}