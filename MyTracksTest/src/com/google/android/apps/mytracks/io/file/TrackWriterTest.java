// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.io.file;

import static org.easymock.EasyMock.expect;

import com.google.android.apps.mytracks.content.MyTracksProvider;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.Factory;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.services.TrackRecordingServiceTest.MockContext;
import com.google.android.apps.mytracks.testing.TestingProviderUtilsFactory;

import android.content.Context;
import android.location.Location;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;

/**
 * Tests for the track writer.
 *
 * @author Rodrigo Damazio
 */
public class TrackWriterTest extends AndroidTestCase {
  /**
   * {@link TrackWriterImpl} subclass which mocks out methods called from
   * {@link TrackWriterImpl#openFile}.
   */
  private static final class OpenFileTrackWriter extends TrackWriterImpl {
    private final ByteArrayOutputStream stream;
    private final boolean canWrite;

    /**
     * Constructor.
     *
     * @param stream the stream to return from
     *        {@link TrackWriterImpl#newOutputStream}, or null to throw a
     *        {@link FileNotFoundException}
     * @param canWrite the value that {@link TrackWriterImpl#canWriteFile} will
     *        return
     */
    private OpenFileTrackWriter(Context context,
        MyTracksProviderUtils providerUtils, Track track,
        TrackFormatWriter writer, ByteArrayOutputStream stream,
        boolean canWrite) {
      super(context, providerUtils, track, writer);

      this.stream = stream;
      this.canWrite = canWrite;
      
      // The directory is set in the canWriteFile. However, this class
      // overwrites canWriteFile, thus needs to set it.
      setDirectory(new File("/"));
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
   * {@link TrackWriterImpl} subclass which mocks out methods called from
   * {@link TrackWriterImpl#writeTrack}.
   */
  private final class WriteTracksTrackWriter extends TrackWriterImpl {
    private final boolean openResult;

    /**
     * Constructor.
     *
     * @param openResult the return value for {@link TrackWriterImpl#openFile}
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
  private TrackWriterImpl writer;
  private IMocksControl mocksControl;
  private MyTracksProviderUtils providerUtils;
  private Factory oldProviderUtilsFactory;

  // State used in specific tests
  private int writeDocumentCalls;
  private int openFileCalls;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    MockContentResolver mockContentResolver = new MockContentResolver();
    RenamingDelegatingContext targetContext = new RenamingDelegatingContext(
        getContext(), getContext(), "test.");
    Context context = new MockContext(mockContentResolver, targetContext);
    MyTracksProvider provider = new MyTracksProvider();
    provider.attachInfo(context, null);
    mockContentResolver.addProvider(MyTracksProviderUtils.AUTHORITY, provider);
    setContext(context);
    providerUtils = MyTracksProviderUtils.Factory.get(context);
    oldProviderUtilsFactory = TestingProviderUtilsFactory.installWithInstance(providerUtils);

    mocksControl = EasyMock.createStrictControl();
    formatWriter = mocksControl.createMock(TrackFormatWriter.class);
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

    mocksControl.replay();
    writer.writeTrack();

    assertEquals(1, writeDocumentCalls);
    assertEquals(1, openFileCalls);
    mocksControl.verify();
  }

  public void testWriteTrack_openFails() {
    writer = new WriteTracksTrackWriter(getContext(), providerUtils, track,
        formatWriter, false);

    mocksControl.replay();
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

  public void testWriteDocument_emptyTrack() throws Exception {
    writer = new TrackWriterImpl(getContext(), providerUtils, track, formatWriter);

    // Set expected mock behavior
    formatWriter.writeHeader();
    formatWriter.writeBeginTrack(null);
    formatWriter.writeEndTrack(null);
    formatWriter.writeFooter();
    formatWriter.close();

    mocksControl.replay();
    writer.writeDocument();

    assertTrue(writer.wasSuccess());
    mocksControl.verify();
  }

  /**
   * Tests when a track only contains invalid locations. Make sure an empty
   * track is written.
   */
  public void testWriteDocument_oneInvalidLocation() throws Exception {
    writer = new TrackWriterImpl(getContext(), providerUtils, track, formatWriter);

    Location[] locs = { new Location("fake0") };
    fillLocations(locs);

    // Make location invalid
    locs[0].setLatitude(100);

    assertEquals(locs.length, providerUtils.bulkInsertTrackPoint(locs, locs.length, TRACK_ID));

    formatWriter.writeHeader();
    formatWriter.writeBeginTrack(null);
    formatWriter.writeEndTrack(null);
    formatWriter.writeFooter();
    formatWriter.close();

    mocksControl.replay();
    writer.writeDocument();

    assertTrue(writer.wasSuccess());
    mocksControl.verify();
  }

  public void testWriteDocument() throws Exception {
    writer = new TrackWriterImpl(getContext(), providerUtils, track, formatWriter);

    final Location[] locs = {
        new Location("fake0"),
        new Location("fake1"),
        new Location("fake2"),
        new Location("fake3"),
        new Location("fake4"),
        new Location("fake5")
    };
    Waypoint[] wps = { new Waypoint(), new Waypoint(), new Waypoint() };

    // Fill locations with valid values
    fillLocations(locs);

    // Make location 3 invalid
    locs[2].setLatitude(100);

    assertEquals(locs.length, providerUtils.bulkInsertTrackPoint(locs, locs.length, TRACK_ID));
    for (int i = 0;  i < wps.length; ++i) {
      Waypoint wpt = wps[i];
      wpt.setTrackId(TRACK_ID);
      assertNotNull(providerUtils.insertWaypoint(wpt));
      wpt.setId(i + 1);
    }

    formatWriter.writeHeader();

    // Expect reading/writing of the waypoints (except the first)
    formatWriter.writeBeginWaypoints();
    formatWriter.writeWaypoint(wptEq(wps[1]));
    formatWriter.writeWaypoint(wptEq(wps[2]));
    formatWriter.writeEndWaypoints();

    // Begin the track
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

    formatWriter.writeFooter();
    formatWriter.close();

    mocksControl.replay();
    writer.writeDocument();

    assertTrue(writer.wasSuccess());
    mocksControl.verify();
  }

  private static Waypoint wptEq(final Waypoint wpt) {
    EasyMock.reportMatcher(new IArgumentMatcher() {
      @Override
      public boolean matches(Object wptObj2) {
        if (wptObj2 == null || wpt == null) return wpt == wptObj2;
        Waypoint wpt2 = (Waypoint) wptObj2;

        return wpt.getId() == wpt2.getId();
      }

      @Override
      public void appendTo(StringBuffer buffer) {
        buffer.append("wptEq(");
        buffer.append(wpt);
        buffer.append(")");
      }
    });
    return null;
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

  private void fillLocations(Location... locs) {
    assertTrue(locs.length < 90);
    for (int i = 0; i < locs.length; i++) {
      Location location = locs[i];
      location.setLatitude(i + 1);
      location.setLongitude(i + 1);
      location.setTime(i + 1000);
    }
  }
}
