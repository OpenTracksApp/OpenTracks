package de.dennisguse.opentracks.content.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.WaypointsColumns;

/**
 * Database helper for creating and upgrading the database.
 */
@VisibleForTesting
public class CustomSQLiteOpenHelper extends SQLiteOpenHelper {

    @VisibleForTesting
    static final int DATABASE_VERSION = 23;

    @VisibleForTesting
    static final String DATABASE_NAME = "database.db";

    public CustomSQLiteOpenHelper(Context context) {
        this(context, DATABASE_NAME);
    }

    @VisibleForTesting
    public CustomSQLiteOpenHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TrackPointsColumns.CREATE_TABLE);
        db.execSQL(TracksColumns.CREATE_TABLE);
        db.execSQL(WaypointsColumns.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
