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
package com.google.android.apps.mytracks.io.file.exporter;

/**
 * Listener for {@link TrackExporter} progress.
 * 
 * @author Jimmy Shih
 */
public interface TrackExporterListener {

  /**
   * Called to update progress.
   * 
   * @param number the number of locations written
   * @param max the maximum number of locations in a track, for calculation of
   *          completion percentage
   */
  public void onProgressUpdate(int number, int max);
}