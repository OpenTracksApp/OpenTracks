package de.dennisguse.opentracks.content.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.UUID;

import de.dennisguse.opentracks.content.data.MarkerColumns;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.util.UUIDUtils;

/**
 * Database helper for creating and upgrading the database.
 */
@VisibleForTesting
public class CustomSQLiteOpenHelper extends SQLiteOpenHelper {

    private static final String TAG = CustomSQLiteOpenHelper.class.getSimpleName();

    private static final int DATABASE_VERSION = 29;

    @VisibleForTesting
    public static final String DATABASE_NAME = "database.db";

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
        db.execSQL(TracksColumns.CREATE_TABLE_INDEX);

        db.execSQL(MarkerColumns.CREATE_TABLE);
        db.execSQL(MarkerColumns.CREATE_TABLE_INDEX);
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
                case 26:
                    upgradeFrom25to26(db);
                    break;
                case 27:
                    upgradeFrom26to27(db);
                    break;
                case 28:
                    upgradeFrom27to28(db);
                    break;
                case 29:
                    upgradeFrom28to29(db);
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
                case 25:
                    downgradeFrom26to25(db);
                    break;
                case 26:
                    downgradeFrom27to26(db);
                    break;
                case 27:
                    downgradeFrom28to27(db);
                    break;
                case 28:
                    downgradeFrom29to28(db);
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
        //Not needed as the deleted columns did not contain any data
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

    /**
     * Add indeces for foreign key trackId
     */
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

    /**
     * Add track UUID to prevent re-import of existing tracks
     */
    private void upgradeFrom25to26(SQLiteDatabase db) {
        db.beginTransaction();

        db.execSQL("ALTER TABLE tracks ADD COLUMN uuid BLOB");
        try (Cursor cursor = db.query("tracks", new String[]{"_id"}, null, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                int trackIdIndex = cursor.getColumnIndexOrThrow("_id");
                do {
                    Track.Id trackId = new Track.Id(cursor.getLong(trackIdIndex));
                    ContentValues cv = new ContentValues();
                    cv.put("uuid", UUIDUtils.toBytes(UUID.randomUUID()));
                    db.update("tracks", cv, "_id = ?", new String[]{String.valueOf(trackId.getId())});
                } while (cursor.moveToNext());
            }
        }

        db.execSQL("CREATE UNIQUE INDEX tracks_uuid_index ON tracks(uuid)");

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void downgradeFrom26to25(SQLiteDatabase db) {
        db.beginTransaction();

        db.execSQL("DROP INDEX tracks_uuid_index");

        db.execSQL("ALTER TABLE tracks RENAME TO tracks_old");
        db.execSQL("CREATE TABLE tracks (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, starttime INTEGER, stoptime INTEGER, numpoints INTEGER, totaldistance FLOAT, totaltime INTEGER, movingtime INTEGER, avgspeed FLOAT, avgmovingspeed FLOAT, maxspeed FLOAT, minelevation FLOAT, maxelevation FLOAT, elevationgain FLOAT, mingrade FLOAT, maxgrade FLOAT, icon TEXT)");
        db.execSQL("INSERT INTO tracks SELECT _id, name, description, category, starttime, stoptime, numpoints, totaldistance, totaltime, movingtime, avgspeed, avgmovingspeed, maxspeed, minelevation, maxelevation, elevationgain, 0, 0, icon FROM tracks_old");
        db.execSQL("DROP TABLE tracks_old");

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * Add elevation gain
     */
    private void upgradeFrom26to27(SQLiteDatabase db) {
        db.beginTransaction();

        db.execSQL("ALTER TABLE trackpoints ADD COLUMN elevation_gain FLOAT");

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void downgradeFrom27to26(SQLiteDatabase db) {
        db.beginTransaction();

        db.execSQL("ALTER TABLE trackpoints RENAME TO trackpoints_old");
        db.execSQL("CREATE TABLE trackpoints (_id INTEGER PRIMARY KEY AUTOINCREMENT, trackid INTEGER, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, speed FLOAT, bearing FLOAT, sensor_heartrate FLOAT, sensor_cadence FLOAT, sensor_power FLOAT)");
        db.execSQL("INSERT INTO trackpoints SELECT _id, trackid, longitude, latitude, time, elevation, accuracy, speed, bearing, sensor_heartrate, sensor_cadence, sensor_power FROM trackpoints_old");
        db.execSQL("DROP TABLE trackpoints_old");

        db.execSQL("CREATE INDEX trackpoints_trackid_index ON trackpoints(trackid)");

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * Add foreign key constraints on trackId
     */
    private void upgradeFrom27to28(SQLiteDatabase db) {
        db.beginTransaction();

        // TrackPoints
        db.execSQL("ALTER TABLE trackpoints RENAME TO trackpoints_old");
        db.execSQL("CREATE TABLE trackpoints (_id INTEGER PRIMARY KEY AUTOINCREMENT, trackid INTEGER NOT NULL, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, speed FLOAT, bearing FLOAT, sensor_heartrate FLOAT, sensor_cadence FLOAT, sensor_power FLOAT, elevation_gain FLOAT, FOREIGN KEY (trackid) REFERENCES tracks(_id) ON UPDATE CASCADE ON DELETE CASCADE)");
        db.execSQL("INSERT INTO trackpoints SELECT _id, trackid, longitude, latitude, time, elevation, accuracy, speed, bearing, sensor_heartrate, sensor_cadence, sensor_power, elevation_gain FROM trackpoints_old");
        db.execSQL("DROP TABLE trackpoints_old");

        db.execSQL("CREATE INDEX trackpoints_trackid_index ON trackpoints(trackid)");

        // Markers
        db.execSQL("ALTER TABLE waypoints RENAME TO markers_old");
        db.execSQL("CREATE TABLE markers (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, icon TEXT, trackid INTEGER NOT NULL, length FLOAT, duration INTEGER, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, bearing FLOAT, photoUrl TEXT, FOREIGN KEY (trackid) REFERENCES tracks(_id) ON UPDATE CASCADE ON DELETE CASCADE)");

        db.execSQL("INSERT INTO markers SELECT _id, name, description, category, icon, trackid, length, duration, longitude, latitude, time, elevation, accuracy, bearing, photoUrl FROM markers_old");
        db.execSQL("DROP TABLE markers_old");

        db.execSQL("CREATE INDEX markers_trackid_index ON markers(trackid)");

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void downgradeFrom28to27(SQLiteDatabase db) {
        db.beginTransaction();

        // TrackPoints
        db.execSQL("ALTER TABLE trackpoints RENAME TO trackpoints_old");
        db.execSQL("CREATE TABLE trackpoints (_id INTEGER PRIMARY KEY AUTOINCREMENT, trackid INTEGER NOT NULL, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, speed FLOAT, bearing FLOAT, sensor_heartrate FLOAT, sensor_cadence FLOAT, sensor_power FLOAT, elevation_gain FLOAT)");
        db.execSQL("INSERT INTO trackpoints SELECT _id, trackid, longitude, latitude, time, elevation, accuracy, speed, bearing, sensor_heartrate, sensor_cadence, sensor_power, elevation_gain FROM trackpoints_old");
        db.execSQL("DROP TABLE trackpoints_old");

        db.execSQL("CREATE INDEX trackpoints_trackid_index ON trackpoints(trackid)");

        // Markers
        db.execSQL("CREATE TABLE waypoints (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, icon TEXT, trackid INTEGER, length FLOAT, duration INTEGER, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, bearing FLOAT, photoUrl TEXT)");

        db.execSQL("INSERT INTO waypoints SELECT _id, name, description, category, icon, trackid, length, duration, longitude, latitude, time, elevation, accuracy, bearing, photoUrl FROM markers");
        db.execSQL("DROP TABLE markers");

        db.execSQL("CREATE INDEX waypoints_trackid_index ON waypoints(trackid)");

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * Add elevation loss.
     */
    private void upgradeFrom28to29(SQLiteDatabase db) {
        db.beginTransaction();

        db.execSQL("ALTER TABLE tracks ADD COLUMN elevationloss FLOAT");
        db.execSQL("ALTER TABLE trackpoints ADD COLUMN elevation_loss FLOAT");

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void downgradeFrom29to28(SQLiteDatabase db) {
        db.beginTransaction();

        // Tracks
        db.execSQL("DROP INDEX tracks_uuid_index");

        db.execSQL("ALTER TABLE tracks RENAME TO tracks_old");
        db.execSQL("CREATE TABLE tracks (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, starttime INTEGER, stoptime INTEGER, numpoints INTEGER, totaldistance FLOAT, totaltime INTEGER, movingtime INTEGER, avgspeed FLOAT, avgmovingspeed FLOAT, maxspeed FLOAT, minelevation FLOAT, maxelevation FLOAT, elevationgain FLOAT, icon TEXT, uuid BLOB)");
        db.execSQL("INSERT INTO tracks SELECT _id, name, description, category, starttime, stoptime, numpoints, totaldistance, totaltime, movingtime, avgspeed, avgmovingspeed, maxspeed, minelevation, maxelevation, elevationgain, icon, uuid FROM tracks_old");
        db.execSQL("DROP TABLE tracks_old");

        db.execSQL("CREATE UNIQUE INDEX tracks_uuid_index ON tracks(uuid)");

        // TrackPoints
        db.execSQL("DROP INDEX trackpoints_trackid_index");

        db.execSQL("ALTER TABLE trackpoints RENAME TO trackpoints_old");
        db.execSQL("CREATE TABLE trackpoints (_id INTEGER PRIMARY KEY AUTOINCREMENT, trackid INTEGER NOT NULL, longitude INTEGER, latitude INTEGER, time INTEGER, elevation FLOAT, accuracy FLOAT, speed FLOAT, bearing FLOAT, sensor_heartrate FLOAT, sensor_cadence FLOAT, sensor_power FLOAT, elevation_gain FLOAT, FOREIGN KEY (trackid) REFERENCES tracks(_id) ON UPDATE CASCADE ON DELETE CASCADE)");
        db.execSQL("INSERT INTO trackpoints SELECT _id, trackid, longitude, latitude, time, elevation, accuracy, speed, bearing, sensor_heartrate, sensor_cadence, sensor_power, elevation_gain FROM trackpoints_old");
        db.execSQL("DROP TABLE trackpoints_old");

        db.execSQL("CREATE INDEX trackpoints_trackid_index ON trackpoints(trackid)");

        db.setTransactionSuccessful();
        db.endTransaction();
    }
}
