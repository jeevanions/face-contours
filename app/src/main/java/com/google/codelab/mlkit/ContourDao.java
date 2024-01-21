package com.google.codelab.mlkit;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ContourDao {
    @Insert
    long insertContourData(ContourData contourData);

    //@Query("SELECT * FROM contour_data")
    //List<ContourData> getAllContourData();

    // ... other CRUD operations as needed
}
