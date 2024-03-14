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
import com.mti.facedetectuc.FaceDetectUC;
import java.util.List;
import java.util.Locale;

/** Face Detector Demo. */
public class FaceDetectorProcessor extends VisionProcessorBase<List<Face>> {

  private static final String TAG = "FaceDetectUC";

  private final FaceDetector detector;
  private final SharedPreferences sharedPreferencesMain;
  private final SharedPreferences.Editor editor;
  private boolean isFaceReady;
  private ImageProcessingListener imageProcessingListener;

  private FaceDetectionListener faceDetectionListener;



  public FaceDetectorProcessor(Context context, SharedPreferences sharedPreferences) {
    super(context);

    sharedPreferencesMain = sharedPreferences;
    editor = sharedPreferencesMain.edit();
    FaceDetectorOptions faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(context);
    Log.v(MANUAL_TESTING_LOG, "Face detector options: " + faceDetectorOptions);
    detector = FaceDetection.getClient(faceDetectorOptions);

  }

  public void setFaceDetectionListener(FaceDetectionListener listener) {
    this.faceDetectionListener = listener;
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

    Log.d(TAG, "detectInImage - FaceDetectorProcessor - isFaceReady:"+isFaceReady);
    if (isFaceReady){
      if (imageProcessingListener != null) {
        Log.d(TAG, "detectInImage - FaceDetectorProcessor - imageProcessingListener != null");
        imageProcessingListener.onImageProcessed(bitmap);

      }
    }
    return detector.process(image);
  }
  @androidx.camera.core.ExperimentalGetImage
  @Override
  protected void onSuccess(@NonNull List<Face> faces, @NonNull GraphicOverlay graphicOverlay) {
    int faceCount = faces.size();
    Log.d(TAG, "faceCount:"+faceCount);
    if (faceDetectionListener != null) {
      faceDetectionListener.onFacesDetected(faceCount);
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Face detection failed " + e);
  }

  public void setValue(String key, String value) {
    Log.d(TAG, "setValue - String key:"+key+" - String value:"+value);
    editor.putString(key, value);
    editor.apply();
  }

  public void setValue(String key, int value) {
    Log.d(TAG, "setValue - String key:"+key+" - int value:"+value);
    editor.putInt(key, value);
    editor.apply();
  }

  public void setValue(String key, boolean value) {
    Log.d(TAG, "setValue - String key:"+key+" - boolean value:"+value);
    editor.putBoolean(key, value);
    editor.apply();
  }

  public String getValue(String key, String defaultValue) {
    Log.d(TAG, "getValue - String key:"+key+" - String value:"+defaultValue);
    return sharedPreferencesMain.getString(key, defaultValue);
  }

  public int getValue(String key, int defaultValue) {
    Log.d(TAG, "getValue - String key:"+key+" - int value:"+defaultValue);
    return sharedPreferencesMain.getInt(key, defaultValue);
  }

  public boolean getValue(String key, boolean defaultValue) {
    Log.d(TAG, "getValue - String key:"+key+" - boolean value:"+defaultValue);
    return sharedPreferencesMain.getBoolean(key, defaultValue);
  }

  public void removeValue(String key) {
    Log.d(TAG, "removeValue - String key:"+key);
    editor.remove(key);
    editor.apply();
  }
  public interface ImageProcessingListener {
    void onImageProcessed(Bitmap bitmap);
  }

  public interface FaceDetectionListener {
    void onFacesDetected(int faceCount);
  }

}
