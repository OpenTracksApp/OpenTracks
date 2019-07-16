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

package com.google.android.apps.mytracks.content;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.apps.mytracks.content.MyTracksProvider.DatabaseHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * Tests {@link MyTracksProvider}.
 * 
 * @author Youtao Liu
 */
@RunWith(AndroidJUnit4.class)
public class MyTracksProviderTest {

  private static final String DATABASE_NAME = "mytrackstest.db";

  private SQLiteDatabase db;
  private MyTracksProvider myTracksProvider;

  @Before
  protected void setUp()  {
    InstrumentationRegistry.getInstrumentation().getContext().deleteDatabase(DATABASE_NAME);
    db = (new DatabaseHelper(InstrumentationRegistry.getInstrumentation().getContext(), DATABASE_NAME)).getWritableDatabase();
    myTracksProvider = new MyTracksProvider();
  }

  /**
   * Tests {@link MyTracksProvider.DatabaseHelper#onCreate(SQLiteDatabase)}.
   */
  public void testDatabaseHelper_OnCreate() {
    Assert.assertTrue(hasTable(TracksColumns.TABLE_NAME));
    Assert.assertTrue(hasTable(TrackPointsColumns.TABLE_NAME));
    Assert.assertTrue(hasTable(WaypointsColumns.TABLE_NAME));
  }

  /**
   * Tests {@link MyTracksProvider#onCreate(android.content.Context)}.
   */
  public void testOnCreate() {
    Assert.assertTrue(myTracksProvider.onCreate(InstrumentationRegistry.getInstrumentation().getContext()));
  }

  /**
   * Tests {@link MyTracksProvider#getType(Uri)}.
   */
  public void testGetType() {
    Assert.assertEquals(TracksColumns.CONTENT_TYPE, myTracksProvider.getType(TracksColumns.CONTENT_URI));
    Assert.assertEquals(
        TrackPointsColumns.CONTENT_TYPE, myTracksProvider.getType(TrackPointsColumns.CONTENT_URI));
    Assert.assertEquals(
        WaypointsColumns.CONTENT_TYPE, myTracksProvider.getType(WaypointsColumns.CONTENT_URI));
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
   * @param table the table name
   * @param column the column name
   */
  private boolean hasColumn(String table, String column) {
    try {
      db.execSQL("SElECT count(*) from  " + table + " order by  " + column);
    } catch (Exception e) {
      if (e.getMessage().indexOf("no such column") > -1) {
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

    DatabaseHelper databaseHelper = new DatabaseHelper(InstrumentationRegistry.getInstrumentation().getContext());
    databaseHelper.onUpgrade(db, oldVersion, MyTracksProvider.DATABASE_VERSION);
  }
}