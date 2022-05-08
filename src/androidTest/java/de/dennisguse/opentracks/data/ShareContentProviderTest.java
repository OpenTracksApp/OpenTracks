package de.dennisguse.opentracks.data;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import android.util.Pair;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

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
        Set<Track.Id> trackIds = new HashSet<>();
        trackIds.add(new Track.Id(1));
        trackIds.add(new Track.Id(3));
        trackIds.add(new Track.Id(5));

        Pair<Uri, String> shareURIandMIME = ShareContentProvider.createURI(trackIds, "TrackName", TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA);

        assertEquals(trackIds, ShareContentProvider.parseURI(shareURIandMIME.first));
    }

    @Test
    public void testCreateURIescapeFilename() {
        Set<Track.Id> trackIds = new HashSet<>();
        trackIds.add(new Track.Id(1));
        Pair<Uri, String> shareURIandMIME = ShareContentProvider.createURI(trackIds, "../../&1=1", TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA);

        assertEquals(Uri.parse("content://de.dennisguse.opentracks.debug.content/tracks/KML_WITH_TRACKDETAIL_AND_SENSORDATA/1/..%2F..%2F%261%3D1.kml"), shareURIandMIME.first);
    }
}