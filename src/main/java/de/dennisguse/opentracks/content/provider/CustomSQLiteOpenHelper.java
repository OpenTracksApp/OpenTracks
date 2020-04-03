package de.dennisguse.opentracks.content.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.WaypointsColumns;

/**
 * Database helper for creating and upgrading the database.
 */
@VisibleForTesting
class CustomSQLiteOpenHelper extends SQLiteOpenHelper {

    @VisibleForTesting
    private static final int DATABASE_VERSION = 25;
    private static final String TAG = CustomSQLiteOpenHelper.class.getSimpleName();

    @VisibleForTesting
    private static final String DATABASE_NAME = "database.db";


    public CustomSQLiteOpenHelper(Context context) {
        this(context, DATABASE_NAME);
    }

    @VisibleForTesting
    public CustomSQLiteOpenHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    @VisibleForTesting
    public CustomSQLiteOpenHelper(Context context, String databaseName, int databaseVersion) {
        super(context, databaseName, null, databaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TrackPointsColumns.CREATE_TABLE);
        db.execSQL(TrackPointsColumns.CREATE_TABLE_INDEX);

        db.execSQL(TracksColumns.CREATE_TABLE);

        db.execSQL(WaypointsColumns.CREATE_TABLE);
        db.execSQL(WaypointsColumns.CREATE_TABLE_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int toVersion = oldVersion + 1; toVersion <= newVersion; toVersion++) {
            Log.i(TAG, "Upgrade from " + oldVersion + " to " + toVersion);
            switch (toVersion) {
                case 24:
                    upgradeFrom23to24(db);
                    break;
                case 25:
                    upgradeFrom24to25(db);
                    break;

                default:
                    throw new RuntimeException("Not implemented: upgrade to " + toVersion);
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int toVersion = oldVersion - 1; toVersion >= newVersion; toVersion--) {
            Log.i(TAG, "Downgrade from " + oldVersion + " to " + toVersion);
            switch (toVersion) {
                case 23:
                    downgradeFrom24to23(db);
                    break;
                case 24:
                    downgradeFrom25to24(db);
                    break;

                default:
                    throw new RuntimeException("Not implemented: downgrade to " + toVersion);
            }
        }
    }

    /**
     * Upgrade from database version 23 (waypoints, tracks): remove unused columns.
     * SQLite3 does not support drop columns; therefore new tables are created and data is copied.
     */
    private void upgradeFrom23to24(SQLiteDatabase db) {
        db.beginTransaction();
        db.execSQL("ALTER TABLE tracks RENAME TO tracks_old");
        db.execSQL("CREATE TABLE tracks (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, starttime INTEGER, stoptime INTEGER, numpoints INTEGER, totaldistance FLOAT, totaltime INTEGER, movingtime INTEGER, avgspeed FLOAT, avgmovingspeed FLOAT, maxspeed FLOAT, minelevation FLOAT, maxelevation FLOAT, elevationgain FLOAT, icon TEXT)");
        db.execSQL("INSERT INTO tracks SELECT _id, name, description, category, starttime, stoptime, numpoints, totaldistance, totaltime, movingtime, avgspeed, avgmovingspeed, maxspeed, minelevation, maxelevation, elevationgain, icon FROM tracks_old");
        db.execSQL("DROP TABLE tracks_old");

        db.execSQL("ALTER TABLE waypoints RENAME TO waypoints_old");
        db.execSQL("CREATE TABLE waypoints (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, icon TEXT, trackid INTEGER, length FLOAT, duration INTEGER, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, bearing FLOAT, photoUrl TEXT)");
        db.execSQL("INSERT INTO waypoints SELECT _id, name, description, category, icon, trackid, length, duration, longitude, latitude, time, elevation, accuracy, bearing, photoUrl FROM waypoints_old");
        db.execSQL("DROP TABLE waypoints_old");

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void downgradeFrom24to23(SQLiteDatabase db) {
        //Not needed as the delete columns did not contain any data
        db.beginTransaction();
        db.execSQL("ALTER TABLE tracks RENAME TO tracks_old");
        db.execSQL("CREATE TABLE tracks (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, starttime INTEGER, stoptime INTEGER, numpoints INTEGER, totaldistance FLOAT, totaltime INTEGER, movingtime INTEGER, avgspeed FLOAT, avgmovingspeed FLOAT, maxspeed FLOAT, minelevation FLOAT, maxelevation FLOAT, elevationgain FLOAT, mingrade FLOAT, maxgrade FLOAT, icon TEXT)");
        db.execSQL("INSERT INTO tracks SELECT _id, name, description, category, starttime, stoptime, numpoints, totaldistance, totaltime, movingtime, avgspeed, avgmovingspeed, maxspeed, minelevation, maxelevation, elevationgain, 0, 0, icon FROM tracks_old");
        db.execSQL("DROP TABLE tracks_old");

        db.execSQL("ALTER TABLE waypoints RENAME TO waypoints_old");
        db.execSQL("CREATE TABLE waypoints (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, icon TEXT, trackid INTEGER, type INTEGER, length FLOAT, duration INTEGER, starttime INTEGER, startid INTEGER, stopid INTEGER, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, speed FLOAT, bearing FLOAT, totaldistance FLOAT, totaltime INTEGER, movingtime INTEGER, avgspeed FLOAT, avgmovingspeed FLOAT, maxspeed FLOAT, minelevation FLOAT, maxelevation FLOAT, elevationgain FLOAT, mingrade FLOAT, maxgrade FLOAT, photoUrl TEXT)");
        db.execSQL("INSERT INTO waypoints SELECT _id, name, description, category, icon, trackid, 0, length, duration, 0, 0, 0, longitude, latitude, time, elevation, accuracy, 0, bearing, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, photoUrl FROM waypoints_old");
        db.execSQL("DROP TABLE waypoints_old");

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void upgradeFrom24to25(SQLiteDatabase db) {
        db.beginTransaction();

        db.execSQL("CREATE INDEX trackpoints_trackid_index ON trackpoints(trackid)");
        db.execSQL("CREATE INDEX waypoints_trackid_index ON waypoints(trackid)");

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void downgradeFrom25to24(SQLiteDatabase db) {
        db.beginTransaction();

        db.execSQL("DROP INDEX trackpoints_trackid_index");
        db.execSQL("DROP INDEX waypoints_trackid_index");

        db.setTransactionSuccessful();
        db.endTransaction();
    }
}
