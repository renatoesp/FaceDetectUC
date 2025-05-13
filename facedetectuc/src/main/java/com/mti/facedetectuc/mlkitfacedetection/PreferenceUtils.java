package com.mti.facedetectuc.mlkitfacedetection;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.gms.common.images.Size;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.mti.facedetectuc.R;
import com.mti.facedetectuc.mlkitfacedetection.CameraSource.SizePair;

/** Utility class to retrieve shared preferences. */
public class PreferenceUtils {

  public static FaceDetectorOptions getFaceDetectorOptions(Context context) {
    int landmarkMode = FaceDetectorOptions.LANDMARK_MODE_NONE;
    int contourMode = FaceDetectorOptions.CONTOUR_MODE_NONE;
    int classificationMode = FaceDetectorOptions.CLASSIFICATION_MODE_ALL;
    int performanceMode = FaceDetectorOptions.PERFORMANCE_MODE_FAST;

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean enableFaceTracking = false;
    float minFaceSize = Float.parseFloat("0.1");

    FaceDetectorOptions.Builder optionsBuilder =
        new FaceDetectorOptions.Builder()
            .setLandmarkMode(landmarkMode)
            .setContourMode(contourMode)
            .setClassificationMode(classificationMode)
            .setPerformanceMode(performanceMode)
            .setMinFaceSize(minFaceSize);
    if (enableFaceTracking) {
      optionsBuilder.enableTracking();
    }
    return optionsBuilder.build();
  }

  public static boolean shouldHideDetectionInfo(Context context) {
    return false;
  }

}
