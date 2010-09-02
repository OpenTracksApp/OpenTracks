// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.io;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.same;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.Factory;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackBuffer;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.testing.TestingProviderUtilsFactory;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.test.AndroidTestCase;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
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

    Location l1 = new Location("fake1");
    Location l2 = new Location("fake2");
    Location l3 = new Location("fake3");
    Location l4 = new Location("fake4");
    Location l5 = new Location("fake5");
    Location l6 = new Location("fake6");
    Waypoint p1 = new Waypoint();
    Waypoint p2 = new Waypoint();

    addLocations(l1, l2, l3, l4, l5, l6);
    stubBufferFill(
        new Location[] { l1, l2, l3, l4 },
        new Location[] { l5, l6 });

    track.setStopId(6L);

    // Make location 3 invalid
    l3.setLatitude(100);

    // Begin the track
    formatWriter.writeHeader();
    formatWriter.writeBeginTrack(l1);

    // Write locations 1-2
    formatWriter.writeOpenSegment();
    formatWriter.writeLocation(l1);
    formatWriter.writeLocation(l2);
    formatWriter.writeCloseSegment();

    // Location 3 is not written - it's invalid

    // Write locations 4-6
    formatWriter.writeOpenSegment();
    formatWriter.writeLocation(l4);
    formatWriter.writeLocation(l5);
    formatWriter.writeLocation(l6);
    formatWriter.writeCloseSegment();

    // End the track
    formatWriter.writeEndTrack(l6);

    // Expect reading/writing of the waypoints
    Cursor cursor = mocksControl.createMock(Cursor.class);
    expect(providerUtils.getWaypointsCursor(
        TRACK_ID, 0, MyTracksConstants.MAX_LOADED_WAYPOINTS_POINTS))
        .andStubReturn(cursor);
    expect(cursor.moveToFirst()).andReturn(true);
    expect(cursor.moveToNext()).andReturn(true);
    expect(providerUtils.createWaypoint(cursor)).andReturn(p1);
    formatWriter.writeWaypoint(p1);
    expect(cursor.moveToNext()).andReturn(true);
    expect(providerUtils.createWaypoint(cursor)).andReturn(p2);
    formatWriter.writeWaypoint(p2);
    expect(cursor.moveToNext()).andReturn(false).anyTimes();
    cursor.close();

    formatWriter.writeFooter();
    formatWriter.close();

    mocksControl.replay();
    writer.writeDocument();

    assertTrue(writer.wasSuccess());
    mocksControl.verify();
  }

  private void addLocations(Location... locs) {
    assertTrue(locs.length < 90);
    for (int i = 0; i < locs.length; i++) {
      Location location = locs[i];
      location.setLatitude(i + 1);
      location.setLongitude(i + 1);
    }
  }

  /**
   * Defines the behaviour of filling the track buffer when a read is
   * requested.
   * The IDs of the locations will be their sequential number.
   *
   * @param feeds is a list of location arrays, each element of which
   *        will be fed into the track buffer on each call
   */
  private void stubBufferFill(final Location[]... feeds) {
    providerUtils.getTrackPoints(same(track), isA(TrackBuffer.class));
    EasyMock.expectLastCall().andStubAnswer(new IAnswer<Void>() {
      private int lastId = 1;
      private int reads = 0;

      @Override
      public Void answer() throws Throwable {
        // Get the buffer from the arguments
        Object[] args = EasyMock.getCurrentArguments();
        assertEquals(2, args.length);
        TrackBuffer buffer = (TrackBuffer) args[1];
        assertNotNull(buffer);

        // Check that we still have data to feed to the buffer
        if (reads >= feeds.length) {
          fail("More buffer reads than expected");
        }

        // Fill the buffer
        buffer.reset();
        Location[] locations = feeds[reads];
        for (int i = 0; i < locations.length; i++) {
          buffer.add(locations[i], lastId + i);
        }

        // Update internal state
        lastId += locations.length;
        reads++;
        return null;
      }
    });
  }
}
