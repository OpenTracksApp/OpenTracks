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

package de.dennisguse.opentracks.io.file.exporter;

import androidx.annotation.NonNull;

import java.io.OutputStream;
import java.util.List;

import de.dennisguse.opentracks.data.models.Track;

/**
 * Track exporting for exporting track to an {@link OutputStream}.
 *
 * @author Jimmy Shih
 */
public interface TrackExporter {

    boolean writeTrack(List<Track> tracks, @NonNull OutputStream outputStream);
}
