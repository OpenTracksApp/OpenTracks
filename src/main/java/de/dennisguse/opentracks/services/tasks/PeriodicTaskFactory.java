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

package de.dennisguse.opentracks.services.tasks;

import android.content.Context;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * An interface for classes that can create {@link Task}.
 *
 * @author Sandor Dornbush
 */
public interface PeriodicTaskFactory {

    @NonNull
    Task create(Context context);

    /**
     * This is interface for a task that will be executed on some schedule.
     *
     * @author Sandor Dornbush
     */
    interface Task {

        /**
         * Sets up this task for subsequent calls to the run method.
         */
        void start();

        /**
         * This method will be called periodically.
         */
        @Deprecated
        void run(@NonNull Track.Id trackId, @NonNull TrackStatistics trackStatistics);

        /**
         * Shuts down this task and clean up resources.
         */
        void shutdown();
    }
}