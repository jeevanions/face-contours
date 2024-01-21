package com.google.codelab.mlkit;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ContourData {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String imageUri;
    public String contourPoints;

    // Add a constructor for convenience
    public ContourData(String imageUri, String contourPoints) {
        this.imageUri = imageUri;
        this.contourPoints = contourPoints;
    }
}
