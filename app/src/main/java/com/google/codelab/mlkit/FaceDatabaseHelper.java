package com.google.codelab.mlkit;

import android.content.ContentValues;
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
                    COLUMN_SIZE + " TEXT," +
                    COLUMN_URI + " TEXT" + ")";

    public FaceDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void  insertFaceDetection(String faces,String size,String imageUri ) {

        // on below line we are creating a variable for
        // our sqlite database and calling writable method
        // as we are writing data in our database.
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values
        ContentValues values = new ContentValues();
        values.put(COLUMN_FACES, faces);
        values.put(COLUMN_SIZE,size);
        values.put(COLUMN_URI,imageUri);

        // after adding all values we are passing
        // content values to our table.
        long newRowId = db.insert(TABLE_NAME, null, values);
        if (newRowId == -1) {
            // Handle error
        }
        // at last we are closing our
        // database after adding database.
        db.close();
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
