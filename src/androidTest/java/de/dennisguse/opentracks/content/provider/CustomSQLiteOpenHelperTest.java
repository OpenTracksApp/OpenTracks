package de.dennisguse.opentracks.content.provider;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.VisibleForTesting;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.WaypointsColumns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class CustomSQLiteOpenHelperTest {

    private static final String TRACKS_CREATE_TABLE_V23 = "CREATE TABLE tracks (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, starttime INTEGER, stoptime INTEGER, numpoints INTEGER, totaldistance FLOAT, totaltime INTEGER, movingtime INTEGER, avgspeed FLOAT, avgmovingspeed FLOAT, maxspeed FLOAT, minelevation FLOAT, maxelevation FLOAT, elevationgain FLOAT, mingrade FLOAT, maxgrade FLOAT, icon TEXT)";
    private static final String TRACKPOINTS_CREATE_TABLE_V23 = "CREATE TABLE trackpoints (_id INTEGER PRIMARY KEY AUTOINCREMENT, trackid INTEGER, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, speed FLOAT, bearing FLOAT, sensor_heartrate FLOAT, sensor_cadence FLOAT, sensor_power FLOAT)";
    private static final String WAYPOINTS_CREATE_TABLE_V23 = "CREATE TABLE waypoints (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, icon TEXT, trackid INTEGER, type INTEGER, length FLOAT, duration INTEGER, starttime INTEGER, startid INTEGER, stopid INTEGER, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, speed FLOAT, bearing FLOAT, totaldistance FLOAT, totaltime INTEGER, movingtime INTEGER, avgspeed FLOAT, avgmovingspeed FLOAT, maxspeed FLOAT, minelevation FLOAT, maxelevation FLOAT, elevationgain FLOAT, mingrade FLOAT, maxgrade FLOAT, photoUrl TEXT)";

    private static final String DATABASE_NAME = "test.db";

    private final Context context = ApplicationProvider.getApplicationContext();

    /**
     * Get the SQL create statements for all tables (ordered by name).
     *
     * @return Map(TableName, SQL)
     */
    @VisibleForTesting
    public static Map<String, String> getTableSQL(SQLiteDatabase db) {
        HashMap<String, String> tableSQL = new HashMap<>();
        try (Cursor cursor = db.query("sqlite_master", new String[]{"name", "SQL"}, "name IN ('" + TracksColumns.TABLE_NAME + "', '" + TrackPointsColumns.TABLE_NAME + "', '" + WaypointsColumns.TABLE_NAME + "')", null, null, null, "name")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    tableSQL.put(cursor.getString(0), cursor.getString(1));
                }
            }
        }
        return tableSQL;
    }

    /**
     * Get the SQL create statements for all indices (ordered by name).
     *
     * @return Map(tableName, SQL)
     */
    @VisibleForTesting
    public static Map<String, String> getIndexSQL(SQLiteDatabase db) {
        HashMap<String, String> indexSQL = new HashMap<>();
        try (Cursor cursor = db.rawQuery("SELECT tbl_name, SQL FROM sqlite_master WHERE type = 'index'", null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    indexSQL.put(cursor.getString(0), cursor.getString(1));
                }
            }
        }
        return indexSQL;
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

            assertTrue(hasSqlCreate(db, WaypointsColumns.CREATE_TABLE));
            assertTrue(hasSqlCreate(db, WaypointsColumns.CREATE_TABLE_INDEX));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void onUpgrade_FromVersion23() {
        createVersion23();

        // Open database with SQL upgrade
        Map<String, String> tableByUpgrade;
        Map<String, String> indicesByUpgrade;
        try (SQLiteDatabase dbUpgraded = new CustomSQLiteOpenHelper(context, DATABASE_NAME).getReadableDatabase()) {
            tableByUpgrade = getTableSQL(dbUpgraded);
            indicesByUpgrade = getIndexSQL(dbUpgraded);
        }
        context.deleteDatabase(DATABASE_NAME);

        // Open database via creation script
        Map<String, String> tablesByCreate;
        Map<String, String> indicesByCreate;
        try (SQLiteDatabase dbCreated = new CustomSQLiteOpenHelper(context, DATABASE_NAME).getReadableDatabase()) {
            tablesByCreate = getTableSQL(dbCreated);
            indicesByCreate = getIndexSQL(dbCreated);
        }

        // then - verify table structure
        assertEquals(3, tableByUpgrade.size());
        assertEquals(tableByUpgrade.size(), tablesByCreate.size());

        assertEquals(tablesByCreate.get(TracksColumns.TABLE_NAME), tableByUpgrade.get(TracksColumns.TABLE_NAME));
        assertEquals(tablesByCreate.get(TrackPointsColumns.TABLE_NAME), tableByUpgrade.get(TrackPointsColumns.TABLE_NAME));
        assertEquals(tablesByCreate.get(WaypointsColumns.TABLE_NAME), tableByUpgrade.get(WaypointsColumns.TABLE_NAME));

        // then - verify custom indices
        assertEquals(3, indicesByCreate.size());
        assertEquals(indicesByUpgrade.get(TracksColumns.TABLE_NAME), indicesByCreate.get(TracksColumns.TABLE_NAME));
        assertEquals(indicesByUpgrade.get(TrackPointsColumns.TABLE_NAME), indicesByCreate.get(TrackPointsColumns.TABLE_NAME));
        assertEquals(indicesByUpgrade.get(WaypointsColumns.TABLE_NAME), indicesByCreate.get(WaypointsColumns.TABLE_NAME));
    }

    @Test
    public void onDowngrade_ToVersion23() {
        // Create most recent database schema
        new CustomSQLiteOpenHelper(context, DATABASE_NAME).getReadableDatabase().close();

        // Downgrade schema to version 23 (base version)
        Map<String, String> tablesByDowngrade;
        Map<String, String> indicesByDowngrade;
        try (SQLiteDatabase db = new CustomSQLiteOpenHelper(context, DATABASE_NAME, 23).getReadableDatabase()) {
            tablesByDowngrade = getTableSQL(db);
            indicesByDowngrade = getIndexSQL(db);
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