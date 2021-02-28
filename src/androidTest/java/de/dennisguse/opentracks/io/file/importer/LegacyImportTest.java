package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.InputStream;
import java.util.List;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Test that legacy KML/GPX formats can still be imported.
 */
@RunWith(JUnit4.class)
public class LegacyImportTest {

    private final Context context = ApplicationProvider.getApplicationContext();
    private final ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

    private Track.Id importTrackId;

    @After
    public void tearDown() {
        if (importTrackId != null) {
            contentProviderUtils.deleteTrack(context, importTrackId);
        }
    }

    /**
     * Check that data with statistics markers is imported and those ignored.
     * Statistics marker were created to avoid recomputing the track statistics (e.g., distance until a certain time).
     */
    @LargeTest
    @Test
    public void kml_with_statistics_marker() {
        // given
        KmlFileTrackImporter trackImporter = new KmlFileTrackImporter(context);
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.debug.test.R.raw.legacy_kml_statistics_marker);

        // when
        // 1. import
        importTrackId = trackImporter.importFile(inputStream).get(0);

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
        List<TrackPoint> importedTrackPoints = contentProviderUtils.getTrackPoints(importTrackId);
        assertEquals(6, importedTrackPoints.size());

        // first 3 trackpoints
        assertTrackpoint(importedTrackPoints.get(0), TrackPoint.Type.SEGMENT_START_AUTOMATIC, "2020-11-28T17:06:22.401Z", 1.234156, 12.340097, 469.286376953125);
        assertTrackpoint(importedTrackPoints.get(1), TrackPoint.Type.TRACKPOINT, "2020-11-28T17:06:25.448Z", 1.23415, 12.340036, 439.1626281738281);
        assertTrackpoint(importedTrackPoints.get(2), TrackPoint.Type.TRACKPOINT, "2020-11-28T17:06:47.888Z", 1.23405, 12.340057, 421.8070983886719);

        // created resume trackpoint with time of next valid trackpoint
        assertTrackpoint(importedTrackPoints.get(3), TrackPoint.Type.SEGMENT_START_AUTOMATIC, "2020-11-28T17:06:55.861Z", 1.23405, 12.340057, 419.93902587890625);
        assertTrackpoint(importedTrackPoints.get(4), TrackPoint.Type.TRACKPOINT, "2020-11-28T17:06:56.905Z", 1.23405, 12.340057, 419.9036560058594);
        assertTrackpoint(importedTrackPoints.get(5), TrackPoint.Type.TRACKPOINT, "2020-11-28T17:07:20.870Z", 1.234046, 12.340082, 417.99432373046875);
    }

    /**
     * At least one valid location is required.
     * Before v3.15.0, Tracks without TrackPoints could be created; such tracks cannot be imported as we cannot restore the TrackStatistics (especially startTime and stopTime).
     */
    @LargeTest
    @Test(expected = ImportParserException.class)
    public void kml_without_locations() {
        // given
        KmlFileTrackImporter trackImporter = new KmlFileTrackImporter(context);
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.debug.test.R.raw.legacy_kml_empty);

        // when
        importTrackId = trackImporter.importFile(inputStream).get(0);
    }

    /**
     * Check that data that contains pause (lat=100, lng=0) and resume (lat=200, lng=0) locations are restored to a segment break.
     */
    @LargeTest
    @Test
    public void gpx_with_pause_resume() {
        // given
        GpxFileTrackImporter trackImporter = new GpxFileTrackImporter(context);
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.debug.test.R.raw.legacy_gpx_pause_resume);

        // when
        // 1. import
        importTrackId = trackImporter.importFile(inputStream).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals("the category", importedTrack.getCategory());
        assertEquals("the description", importedTrack.getDescription());
        assertEquals("2021-01-07 22:51", importedTrack.getName());
        assertEquals("UNKNOWN", importedTrack.getIcon());

        // 3. trackpoints
        List<TrackPoint> importedTrackPoints = contentProviderUtils.getTrackPoints(importTrackId);
        assertEquals(6, importedTrackPoints.size());

        // first segment
        assertTrackpoint(importedTrackPoints.get(0), TrackPoint.Type.SEGMENT_START_AUTOMATIC, "2021-01-07T21:51:59.179Z", 14.0, 3.0, 10.0);
        assertTrackpoint(importedTrackPoints.get(1), TrackPoint.Type.TRACKPOINT, "2021-01-07T21:52:00.653Z", 14.001, 3.0, 10.0);
        assertTrackpoint(importedTrackPoints.get(2), TrackPoint.Type.TRACKPOINT, "2021-01-07T21:52:01.010Z", 14.002, 3.0, 10.0);
        assertTrackpoint(importedTrackPoints.get(3), TrackPoint.Type.SEGMENT_END_MANUAL, "2021-01-07T21:52:02.658Z", null, null, null);

        // created resume trackpoint with time of next valid trackpoint
        assertTrackpoint(importedTrackPoints.get(4), TrackPoint.Type.SEGMENT_START_MANUAL, "2021-01-07T21:52:03.873Z", null, null, null);
        assertTrackpoint(importedTrackPoints.get(5), TrackPoint.Type.TRACKPOINT, "2021-01-07T21:52:04.103Z", 14.003, 3.0, 10.0);
    }

    private void assertTrackpoint(final TrackPoint trackPoint, final TrackPoint.Type type, final String when, final Double longitude, final Double latitude, final Double altitude) {
        assertEquals(StringUtils.parseTime(when), trackPoint.getTime());
        assertEquals(type, trackPoint.getType());

        if (longitude == null) {
            assertFalse(trackPoint.hasLocation());
        } else {
            assertEquals(latitude, (Double) trackPoint.getLatitude());
            assertEquals(longitude, (Double) trackPoint.getLongitude());
        }

        if (altitude == null) {
            assertFalse(trackPoint.hasAltitude());
        } else {
            assertEquals(altitude, (Double) trackPoint.getAltitude());
        }

    }
}