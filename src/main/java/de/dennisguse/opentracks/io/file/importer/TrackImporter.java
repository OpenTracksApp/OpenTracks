/*
 * Copyright 2013 Google Inc.
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
package de.dennisguse.opentracks.io.file.importer;

import java.io.InputStream;

/**
 * Interface for a track importer.
 *
 * @author Jimmy Shih
 */
public interface TrackImporter {

    /**
     * Import a file.
     *
     * @param inputStream the file's input stream
     * @return the imported track id or RECORDING_TRACK_ID_DEFAULT.
     */
    //TODO Figure out how can make the import an atomic operation (incl. database transaction rollback).
    long importFile(InputStream inputStream);
}
