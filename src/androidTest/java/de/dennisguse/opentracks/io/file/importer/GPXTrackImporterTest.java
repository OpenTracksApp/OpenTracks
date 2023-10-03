package de.dennisguse.opentracks.io.file.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * Test that legacy KML/GPX formats can still be imported.
 */
@RunWith(JUnit4.class)
public class GPXTrackImporterTest {

    private final Context context = ApplicationProvider.getApplicationContext();
    private final ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

    private TrackImporter trackImporter;

    private Track.Id importTrackId;

    @Before
    public void setUp() {
        trackImporter = new TrackImporter(context, contentProviderUtils, Distance.of(200), true);
    }

    @After
    public void tearDown() {
        if (importTrackId != null) {
            contentProviderUtils.deleteTrack(context, importTrackId);
        }
    }

    /**
     * Check that data that contains pause (lat=100, lng=0) and resume (lat=200, lng=0) locations are restored to a segment break.
     */
    @LargeTest
    @Test
    public void gpx_with_pause_resume() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new GpxTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.test.R.raw.legacy_gpx_pause_resume);

        // when
        // 1. import
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 2. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals(ActivityType.UNKNOWN, importedTrack.getActivityType());
        assertEquals("the category", importedTrack.getActivityTypeLocalized());
        assertEquals("the description", importedTrack.getDescription());
        assertEquals("2021-01-07 22:51", importedTrack.getName());
        assertEquals(ActivityType.UNKNOWN, importedTrack.getActivityType());

        //TODO Check trackstatistics

        // 3. trackpoints
        List<TrackPoint> importedTrackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
        assertEquals(6, importedTrackPoints.size());

        // first segment
        TrackPointAssert a = new TrackPointAssert();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC, Instant.parse("2021-01-07T21:51:59.179Z"))
                        .setLatitude(3)
                        .setLongitude(14)
                        .setAltitude(10),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2021-01-07T21:52:00.653Z"))
                        .setLatitude(3)
                        .setLongitude(14.001)
                        .setAltitude(10)
                        .setSpeed(Speed.of(75.4192)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2021-01-07T21:52:01.010Z"))
                        .setLatitude(3)
                        .setLongitude(14.002)
                        .setAltitude(10)
                        .setSpeed(Speed.of(311.3948)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse("2021-01-07T21:52:02.658Z")),

                // created resume trackpoint with time of next valid trackpoint
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse("2021-01-07T21:52:03.873Z")),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2021-01-07T21:52:04.103Z"))
                        .setLatitude(3)
                        .setLongitude(14.003)
                        .setAltitude(10)
        ), importedTrackPoints);
    }

    @LargeTest
    @Test
    public void gpx_without_speed() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new GpxTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.test.R.raw.gpx11_without_speed);

        // when
        // 1. import
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 2. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals("", importedTrack.getActivityTypeLocalized());
        assertEquals("", importedTrack.getDescription());
        assertEquals("20210907_213924.gpx", importedTrack.getName());
        assertEquals(ActivityType.UNKNOWN, importedTrack.getActivityType());

        // 3. trackstatistics
        TrackStatistics trackStatistics = importedTrack.getTrackStatistics();
        assertEquals(0.75, trackStatistics.getMaxSpeed().toMPS(), 0.01);
        assertEquals(Duration.ofSeconds(101), trackStatistics.getMovingTime());

        // 4. trackpoints
        List<TrackPoint> importedTrackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
        assertEquals(3, importedTrackPoints.size());

        TrackPointAssert a = new TrackPointAssert();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC, Instant.parse("2021-09-07T22:10:19Z"))
                        .setLatitude(30.14185982)
                        .setLongitude(-40.3863038)
                        .setAltitude(-5),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2021-09-07T22:11:07Z"))
                        .setLatitude(30.14184657)
                        .setLongitude(-40.38670089)
                        .setAltitude(-5)
                        .setSpeed(Speed.of(0.7976524233818054)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2021-09-07T22:12:00Z"))
                        .setLatitude(30.14185982)
                        .setLongitude(-40.3863038)
                        .setAltitude(-5)
                        .setSpeed(Speed.of(0.7224021553993225))
        ), importedTrackPoints);
    }

    @LargeTest
    @Test
    public void gpx_speed_no_namespace() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new GpxTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.test.R.raw.gpx11_with_speed_no_namespace);

        // when
        // 1. import
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 2. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals("", importedTrack.getActivityTypeLocalized());
        assertEquals("", importedTrack.getDescription());
        assertEquals("20210907_213924.gpx", importedTrack.getName());
        assertEquals(ActivityType.UNKNOWN, importedTrack.getActivityType());

        // 3. trackstatistics
        TrackStatistics trackStatistics = importedTrack.getTrackStatistics();
        assertEquals(5.0, trackStatistics.getMaxSpeed().toMPS(), 0.01);
        assertEquals(Duration.ofSeconds(101), trackStatistics.getMovingTime());

        // 4. trackpoints
        List<TrackPoint> importedTrackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
        assertEquals(3, importedTrackPoints.size());

        TrackPointAssert a = new TrackPointAssert();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC, Instant.parse("2021-09-07T22:10:19Z"))
                        .setLatitude(30.14185982)
                        .setLongitude(-40.3863038)
                        .setAltitude(-5)
                        .setSpeed(Speed.of(5)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2021-09-07T22:11:07Z"))
                        .setLatitude(30.14184657)
                        .setLongitude(-40.38670089)
                        .setAltitude(-5)
                        .setSpeed(Speed.of(0.7976524233818054))
                        .setSpeed(Speed.of(4)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2021-09-07T22:12:00Z"))
                        .setLatitude(30.14185982)
                        .setLongitude(-40.3863038)
                        .setAltitude(-5)
                        .setSpeed(Speed.of(3))
        ), importedTrackPoints);
    }

    @LargeTest
    @Test
    public void importExportTest_timezone() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new GpxTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.test.R.raw.gpx_timezone);
        InputStream inputStreamExpected = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.test.R.raw.gpx_timezone);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // when
        // 1. import
        importTrackId = importer.importFile(inputStream).get(0);
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);

        TrackExporter trackExporter = TrackFileFormat.GPX.createTrackExporter(context, contentProviderUtils);
        trackExporter.writeTrack(List.of(importedTrack), outputStream);

        // then
        String expected = new BufferedReader(
                new InputStreamReader(inputStreamExpected, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n")) + "\n";

        assertEquals(expected, outputStream.toString()); //TODO inputStream.readAllBytes() ?
    }
}