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
import static org.junit.Assert.assertNotNull;

/**
 * Imports a legacy kml file with a statistics marker at the beginning
 */
@RunWith(JUnit4.class)
public class LegacyKmlImportTest {

    private final Context context = ApplicationProvider.getApplicationContext();
    private final ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

    private Track.Id importTrackId;

    @After
    public void tearDown() {
        if (importTrackId != null) {
            contentProviderUtils.deleteTrack(context, importTrackId);
        }
    }

    @LargeTest
    @Test
    public void kml_legacy_track_import() {
        // given
        KmlFileTrackImporter trackImporter = new KmlFileTrackImporter(context);
        InputStream inputStream  = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.debug.test.R.raw.legacy_track);

        // when
        // 1. import
        importTrackId = trackImporter.importFile(inputStream);

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
        assertEquals(8, importedTrackPoints.size()); // 6 trackpoints + pause and resume

        // first 3 trackpoints
        assertTrackpoint(importedTrackPoints.get(0), "2020-11-28T17:06:22.401Z", 1.234156, 12.340097, 469.286376953125);
        assertTrackpoint(importedTrackPoints.get(1), "2020-11-28T17:06:25.448Z", 1.23415, 12.340036, 439.1626281738281);
        assertTrackpoint(importedTrackPoints.get(2), "2020-11-28T17:06:47.888Z", 1.23405, 12.340057, 421.8070983886719);

        // created pause trackpoint with time of previous valid trackpoint
        assertTrackpoint(importedTrackPoints.get(3), "2020-11-28T17:06:47.888Z", 0.0, 100, 0.0);
        // created resume trackpoint with time of next valid trackpoint
        assertTrackpoint(importedTrackPoints.get(4), "2020-11-28T17:06:55.861Z", 0.0, 200, 0.0);

        // last 3 trackpoints
        assertTrackpoint(importedTrackPoints.get(5), "2020-11-28T17:06:55.861Z", 1.23405, 12.340057, 419.93902587890625);
        assertTrackpoint(importedTrackPoints.get(6), "2020-11-28T17:06:56.905Z", 1.23405, 12.340057, 419.9036560058594);
        assertTrackpoint(importedTrackPoints.get(7), "2020-11-28T17:07:20.870Z", 1.234046, 12.340082, 417.99432373046875);
    }

    private void assertTrackpoint(final TrackPoint trackPoint, final String when, final double longitude, final double latitude, final double altitude) {
        assertEquals(StringUtils.parseTime(when), trackPoint.getTime());
        assertEquals(latitude, trackPoint.getLatitude(), 0.001d);
        assertEquals(longitude, trackPoint.getLongitude(), 0.001d);
        assertEquals(altitude, trackPoint.getAltitude(), 0.001d);
    }

}