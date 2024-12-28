package de.dennisguse.opentracks.data;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import android.util.Pair;

import org.junit.Test;

import java.util.Set;

import de.dennisguse.opentracks.BuildConfig;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

public class ShareContentProviderTest {

    @Test
    public void testCreateAndParseURI_invalid() {
        Uri uri = Uri.parse("content://de.dennisguse.opentracks.debug.content/tracks/1");

        assertEquals(0, ShareContentProvider.parseURI(uri).size());
    }

    @Test
    public void testCreateAndParseURI_valid() {
        Set<Track.Id> trackIds = Set.of(new Track.Id(1), new Track.Id(3), new Track.Id(5));

        Pair<Uri, String> shareURIandMIME = ShareContentProvider.createURI(trackIds, "filename.suffix", TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA);

        assertEquals(trackIds, ShareContentProvider.parseURI(shareURIandMIME.first));
    }

    @Test
    public void testCreateURIescapeFilename() {
        Pair<Uri, String> shareURIandMIME = ShareContentProvider.createURI(Set.of(new Track.Id(1)), "../../&1=1", TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA);


        assertEquals(Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".content/tracks/KML_WITH_TRACKDETAIL_AND_SENSORDATA/1/..%2F..%2F%261%3D1"), shareURIandMIME.first);
    }
}