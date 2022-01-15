/*
 * Copyright 2012 Google Inc.
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

package de.dennisguse.opentracks.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import de.dennisguse.opentracks.data.tables.MarkerColumns;
import de.dennisguse.opentracks.data.tables.TrackPointsColumns;
import de.dennisguse.opentracks.data.tables.TracksColumns;

/**
 * Tests {@link CustomContentProvider}.
 *
 * @author Youtao Liu
 */
public class CustomContentProviderTest {

    private CustomContentProvider customContentProvider;
    private final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        customContentProvider = new CustomContentProvider() {
        };
    }

    /**
     * Tests {@link CustomContentProvider#onCreate(android.content.Context)}.
     */
    @Test
    public void testOnCreate() {
        assertTrue(customContentProvider.onCreate(context));
    }

    /**
     * Tests {@link CustomContentProvider#getType(Uri)}.
     */
    @Test
    public void testGetType() {
        assertEquals(TracksColumns.CONTENT_TYPE, customContentProvider.getType(TracksColumns.CONTENT_URI));
        assertEquals(TracksColumns.CONTENT_ITEMTYPE, customContentProvider.getType(ContentUris.appendId(TracksColumns.CONTENT_URI.buildUpon(), 1).build()));

        assertEquals(TrackPointsColumns.CONTENT_TYPE, customContentProvider.getType(TrackPointsColumns.CONTENT_URI_BY_ID));
        assertEquals(TrackPointsColumns.CONTENT_ITEMTYPE, customContentProvider.getType(ContentUris.appendId(TrackPointsColumns.CONTENT_URI_BY_TRACKID.buildUpon(), 1).build()));

        assertEquals(MarkerColumns.CONTENT_TYPE, customContentProvider.getType(MarkerColumns.CONTENT_URI));
        assertEquals(MarkerColumns.CONTENT_ITEMTYPE, customContentProvider.getType(ContentUris.appendId(MarkerColumns.CONTENT_URI.buildUpon(), 1).build()));
    }
}