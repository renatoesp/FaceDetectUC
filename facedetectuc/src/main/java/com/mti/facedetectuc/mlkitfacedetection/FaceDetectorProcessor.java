package com.mti.facedetectuc.mlkitfacedetection;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.mti.facedetectuc.LottieAnimationController;

import java.util.List;
import java.util.Locale;

/** Face Detector Demo. */
public class FaceDetectorProcessor extends VisionProcessorBase<List<Face>> {

  private static final String TAG = "FaceDetectUC";

  private final FaceDetector detector;
  private final SharedPreferences sharedPreferencesMain;
  private final SharedPreferences.Editor editor;
  private final LottieAnimationController animationControllerMain;
  private boolean isFaceReady;


  private ImageProcessingListener imageProcessingListener;
  public FaceDetectorProcessor(Context context, SharedPreferences sharedPreferences, LottieAnimationController animationController) {
    super(context);
    sharedPreferencesMain = sharedPreferences;
    editor = sharedPreferencesMain.edit();
    animationControllerMain = animationController;
    FaceDetectorOptions faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(context);
    Log.v(MANUAL_TESTING_LOG, "Face detector options: " + faceDetectorOptions);
    detector = FaceDetection.getClient(faceDetectorOptions);
  }


  public void setImageProcessingListener(ImageProcessingListener listener) {
    this.imageProcessingListener = listener;
  }

  @Override
  public void stop() {
    super.stop();
    detector.close();
  }

  @Override
  protected Task<List<Face>> detectInImage(InputImage image, Bitmap bitmap) {

    isFaceReady = getValue("FaceReady",false);

    if (isFaceReady){
      Log.d(TAG, "detectInImage - FaceDetectorProcessor - isFaceReady:"+isFaceReady);

      if (imageProcessingListener != null) {
        Log.d(TAG, "detectInImage - FaceDetectorProcessor - imageProcessingListener != null");
        imageProcessingListener.onImageProcessed(bitmap);
      }
    }
    return detector.process(image);
  }

  @Override
  protected void onSuccess(@NonNull List<Face> faces, @NonNull GraphicOverlay graphicOverlay) {
    for (Face face : faces) {
      graphicOverlay.add(new FaceGraphic(graphicOverlay, face,sharedPreferencesMain,animationControllerMain));

//      logExtrasForTesting(face);
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

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Face detection failed " + e);
  }

  public boolean getValue(String key, boolean defaultValue) {
    return sharedPreferencesMain.getBoolean(key, defaultValue);
  }
  public interface ImageProcessingListener {
    void onImageProcessed(Bitmap bitmap);
  }
}
