/*
 * Copyright 2010 Google Inc.
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

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.util.PreferencesUtils;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GpxFileTrackImporter}.
 *
 * @author Steffen Horlacher
 */
@RunWith(MockitoJUnitRunner.class)
public class GpxFileTrackImporterTest extends AbstractTestFileTrackImporter {

    private static final String VALID_ONE_TRACK_ONE_SEGMENT_GPX = "<gpx><trk>"
            + getNameAndDescription(TRACK_NAME_0, TRACK_DESCRIPTION_0) + "<trkseg>"
            + getTrackPoint(0, TRACK_TIME_0) + getTrackPoint(1, TRACK_TIME_1) + "</trkseg></trk></gpx>";
    private static final String VALID_ONE_TRACK_TWO_SEGMENTS_GPX = "<gpx><trk>"
            + getNameAndDescription(TRACK_NAME_0, TRACK_DESCRIPTION_0) + "<trkseg>"
            + getTrackPoint(0, TRACK_TIME_0) + getTrackPoint(1, TRACK_TIME_1) + "</trkseg><trkseg>"
            + getTrackPoint(2, TRACK_TIME_2) + getTrackPoint(3, TRACK_TIME_3) + "</trkseg></trk></gpx>";
    private static final String VALID_ONE_TRACK_TWO_SEGMENTS_NO_TIME_GPX = "<gpx><trk>"
            + getNameAndDescription(TRACK_NAME_0, TRACK_DESCRIPTION_0) + "<trkseg>"
            + getTrackPoint(0, null) + getTrackPoint(1, null) + "</trkseg><trkseg>"
            + getTrackPoint(2, null) + getTrackPoint(3, null) + "</trkseg></trk></gpx>";

    private static final String INVALID_XML_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.substring(0, VALID_ONE_TRACK_ONE_SEGMENT_GPX.length() - 50);
    private static final String INVALID_LOCATION_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.replaceAll(Double.toString(TRACK_LATITUDE), "1000.0");
    private static final String INVALID_TIME_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.replaceAll(TRACK_TIME_0, "invalid");
    private static final String INVALID_ALTITUDE_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.replaceAll(Double.toString(TRACK_ELEVATION), "invalid");
    private static final String INVALID_LATITUDE_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.replaceAll(Double.toString(TRACK_LATITUDE), "invalid");
    private static final String INVALID_LONGITUDE_GPX = VALID_ONE_TRACK_ONE_SEGMENT_GPX.replaceAll(Double.toString(TRACK_LONGITUDE), "invalid");

    private static String getNameAndDescription(String name, String description) {
        return "<name><![CDATA[" + name + "]]></name>" + "<desc><![CDATA[" + description + "]]></desc>";
    }

    private static String getTrackPoint(int index, String time) {
        String latitude = Double.toString(TRACK_LATITUDE + index);
        String longitude = Double.toString(TRACK_LONGITUDE + index);
        String elevation = Double.toString(TRACK_ELEVATION + index);
        StringBuilder buffer = new StringBuilder();
        buffer.append("<trkpt lat=\"" + latitude + "\" lon=\"" + longitude + "\"><ele>" + elevation + "</ele>");
        if (time != null) {
            buffer.append("<time>" + time + "</time>");
        }
        buffer.append("</trkpt>");
        return buffer.toString();
    }

    @Test
    public void testOneTrackOneSegment() throws Exception {
        // given
        Location location0 = createLocation(0, DATE_FORMAT_0.parse(TRACK_TIME_0).getTime());
        Location location1 = createLocation(1, DATE_FORMAT_1.parse(TRACK_TIME_1).getTime());

        when(contentProviderUtils.insertTrack((Track) any())).thenReturn(TRACK_ID_0_URI);
        expectFirstTrackPoint(location0, TRACK_ID_0, TRACK_POINT_ID_0);

        // A flush happens at the end
        when(contentProviderUtils.bulkInsertTrackPoint((Location[]) any(), eq(1), eq(TRACK_ID_0))).thenReturn(1);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID_0)).thenReturn(TRACK_POINT_ID_1);
        when(contentProviderUtils.getTrack(PreferencesUtils.getRecordingTrackId(context))).thenReturn(null);
        ArgumentCaptor<Track> trackCaptor = ArgumentCaptor.forClass(Track.class);
        expectTrackUpdate(trackCaptor, true, TRACK_ID_0);

        // when
        InputStream inputStream = new ByteArrayInputStream(VALID_ONE_TRACK_ONE_SEGMENT_GPX.getBytes());
        GpxFileTrackImporter gpxFileTrackImporter = new GpxFileTrackImporter(context, contentProviderUtils);
        long trackId = gpxFileTrackImporter.importFile(inputStream);

        // then
        Assert.assertEquals(TRACK_ID_0, trackId);

        verify(contentProviderUtils, atLeastOnce()).updateTrack(trackCaptor.capture());
        long time0 = DATE_FORMAT_0.parse(TRACK_TIME_0).getTime();
        long time1 = DATE_FORMAT_1.parse(TRACK_TIME_1).getTime();
        Assert.assertEquals(time1 - time0, trackCaptor.getValue().getTripStatistics().getTotalTime());
        verifyTrack(trackCaptor.getValue(), TRACK_NAME_0, TRACK_DESCRIPTION_0, time0);
    }

    @Test
    public void testOneTrackTwoSegments() throws Exception {
        // given
        Location location0 = createLocation(0, DATE_FORMAT_0.parse(TRACK_TIME_0).getTime());

        when(contentProviderUtils.insertTrack((Track) any())).thenReturn(TRACK_ID_0_URI);
        expectFirstTrackPoint(location0, TRACK_ID_0, TRACK_POINT_ID_0);
        // A flush happens at the end
        when(contentProviderUtils.bulkInsertTrackPoint((Location[]) any(), eq(5), eq(TRACK_ID_0))).thenReturn(5);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID_0)).thenReturn(TRACK_POINT_ID_3);
        when(contentProviderUtils.getTrack(PreferencesUtils.getRecordingTrackId(context))).thenReturn(null);

        ArgumentCaptor<Track> trackCaptor = ArgumentCaptor.forClass(Track.class);
        expectTrackUpdate(trackCaptor, true, TRACK_ID_0);

        // when
        InputStream inputStream = new ByteArrayInputStream(VALID_ONE_TRACK_TWO_SEGMENTS_GPX.getBytes());
        GpxFileTrackImporter gpxFileTrackImporter = new GpxFileTrackImporter(context, contentProviderUtils);
        long trackId = gpxFileTrackImporter.importFile(inputStream);

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

    @Test
    public void testOneTrackTwoSegmentsNoTime() {
        // given
        when(contentProviderUtils.insertTrack((Track) any())).thenReturn(TRACK_ID_0_URI);
        expectFirstTrackPoint(null, TRACK_ID_0, TRACK_POINT_ID_0);

        // A flush happens at the end
        when(contentProviderUtils.bulkInsertTrackPoint((Location[]) any(), eq(5), eq(TRACK_ID_0))).thenReturn(5);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID_0)).thenReturn(TRACK_POINT_ID_3);
        when(contentProviderUtils.getTrack(PreferencesUtils.getRecordingTrackId(context))).thenReturn(null);

        ArgumentCaptor<Track> trackCaptor = ArgumentCaptor.forClass(Track.class);
        expectTrackUpdate(trackCaptor, true, TRACK_ID_0);

        // when
        InputStream inputStream = new ByteArrayInputStream(VALID_ONE_TRACK_TWO_SEGMENTS_NO_TIME_GPX.getBytes());
        GpxFileTrackImporter gpxFileTrackImporter = new GpxFileTrackImporter(context, contentProviderUtils);
        long trackId = gpxFileTrackImporter.importFile(inputStream);

        // then
        verify(contentProviderUtils, atLeastOnce()).updateTrack(trackCaptor.capture());
        Assert.assertEquals(TRACK_ID_0, trackId);

        Assert.assertEquals(0, trackCaptor.getValue().getTripStatistics().getTotalTime());
        verifyTrack(trackCaptor.getValue(), TRACK_NAME_0, TRACK_DESCRIPTION_0, -1L);
    }

    @Test
    public void testInvalidXml() {
        testInvalidGpx(INVALID_XML_GPX);
    }

    @Test
    public void testInvalidLocation() {
        testInvalidGpx(INVALID_LOCATION_GPX);
    }

    @Test
    public void testInvalidTime() {
        testInvalidGpx(INVALID_TIME_GPX);
    }

    @Test
    public void testInvalidAltitude() {
        testInvalidGpx(INVALID_ALTITUDE_GPX);
    }

    @Test
    public void testInvalidLatitude() {
        testInvalidGpx(INVALID_LATITUDE_GPX);
    }

    @Test
    public void testInvalidLongitude() {
        testInvalidGpx(INVALID_LONGITUDE_GPX);
    }

    private void testInvalidGpx(String xml) {
        when(contentProviderUtils.insertTrack((Track) any())).thenReturn(TRACK_ID_0_URI);

        // For the following, use StubReturn since we don't care whether they are invoked or not.
        when(contentProviderUtils.bulkInsertTrackPoint((Location[]) any(), anyInt(), anyLong())).thenReturn(1);
        when(contentProviderUtils.getTrack(PreferencesUtils.getRecordingTrackId(context))).thenReturn(null);
        contentProviderUtils.deleteTrack(context, TRACK_ID_0);

        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        GpxFileTrackImporter gpxFileTrackImporter = new GpxFileTrackImporter(context, contentProviderUtils);
        long trackId = gpxFileTrackImporter.importFile(inputStream);
        Assert.assertEquals(-1L, trackId);
    }
}
