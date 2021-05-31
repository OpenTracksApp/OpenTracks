package de.dennisguse.opentracks.io.file.importer;

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
import java.util.List;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test that legacy KML/GPX formats can still be imported.
 */
@RunWith(JUnit4.class)
public class KMLImportTest {

    private final Context context = ApplicationProvider.getApplicationContext();
    private final ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

    private TrackImporter trackImporter;

    private Track.Id importTrackId;

    @Before
    public void setUp() {
        trackImporter = new TrackImporter(context, contentProviderUtils, Distance.of(10), Distance.of(200), true);
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
        XMLImporter importer = new XMLImporter(new KmlTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.debug.test.R.raw.kml22_order_location_and_when);

        // when
        // 1. import
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals("", importedTrack.getCategory());
        assertEquals("", importedTrack.getDescription());
        assertEquals("", importedTrack.getName());
        assertEquals("", importedTrack.getIcon());

        // 2. markers
        assertEquals(0, contentProviderUtils.getMarkerCount(importTrackId));

        // 3. trackpoints
        List<TrackPoint> importedTrackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
        assertEquals(5, importedTrackPoints.size());

        GPXImportTest.assertTrackpoint(importedTrackPoints.get(0), TrackPoint.Type.SEGMENT_START_MANUAL, "2021-05-29T18:06:21.767Z", null, null, null);
        GPXImportTest.assertTrackpoint(importedTrackPoints.get(1), TrackPoint.Type.TRACKPOINT, "2021-05-29T18:06:22.042Z", 14.0, 3.0, 10.0);
        GPXImportTest.assertTrackpoint(importedTrackPoints.get(2), TrackPoint.Type.TRACKPOINT, "2021-05-29T18:06:22.192Z", 14.001, 3.0, 10.0);
        GPXImportTest.assertTrackpoint(importedTrackPoints.get(3), TrackPoint.Type.TRACKPOINT, "2021-05-29T18:06:22.318Z", 14.002, 3.0, 10.0);
        GPXImportTest.assertTrackpoint(importedTrackPoints.get(4), TrackPoint.Type.SEGMENT_END_MANUAL, "2021-05-29T18:06:22.512Z", null, null, null);
    }

    /**
     * Check that data with statistics markers is imported and those ignored.
     * Statistics marker were created to avoid recomputing the track statistics (e.g., distance until a certain time).
     */
    @LargeTest
    @Test
    public void kml22_with_statistics_marker() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new KmlTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.debug.test.R.raw.legacy_kml22_statistics_marker);

        // when
        // 1. import
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals("unknown", importedTrack.getCategory());
        assertEquals("Test Track", importedTrack.getDescription());
        assertEquals("2020-11-28 18:06", importedTrack.getName());
        assertEquals("UNKNOWN", importedTrack.getIcon());

        // 2. markers
        assertEquals(0, contentProviderUtils.getMarkerCount(importTrackId));

        // 3. trackpoints
        List<TrackPoint> importedTrackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
        assertEquals(6, importedTrackPoints.size());

        // first 3 trackpoints
        GPXImportTest.assertTrackpoint(importedTrackPoints.get(0), TrackPoint.Type.SEGMENT_START_AUTOMATIC, "2020-11-28T17:06:22.401Z", 1.234156, 12.340097, 469.286376953125);
        GPXImportTest.assertTrackpoint(importedTrackPoints.get(1), TrackPoint.Type.TRACKPOINT, "2020-11-28T17:06:25.448Z", 1.23415, 12.340036, 439.1626281738281);
        GPXImportTest.assertTrackpoint(importedTrackPoints.get(2), TrackPoint.Type.TRACKPOINT, "2020-11-28T17:06:47.888Z", 1.23405, 12.340057, 421.8070983886719);

        // created resume trackpoint with time of next valid trackpoint
        GPXImportTest.assertTrackpoint(importedTrackPoints.get(3), TrackPoint.Type.SEGMENT_START_AUTOMATIC, "2020-11-28T17:06:55.861Z", 1.23405, 12.340057, 419.93902587890625);
        GPXImportTest.assertTrackpoint(importedTrackPoints.get(4), TrackPoint.Type.TRACKPOINT, "2020-11-28T17:06:56.905Z", 1.23405, 12.340057, 419.9036560058594);
        GPXImportTest.assertTrackpoint(importedTrackPoints.get(5), TrackPoint.Type.TRACKPOINT, "2020-11-28T17:07:20.870Z", 1.234046, 12.340082, 417.99432373046875);
    }

    /**
     * At least one valid location is required.
     * Before v3.15.0, Tracks without TrackPoints could be created; such tracks cannot be imported as we cannot restore the TrackStatistics (especially startTime and stopTime).
     */
    @LargeTest
    @Test(expected = ImportParserException.class)
    public void kml_without_locations() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new KmlTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.debug.test.R.raw.kml22_without_locations);

        // when
        importTrackId = importer.importFile(inputStream).get(0);
    }
}