// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.io;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.leq;
import static org.easymock.EasyMock.same;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.Factory;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.testing.TestingProviderUtilsFactory;

import android.content.Context;
import android.database.MatrixCursor;
import android.location.Location;
import android.provider.BaseColumns;
import android.test.AndroidTestCase;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;

/**
 * Tests for the track writer.
 *
 * @author Rodrigo Damazio
 */
public class TrackWriterTest extends AndroidTestCase {
  /**
   * {@link TrackWriter} subclass which mocks out methods called from
   * {@link TrackWriter#openFile}.
   */
  private static final class OpenFileTrackWriter extends TrackWriter {
    private final ByteArrayOutputStream stream;
    private final boolean canWrite;

    /**
     * Constructor.
     *
     * @param stream the stream to return from
     *        {@link TrackWriter#newOutputStream}, or null to throw a
     *        {@link FileNotFoundException}
     * @param canWrite the value that {@link TrackWriter#canWriteFile} will
     *        return
     */
    private OpenFileTrackWriter(Context context,
        MyTracksProviderUtils providerUtils, Track track,
        TrackFormatWriter writer, ByteArrayOutputStream stream,
        boolean canWrite) {
      super(context, providerUtils, track, writer);

      this.stream = stream;
      this.canWrite = canWrite;
    }

    @Override
    protected boolean canWriteFile() {
      return canWrite;
    }

    @Override
    protected OutputStream newOutputStream(String fileName)
        throws FileNotFoundException {
      assertEquals(FULL_TRACK_NAME, fileName);

      if (stream == null) {
        throw new FileNotFoundException();
      }

      return stream;
    }
  }

  /**
   * {@link TrackWriter} subclass which mocks out methods called from
   * {@link TrackWriter#writeTrack}.
   */
  private final class WriteTracksTrackWriter extends TrackWriter {
    private final boolean openResult;

    /**
     * Constructor.
     *
     * @param openResult the return value for {@link TrackWriter#openFile}
     */
    private WriteTracksTrackWriter(Context context,
        MyTracksProviderUtils providerUtils, Track track,
        TrackFormatWriter writer, boolean openResult) {
      super(context, providerUtils, track, writer);
      this.openResult = openResult;
    }

    @Override
    protected boolean openFile() {
      openFileCalls++;
      return openResult;
    }

    @Override
    void writeDocument() {
      writeDocumentCalls++;
    }

    @Override
    protected void runOnUiThread(Runnable runnable) {
      runnable.run();
    }
  }

  private static final long TRACK_ID = 1234567L;
  private static final String EXTENSION = "ext";
  private static final String TRACK_NAME = "Swimming across the pacific";
  private static final String FULL_TRACK_NAME =
      "Swimming across the pacific.ext";

  private Track track;
  private TrackFormatWriter formatWriter;
  private TrackWriter writer;
  private IMocksControl mocksControl;
  private MyTracksProviderUtils providerUtils;
  private Factory oldProviderUtilsFactory;

  // State used in specific tests
  private int writeDocumentCalls;
  private int openFileCalls;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    mocksControl = EasyMock.createStrictControl();
    formatWriter = mocksControl.createMock(TrackFormatWriter.class);
    providerUtils = mocksControl.createMock(MyTracksProviderUtils.class);
    oldProviderUtilsFactory =
        TestingProviderUtilsFactory.installWithInstance(providerUtils);

    expect(formatWriter.getExtension()).andStubReturn(EXTENSION);

    track = new Track();
    track.setName(TRACK_NAME);
    track.setId(TRACK_ID);
  }

  @Override
  protected void tearDown() throws Exception {
    TestingProviderUtilsFactory.restoreOldFactory(oldProviderUtilsFactory);
    super.tearDown();
  }

  public void testWriteTrack() {
    writer = new WriteTracksTrackWriter(getContext(), providerUtils, track,
        formatWriter, true);

    // Expect the completion callback to be run
    Runnable completionCallback = mocksControl.createMock(Runnable.class);
    completionCallback.run();

    mocksControl.replay();
    writer.setOnCompletion(completionCallback);
    writer.writeTrack();

    assertEquals(1, writeDocumentCalls);
    assertEquals(1, openFileCalls);
    mocksControl.verify();
  }

  public void testWriteTrack_openFails() {
    writer = new WriteTracksTrackWriter(getContext(), providerUtils, track,
        formatWriter, false);

    // Expect the completion callback to be run
    Runnable completionCallback = mocksControl.createMock(Runnable.class);
    completionCallback.run();

    mocksControl.replay();
    writer.setOnCompletion(completionCallback);
    writer.writeTrack();

    assertEquals(0, writeDocumentCalls);
    assertEquals(1, openFileCalls);
    mocksControl.verify();
  }
  
  public void testOpenFile() {
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    writer = new OpenFileTrackWriter(
        getContext(), providerUtils, track, formatWriter, stream, true);

    formatWriter.prepare(track, stream);

    mocksControl.replay();
    assertTrue(writer.openFile());
    mocksControl.verify();
  }

  public void testOpenFile_cantWrite() {
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    writer = new OpenFileTrackWriter(
        getContext(), providerUtils, track, formatWriter, stream, false);

    mocksControl.replay();
    assertFalse(writer.openFile());
    mocksControl.verify();
  }

  public void testOpenFile_streamError() {
    writer = new OpenFileTrackWriter(
        getContext(), providerUtils, track, formatWriter, null, true);

    mocksControl.replay();
    assertFalse(writer.openFile());
    mocksControl.verify();
  }

  public void testWriteDocument_emptyTrack() {
    writer = new TrackWriter(getContext(), providerUtils, track, formatWriter);

    // Don't let it write any waypoints
    expect(providerUtils.getWaypointsCursor(
        TRACK_ID, 0, MyTracksConstants.MAX_LOADED_WAYPOINTS_POINTS))
        .andStubReturn(null);
    expect(providerUtils.getLocationsCursor(
        eq(TRACK_ID), leq(0L), leq(0), eq(false))).andStubReturn(null);

    // Set expected mock behavior
    formatWriter.writeHeader();
    formatWriter.writeFooter();
    formatWriter.close();

    mocksControl.replay();
    writer.writeDocument();

    assertTrue(writer.wasSuccess());
    mocksControl.verify();
  }

  public void testWriteDocument() {
    writer = new TrackWriter(getContext(), providerUtils, track, formatWriter);

    final Location[] locs = {
        new Location("fake0"),
        new Location("fake1"),
        new Location("fake2"),
        new Location("fake3"),
        new Location("fake4"),
        new Location("fake5"),
    };
    Waypoint[] wps = { new Waypoint(), new Waypoint(), new Waypoint() };

    // Fill locations with valid values
    fillLocations(locs);

    // Make location 3 invalid
    locs[2].setLatitude(100);

    // Set up cursors
    // We use fake columns since the cursor is only read by the provider utils
    final MatrixCursor locCursor =
        new MatrixCursor(new String[] { BaseColumns._ID }, 6);
    for (int i = 1; i <= 6; i++) {
      locCursor.newRow().add(i);
    }
    expect(providerUtils.getLocationsCursor(
        eq(TRACK_ID), leq(0L), leq(0), eq(false))).andStubReturn(locCursor);
    providerUtils.fillLocation(same(locCursor), isA(Location.class));
    EasyMock.expectLastCall().andStubAnswer(new IAnswer<Void>() {
      @Override
      public Void answer() throws Throwable {
        Location loc = (Location) EasyMock.getCurrentArguments()[1];
        loc.set(locs[locCursor.getPosition()]);
        return null;
      }
    });

    MatrixCursor wpCursor =
        new MatrixCursor(new String[] { BaseColumns._ID }, 3);
    wpCursor.newRow().add(1);
    wpCursor.newRow().add(2);
    wpCursor.newRow().add(3);
    expect(providerUtils.getWaypointsCursor(
        eq(TRACK_ID), leq(0L),
        eq(MyTracksConstants.MAX_LOADED_WAYPOINTS_POINTS)))
        .andStubReturn(wpCursor);
    expect(providerUtils.createWaypoint(wpCursor))
        .andStubAnswer(stubCursorToArray(wpCursor, wps));

    // Begin the track
    formatWriter.writeHeader();
    formatWriter.writeBeginTrack(locEq(locs[0]));

    // Write locations 1-2
    formatWriter.writeOpenSegment();
    formatWriter.writeLocation(locEq(locs[0]));
    formatWriter.writeLocation(locEq(locs[1]));
    formatWriter.writeCloseSegment();

    // Location 3 is not written - it's invalid

    // Write locations 4-6
    formatWriter.writeOpenSegment();
    formatWriter.writeLocation(locEq(locs[3]));
    formatWriter.writeLocation(locEq(locs[4]));
    formatWriter.writeLocation(locEq(locs[5]));
    formatWriter.writeCloseSegment();

    // End the track
    formatWriter.writeEndTrack(locEq(locs[5]));

    // Expect reading/writing of the waypoints (except the first)
    formatWriter.writeWaypoint(wps[1]);
    formatWriter.writeWaypoint(wps[2]);

    formatWriter.writeFooter();
    formatWriter.close();

    mocksControl.replay();
    writer.writeDocument();

    assertTrue(writer.wasSuccess());
    mocksControl.verify();
  }

  private static Location locEq(final Location loc) {
    EasyMock.reportMatcher(new IArgumentMatcher() {
      @Override
      public boolean matches(Object locObj2) {
        if (locObj2 == null || loc == null) return loc == locObj2;
        Location loc2 = (Location) locObj2;

        return loc.hasAccuracy() == loc2.hasAccuracy()
            && (!loc.hasAccuracy() || loc.getAccuracy() == loc2.getAccuracy())
            && loc.hasAltitude() == loc2.hasAltitude()
            && (!loc.hasAltitude() || loc.getAltitude() == loc2.getAltitude())
            && loc.hasBearing() == loc2.hasBearing()
            && (!loc.hasBearing() || loc.getBearing() == loc2.getBearing())
            && loc.hasSpeed() == loc2.hasSpeed()
            && (!loc.hasSpeed() || loc.getSpeed() == loc2.getSpeed())
            && loc.getLatitude() == loc2.getLatitude()
            && loc.getLongitude() == loc2.getLongitude()
            && loc.getTime() == loc2.getTime();
      }
      
      @Override
      public void appendTo(StringBuffer buffer) {
        buffer.append("locEq(");
        buffer.append(loc);
        buffer.append(")");
      }
    });
    return null;
  }
  
  private <T> IAnswer<T> stubCursorToArray(
      final MatrixCursor cursor, final T[] values) {
    return new IAnswer<T>() {
      @Override
      public T answer() throws Throwable {
        return values[cursor.getPosition()];
      }
    };
  }

  private void fillLocations(Location... locs) {
    assertTrue(locs.length < 90);
    for (int i = 0; i < locs.length; i++) {
      Location location = locs[i];
      location.setLatitude(i + 1);
      location.setLongitude(i + 1);
    }
  }
}
