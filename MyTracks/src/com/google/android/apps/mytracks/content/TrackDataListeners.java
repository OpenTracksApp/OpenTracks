/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks.content;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.content.TrackDataHub.ListenerDataType;

import android.util.Log;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manager for the external data listeners and their listening types.
 *
 * @author Rodrigo Damazio
 */
class TrackDataListeners {

  /** Internal representation of a listener's registration. */
  static class ListenerRegistration {
    final TrackDataListener listener;
    final EnumSet<ListenerDataType> types;
    // TODO: Add the last-notified point ID here, to allow pausing/resuming.

    public ListenerRegistration(TrackDataListener listener,
        EnumSet<ListenerDataType> types) {
      this.listener = listener;
      this.types = types;
    }

    public boolean isInterestedIn(ListenerDataType type) {
      return types.contains(type);
    }
  }

  /** Map of external listener to its registration details. */
  private final Map<TrackDataListener, ListenerRegistration> registeredListeners =
      new HashMap<TrackDataListener, ListenerRegistration>();

  /** Map of data type to external listeners interested in it. */
  private final Map<ListenerDataType, Set<TrackDataListener>> listenerSetsPerType =
      new EnumMap<ListenerDataType, Set<TrackDataListener>>(ListenerDataType.class);

  public TrackDataListeners() {
    // Create sets for all data types at startup.
    for (ListenerDataType type : ListenerDataType.values()) {
      listenerSetsPerType.put(type, new LinkedHashSet<TrackDataListener>());
    }
  }

  /**
   * Registers a listener to send data to.
   * It is ok to call this method before {@link start}, and in that case
   * the data will only be passed to listeners when {@link start} is called.
   *
   * @param listener the listener to register
   * @param dataTypes the type of data that the listener is interested in
   */
  public ListenerRegistration registerTrackDataListener(final TrackDataListener listener, EnumSet<ListenerDataType> dataTypes) {
    Log.d(TAG, "Registered track data listener: " + listener);
    ListenerRegistration registration = new ListenerRegistration(listener, dataTypes);

    if (registeredListeners.get(listener) != null) {
      throw new IllegalStateException("Listener already registered");
    }
    registeredListeners.put(listener, registration);

    for (ListenerDataType type : dataTypes) {
      // This is guaranteed not to be null.
      Set<TrackDataListener> typeSet = listenerSetsPerType.get(type);
      typeSet.add(listener);
    }

    return registration;
  }

  /**
   * Unregisters a listener to send data to.
   *
   * @param listener the listener to unregister
   */
  public void unregisterTrackDataListener(TrackDataListener listener) {
    Log.d(TAG, "Unregistered track data listener: " + listener);
    // Remove and keep the corresponding registration.
    ListenerRegistration match = registeredListeners.remove(listener);
    if (match == null) {
      Log.w(TAG, "Tried to unregister listener which is not registered.");
      return;
    }

    // Remove it from the per-type sets
    for (ListenerDataType type : match.types) {
      listenerSetsPerType.get(type).remove(listener);
    }
  }

  public ListenerRegistration getRegistration(TrackDataListener listener) {
    return registeredListeners.get(listener);
  }

  public Set<TrackDataListener> getListenersFor(ListenerDataType type) {
    return listenerSetsPerType.get(type);
  }

  public EnumSet<ListenerDataType> getAllRegisteredTypes() {
    EnumSet<ListenerDataType> listeners = EnumSet.noneOf(ListenerDataType.class);
    for (ListenerRegistration registration : this.registeredListeners.values()) {
      listeners.addAll(registration.types);
    }
    return listeners;
  }

  public boolean hasListeners() {
    return !registeredListeners.isEmpty();
  }
}
