package com.google.codelab.mlkit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FaceDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "faceDatabase.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "face_data";
    public static final String COLUMN_ID = "id";

    public static final String COLUMN_FACES = "faces";
    public static final String COLUMN_URI = "uri";
    public static final String COLUMN_SIZE = "size";


    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_FACES + " TEXT," +
                    COLUMN_URI + " TEXT" + ")";

    public FaceDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database version upgrades here
    }
}
