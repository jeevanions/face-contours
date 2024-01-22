/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.codelab.mlkit;

import static com.google.codelab.mlkit.FaceDatabaseHelper.COLUMN_FACES;
import static com.google.codelab.mlkit.FaceDatabaseHelper.COLUMN_SIZE;
import static com.google.codelab.mlkit.FaceDatabaseHelper.COLUMN_URI;
import static com.google.codelab.mlkit.FaceDatabaseHelper.TABLE_NAME;
import static java.lang.Math.max;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.annotation.KeepName;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.base.Strings;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Activity demonstrating different image detector features with a still image from camera. */
@KeepName
public final class StillImageActivity extends AppCompatActivity {

  private static final String TAG = "StillImageActivity";
  private static final String FACE_DETECTION = "Face Detection";
  private ProgressBar progressBar;

  public static final String SIZE_SCREEN = "w:screen"; // Match screen width
  public static final String SIZE_1024_768 = "w:1024"; // ~1024*768 in a normal ratio
  public static final String SIZE_640_480 = "w:640"; // ~640*480 in a normal ratio
  public static final String SIZE_ORIGINAL = "w:original"; // Original image size

  private static final String KEY_IMAGE_URI = "com.google.codelab.mlkit.KEY_IMAGE_URI";
  private static final String KEY_SELECTED_SIZE = "com.google.codelab.mlkit.KEY_SELECTED_SIZE";

  private static final int REQUEST_IMAGE_CAPTURE = 1001;
  private static final int REQUEST_CHOOSE_IMAGE = 1002;

  private ImageView preview;
  private GraphicOverlay graphicOverlay;
//  private String selectedMode = OBJECT_DETECTION;
  private String selectedSize = SIZE_SCREEN;

  boolean isLandScape;

  private Uri imageUri;
  private int imageMaxWidth;
  private int imageMaxHeight;

  private Button mBtnOpenGallery, mBtnTakePhoto,mBtnSaveContours;

  private FaceDetector detector;

  private FaceDatabaseHelper faceDatabaseHelper;;

  private void initDb()
  {
    if(faceDatabaseHelper != null){
      faceDatabaseHelper.close();
    }
    faceDatabaseHelper = new FaceDatabaseHelper(this);

  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_still_image);
    createDetector();
    this.initDb();
    progressBar =  findViewById(R.id.progressBar);

    mBtnOpenGallery =  findViewById(R.id.open_gallery);
    mBtnTakePhoto = findViewById(R.id.take_photo);
    mBtnOpenGallery.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startChooseImageIntentForResult();
      }
    });

    mBtnTakePhoto.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startCameraIntentForResult();
      }
    });

    preview = findViewById(R.id.preview);
    graphicOverlay = findViewById(R.id.graphic_overlay);

    populateSizeSelector();

    isLandScape =
        (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

    if (savedInstanceState != null) {
      imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI);
      selectedSize = savedInstanceState.getString(KEY_SELECTED_SIZE);
    }

    View rootView = findViewById(R.id.root);
    rootView
        .getViewTreeObserver()
        .addOnGlobalLayoutListener(
            new OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                imageMaxWidth = rootView.getWidth();
                imageMaxHeight = rootView.getHeight() - findViewById(R.id.control).getHeight();
                if (SIZE_SCREEN.equals(selectedSize)) {
                  tryReloadAndDetectInImage(false,"");
                }
              }
            });

    mBtnSaveContours = findViewById(R.id.save_contours);

    mBtnSaveContours.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        tryReloadAndDetectInImage(true,SIZE_ORIGINAL);
      }
    });

  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    createDetector();
    tryReloadAndDetectInImage(false,"");
  }

  @Override
  public void onPause() {
    super.onPause();
    if (detector != null) {
      detector.close();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (detector != null) {
      detector.close();
    }
  }

  private void populateSizeSelector() {
    Spinner sizeSpinner = findViewById(R.id.size_selector);
    List<String> options = new ArrayList<>();
    options.add(SIZE_SCREEN);
    options.add(SIZE_1024_768);
    options.add(SIZE_640_480);
    options.add(SIZE_ORIGINAL);

    // Creating adapter for featureSpinner
    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // attaching data adapter to spinner
    sizeSpinner.setAdapter(dataAdapter);
    sizeSpinner.setOnItemSelectedListener(
        new OnItemSelectedListener() {

          @Override
          public void onItemSelected(
              AdapterView<?> parentView, View selectedItemView, int pos, long id) {
            selectedSize = parentView.getItemAtPosition(pos).toString();
            tryReloadAndDetectInImage(false,"");
          }

          @Override
          public void onNothingSelected(AdapterView<?> arg0) {}
        });
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(KEY_IMAGE_URI, imageUri);
    outState.putString(KEY_SELECTED_SIZE, selectedSize);
  }

  private void startCameraIntentForResult() {
    // Clean up last time's image
    imageUri = null;
    preview.setImageBitmap(null);

    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      ContentValues values = new ContentValues();
      values.put(MediaStore.Images.Media.TITLE, "New Picture");
      values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
      imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }
  }

  private void startChooseImageIntentForResult() {
    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      tryReloadAndDetectInImage(false,"");
    } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == RESULT_OK) {
      // In this case, imageUri is returned by the chooser, save it.
      imageUri = data.getData();
      tryReloadAndDetectInImage(false,"");
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void tryReloadAndDetectInImage(boolean saveRecording,String size) {
    Log.d(TAG, "Try reload and detect image");
    try {
      if (imageUri == null) {
        return;
      }

      if (SIZE_SCREEN.equals(selectedSize) && imageMaxWidth == 0) {
        // UI layout has not finished yet, will reload once it's ready.
        return;
      }

      Bitmap imageBitmap = BitmapUtils.getBitmapFromContentUri(getContentResolver(), imageUri);
      if (imageBitmap == null) {
        return;
      }

      // Clear the overlay first
      graphicOverlay.clear();

      Bitmap resizedBitmap;
      if (!Strings.isNullOrEmpty(size)){
        selectedSize = size;
      }
      if (selectedSize.equals(SIZE_ORIGINAL)) {
        resizedBitmap = imageBitmap;
      } else {
        // Get the dimensions of the image view
        Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

        // Determine how much to scale down the image
        float scaleFactor =
            max(
                (float) imageBitmap.getWidth() / (float) targetedSize.first,
                (float) imageBitmap.getHeight() / (float) targetedSize.second);

        resizedBitmap =
            Bitmap.createScaledBitmap(
                imageBitmap,
                (int) (imageBitmap.getWidth() / scaleFactor),
                (int) (imageBitmap.getHeight() / scaleFactor),
                true);
      }

      preview.setImageBitmap(resizedBitmap);

      if (detector != null) {
        graphicOverlay.setImageSourceInfo(
            resizedBitmap.getWidth(), resizedBitmap.getHeight(), /* isFlipped= */ false);
        if (saveRecording){
          runFaceContourDetection(resizedBitmap,true);

        }else {
          runFaceContourDetection(resizedBitmap,false);
        }
      } else {
        Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error");
      }
    } catch (IOException e) {
      Log.e(TAG, "Error retrieving saved image");
      imageUri = null;
    }
  }


  private void runFaceContourDetection(Bitmap bitmapImage,boolean save) {
    InputImage image = InputImage.fromBitmap(bitmapImage, 0);


    showProgressBar(true);
    // Run the face contour detection on a background thread
    AsyncTask.execute(new Runnable() {
      @Override
      public void run() {
        if(detector == null) return;
        detector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<Face>>() {
                          @Override
                          public void onSuccess(List<Face> faces) {
                            // Process the result on the main UI thread
                            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                processFaceContourDetectionResult(faces);
                                hideProgressBar();
                                saveFaceContourDetectionResult(faces);
                              }
                            });
                          }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                          @Override
                          public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                e.printStackTrace();
                                hideProgressBar();
                              }
                            });
                          }
                        });
      }
    });
  }

  private void showProgressBar(boolean show) {
    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
  }
  private void showToast(String message) {
    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
  }

  private void hideProgressBar(){
    progressBar.setVisibility(View.GONE);
  }

  private void processFaceContourDetectionResult(List<Face> faces) {
    // Task completed successfully
    if (faces.size() == 0) {
      showToast("No face found");
      return;
    }
    graphicOverlay.clear();
    for (int i = 0; i < faces.size(); ++i) {
      Face face = faces.get(i);
      FaceContourGraphic faceGraphic = new FaceContourGraphic(graphicOverlay);
      graphicOverlay.add(faceGraphic);
      faceGraphic.updateFace(face);
    }
  }

  private void saveFaceContourDetectionResult(List<Face> faces) {
    String stringFaces = serializeFaceList(faces);
    faceDatabaseHelper.insertFaceDetection(stringFaces,SIZE_ORIGINAL,imageUri.toString());

  }

  public String serializeFaceList(List<Face> faces) {
    List<String> faceStrings = new ArrayList<>();
    for (Face face : faces) {
      faceStrings.add(face.toString());
    }
    return TextUtils.join(";", faceStrings); // Using ";" as a delimiter
  }

  public void insertFaceData(String faces,String size,String imageUri) {
    // Convert Face object to FaceRecord or similar
    FaceRecord record = new FaceRecord();

    // Get database writable instance

    SQLiteDatabase db = faceDatabaseHelper.getWritableDatabase();

    // Create a new map of values
    ContentValues values = new ContentValues();
    values.put(COLUMN_FACES, faces);
    values.put(COLUMN_SIZE,size);
    values.put(COLUMN_URI,imageUri);
    // Insert the new row
    long newRowId = db.insert(TABLE_NAME, null, values);
    if (newRowId == -1) {
      // Handle error
    }
  }

  private Pair<Integer, Integer> getTargetedWidthHeight() {
    int targetWidth;
    int targetHeight;

    switch (selectedSize) {
      case SIZE_SCREEN:
        targetWidth = imageMaxWidth;
        targetHeight = imageMaxHeight;
        break;
      case SIZE_640_480:
        targetWidth = isLandScape ? 640 : 480;
        targetHeight = isLandScape ? 480 : 640;
        break;
      case SIZE_1024_768:
        targetWidth = isLandScape ? 1024 : 768;
        targetHeight = isLandScape ? 768 : 1024;
        break;
      default:
        throw new IllegalStateException("Unknown size");
    }

    return new Pair<>(targetWidth, targetHeight);
  }

  private void createDetector() {
    if (detector != null) {
      detector.close();
    }
    try {
          Log.i(TAG, "Using Face Detector Processor");
      FaceDetectorOptions options =
              new FaceDetectorOptions.Builder()
                      .setPerformanceMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                      .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                      .build();
      detector = FaceDetection.getClient(options);

    } catch (Exception e) {
      Log.e(TAG, "Can not create image processor: Face Detection", e);
      Toast.makeText(
              getApplicationContext(),
              "Can not create image processor: " + e.getMessage(),
              Toast.LENGTH_LONG)
          .show();
    }
  }
}
