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
 *//*


package com.google.codelab.mlkit;


import static com.google.codelab.mlkit.FaceDatabaseHelper.COLUMN_FACES;
import static com.google.codelab.mlkit.FaceDatabaseHelper.COLUMN_SIZE;
import static com.google.codelab.mlkit.FaceDatabaseHelper.TABLE_NAME;
import static com.google.codelab.mlkit.FaceDatabaseHelper.COLUMN_URI;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.odml.image.BitmapMlImageBuilder;
import com.google.android.odml.image.ByteBufferMlImageBuilder;
import com.google.android.odml.image.MlImage;
import com.google.codelab.mlkit.preference.PreferenceUtils;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.graphics.PointF;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.Locale;

*/
/** Face Detector Demo. *//*

public class FaceDetectorProcessor  {

  private static final String TAG = "FaceDetectorProcessor";

  private final FaceDetector detector;

  private final ActivityManager activityManager;
  private final Timer fpsTimer = new Timer();
  private final ScopedExecutor executor;
  private final TemperatureMonitor temperatureMonitor;


  // Whether this processor is already shut down
  private boolean isShutdown;
  protected static final String MANUAL_TESTING_LOG = "LogTagForTest";

  // Used to calculate latency, running in the same thread, no sync needed.
  private int numRuns = 0;
  private long totalFrameMs = 0;
  private long maxFrameMs = 0;
  private long minFrameMs = Long.MAX_VALUE;
  private long totalDetectorMs = 0;
  private long maxDetectorMs = 0;
  private long minDetectorMs = Long.MAX_VALUE;

  // Frame count that have been processed so far in an one second interval to calculate FPS.
  private int frameProcessedInOneSecondInterval = 0;
  private int framesPerSecond = 0;

  // To keep the latest images and its metadata.
  @GuardedBy("this")
  private ByteBuffer latestImage;

  @GuardedBy("this")
  private FrameMetadata latestImageMetaData;
  // To keep the images and metadata in process.
  @GuardedBy("this")
  private ByteBuffer processingImage;

  @GuardedBy("this")
  private FrameMetadata processingMetaData;
  private FaceDatabaseHelper faceDatabaseHelper;
  public FaceDetectorProcessor(Context context) {

    FaceDetectorOptions faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(context);
    Log.v(MANUAL_TESTING_LOG, "Face detector options: " + faceDetectorOptions);
    detector = FaceDetection.getClient(faceDetectorOptions);
    activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    executor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);
    fpsTimer.scheduleAtFixedRate(
            new TimerTask() {
              @Override
              public void run() {
                framesPerSecond = frameProcessedInOneSecondInterval;
                frameProcessedInOneSecondInterval = 0;
              }
            },
            */
/* delay= *//*
 0,
            */
/* period= *//*
 1000);
    temperatureMonitor = new TemperatureMonitor(context);
    faceDatabaseHelper = new FaceDatabaseHelper(context);
  }

  public void stop() {
    detector.close();
    executor.shutdown();
    isShutdown = true;
    resetLatencyStats();
    fpsTimer.cancel();
    temperatureMonitor.stop();
  }

  protected Task<List<Face>> detectInImage(InputImage image) {
    return ;
  }

  protected void onSuccess(@NonNull List<Face> faces, @NonNull GraphicOverlay graphicOverlay,String size, String imageUri, boolean save) {
    for (Face face : faces) {
      graphicOverlay.add(new FaceGraphic(graphicOverlay, face));
      logExtrasForTesting(face);
    }
    String facesString = this.serializeFaceList(faces);
    if (save) {
      this.insertFaceData(facesString, size, imageUri);
    }
  }


  private static void logExtrasForTesting(Face face) {
    if (face != null) {
      Log.v(MANUAL_TESTING_LOG, "face bounding box: " + face.getBoundingBox().flattenToString());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle X: " + face.getHeadEulerAngleX());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Y: " + face.getHeadEulerAngleY());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Z: " + face.getHeadEulerAngleZ());

      // All landmarks
      int[] landMarkTypes =
          new int[] {
            FaceLandmark.MOUTH_BOTTOM,
            FaceLandmark.MOUTH_RIGHT,
            FaceLandmark.MOUTH_LEFT,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EAR,
            FaceLandmark.LEFT_EAR,
            FaceLandmark.RIGHT_CHEEK,
            FaceLandmark.LEFT_CHEEK,
            FaceLandmark.NOSE_BASE
          };
      String[] landMarkTypesStrings =
          new String[] {
            "MOUTH_BOTTOM",
            "MOUTH_RIGHT",
            "MOUTH_LEFT",
            "RIGHT_EYE",
            "LEFT_EYE",
            "RIGHT_EAR",
            "LEFT_EAR",
            "RIGHT_CHEEK",
            "LEFT_CHEEK",
            "NOSE_BASE"
          };
      for (int i = 0; i < landMarkTypes.length; i++) {
        FaceLandmark landmark = face.getLandmark(landMarkTypes[i]);
        if (landmark == null) {
          Log.v(
              MANUAL_TESTING_LOG,
              "No landmark of type: " + landMarkTypesStrings[i] + " has been detected");
        } else {
          PointF landmarkPosition = landmark.getPosition();
          String landmarkPositionStr =
              String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y);
          Log.v(
              MANUAL_TESTING_LOG,
              "Position for face landmark: "
                  + landMarkTypesStrings[i]
                  + " is :"
                  + landmarkPositionStr);
        }
      }
      Log.v(
          MANUAL_TESTING_LOG,
          "face left eye open probability: " + face.getLeftEyeOpenProbability());
      Log.v(
          MANUAL_TESTING_LOG,
          "face right eye open probability: " + face.getRightEyeOpenProbability());
      Log.v(MANUAL_TESTING_LOG, "face smiling probability: " + face.getSmilingProbability());
      Log.v(MANUAL_TESTING_LOG, "face tracking id: " + face.getTrackingId());
    }
  }

  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Face detection failed " + e);
  }



  // -----------------Code for processing single still image----------------------------------------


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
  }

  public void processBitmapAndSave(Bitmap bitmap, final GraphicOverlay graphicOverlay, String size, String imageUri){
    long frameStartMs = SystemClock.elapsedRealtime();

    if (isMlImageEnabled(graphicOverlay.getContext())) {
      MlImage mlImage = new BitmapMlImageBuilder(bitmap).build();
      requestDetectInImage(
              mlImage,
              graphicOverlay,
              */
/* originalCameraImage= *//*
 null,
              */
/* shouldShowFps= *//*
 false,
              frameStartMs,size, imageUri,true);
      mlImage.close();

      return;
    }

    requestDetectInImage(
            InputImage.fromBitmap(bitmap, 0),
            graphicOverlay,
            */
/* originalCameraImage= *//*
 null,
            */
/* shouldShowFps= *//*
 false,
            frameStartMs,size,imageUri,true);

  }

  public void processBitmap(Bitmap bitmap, final GraphicOverlay graphicOverlay) {
    long frameStartMs = SystemClock.elapsedRealtime();

    if (isMlImageEnabled(graphicOverlay.getContext())) {
      MlImage mlImage = new BitmapMlImageBuilder(bitmap).build();
      requestDetectInImage(
              mlImage,
              graphicOverlay,
              */
/* originalCameraImage= *//*
 null,
              */
/* shouldShowFps= *//*
 false,
              frameStartMs,"","",false);
      mlImage.close();

      return;
    }

    requestDetectInImage(
            InputImage.fromBitmap(bitmap, 0),
            graphicOverlay,
            */
/* originalCameraImage= *//*
 null,
            */
/* shouldShowFps= *//*
 false,
            frameStartMs,"","",false);
  }

  // -----------------Code for processing live preview frame from Camera1 API-----------------------
  public synchronized void processByteBuffer(
          ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay) {
    latestImage = data;
    latestImageMetaData = frameMetadata;
    if (processingImage == null && processingMetaData == null) {
      processLatestImage(graphicOverlay);
    }
  }

  private synchronized void processLatestImage(final GraphicOverlay graphicOverlay) {
    processingImage = latestImage;
    processingMetaData = latestImageMetaData;
    latestImage = null;
    latestImageMetaData = null;
    if (processingImage != null && processingMetaData != null && !isShutdown) {
      processImage(processingImage, processingMetaData, graphicOverlay);
    }
  }

  private void processImage(
          ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay) {
    long frameStartMs = SystemClock.elapsedRealtime();

    // If live viewport is on (that is the underneath surface view takes care of the camera preview
    // drawing), skip the unnecessary bitmap creation that used for the manual preview drawing.
    Bitmap bitmap =
            PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.getContext())
                    ? null
                    : BitmapUtils.getBitmap(data, frameMetadata);

    if (isMlImageEnabled(graphicOverlay.getContext())) {
      MlImage mlImage =
              new ByteBufferMlImageBuilder(
                      data,
                      frameMetadata.getWidth(),
                      frameMetadata.getHeight(),
                      MlImage.IMAGE_FORMAT_NV21)
                      .setRotation(frameMetadata.getRotation())
                      .build();

      requestDetectInImage(mlImage, graphicOverlay, bitmap, */
/* shouldShowFps= *//*
 true, frameStartMs,"","",false)
              .addOnSuccessListener(executor, results -> processLatestImage(graphicOverlay));

      // This is optional. Java Garbage collection can also close it eventually.
      mlImage.close();
      return;
    }

    requestDetectInImage(
            InputImage.fromByteBuffer(
                    data,
                    frameMetadata.getWidth(),
                    frameMetadata.getHeight(),
                    frameMetadata.getRotation(),
                    InputImage.IMAGE_FORMAT_NV21),
            graphicOverlay,
            bitmap,
            */
/* shouldShowFps= *//*
 true,
            frameStartMs,"","",false)
            .addOnSuccessListener(executor, results -> processLatestImage(graphicOverlay));
  }

  // -----------------Code for processing live preview frame from CameraX API-----------------------


  // -----------------Common processing logic-------------------------------------------------------
  private void requestDetectInImage(
          final InputImage image,
          final GraphicOverlay graphicOverlay,
          @Nullable final Bitmap originalCameraImage,
          boolean shouldShowFps,
          long frameStartMs,
          String size,
          String imageUri,
          boolean save) {
    List<Face> faces = detector.process(image).getResult() ; // graphicOverlay, originalCameraImage, shouldShowFps, frameStartMs,size,imageUri ,save);
  }

  private void requestDetectInImage(
          final MlImage image,
          final GraphicOverlay graphicOverlay,
          @Nullable final Bitmap originalCameraImage,
          boolean shouldShowFps,
          long frameStartMs,
          String size,
          String imageUri,
          boolean save) {

    List<Face> faces = detector.process(image).getResult() ; // graphicOverlay, originalCameraImage, shouldShowFps, frameStartMs,size,imageUri ,save);
    processIt(faces,graphicOverlay, originalCameraImage, shouldShowFps, frameStartMs,size,imageUri ,save);

  }

  private void processIt(
          List<Face> results ,
          final GraphicOverlay graphicOverlay,
          @Nullable final Bitmap originalCameraImage,
          boolean shouldShowFps,
          long frameStartMs,
          String size,
          String imageUri,
          boolean save) {
    final long detectorStartMs = SystemClock.elapsedRealtime();

    try {
      long endMs = SystemClock.elapsedRealtime();
      long currentFrameLatencyMs = endMs - frameStartMs;
      long currentDetectorLatencyMs = endMs - detectorStartMs;
      if (numRuns >= 500) {
        resetLatencyStats();
      }
      numRuns++;
      frameProcessedInOneSecondInterval++;
      totalFrameMs += currentFrameLatencyMs;
      maxFrameMs = max(currentFrameLatencyMs, maxFrameMs);
      minFrameMs = min(currentFrameLatencyMs, minFrameMs);
      totalDetectorMs += currentDetectorLatencyMs;
      maxDetectorMs = max(currentDetectorLatencyMs, maxDetectorMs);
      minDetectorMs = min(currentDetectorLatencyMs, minDetectorMs);

      // Only log inference info once per second. When frameProcessedInOneSecondInterval is
      // equal to 1, it means this is the first frame processed during the current second.
      if (frameProcessedInOneSecondInterval == 1) {
        Log.d(TAG, "Num of Runs: " + numRuns);
        Log.d(
                TAG,
                "Frame latency: max="
                        + maxFrameMs
                        + ", min="
                        + minFrameMs
                        + ", avg="
                        + totalFrameMs / numRuns);
        Log.d(
                TAG,
                "Detector latency: max="
                        + maxDetectorMs
                        + ", min="
                        + minDetectorMs
                        + ", avg="
                        + totalDetectorMs / numRuns);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem / 0x100000L;
        Log.d(TAG, "Memory available in system: " + availableMegs + " MB");
        temperatureMonitor.logTemperature();
      }

      graphicOverlay.clear();
      if (originalCameraImage != null) {
        graphicOverlay.add(new CameraImageGraphic(graphicOverlay, originalCameraImage));
      }
      if (!PreferenceUtils.shouldHideDetectionInfo(graphicOverlay.getContext())) {
        graphicOverlay.add(
                new InferenceInfoGraphic(
                        graphicOverlay,
                        currentFrameLatencyMs,
                        currentDetectorLatencyMs,
                        shouldShowFps ? framesPerSecond : null));
      }
      graphicOverlay.postInvalidate();
    }
catch (Exception e)
{
  //if failed
  graphicOverlay.clear();
  graphicOverlay.postInvalidate();
  String error = "Failed to process. Error: " + e.getLocalizedMessage();
  Toast.makeText(
                  graphicOverlay.getContext(),
                  error + "\nCause: " + e.getCause(),
                  Toast.LENGTH_SHORT)
          .show();
  Log.d(TAG, error);
  e.printStackTrace();
}

  }


  private void resetLatencyStats() {
    numRuns = 0;
    totalFrameMs = 0;
    maxFrameMs = 0;
    minFrameMs = Long.MAX_VALUE;
    totalDetectorMs = 0;
    maxDetectorMs = 0;
    minDetectorMs = Long.MAX_VALUE;
  }

  protected boolean isMlImageEnabled(Context context) {
    return false;
  }
}
*/
