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
package com.google.android.apps.mytracks.io.backup;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.content.Track;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Manager responsible for reading and writing the backup state.
 * 
 * @author Rodrigo Damazio
 */
class BackupStateManager {
  private static final byte BACKUP_VERSION = 1;

  /**
   * The state of a single track.
   */
  public static class TrackState {
    public final long trackId;
    public final String name;
    public final String description;
    public final String category;

    TrackState(long trackId, String name, String description, String category) {
      this.trackId = trackId;
      this.name = name;
      this.description = description;
      this.category = category;
    }

    /**
     * Checks and returns whether this state is representative of the given
     * track at its current state.
     */
    public boolean represents(Track track) {
      return trackId == track.getId() &&
          name.equals(track.getName()) &&
          description.equals(track.getDescription()) &&
          category.equals(track.getCategory());
    }
  }

  // File descriptors to read/write state from/to
  private final ParcelFileDescriptor oldState;
  private final ParcelFileDescriptor newState;

  // Actual state data read from/to be written to the descriptors above
  private Map<Long, TrackState> oldTracks;
  private List<TrackState> newTrackState;

  public BackupStateManager(ParcelFileDescriptor oldState, ParcelFileDescriptor newState) {
    this.oldState = oldState;
    this.newState = newState;
  }

  /**
   * Reads the old track state.
   * 
   * If any errors occur while reading, this will return a partial or empty map.
   *
   * @return a map of track ID to its state
   */
  public Map<Long, TrackState> getOldTrackState() {
    if (oldTracks != null) {
      return oldTracks;
    }

    oldTracks = new TreeMap<Long, TrackState>();
    if (!hasOldState()) {
      return oldTracks;
    }

    try {
      InputStream stateInputStream = openStateInput();
      DataInputStream stateInput = new DataInputStream(stateInputStream);

      // Read the version from the header
      if (stateInput.readByte() != BACKUP_VERSION) {
        throw new IllegalStateException("Bad backup version");
      }

      int previousNumberOfTracks = stateInput.readInt();
      for (int i = 0; i < previousNumberOfTracks; i++) {
        TrackState oldTrack = readTrackState(stateInput);
        oldTracks.put(oldTrack.trackId, oldTrack);
      }
    } catch (IOException e) {
      // If we fail to read, return whatever we got - this only means
      // we'll be backing up more than we should.
      Log.e(MyTracksConstants.TAG, "Unable to read old state", e);
    }
    return oldTracks;
  }

  /**
   * Returns whether we have a known old state.
   */
  protected boolean hasOldState() {
    return oldState != null;
  }

  /**
   * Opens the old state and returns a stream to read it.
   */
  protected InputStream openStateInput() {
    return new FileInputStream(oldState.getFileDescriptor());
  }

  /**
   * Opens the new state and returns a stream to write to it.
   */
  protected OutputStream openStateOutput() {
    return new FileOutputStream(newState.getFileDescriptor());
  }

  /**
   * Reads the state of a single track.
   *
   * @param stateInput the stream to read from
   * @return the state read
   * @throws IOException if reading fails
   */
  private TrackState readTrackState(DataInputStream stateInput) throws IOException {
    long trackId = stateInput.readLong();
    String name = stateInput.readUTF();
    String desc = stateInput.readUTF();
    String category = stateInput.readUTF();
    return new TrackState(trackId, name, desc, category);
  }

  /**
   * Adds a track to the current/new state.
   * Please notice that the state is not actually written until
   * {@link #flushNewState} is called.
   */
  public void addTrackState(Track track) {
    ensureTrackState();

    newTrackState.add(new TrackState(track.getId(), track.getName(),
        track.getDescription(), track.getCategory()));
  }

  /**
   * Actually writes out the new state to its output.
   *
   * @throws IOException if there are errors while writing
   */
  public void flushNewState() throws IOException {
    ensureTrackState();

    OutputStream stateOutputStream = openStateOutput();
    DataOutputStream stateOutput = new DataOutputStream(stateOutputStream);
  
    stateOutput.writeByte(BACKUP_VERSION);

    stateOutput.writeInt(newTrackState.size());
    for (TrackState track : newTrackState) {
      writeTrackState(stateOutput, track);
    }

    stateOutput.flush();
  }

  /**
   * Ensures the track state buffer exists.
   */
  private void ensureTrackState() {
    if (newTrackState == null) {
      newTrackState = new ArrayList<TrackState>();
    }
  }

  /**
   * Writes the state of a single track.
   *
   * @param stateOutput the stream to write to
   * @param track the track state to write
   * @throws IOException if there are errors while writing
   */
  private void writeTrackState(DataOutputStream stateOutput, TrackState track) throws IOException {
    stateOutput.writeLong(track.trackId);
    stateOutput.writeUTF(track.name);
    stateOutput.writeUTF(track.description);
    stateOutput.writeUTF(track.category);
  }
}
