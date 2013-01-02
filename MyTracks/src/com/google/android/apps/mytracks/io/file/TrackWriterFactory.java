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

package com.google.android.apps.mytracks.io.file;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;

import android.content.Context;
import android.util.Log;


/**
 * A factory to produce track writers for any format.
 * 
 * @author Rodrigo Damazio
 */
public class TrackWriterFactory {

  private TrackWriterFactory() {}
  
  /**
   * Creates a new track writer to write the track with the given ID.
   * 
   * @param context the context in which the track will be read
   * @param providerUtils the data provider utils to read the track with
   * @param trackId the ID of the track to be written
   * @param format the output format to write in
   * @return the new track writer
   */
  public static TrackWriter newWriter(
      Context context, MyTracksProviderUtils providerUtils, long trackId, TrackFileFormat format) {
    Track track = providerUtils.getTrack(trackId);
    if (track == null) {
      Log.d(TAG, "No track for " + trackId);
      return null;
    }

    TrackFormatWriter writer = format.newFormatWriter(context);
    return new TrackWriterImpl(context, providerUtils, track, writer);
  }
}
