package de.dennisguse.opentracks.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dennisguse.opentracks.data.tables.MarkerColumns;
import de.dennisguse.opentracks.data.tables.TrackPointsColumns;
import de.dennisguse.opentracks.data.tables.TracksColumns;

@RunWith(AndroidJUnit4.class)
public class CustomSQLiteOpenHelperTest {

    private static final String TRACKS_CREATE_TABLE_V23 = "CREATE TABLE tracks (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, starttime INTEGER, stoptime INTEGER, numpoints INTEGER, totaldistance FLOAT, totaltime INTEGER, movingtime INTEGER, avgspeed FLOAT, avgmovingspeed FLOAT, maxspeed FLOAT, minelevation FLOAT, maxelevation FLOAT, elevationgain FLOAT, mingrade FLOAT, maxgrade FLOAT, icon TEXT)";
    private static final String TRACKPOINTS_CREATE_TABLE_V23 = "CREATE TABLE trackpoints (_id INTEGER PRIMARY KEY AUTOINCREMENT, trackid INTEGER, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, speed FLOAT, bearing FLOAT, sensor_heartrate FLOAT, sensor_cadence FLOAT, sensor_power FLOAT)";
    private static final String WAYPOINTS_CREATE_TABLE_V23 = "CREATE TABLE waypoints (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, icon TEXT, trackid INTEGER, type INTEGER, length FLOAT, duration INTEGER, starttime INTEGER, startid INTEGER, stopid INTEGER, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, speed FLOAT, bearing FLOAT, totaldistance FLOAT, totaltime INTEGER, movingtime INTEGER, avgspeed FLOAT, avgmovingspeed FLOAT, maxspeed FLOAT, minelevation FLOAT, maxelevation FLOAT, elevationgain FLOAT, mingrade FLOAT, maxgrade FLOAT, photoUrl TEXT)";

    private static final String DATABASE_NAME = "test.db";

    private final Context context = ApplicationProvider.getApplicationContext();

    /**
     * Get the SQL create statements for all SQLite elements of type (ordered by name).
     *
     * @param type index, table
     * @return Map(name, SQL)
     */
    public static Map<String, String> getSQL(SQLiteDatabase db, String type) {
        HashMap<String, String> sqlMap = new HashMap<>();
        try (Cursor cursor = db.query("sqlite_master", new String[]{"name", "SQL"}, "type=?", new String[]{type}, null, null, "name")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    sqlMap.put(cursor.getString(0), cursor.getString(1));
                }
            }
        }
        return sqlMap;
    }

    /**
     * Returns true if a SQL create statement is in the schema.
     *
     * @param sqlCreate the table name
     */
    private static boolean hasSqlCreate(SQLiteDatabase db, String sqlCreate) {
        try (Cursor cursor = db.rawQuery("SELECT SQL FROM sqlite_master", null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String sql = cursor.getString(0);
                    if (sqlCreate.equals(sql)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Before
    @After
    public void setUp() {
        context.deleteDatabase(DATABASE_NAME);
    }

    @Test
    public void onCreate() {
        try (SQLiteDatabase db = new CustomSQLiteOpenHelper(context, DATABASE_NAME).getWritableDatabase()) {
            assertTrue(hasSqlCreate(db, TracksColumns.CREATE_TABLE));

            assertTrue(hasSqlCreate(db, TrackPointsColumns.CREATE_TABLE));
            assertTrue(hasSqlCreate(db, TrackPointsColumns.CREATE_TABLE_INDEX));

            assertTrue(hasSqlCreate(db, MarkerColumns.CREATE_TABLE));
            assertTrue(hasSqlCreate(db, MarkerColumns.CREATE_TABLE_INDEX));
        } catch (Exception e) {
            fail("Database could not be created: " + e);
        }
    }

    @Test
    public void onUpgrade_FromVersion23() {
        createVersion23();

        // Open database with SQL upgrade
        Map<String, String> tableByUpgrade;
        Map<String, String> indicesByUpgrade;
        try (SQLiteDatabase dbUpgraded = new CustomSQLiteOpenHelper(context, DATABASE_NAME).getReadableDatabase()) {
            tableByUpgrade = getSQL(dbUpgraded, "table");
            indicesByUpgrade = getSQL(dbUpgraded, "index");
        }
        context.deleteDatabase(DATABASE_NAME);

        // Open database via creation script
        Map<String, String> tablesByCreate;
        Map<String, String> indicesByCreate;
        try (SQLiteDatabase dbCreated = new CustomSQLiteOpenHelper(context, DATABASE_NAME).getReadableDatabase()) {
            tablesByCreate = getSQL(dbCreated, "table");
            indicesByCreate = getSQL(dbCreated, "index");
        }


        // then - verify table structure
        int tableCount = 3 + 2; //Three with data tables + two SQLite
        assertEquals(tableCount, tableByUpgrade.size());
        assertEquals(tableByUpgrade.size(), tablesByCreate.size());

        assertEquals(tablesByCreate.get(TracksColumns.TABLE_NAME), tableByUpgrade.get(TracksColumns.TABLE_NAME));
        assertEquals(tablesByCreate.get(TrackPointsColumns.TABLE_NAME), tableByUpgrade.get(TrackPointsColumns.TABLE_NAME));
        assertEquals(tablesByCreate.get(MarkerColumns.TABLE_NAME), tableByUpgrade.get(MarkerColumns.TABLE_NAME));

        // then - verify custom indices
        assertEquals(3, indicesByCreate.size());
        assertEquals(indicesByUpgrade.get(TracksColumns.TABLE_NAME), indicesByCreate.get(TracksColumns.TABLE_NAME));
        assertEquals(indicesByUpgrade.get(TrackPointsColumns.TABLE_NAME), indicesByCreate.get(TrackPointsColumns.TABLE_NAME));
        assertEquals(indicesByUpgrade.get(MarkerColumns.TABLE_NAME), indicesByCreate.get(MarkerColumns.TABLE_NAME));
    }

    @Test
    public void onDowngrade_ToVersion23() {
        // Create most recent database schema
        new CustomSQLiteOpenHelper(context, DATABASE_NAME).getReadableDatabase().close();

        // Downgrade schema to version 23 (base version)
        Map<String, String> tablesByDowngrade;
        Map<String, String> indicesByDowngrade;
        try (SQLiteDatabase db = new CustomSQLiteOpenHelper(context, DATABASE_NAME, 23).getReadableDatabase()) {
            tablesByDowngrade = getSQL(db, "table");
            indicesByDowngrade = getSQL(db, "index");
        }

        // then - verify table structure
        assertEquals(TRACKS_CREATE_TABLE_V23, tablesByDowngrade.get("tracks"));
        assertEquals(TRACKPOINTS_CREATE_TABLE_V23, tablesByDowngrade.get("trackpoints"));
        assertEquals(WAYPOINTS_CREATE_TABLE_V23, tablesByDowngrade.get("waypoints"));

        // then - verify custom indices
        assertEquals(0, indicesByDowngrade.size());
    }

    @Test
    public void track_uuid_unique() {
        try (SQLiteDatabase db = new CustomSQLiteOpenHelper(context, DATABASE_NAME).getWritableDatabase()) {
            db.execSQL("INSERT INTO tracks (uuid) VALUES (0x00)");
            db.execSQL("INSERT INTO tracks (uuid) VALUES (0x00)");
            fail("unique constraint not enforced");
        } catch (SQLiteConstraintException e) {
            assertTrue(e.getMessage().contains("UNIQUE constraint failed: tracks.uuid"));
        }
    }

    @Test
    public void upgrade_data_to_30() {
        // given: a track in version 29
        createVersion23();
        try (SQLiteDatabase db29 = new CustomSQLiteOpenHelper(context, DATABASE_NAME, 29).getWritableDatabase()) {
            db29.beginTransaction();
            db29.execSQL("INSERT INTO tracks (_id) VALUES (1)");

            // Record -> stop
            db29.execSQL("INSERT INTO tracks (_id) VALUES (2)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (2, 2.1, 2.1)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (2, 2.2, 2.2)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (2, 2.3, 2.3)");

            // Record -> pause -> stop
            db29.execSQL("INSERT INTO tracks (_id) VALUES (3)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (3, 3.1, 3.1)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (3, 3.2, 3.2)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (3, 0.0, 100)"); //manual PAUSE

            // Record -> segment marker (distance to previous) -> pause -> resume -> stop
            db29.execSQL("INSERT INTO tracks (_id) VALUES (4)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (4, 4.1, 4.1)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (4, 4.2, 4.2)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (4, 0.0, 100 * 1E6)"); //SegmentEndMarker; will be deleted
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (4, 4.3, 4.3)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (4, 4.4, 4.4)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (4, 0.0, 100 * 1E6)"); //manual PAUSE
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (4, 0.0, 200 * 1E6)"); //manual RESUME
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (4, 4.5, 4.5)");
            db29.execSQL("INSERT INTO trackpoints (trackId, longitude, latitude) VALUES (4, 4.6, 4.6)");

            db29.setTransactionSuccessful();
            db29.endTransaction();
        }

        // when / then
        try (SQLiteDatabase db30 = new CustomSQLiteOpenHelper(context, DATABASE_NAME, 30).getWritableDatabase()) {
            {
                // Track 1
                SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                queryBuilder.setTables("trackpoints");
                queryBuilder.appendWhere("trackid = 1");
                try (Cursor cursor = queryBuilder.query(db30, null, null, null, null, null, "_id")) {
                    assertEquals(0, cursor.getCount());
                }
            }

            {
                // Track 2
                SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                queryBuilder.setTables("trackpoints");
                queryBuilder.appendWhere("trackid = 2");
                try (Cursor cursor = queryBuilder.query(db30, null, null, null, null, null, "_id")) {
                    assertEquals(3, cursor.getCount());
                }
            }

            {
                // Track 3
                SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                queryBuilder.setTables("trackpoints");
                queryBuilder.appendWhere("trackid = 3");
                try (Cursor cursor = queryBuilder.query(db30, null, null, null, null, null, "_id")) {
                    assertEquals(3, cursor.getCount());
                }
            }

            {
                // Track 4
                SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                queryBuilder.setTables("trackpoints");
                queryBuilder.appendWhere("trackid = 4");
                try (Cursor cursor = queryBuilder.query(db30, null, null, null, null, null, "_id")) {
                    assertEquals(8, cursor.getCount());

                    List<Integer> types = new ArrayList<>();
                    List<Double> latitude = new ArrayList<>();
                    cursor.moveToFirst();
                    do {
                        types.add(cursor.getInt(cursor.getColumnIndexOrThrow("type")));
                        if (!cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))) {
                            latitude.add(cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
                        } else {
                            latitude.add(-1.0);
                        }
                    } while (cursor.moveToNext());

                    assertEquals(List.of(0, 0, -1, 0, 1, -2, 0, 0), types);
                    assertEquals(List.of(4.1, 4.2, 4.3, 4.4, -1.0, -1.0, 4.5, 4.6), latitude);
                }
            }
        }
    }

    private void createVersion23() {
        // Manually create database schema with version 23 (base version)
        SQLiteDatabase dbBase = new SQLiteOpenHelper(context, DATABASE_NAME, null, 23) {
            @Override
            public void onCreate(SQLiteDatabase db) {
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            }
        }.getWritableDatabase();

        dbBase.execSQL(TRACKS_CREATE_TABLE_V23);
        dbBase.execSQL(TRACKPOINTS_CREATE_TABLE_V23);
        dbBase.execSQL(WAYPOINTS_CREATE_TABLE_V23);

        dbBase.close();
    }
}