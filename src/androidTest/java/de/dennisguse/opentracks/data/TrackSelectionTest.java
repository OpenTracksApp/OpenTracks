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
        assertNull(selection.selection());
        assertNull(selection.selectionArgs());
    }

    @Test
    public void testFilterBuildSelection_onlyOneTrackId() {
        // given
        Track.Id trackId = new Track.Id(1);
        TrackSelection filter = new TrackSelection().addTrackId(trackId);

        // when
        SelectionData selection = filter.buildSelection();

        // Then
        assertEquals(selection.selection(), "_id IN (?)");
        assertEquals(selection.selectionArgs().length, 1);
        assertEquals(selection.selectionArgs()[0], "1");
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
        assertEquals(selection.selection(), "_id IN (?,?,?)");
        assertEquals(selection.selectionArgs().length, 3);
        assertEquals(selection.selectionArgs()[0], "1");
        assertEquals(selection.selectionArgs()[1], "2");
        assertEquals(selection.selectionArgs()[2], "3");
    }

    @Test
    public void testFilterBuildSelection_onlyOneCategory() {
        // given
        TrackSelection filter = new TrackSelection().addActivityType("running");

        // when
        SelectionData selection = filter.buildSelection();

        // Then
        assertEquals(selection.selection(), "category IN (?)");
        assertEquals(selection.selectionArgs().length, 1);
        assertEquals(selection.selectionArgs()[0], "running");
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
        assertEquals(selection.selection(), "category IN (?,?,?,?)");
        assertEquals(selection.selectionArgs().length, 4);
        assertEquals(selection.selectionArgs()[0], "running");
        assertEquals(selection.selectionArgs()[1], "road biking");
        assertEquals(selection.selectionArgs()[2], "mountain biking");
        assertEquals(selection.selectionArgs()[3], "trail walking");
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
        assertNull(selectionWrong1.selection());
        assertNull(selectionWrong1.selectionArgs());

        assertNull(selectionWrong2.selection());
        assertNull(selectionWrong2.selectionArgs());

        assertEquals(selectionOk.selection(), "starttime BETWEEN ? AND ?");
        assertEquals(selectionOk.selectionArgs().length, 2);
        assertEquals(selectionOk.selectionArgs()[0], Long.toString(instant.toEpochMilli()));
        assertEquals(selectionOk.selectionArgs()[1], Long.toString(instant.toEpochMilli() + oneDay));
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
        assertEquals(selection.selection(), "_id IN (?,?,?) AND category IN (?,?)");
        assertEquals(selection.selectionArgs().length, 5);
        assertEquals(selection.selectionArgs()[0], "1");
        assertEquals(selection.selectionArgs()[1], "2");
        assertEquals(selection.selectionArgs()[2], "3");
        assertEquals(selection.selectionArgs()[3], "running");
        assertEquals(selection.selectionArgs()[4], "road biking");
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
        assertEquals(selection.selection(), "_id IN (?,?,?) AND starttime BETWEEN ? AND ?");
        assertEquals(selection.selectionArgs().length, 5);
        assertEquals(selection.selectionArgs()[0], "1");
        assertEquals(selection.selectionArgs()[1], "2");
        assertEquals(selection.selectionArgs()[2], "3");
        assertEquals(selection.selectionArgs()[3], Long.toString(instant.toEpochMilli()));
        assertEquals(selection.selectionArgs()[4], Long.toString(instant.toEpochMilli() + oneDay));
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
        assertEquals(selection.selection(), "category IN (?,?) AND starttime BETWEEN ? AND ?");
        assertEquals(selection.selectionArgs().length, 4);
        assertEquals(selection.selectionArgs()[0], "running");
        assertEquals(selection.selectionArgs()[1], "road biking");
        assertEquals(selection.selectionArgs()[2], Long.toString(instant.toEpochMilli()));
        assertEquals(selection.selectionArgs()[3], Long.toString(instant.toEpochMilli() + oneDay));
    }
}
