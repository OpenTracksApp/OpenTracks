package de.dennisguse.opentracks;

public interface AppConfig {
    /**
     * Returns the name of the database used by SQLiteOpenHelper.
     * See {@link android.database.sqlite.SQLiteOpenHelper} for details.
     * @return SQLite database name.
     */
    String getDatabaseName();
}
