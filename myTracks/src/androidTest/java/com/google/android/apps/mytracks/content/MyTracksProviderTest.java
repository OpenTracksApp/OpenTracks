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

import com.google.android.apps.mytracks.content.MyTracksProvider.DatabaseHelper;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.AndroidTestCase;

/**
 * Tests {@link MyTracksProvider}.
 * 
 * @author Youtao Liu
 */
public class MyTracksProviderTest extends AndroidTestCase {

  private static final String DATABASE_NAME = "mytrackstest.db";

  private SQLiteDatabase db;
  private MyTracksProvider myTracksProvider;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    getContext().deleteDatabase(DATABASE_NAME);
    db = (new DatabaseHelper(getContext(), DATABASE_NAME)).getWritableDatabase();
    myTracksProvider = new MyTracksProvider();
  }

  /**
   * Tests {@link MyTracksProvider.DatabaseHelper#onCreate(SQLiteDatabase)}.
   */
  public void testDatabaseHelper_OnCreate() {
    assertTrue(hasTable(TracksColumns.TABLE_NAME));
    assertTrue(hasTable(TrackPointsColumns.TABLE_NAME));
    assertTrue(hasTable(WaypointsColumns.TABLE_NAME));
  }

  /**
   * Tests {@link MyTracksProvider.DatabaseHelper#onUpgrade(SQLiteDatabase, int,
   * int)} when version is less than 17.
   */
  public void testDatabaseHelper_onUpgrade_Version16() {
    dropTable(TracksColumns.TABLE_NAME);
    dropTable(TrackPointsColumns.TABLE_NAME);
    dropTable(WaypointsColumns.TABLE_NAME);

    int oldVersion = 16;
    DatabaseHelper databaseHelper = new DatabaseHelper(getContext());
    databaseHelper.onUpgrade(db, oldVersion, MyTracksProvider.DATABASE_VERSION);

    assertTrue(hasTable(TracksColumns.TABLE_NAME));
    assertTrue(hasTable(TrackPointsColumns.TABLE_NAME));
    assertTrue(hasTable(WaypointsColumns.TABLE_NAME));
  }

  /**
   * Tests {@link MyTracksProvider.DatabaseHelper#onUpgrade(SQLiteDatabase, int,
   * int)} when version is 17.
   */
  public void testDatabaseHelper_onUpgrade_Version17() {
    setupUpgrade(17);

    assertTrue(hasColumn(TrackPointsColumns.TABLE_NAME, TrackPointsColumns.SENSOR));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.TABLEID));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.ICON));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.DRIVEID));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.MODIFIEDTIME));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDWITHME));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDOWNER));
    assertTrue(hasColumn(WaypointsColumns.TABLE_NAME, WaypointsColumns.PHOTOURL));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.CALORIE));
  }

  /**
   * Tests {@link MyTracksProvider.DatabaseHelper#onUpgrade(SQLiteDatabase, int,
   * int)} when version is 18.
   */
  public void testDatabaseHelper_onUpgrade_Version18() {
    setupUpgrade(18);

    assertFalse(hasColumn(TrackPointsColumns.TABLE_NAME, TrackPointsColumns.SENSOR));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.TABLEID));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.ICON));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.DRIVEID));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.MODIFIEDTIME));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDWITHME));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDOWNER));
    assertTrue(hasColumn(WaypointsColumns.TABLE_NAME, WaypointsColumns.PHOTOURL));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.CALORIE));
  }

  /**
   * Tests {@link MyTracksProvider.DatabaseHelper#onUpgrade(SQLiteDatabase, int,
   * int)} when version is 19.
   */
  public void testDatabaseHelper_onUpgrade_Version19() {
    setupUpgrade(19);

    assertFalse(hasColumn(TrackPointsColumns.TABLE_NAME, TrackPointsColumns.SENSOR));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.TABLEID));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.ICON));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.DRIVEID));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.MODIFIEDTIME));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDWITHME));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDOWNER));
    assertTrue(hasColumn(WaypointsColumns.TABLE_NAME, WaypointsColumns.PHOTOURL));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.CALORIE));
  }

  /**
   * Tests {@link MyTracksProvider.DatabaseHelper#onUpgrade(SQLiteDatabase, int,
   * int)} when version is 20.
   */
  public void testDatabaseHelper_onUpgrade_Version20() {
    setupUpgrade(20);

    assertFalse(hasColumn(TrackPointsColumns.TABLE_NAME, TrackPointsColumns.SENSOR));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.TABLEID));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.ICON));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.DRIVEID));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.MODIFIEDTIME));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDWITHME));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDOWNER));
    assertTrue(hasColumn(WaypointsColumns.TABLE_NAME, WaypointsColumns.PHOTOURL));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.CALORIE));
  }

  /**
   * Tests {@link MyTracksProvider.DatabaseHelper#onUpgrade(SQLiteDatabase, int,
   * int)} when version is 21.
   */
  public void testDatabaseHelper_onUpgrade_Version21() {
    setupUpgrade(21);

    assertFalse(hasColumn(TrackPointsColumns.TABLE_NAME, TrackPointsColumns.SENSOR));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.TABLEID));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.ICON));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.DRIVEID));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.MODIFIEDTIME));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDWITHME));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDOWNER));
    assertTrue(hasColumn(WaypointsColumns.TABLE_NAME, WaypointsColumns.PHOTOURL));
    assertTrue(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.CALORIE));
  }

  /**
   * Tests {@link MyTracksProvider.DatabaseHelper#onUpgrade(SQLiteDatabase, int,
   * int)} when version is 22.
   */
  public void testDatabaseHelper_onUpgrade_Version22() {
    setupUpgrade(22);

    assertFalse(hasColumn(TrackPointsColumns.TABLE_NAME, TrackPointsColumns.SENSOR));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.TABLEID));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.ICON));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.DRIVEID));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.MODIFIEDTIME));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDWITHME));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.SHAREDOWNER));
    assertFalse(hasColumn(WaypointsColumns.TABLE_NAME, WaypointsColumns.PHOTOURL));
    assertFalse(hasColumn(TracksColumns.TABLE_NAME, TracksColumns.CALORIE));
  }

  /**
   * Tests {@link MyTracksProvider#onCreate(android.content.Context)}.
   */
  public void testOnCreate() {
    assertTrue(myTracksProvider.onCreate(getContext()));
  }

  /**
   * Tests {@link MyTracksProvider#getType(Uri)}.
   */
  public void testGetType() {
    assertEquals(TracksColumns.CONTENT_TYPE, myTracksProvider.getType(TracksColumns.CONTENT_URI));
    assertEquals(
        TrackPointsColumns.CONTENT_TYPE, myTracksProvider.getType(TrackPointsColumns.CONTENT_URI));
    assertEquals(
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

    DatabaseHelper databaseHelper = new DatabaseHelper(getContext());
    databaseHelper.onUpgrade(db, oldVersion, MyTracksProvider.DATABASE_VERSION);
  }
}