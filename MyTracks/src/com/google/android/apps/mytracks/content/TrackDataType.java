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

/**
 * Types of track data.
 * 
 * @author Jimmy Shih
 */
public enum TrackDataType {
  TRACKS_TABLE, // tracks table changes
  WAYPOINTS_TABLE, // waypoints table changes
  SAMPLED_IN_TRACK_POINTS_TABLE, // sampled-in track points table changes
  SAMPLED_OUT_TRACK_POINTS_TABLE, // sampled-out track points table changes
  PREFERENCE // preference changes
}