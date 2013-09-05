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

import android.util.Log;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages register/unregister {@link TrackDataListener} and keeping the state
 * for each registered listener.
 * 
 * @author Rodrigo Damazio
 */
public class TrackDataManager {

  // Map of listener to its track data types
  private final Map<TrackDataListener, EnumSet<TrackDataType>>
      listenerToTypesMap = new HashMap<TrackDataListener, EnumSet<TrackDataType>>();

  // Map of track data type to listeners
  private final Map<TrackDataType, Set<TrackDataListener>>
      typeToListenersMap = new EnumMap<TrackDataType, Set<TrackDataListener>>(TrackDataType.class);

  public TrackDataManager() {
    for (TrackDataType trackDataType : TrackDataType.values()) {
      typeToListenersMap.put(trackDataType, new LinkedHashSet<TrackDataListener>());
    }
  }

  /**
   * Registers a listener.
   * 
   * @param listener the listener
   * @param trackDataTypes the track data types the listener is interested
   */
  public void registerListener(
      TrackDataListener listener, EnumSet<TrackDataType> trackDataTypes) {
    if (listenerToTypesMap.containsKey(listener)) {
      Log.w(TAG, "Tried to register a listener that is already registered. Ignore.");
      return;
    }
    listenerToTypesMap.put(listener, trackDataTypes);
    for (TrackDataType trackDataType : trackDataTypes) {
      typeToListenersMap.get(trackDataType).add(listener);
    }
  }

  /**
   * Unregisters a listener.
   * 
   * @param listener the listener
   */
  public void unregisterListener(TrackDataListener listener) {
    EnumSet<TrackDataType> removedTypes = listenerToTypesMap.remove(listener);
    if (removedTypes == null) {
      Log.w(TAG, "Tried to unregister a listener that is not registered. Ignore.");
      return;
    }

    // Remove the listener from the typeToListenersMap
    for (TrackDataType trackDataType : removedTypes) {
      typeToListenersMap.get(trackDataType).remove(listener);
    }
  }

  /**
   * Gets the number of {@link TrackDataListener}.
   */
  public int getNumberOfListeners() {
    return listenerToTypesMap.size();
  }

  /**
   * Gets the track data types for a listener.
   * 
   * @param listener the listener
   */
  public EnumSet<TrackDataType> getTrackDataTypes(TrackDataListener listener) {
    return listenerToTypesMap.get(listener);
  }

  /**
   * Gets the listeners for a {@link TrackDataType}.
   * 
   * @param type the type
   */
  public Set<TrackDataListener> getListeners(TrackDataType type) {
    return typeToListenersMap.get(type);
  }

  /**
   * Gets all the registered {@link TrackDataType}.
   */
  public EnumSet<TrackDataType> getRegisteredTrackDataTypes() {
    EnumSet<TrackDataType> types = EnumSet.noneOf(TrackDataType.class);
    for (EnumSet<TrackDataType> value : listenerToTypesMap.values()) {
      types.addAll(value);
    }
    return types;
  }
}
