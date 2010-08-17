package com.google.android.apps.mytracks.io.backup;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.backup.BackupStateManager.TrackState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Tests for {@link BackupStateManager}.
 *
 * @author Rodrigo Damazio
 */
public class BackupStateManagerTest extends TestCase {

  /**
   * Testable version of the state manager which doesn't use actual files.
   */
  private static class TestableBackupStateManager extends BackupStateManager {
    private final byte[] oldStateData;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public TestableBackupStateManager(byte[] oldStateData) {
      super(null, null);

      this.oldStateData = oldStateData;
    }

    @Override
    protected OutputStream openStateOutput() {
      return outputStream;
    }

    public ByteArrayOutputStream getOutputStream() {
      return outputStream;
    }

    @Override
    protected InputStream openStateInput() {
      if (!hasOldState()) {
        throw new IllegalStateException("Don't have old state");
      }

      return new ByteArrayInputStream(oldStateData);
    }

    @Override
    protected boolean hasOldState() {
      return oldStateData != null;
    }
  }

  public void testTrackStateRepresents() {
    TrackState state = new TrackState(123L, "name", "desc", "cat");
    Track stateTrack = makeTrack(123L, "name", "desc", "cat");

    assertTrue(state.represents(stateTrack));
  }

  public void testTrackStateRepresents_wrongId() {
    TrackState state = new TrackState(123L, "name", "desc", "cat");
    Track stateTrack = makeTrack(456L, "name", "desc", "cat");

    assertFalse(state.represents(stateTrack));
  }

  public void testTrackStateRepresents_wrongName() {
    TrackState state = new TrackState(123L, "name", "desc", "cat");
    Track stateTrack = makeTrack(123L, "name2", "desc", "cat");

    assertFalse(state.represents(stateTrack));
  }

  public void testTrackStateRepresents_wrongDescription() {
    TrackState state = new TrackState(123L, "name", "desc", "cat");
    Track stateTrack = makeTrack(123L, "name", "desc2", "cat");

    assertFalse(state.represents(stateTrack));
  }

  public void testTrackStateRepresents_wrongCategory() {
    TrackState state = new TrackState(123L, "name", "desc", "cat");
    Track stateTrack = makeTrack(123L, "name", "desc", "cat2");

    assertFalse(state.represents(stateTrack));
  }

  public void testGetOldTrackState_empty() {
    TestableBackupStateManager stateManager = new TestableBackupStateManager(null);
    Map<Long, TrackState> oldState = stateManager.getOldTrackState();
    assertTrue(oldState.isEmpty());
  }

  public void testWriteReadState() throws Exception {
    // Initially create a state manager which knows no state
    TestableBackupStateManager stateManager = new TestableBackupStateManager(null);

    // Add some state to it
    stateManager.addTrackState(makeTrack(123L, "Name1"));
    stateManager.addTrackState(makeTrack(456L, "Name2"));
    stateManager.flushNewState();

    // Get the result
    byte[] stateData = stateManager.getOutputStream().toByteArray();

    // Now create a state manager with the previous serialized state
    stateManager = new TestableBackupStateManager(stateData);

    // Read its old state and verify it
    Map<Long, TrackState> oldState = stateManager.getOldTrackState();
    assertEquals(oldState.toString(), 2, oldState.size());
    assertTrue(oldState.containsKey(123L));
    assertTrue(oldState.containsKey(456L));
    assertStateEquals(123L, "Name1", oldState.get(123L));
    assertStateEquals(456L, "Name2", oldState.get(456L));

    // Write a new, different state
    stateManager.addTrackState(makeTrack(123L, "Name1"));
    stateManager.addTrackState(makeTrack(987L, "Name3"));
    stateManager.flushNewState();

    // Get the new result
    stateData = stateManager.getOutputStream().toByteArray();

    // Create another state manager to read this last state
    stateManager = new TestableBackupStateManager(stateData);

    // Read and verify it
    oldState = stateManager.getOldTrackState();
    assertEquals(oldState.toString(), 2, oldState.size());
    assertTrue(oldState.containsKey(123L));
    assertTrue(oldState.containsKey(987L));
    assertStateEquals(123L, "Name1", oldState.get(123L));
    assertStateEquals(987L, "Name3", oldState.get(987L));
  }

  private Track makeTrack(long id, String name) {
    return makeTrack(id, name, "Description for " + name, "Category for " + name);
  }

  private Track makeTrack(long id, String name, String description, String category) {
    Track track = new Track();
    track.setId(id);
    track.setName(name);
    track.setDescription(description);
    track.setCategory(category);
    return track;
  }

  private void assertStateEquals(long id, String name, TrackState state) {
    assertEquals(id, state.trackId);
    assertEquals(name, state.name);
    assertEquals("Description for " + name, state.description);
    assertEquals("Category for " + name, state.category);
  }
}
