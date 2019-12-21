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

package de.dennisguse.opentracks.content;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.dennisguse.opentracks.content.CustomContentProvider.DatabaseHelper;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.WaypointsColumns;

/**
 * Tests {@link CustomContentProvider}.
 *
 * @author Youtao Liu
 */
public class CustomContentProviderTest {

    private static final String DATABASE_NAME = "test.db";

    private SQLiteDatabase db;
    private CustomContentProvider customContentProvider;
    private Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        context.deleteDatabase(DATABASE_NAME);
        db = (new DatabaseHelper(context, DATABASE_NAME)).getWritableDatabase();
        customContentProvider = new CustomContentProvider() {
        };
    }

    /**
     * Tests {@link CustomContentProvider.DatabaseHelper#onCreate(SQLiteDatabase)}.
     */
    @Test
    public void testDatabaseHelper_OnCreate() {
        Assert.assertTrue(hasTable(TracksColumns.TABLE_NAME));
        Assert.assertTrue(hasTable(TrackPointsColumns.TABLE_NAME));
        Assert.assertTrue(hasTable(WaypointsColumns.TABLE_NAME));
    }

    /**
     * Tests {@link CustomContentProvider#onCreate(android.content.Context)}.
     */
    @Test
    public void testOnCreate() {
        Assert.assertTrue(customContentProvider.onCreate(context));
    }

    /**
     * Tests {@link CustomContentProvider#getType(Uri)}.
     */
    @Test
    public void testGetType() {
        Assert.assertEquals(TracksColumns.CONTENT_TYPE, customContentProvider.getType(TracksColumns.CONTENT_URI));
        Assert.assertEquals(TrackPointsColumns.CONTENT_TYPE, customContentProvider.getType(TrackPointsColumns.CONTENT_URI));
        Assert.assertEquals(WaypointsColumns.CONTENT_TYPE, customContentProvider.getType(WaypointsColumns.CONTENT_URI));
    }

    /**
     * Creates a table, containing one column.
     *
     * @param table the table name
     */
    private void createTable(String table) {
        db.execSQL("CREATE TABLE " + table + " (test INTEGER)");
    }

    /**
     * Drops a table in database.
     *
     * @param table the table name
     */
    private void dropTable(String table) {
        db.execSQL("Drop TABLE " + table);
    }

    /**
     * Returns true if the table exists.
     *
     * @param table the table name
     */
    private boolean hasTable(String table) {
        try {
            db.rawQuery("select count(*) from " + table, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if the column in the table exists.
     *
     * @param table  the table name
     * @param column the column name
     */
    private boolean hasColumn(String table, String column) {
        try {
            db.execSQL("SElECT count(*) from  " + table + " order by  " + column);
        } catch (Exception e) {
            if (e.getMessage().contains("no such column")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets up upgrade.
     *
     * @param oldVersion thd old database version
     */
    private void setupUpgrade(int oldVersion) {
        dropTable(TracksColumns.TABLE_NAME);
        dropTable(TrackPointsColumns.TABLE_NAME);
        dropTable(WaypointsColumns.TABLE_NAME);
        createTable(TracksColumns.TABLE_NAME);
        createTable(TrackPointsColumns.TABLE_NAME);
        createTable(WaypointsColumns.TABLE_NAME);

        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        databaseHelper.onUpgrade(db, oldVersion, CustomContentProvider.DATABASE_VERSION);
    }
}