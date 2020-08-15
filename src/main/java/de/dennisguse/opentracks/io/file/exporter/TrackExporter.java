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

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.OutputStream;

/**
 * Track exporting for exporting track to an {@link OutputStream}.
 *
 * @author Jimmy Shih
 */
public interface TrackExporter {

    /**
     * Write track to an output stream.
     * Depending on the implementation a context might be required.
     *
     * @param outputStream the output stream
     */
    boolean writeTrack(@NonNull OutputStream outputStream);
}
