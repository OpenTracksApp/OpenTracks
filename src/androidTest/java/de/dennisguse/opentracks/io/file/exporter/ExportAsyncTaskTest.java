package de.dennisguse.opentracks.io.file.exporter;

import android.content.Context;
import android.util.Pair;

import androidx.documentfile.provider.DocumentFile;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.TrackNameUtils;

public class ExportAsyncTaskTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    private final ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

    private static final String TRACK_ICON = "the track icon";
    private static final String TRACK_CATEGORY = "the category";
    private static final String TRACK_DESCRIPTION = "the description";
    private final long currentTimeMillis = System.currentTimeMillis();

    private static final int NUM_TRACKS = 3;
    private String[] trackStartTimes = new String[NUM_TRACKS];
    private String[] trackEndTimes = new String[NUM_TRACKS];
    private long[] trackIdList = new long[NUM_TRACKS];

    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'", Locale.US);

    private File exportDirectoryFile;
    private DocumentFile exportDirectory;

    @Before
    public void setUp() throws Exception {
        // Begins with all empty.
        contentProviderUtils.deleteAllTracks(context);

        // Creates NUM_TRACKS start/end times.
        loadStartEndTimes();

        // Creates all tracks for the tests.
        trackIdList = createAllTracks();

        // Temporary directory work.
        exportDirectoryFile = new File(context.getCacheDir().getAbsolutePath() + "/OpenTracks" + System.currentTimeMillis() + "/");
        exportDirectoryFile.mkdir();
        exportDirectory = DocumentFile.fromFile(exportDirectoryFile);

        Assert.assertTrue(exportDirectoryFile.isDirectory());
    }

    @After
    public void tearDown() {
        for (int i = 0; i < trackIdList.length; i++) {
            contentProviderUtils.deleteAllTracks(context);
            contentProviderUtils.deleteTrack(context, trackIdList[i]);
        }

        FileUtils.deleteDirectoryRecurse(exportDirectoryFile);
    }

    private String addOneDay(String dateTimeStr) throws ParseException {
        Calendar c = Calendar.getInstance();
        c.setTime(DATE_FORMAT.parse(dateTimeStr));
        c.add(Calendar.DATE, 1);
        return DATE_FORMAT.format(c.getTime());
    }

    private void loadStartEndTimes() throws ParseException {
        String firstStart = "2010-01-01T08:21:33Z";
        String firstEnd = "2010-01-01T09:27:18Z";
        trackStartTimes[0] = firstStart;
        trackEndTimes[0] = firstEnd;
        Calendar cStart = Calendar.getInstance();
        Calendar cEnd = Calendar.getInstance();

        cStart.setTime(DATE_FORMAT.parse(firstStart));
        cEnd.setTime(DATE_FORMAT.parse(firstEnd));
        for (int i = 1; i < NUM_TRACKS; i++) {
            trackStartTimes[i] = addOneDay(trackStartTimes[i - 1]);
            trackEndTimes[i] = addOneDay(trackEndTimes[i - 1]);
        }
    }

    /**
     * Creates a track.
     */
    private void createTrack(long id, String startTime, String endTime) throws ParseException {
        Pair<Track, TrackPoint[]> track = TestDataUtil.createTrack(id, 10);
        track.first.setIcon(TRACK_ICON);
        track.first.setCategory(TRACK_CATEGORY);
        track.first.setDescription(TRACK_DESCRIPTION);
        TrackStatistics trackStatistics = track.first.getTrackStatistics();
        trackStatistics.setStartTime_ms(DATE_FORMAT.parse(startTime).getTime());
        trackStatistics.setStopTime_ms(DATE_FORMAT.parse(endTime).getTime());
        contentProviderUtils.insertTrack(track.first);
        contentProviderUtils.bulkInsertTrackPoint(track.second, track.first.getId());
    }

    /**
     * Creates NUM_TRACKS tracks.
     *
     * @return The list of track's id.
     * @throws ParseException
     */
    private long[] createAllTracks() throws ParseException {
        int numTracks = trackStartTimes.length;
        long[] trackIds = new long[numTracks];

        for (int i = 0; i < numTracks; i++) {
            trackIds[i] = currentTimeMillis + i;
            createTrack(trackIds[i], trackStartTimes[i], trackEndTimes[i]);
        }

        Assert.assertEquals(trackIds.length, contentProviderUtils.getAllTracks().size());

        return trackIds;
    }

    /**
     * Tests the method {@link ExportAsyncTask#exportAllTracks(Track)}.
     */
    @Test
    public void testExportAllTracks() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        // Export all tracks results in NUM_TRACKS files in export directory.

        // given
        ExportActivity exportActivity = Mockito.mock(ExportActivity.class);
        TrackFileFormat trackFileFormat = TrackFileFormat.KML_ONLY_TRACK;
        // Method to test in ExportAsyncTask.
        Method method = ExportAsyncTask.class.getDeclaredMethod("exportAllTracks");
        method.setAccessible(true);

        // when
        Mockito.when(exportActivity.getApplicationContext()).thenReturn(context);
        ExportAsyncTask exportAsyncTask = new ExportAsyncTask(exportActivity, trackFileFormat, exportDirectory);
        method.invoke(exportAsyncTask);

        // then
        // number of files exported.
        int numFiles = exportDirectoryFile.list().length;
        Assert.assertEquals(NUM_TRACKS, numFiles);

        // names of the exported files.
        if (numFiles == NUM_TRACKS) {
            List<String> list = new ArrayList<>(Arrays.asList(exportDirectoryFile.list()));
            for (int i = 0; i < NUM_TRACKS; i++) {
                Track track = contentProviderUtils.getTrack(trackIdList[i]);
                String prefix = TrackNameUtils.getPrefixExportTrack(track) + "-";
                String suffix = "." + trackFileFormat.getExtension();
                String fileName = prefix + track.getName() + suffix;
                Assert.assertTrue(list.contains(fileName));
                list.remove(list.indexOf(fileName));
            }
        }
    }

    /**
     * Tests the method {@link ExportAsyncTask#exportAllTracks(Track)}.
     */
    @Test
    public void testExportAllTracks_exportTwice() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        // Exporting all twice results in NUM_TRACKS (not duplication).

        // given
        ExportActivity exportActivity = Mockito.mock(ExportActivity.class);
        TrackFileFormat trackFileFormat = TrackFileFormat.KML_ONLY_TRACK;
        // Method to test in ExportAsyncTask.
        Method method = ExportAsyncTask.class.getDeclaredMethod("exportAllTracks");
        method.setAccessible(true);

        // when
        Mockito.when(exportActivity.getApplicationContext()).thenReturn(context);
        ExportAsyncTask exportAsyncTask = new ExportAsyncTask(exportActivity, trackFileFormat, exportDirectory);
        method.invoke(exportAsyncTask); // first time
        method.invoke(exportAsyncTask); // second time

        // then
        // number of files exported.
        int numFiles = exportDirectoryFile.list().length;
        Assert.assertEquals(NUM_TRACKS, numFiles);

        // names of the exported files.
        if (numFiles == NUM_TRACKS) {
            List<String> list = new ArrayList<>(Arrays.asList(exportDirectoryFile.list()));
            for (int i = 0; i < NUM_TRACKS; i++) {
                Track track = contentProviderUtils.getTrack(trackIdList[i]);
                String prefix = TrackNameUtils.getPrefixExportTrack(track) + "-";
                String suffix = "." + trackFileFormat.getExtension();
                String fileName = prefix + track.getName() + suffix;
                Assert.assertTrue(list.contains(fileName));
                list.remove(list.indexOf(fileName));
            }
        }
    }

    /**
     * Tests the method {@link ExportAsyncTask#exportAllTracks(Track)}.
     */
    @Test
    public void testExportAllTracks_exportTwiceChangingATrackName() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        // Exporting all twice with track name changed (not duplication although a track change its name).

        // 1. given
        ExportActivity exportActivity = Mockito.mock(ExportActivity.class);
        TrackFileFormat trackFileFormat = TrackFileFormat.KML_ONLY_TRACK;
        // Method to test in ExportAsyncTask.
        Method method = ExportAsyncTask.class.getDeclaredMethod("exportAllTracks");
        method.setAccessible(true);

        // 1. when
        Mockito.when(exportActivity.getApplicationContext()).thenReturn(context);
        ExportAsyncTask exportAsyncTask = new ExportAsyncTask(exportActivity, trackFileFormat, exportDirectory);
        method.invoke(exportAsyncTask);

        // 1. then
        // number of files exported.
        int numFiles = exportDirectoryFile.list().length;
        Assert.assertEquals(NUM_TRACKS, numFiles);

        // names of the exported files.
        if (numFiles == NUM_TRACKS) {
            List<String> list = new ArrayList<>(Arrays.asList(exportDirectoryFile.list()));
            for (int i = 0; i < NUM_TRACKS; i++) {
                Track track = contentProviderUtils.getTrack(trackIdList[i]);
                String prefix = TrackNameUtils.getPrefixExportTrack(track) + "-";
                String suffix = "." + trackFileFormat.getExtension();
                String fileName = prefix + track.getName() + suffix;
                Assert.assertTrue(list.contains(fileName));
                list.remove(list.indexOf(fileName));
            }
        }

        // 2. given
        // changes the first track name.
        Track track = contentProviderUtils.getTrack(trackIdList[0]);
        String latterName = "new name";
        track.setName(latterName);
        contentProviderUtils.updateTrack(track);

        // 2. when
        // export all again.
        method.invoke(exportAsyncTask);

        // 3. then
        // the same number of exported tracks but one of them with new name.
        numFiles = exportDirectoryFile.list().length;
        Assert.assertEquals(NUM_TRACKS, numFiles);
        if (numFiles == NUM_TRACKS) {
            List<String> list = new ArrayList<>(Arrays.asList(exportDirectoryFile.list()));
            String prefix = TrackNameUtils.getPrefixExportTrack(track) + "-";
            String suffix = "." + trackFileFormat.getExtension();
            String fileName = prefix + latterName + suffix;
            Assert.assertTrue(list.contains(fileName));
        }
    }

    /**
     * Tests the method {@link ExportAsyncTask#exportAllTracks()}.
     */
    @Test
    public void testExportAllTracks_exportAllTracksAndNewOne() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ParseException {
        // Exporting all tracks results in NUM_TRACKS tracks exported. Later add one more track and export all again results in NUM_TRACKS + 1 tracks.

        // 1. given
        List<Track> trackList = contentProviderUtils.getAllTracks();
        Assert.assertEquals(NUM_TRACKS, trackList.size());
        ExportActivity exportActivity = Mockito.mock(ExportActivity.class);
        TrackFileFormat trackFileFormat = TrackFileFormat.KML_ONLY_TRACK;
        // Method to test in ExportAsyncTask.
        Method method = ExportAsyncTask.class.getDeclaredMethod("exportAllTracks");
        method.setAccessible(true);

        // 1. when
        Mockito.when(exportActivity.getApplicationContext()).thenReturn(context);
        ExportAsyncTask exportAsyncTask = new ExportAsyncTask(exportActivity, trackFileFormat, exportDirectory);
        method.invoke(exportAsyncTask);

        // 1. then
        // number of files exported.
        int numFiles = exportDirectoryFile.list().length;
        Assert.assertEquals(NUM_TRACKS, numFiles);

        // names of the exported files.
        if (numFiles == NUM_TRACKS) {
            List<String> list = new ArrayList<>(Arrays.asList(exportDirectoryFile.list()));
            for (int i = 0; i < NUM_TRACKS; i++) {
                Track track = contentProviderUtils.getTrack(trackIdList[i]);
                String prefix = TrackNameUtils.getPrefixExportTrack(track) + "-";
                String suffix = "." + trackFileFormat.getExtension();
                String fileName = prefix + track.getName() + suffix;
                Assert.assertTrue(list.contains(fileName));
                list.remove(list.indexOf(fileName));
            }
        }

        // New one and export all again.
        // 2. given
        long newTrackId = currentTimeMillis + NUM_TRACKS + 1;
        createTrack(newTrackId, addOneDay(trackStartTimes[NUM_TRACKS - 1]), addOneDay(trackEndTimes[NUM_TRACKS - 1]));
        Track track = contentProviderUtils.getTrack(newTrackId);
        trackList.add(track);
        long[] newTrackIdList = new long[trackIdList.length + 1];
        for (int i = 0; i < trackIdList.length; i++) {
            newTrackIdList[i] = trackIdList[i];
        }
        newTrackIdList[trackIdList.length] = track.getId();

        // 2. when
        method.invoke(exportAsyncTask);

        // 2. then
        // number of files exported: now there is another one.
        numFiles = exportDirectoryFile.list().length;
        Assert.assertEquals(NUM_TRACKS + 1, numFiles);

        // names of the exported files.
        if (numFiles == NUM_TRACKS + 1) {
            List<String> list = new ArrayList<>(Arrays.asList(exportDirectoryFile.list()));
            for (int i = 0; i < NUM_TRACKS + 1; i++) {
                track = contentProviderUtils.getTrack(newTrackIdList[i]);
                String prefix = TrackNameUtils.getPrefixExportTrack(track) + "-";
                String suffix = "." + trackFileFormat.getExtension();
                String fileName = prefix + track.getName() + suffix;
                Assert.assertTrue(list.contains(fileName));
                list.remove(list.indexOf(fileName));
            }
        }
    }
}