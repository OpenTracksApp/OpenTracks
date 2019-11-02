/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.io.file.importer;

import android.location.Location;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.util.PreferencesUtils;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link KmlFileTrackImporter}.
 *
 * @author Jimmy Shih
 */
@RunWith(MockitoJUnitRunner.class)
public class KmlFileTrackImporterTest extends AbstractTestFileTrackImporter {

    private static final String VALID_ONE_TRACK_ONE_SEGMENT_GPX = "<kml xmlns:gx=\"http://www.google.com/kml/ext/2.2\"><Placemark>"
                    + getNameAndDescription(TRACK_NAME_0, TRACK_DESCRIPTION_0) + "<gx:MultiTrack><gx:Track>"
                    + getTrackPoint(0, TRACK_TIME_0) + getTrackPoint(1, TRACK_TIME_1)
                    + "</gx:Track></gx:MultiTrack></Placemark></kml>";
    private static final String VALID_ONE_TRACK_TWO_SEGMENTS_GPX = "<kml xmlns:gx=\"http://www.google.com/kml/ext/2.2\"><Placemark>"
                    + getNameAndDescription(TRACK_NAME_0, TRACK_DESCRIPTION_0) + "<gx:MultiTrack><gx:Track>"
                    + getTrackPoint(0, TRACK_TIME_0) + getTrackPoint(1, TRACK_TIME_1) + "</gx:Track><gx:Track>"
                    + getTrackPoint(2, TRACK_TIME_2) + getTrackPoint(3, TRACK_TIME_3)
                    + "</gx:Track></gx:MultiTrack></Placemark></kml>";

    private static String getNameAndDescription(String name, String description) {
        return "<name><![CDATA[" + name + "]]></name><description><![CDATA[" + description
                + "]]></description>";
    }

    private static String getTrackPoint(int index, String time) {
        String latitude = Double.toString(TRACK_LATITUDE + index);
        String longitude = Double.toString(TRACK_LONGITUDE + index);
        String altitude = Double.toString(TRACK_ELEVATION + index);
        return "<when>" + time + "</when>" + "<gx:coord>" + longitude + " " + latitude + " " + altitude + "</gx:coord>";
    }

    @Test
    public void testOneTrackOneSegment() throws Exception {
        // given
        Location location0 = createLocation(0, DATE_FORMAT_0.parse(TRACK_TIME_0).getTime());
        Location location1 = createLocation(1, DATE_FORMAT_1.parse(TRACK_TIME_1).getTime());

        contentProviderUtils.clearTrack(context, TRACK_ID_0);
        expectFirstTrackPoint(location0, TRACK_ID_0, TRACK_POINT_ID_0);

        // A flush happens at the end
        when(contentProviderUtils.bulkInsertTrackPoint((Location[]) any(), eq(1), eq(TRACK_ID_0))).thenReturn(1);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID_0)).thenReturn(TRACK_POINT_ID_1);
        when(contentProviderUtils.getTrack(PreferencesUtils.getRecordingTrackId(context))).thenReturn(null);

        ArgumentCaptor<Track> trackCaptor = ArgumentCaptor.forClass(Track.class);
        expectTrackUpdate(trackCaptor, true, TRACK_ID_0);

        // when
        InputStream inputStream = new ByteArrayInputStream(VALID_ONE_TRACK_ONE_SEGMENT_GPX.getBytes());
        KmlFileTrackImporter kmlFileTrackImporter = new KmlFileTrackImporter(context, TRACK_ID_0, contentProviderUtils);
        long trackId = kmlFileTrackImporter.importFile(inputStream);

        // then
        verify(contentProviderUtils, atLeastOnce()).updateTrack(trackCaptor.capture());
        Assert.assertEquals(TRACK_ID_0, trackId);

        long time0 = DATE_FORMAT_0.parse(TRACK_TIME_0).getTime();
        long time1 = DATE_FORMAT_1.parse(TRACK_TIME_1).getTime();
        Assert.assertEquals(time1 - time0, trackCaptor.getValue().getTripStatistics().getTotalTime());
        verifyTrack(trackCaptor.getValue(), TRACK_NAME_0, TRACK_DESCRIPTION_0, time0);
    }

    @Test
    public void testOneTrackTwoSegments() throws Exception {
        // given
        Location location0 = createLocation(0, DATE_FORMAT_0.parse(TRACK_TIME_0).getTime());

        contentProviderUtils.clearTrack(context, TRACK_ID_0);
        expectFirstTrackPoint(location0, TRACK_ID_0, TRACK_POINT_ID_0);

        // A flush happens at the end
        when(contentProviderUtils.bulkInsertTrackPoint((Location[]) any(), eq(5), eq(TRACK_ID_0))).thenReturn(5);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID_0)).thenReturn(TRACK_POINT_ID_3);
        when(contentProviderUtils.getTrack(PreferencesUtils.getRecordingTrackId(context))).thenReturn(null);

        ArgumentCaptor<Track> trackCaptor = ArgumentCaptor.forClass(Track.class);
        expectTrackUpdate(trackCaptor, true, TRACK_ID_0);

        // when
        InputStream inputStream = new ByteArrayInputStream(VALID_ONE_TRACK_TWO_SEGMENTS_GPX.getBytes());
        KmlFileTrackImporter kmlFileTrackImporter = new KmlFileTrackImporter(context, TRACK_ID_0, contentProviderUtils);
        long trackId = kmlFileTrackImporter.importFile(inputStream);

        // then
        verify(contentProviderUtils, atLeastOnce()).updateTrack(trackCaptor.capture());
        Assert.assertEquals(TRACK_ID_0, trackId);

        long time0 = DATE_FORMAT_0.parse(TRACK_TIME_0).getTime();
        long time1 = DATE_FORMAT_1.parse(TRACK_TIME_1).getTime();
        long time2 = DATE_FORMAT_1.parse(TRACK_TIME_2).getTime();
        long time3 = DATE_FORMAT_1.parse(TRACK_TIME_3).getTime();
        Assert.assertEquals(time1 - time0 + time3 - time2, trackCaptor.getValue().getTripStatistics().getTotalTime());

        verifyTrack(trackCaptor.getValue(), TRACK_NAME_0, TRACK_DESCRIPTION_0, DATE_FORMAT_0.parse(TRACK_TIME_0).getTime());
    }
}
