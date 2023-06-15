package de.dennisguse.opentracks.data;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;

import de.dennisguse.opentracks.data.models.Track;

@RunWith(AndroidJUnit4.class)
public class TrackSelectionTest extends TestCase {
    @Test
    public void testFilterBuildSelection_empty() {
        // given
        TrackSelection filter = new TrackSelection();

        // when
        SelectionData selection = filter.buildSelection();

        // Then
        assertNull(selection.getSelection());
        assertNull(selection.getSelectionArgs());
    }

    @Test
    public void testFilterBuildSelection_onlyOneTrackId() {
        // given
        Track.Id trackId = new Track.Id(1);
        TrackSelection filter = new TrackSelection().addTrackId(trackId);

        // when
        SelectionData selection = filter.buildSelection();

        // Then
        assertEquals(selection.getSelection(), "_id IN (?)");
        assertEquals(selection.getSelectionArgs().length, 1);
        assertEquals(selection.getSelectionArgs()[0], "1");
    }

    @Test
    public void testFilterBuildSelection_severalTracksId() {
        // given
        Track.Id trackId1 = new Track.Id(1);
        Track.Id trackId2 = new Track.Id(2);
        Track.Id trackId3 = new Track.Id(3);
        TrackSelection filter = new TrackSelection()
                .addTrackId(trackId1)
                .addTrackId(trackId2)
                .addTrackId(trackId3);

        // when
        SelectionData selection = filter.buildSelection();

        // Then
        assertEquals(selection.getSelection(), "_id IN (?,?,?)");
        assertEquals(selection.getSelectionArgs().length, 3);
        assertEquals(selection.getSelectionArgs()[0], "1");
        assertEquals(selection.getSelectionArgs()[1], "2");
        assertEquals(selection.getSelectionArgs()[2], "3");
    }

    @Test
    public void testFilterBuildSelection_onlyOneCategory() {
        // given
        TrackSelection filter = new TrackSelection().addActivityType("running");

        // when
        SelectionData selection = filter.buildSelection();

        // Then
        assertEquals(selection.getSelection(), "category IN (?)");
        assertEquals(selection.getSelectionArgs().length, 1);
        assertEquals(selection.getSelectionArgs()[0], "running");
    }

    @Test
    public void testFilterBuildSelection_severalCategories() {
        // given
        TrackSelection filter = new TrackSelection()
                .addActivityType("running")
                .addActivityType("road biking")
                .addActivityType("mountain biking")
                .addActivityType("trail walking");

        // when
        SelectionData selection = filter.buildSelection();

        // Then
        assertEquals(selection.getSelection(), "category IN (?,?,?,?)");
        assertEquals(selection.getSelectionArgs().length, 4);
        assertEquals(selection.getSelectionArgs()[0], "running");
        assertEquals(selection.getSelectionArgs()[1], "road biking");
        assertEquals(selection.getSelectionArgs()[2], "mountain biking");
        assertEquals(selection.getSelectionArgs()[3], "trail walking");
    }

    @Test
    public void testFilterBuildSelection_onlyDateRange() {
        // given
        Instant instant = Instant.now();
        long oneDay = 24 * 60 * 60 * 1000;
        TrackSelection filterWrong1 = new TrackSelection().addDateRange(instant, null);
        TrackSelection filterWrong2 = new TrackSelection().addDateRange(null, instant);
        TrackSelection filterOk = new TrackSelection().addDateRange(instant, instant.plus(Duration.ofDays(1)));

        // when
        SelectionData selectionWrong1 = filterWrong1.buildSelection();
        SelectionData selectionWrong2 = filterWrong2.buildSelection();
        SelectionData selectionOk = filterOk.buildSelection();

        // Then
        assertNull(selectionWrong1.getSelection());
        assertNull(selectionWrong1.getSelectionArgs());

        assertNull(selectionWrong2.getSelection());
        assertNull(selectionWrong2.getSelectionArgs());

        assertEquals(selectionOk.getSelection(), "starttime BETWEEN ? AND ?");
        assertEquals(selectionOk.getSelectionArgs().length, 2);
        assertEquals(selectionOk.getSelectionArgs()[0], Long.toString(instant.toEpochMilli()));
        assertEquals(selectionOk.getSelectionArgs()[1], Long.toString(instant.toEpochMilli() + oneDay));
    }

    @Test
    public void testFilterBuildSelection_tracksId_and_categories() {
        // given
        Track.Id trackId1 = new Track.Id(1);
        Track.Id trackId2 = new Track.Id(2);
        Track.Id trackId3 = new Track.Id(3);
        TrackSelection filter = new TrackSelection()
                .addTrackId(trackId1)
                .addTrackId(trackId2)
                .addTrackId(trackId3)
                .addActivityType("running")
                .addActivityType("road biking");

        // when
        SelectionData selection = filter.buildSelection();

        // Then
        assertEquals(selection.getSelection(), "_id IN (?,?,?) AND category IN (?,?)");
        assertEquals(selection.getSelectionArgs().length, 5);
        assertEquals(selection.getSelectionArgs()[0], "1");
        assertEquals(selection.getSelectionArgs()[1], "2");
        assertEquals(selection.getSelectionArgs()[2], "3");
        assertEquals(selection.getSelectionArgs()[3], "running");
        assertEquals(selection.getSelectionArgs()[4], "road biking");
    }

    @Test
    public void testFilterBuildSelection_tracksId_and_dateRange() {
        // given
        Instant instant = Instant.now();
        long oneDay = 24 * 60 * 60 * 1000;

        Track.Id trackId1 = new Track.Id(1);
        Track.Id trackId2 = new Track.Id(2);
        Track.Id trackId3 = new Track.Id(3);

        TrackSelection filter = new TrackSelection()
                .addTrackId(trackId1)
                .addTrackId(trackId2)
                .addTrackId(trackId3)
                .addDateRange(instant, instant.plusMillis(oneDay));

        // when
        SelectionData selection = filter.buildSelection();

        // Then
        assertEquals(selection.getSelection(), "_id IN (?,?,?) AND starttime BETWEEN ? AND ?");
        assertEquals(selection.getSelectionArgs().length, 5);
        assertEquals(selection.getSelectionArgs()[0], "1");
        assertEquals(selection.getSelectionArgs()[1], "2");
        assertEquals(selection.getSelectionArgs()[2], "3");
        assertEquals(selection.getSelectionArgs()[3], Long.toString(instant.toEpochMilli()));
        assertEquals(selection.getSelectionArgs()[4], Long.toString(instant.toEpochMilli() + oneDay));
    }

    @Test
    public void testFilterBuildSelection_categories_and_dateRange() {
        // given
        Instant instant = Instant.now();
        long oneDay = 24 * 60 * 60 * 1000;

        TrackSelection filter = new TrackSelection()
                .addActivityType("running")
                .addActivityType("road biking")
                .addDateRange(instant, instant.plusMillis(oneDay));

        // when
        SelectionData selection = filter.buildSelection();

        // Then
        assertEquals(selection.getSelection(), "category IN (?,?) AND starttime BETWEEN ? AND ?");
        assertEquals(selection.getSelectionArgs().length, 4);
        assertEquals(selection.getSelectionArgs()[0], "running");
        assertEquals(selection.getSelectionArgs()[1], "road biking");
        assertEquals(selection.getSelectionArgs()[2], Long.toString(instant.toEpochMilli()));
        assertEquals(selection.getSelectionArgs()[3], Long.toString(instant.toEpochMilli() + oneDay));
    }
}
