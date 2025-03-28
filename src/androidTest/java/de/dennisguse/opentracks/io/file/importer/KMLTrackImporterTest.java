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

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;

/**
 * Test that legacy KML/GPX formats can still be imported.
 */
@RunWith(JUnit4.class)
public class KMLTrackImporterTest {

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
     * KML does not specify the order of gx:coords and when; only count has to be the same.
     */
    @LargeTest
    @Test
    public void kml22_order_location_and_when() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new KMLTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.test.R.raw.kml22_order_location_and_when);

        // when
        // 1. import
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals("", importedTrack.getActivityTypeLocalized());
        assertEquals("", importedTrack.getDescription());
        assertEquals("", importedTrack.getName());
        assertEquals(ActivityType.UNKNOWN, importedTrack.getActivityType());

        // 2. markers
        assertEquals(0, contentProviderUtils.getMarkers(importTrackId).size());

        // 3. trackpoints
        List<TrackPoint> importedTrackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse("2021-05-29T18:06:21.767Z")),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2021-05-29T18:06:22.042Z"),
                                3d, 14d, null,
                                Altitude.WGS84.of(10), null,
                                null,
                                null)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2021-05-29T18:06:22.192Z"),
                                3d, 14.001, null,
                                Altitude.WGS84.of(10), null,
                                null,
                                Speed.of(741.1196)
                        )),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2021-05-29T18:06:22.318Z"),
                                3d, 14.002, null,
                                Altitude.WGS84.of(10), null,
                                null,
                                Speed.of(882.2853))),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse("2021-05-29T18:06:22.512Z"))
        ), importedTrackPoints);
    }

    /**
     * Coordinates / when might not be ordered by increasing time.
     */
    @LargeTest
    @Test
    public void kml22_time_decreases() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new KMLTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.test.R.raw.kml22_time_decreases);

        // when
        // 1. import
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals("", importedTrack.getActivityTypeLocalized());
        assertEquals("", importedTrack.getDescription());
        assertEquals("", importedTrack.getName());
        assertEquals(ActivityType.UNKNOWN, importedTrack.getActivityType());

        // 2. markers
        assertEquals(0, contentProviderUtils.getMarkers(importTrackId).size());

        // 3. trackpoints
        List<TrackPoint> importedTrackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);

        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(Instant.parse("2021-05-29T18:06:21.767Z"),
                                3d, 14d, null,
                                Altitude.WGS84.of(10), null,
                                null,
                                null)),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse("2021-05-29T18:06:22.042Z"))
        ), importedTrackPoints);
    }

    /**
     * Check that data with statistics markers is imported and those ignored.
     * Statistics marker were created to avoid recomputing the track statistics (e.g., distance until a certain time).
     */
    @LargeTest
    @Test
    public void kml22_with_statistics_marker() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new KMLTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.test.R.raw.legacy_kml22_statistics_marker);

        // when
        // 1. import
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals("unknown", importedTrack.getActivityTypeLocalized());
        assertEquals("Test Track", importedTrack.getDescription());
        assertEquals("2020-11-28 18:06", importedTrack.getName());
        assertEquals(ActivityType.UNKNOWN, importedTrack.getActivityType());

        // 2. markers
        assertEquals(1, contentProviderUtils.getMarkers(importTrackId).size());

        // 3. trackpoints
        List<TrackPoint> importedTrackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);

        new TrackPointAssert().assertEquals(List.of(
                // first 3 trackpoints
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC,
                        new Position(
                                Instant.parse("2020-11-28T17:06:22.401Z"),
                                12.340097, 1.234156, null,
                                Altitude.WGS84.of(469.286376953125), null,
                                null,
                                Speed.of(0.539)))
                        .setAltitudeGain(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2020-11-28T17:06:25.448Z"),
                                12.340036, 1.23415, null,
                                Altitude.WGS84.of(439.1626281738281), null,
                                null,
                                Speed.of(0.1577)))
                        .setAltitudeGain(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2020-11-28T17:06:47.888Z"),
                                12.340057, 1.23405, null,
                                Altitude.WGS84.of(421.8070983886719), null,
                                null,
                                Speed.of(0)))
                        .setAltitudeGain(0f),
                // created resume trackpoint with time of next valid trackpoint
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC,
                        new Position(
                                Instant.parse("2020-11-28T17:06:55.861Z"),
                                12.340057, 1.23405, null,
                                Altitude.WGS84.of(419.93902587890625), null,
                                null,
                                Speed.of(0)))
                        .setAltitudeGain(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2020-11-28T17:06:56.905Z"),
                                12.340057, 1.23405, null,
                                Altitude.WGS84.of(419.9036560058594), null,
                                null,
                                Speed.of(0)))
                        .setAltitudeGain(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse("2020-11-28T17:07:20.870Z"),
                                12.340082, 1.234046, null,
                                Altitude.WGS84.of(417.99432373046875), null,
                                null,
                                Speed.of(0)))
                        .setAltitudeGain(0f)
        ), importedTrackPoints);
    }

    /**
     * At least one valid location is required.
     * Before v3.15.0, Tracks without TrackPoints could be created; such tracks cannot be imported as we cannot restore the TrackStatistics (especially startTime and stopTime).
     */
    @LargeTest
    @Test(expected = ImportParserException.class)
    public void kml_without_locations() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new KMLTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.test.R.raw.kml22_without_locations);

        // when
        importTrackId = importer.importFile(inputStream).get(0);
    }

    @LargeTest
    @Test(expected = ImportParserException.class)
    public void kml_when_locations_different() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new KMLTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.test.R.raw.kml22_when_locations_different);

        // when
        importTrackId = importer.importFile(inputStream).get(0);
    }
}