package de.dennisguse.opentracks;

public class TestApplication extends Startup {
    @Override
    public String getDatabaseName() {
        // null will make SQLiteOpenHelper create an in-memory database
        return null;
    }
}
