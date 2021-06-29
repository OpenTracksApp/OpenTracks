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
import de.dennisguse.opentracks.content.data.Speed;
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
public class GPXImportTest {

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
     * Check that data that contains pause (lat=100, lng=0) and resume (lat=200, lng=0) locations are restored to a segment break.
     */
    @LargeTest
    @Test
    public void gpx_with_pause_resume() throws IOException {
        // given
        XMLImporter importer = new XMLImporter(new GpxTrackImporter(context, trackImporter));
        InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(de.dennisguse.opentracks.debug.test.R.raw.legacy_gpx_pause_resume);

        // when
        // 1. import
        importTrackId = importer.importFile(inputStream).get(0);

        // then
        // 1. track
        Track importedTrack = contentProviderUtils.getTrack(importTrackId);
        assertNotNull(importedTrack);
        assertEquals("the category", importedTrack.getCategory());
        assertEquals("the description", importedTrack.getDescription());
        assertEquals("2021-01-07 22:51", importedTrack.getName());
        assertEquals("UNKNOWN", importedTrack.getIcon());

        // 3. trackpoints
        List<TrackPoint> importedTrackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, importTrackId);
        assertEquals(6, importedTrackPoints.size());

        // first segment
        TrackPointAssert a = new TrackPointAssert();
        a.assertEquals(List.of(
                a.expect(TrackPoint.Type.SEGMENT_START_AUTOMATIC, "2021-01-07T21:51:59.179Z", 14.0, 3.0, 10.0),
                a.expect(TrackPoint.Type.TRACKPOINT, "2021-01-07T21:52:00.653Z", 14.001, 3.0, 10.0)
                        .setSpeed(Speed.of(75.4192)),
                a.expect(TrackPoint.Type.TRACKPOINT, "2021-01-07T21:52:01.010Z", 14.002, 3.0, 10.0)
                        .setSpeed(Speed.of(311.3948)),
                a.expect(TrackPoint.Type.SEGMENT_END_MANUAL, "2021-01-07T21:52:02.658Z"),

                // created resume trackpoint with time of next valid trackpoint
                a.expect(TrackPoint.Type.SEGMENT_START_MANUAL, "2021-01-07T21:52:03.873Z"),
                a.expect(TrackPoint.Type.TRACKPOINT, "2021-01-07T21:52:04.103Z", 14.003, 3.0, 10.0)
        ), importedTrackPoints);
    }
}