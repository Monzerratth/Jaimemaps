package com.example.jaimemaps;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;


public class MarkerDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "jaimemaps.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE = "markers";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_LAT = "lat";
    private static final String COL_LNG = "lng";

    public static class SavedMarker {
        public final long id;
        public final String title;
        public final double lat;
        public final double lng;

        SavedMarker(long id, String title, double lat, double lng) {
            this.id = id;
            this.title = title;
            this.lat = lat;
            this.lng = lng;
        }
    }

    public MarkerDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // no incluye ningún dato del usuario.
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITLE + " TEXT NOT NULL, "
                + COL_LAT + " REAL NOT NULL, "
                + COL_LNG + " REAL NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }


    public long insertMarker(String title, double lat, double lng) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_LAT, lat);
        values.put(COL_LNG, lng);
        return getWritableDatabase().insert(TABLE, null, values);
    }

    public List<SavedMarker> getAllMarkers() {
        List<SavedMarker> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE,
                new String[]{COL_ID, COL_TITLE, COL_LAT, COL_LNG},
                null, null, null, null, COL_ID + " ASC")) {
            while (c.moveToNext()) {
                result.add(new SavedMarker(c.getLong(0), c.getString(1),
                        c.getDouble(2), c.getDouble(3)));
            }
        }
        return result;
    }


    public List<SavedMarker> searchByTitle(String text) {
        List<SavedMarker> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE,
                new String[]{COL_ID, COL_TITLE, COL_LAT, COL_LNG},
                COL_TITLE + " LIKE ?", new String[]{"%" + text + "%"},
                null, null, COL_ID + " ASC")) {
            while (c.moveToNext()) {
                result.add(new SavedMarker(c.getLong(0), c.getString(1),
                        c.getDouble(2), c.getDouble(3)));
            }
        }
        return result;
    }

    public int deleteAllMarkers() {
        return getWritableDatabase().delete(TABLE, null, null);
    }
}
