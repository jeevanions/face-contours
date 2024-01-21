package com.google.codelab.mlkit;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ContourData.class}, version = 1)
public abstract class ContourDatabase extends RoomDatabase {
    public abstract ContourDao contourDao();
}
