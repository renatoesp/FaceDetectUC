package com.mti.facedetectuc.mlkitfacedetection;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.face.FaceLandmark.LandmarkType;
import com.mti.facedetectuc.LottieAnimationController;
import com.mti.facedetectuc.mlkitfacedetection.GraphicOverlay.Graphic;

import java.util.Locale;

/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
public class FaceGraphic extends Graphic {
  private static final String PREFS_NAME = "FaceDetectUC";
  private static final float FACE_POSITION_RADIUS = 2.0f;
  private static final float ID_TEXT_SIZE = 30.0f;
  private static final float ID_TEXT_SIZE_MESSAGE = 20.0f;
  private static final float ID_Y_OFFSET = 30.0f;
  private static final float BOX_STROKE_WIDTH = 3.0f;
  private static final int NUM_COLORS = 10;
  private static final int[][] COLORS =
      new int[][] {
        // {Text color, background color}
        {Color.BLACK, Color.WHITE},
        {Color.WHITE, Color.MAGENTA},
        {Color.BLACK, Color.LTGRAY},
        {Color.WHITE, Color.RED},
        {Color.WHITE, Color.BLUE},
        {Color.WHITE, Color.DKGRAY},
        {Color.BLACK, Color.CYAN},
        {Color.BLACK, Color.YELLOW},
        {Color.WHITE, Color.BLACK},
        {Color.BLACK, Color.GREEN}
      };

  private final Paint facePositionPaint;
  private final Paint[] idPaints;
  private final Paint[] boxPaints;
  private final Paint[] labelPaints;
  private boolean isEyesClosed;
  private volatile Face face;
  private final SharedPreferences sharedPreferencesMain;
  private final SharedPreferences.Editor editor;
  private final GraphicOverlay graphicOverlay;
  private final LottieAnimationController animationControllerMain;
  FaceGraphic(GraphicOverlay overlay, Face face, SharedPreferences sharedPreferences, LottieAnimationController animationController) {
    super(overlay);
    graphicOverlay = overlay;
    this.face = face;

    animationControllerMain = animationController;
    sharedPreferencesMain = sharedPreferences;
    editor = sharedPreferencesMain.edit();
/*
    isEyesClosed = getValue("isEyesClosed",false);
    Log.d(PREFS_NAME, "isEyesClosed:"+isEyesClosed);
    if (!isEyesClosed) {
      if (face.getLeftEyeOpenProbability() != null && face.getRightEyeOpenProbability() != null) {
        if (face.getLeftEyeOpenProbability() < 0.04 && face.getRightEyeOpenProbability() < 0.04) {
          isEyesClosed = true;
        } else {
          isEyesClosed = false;
        }
      }
      Log.d(PREFS_NAME, "1 isEyesClosed:"+isEyesClosed);

      setValue("isEyesClosed", isEyesClosed);
    }
    else{
//      animationControllerMain.setAnimationFile("loading.json");
//      animationControllerMain.playAnimation();
      if (face.getLeftEyeOpenProbability() > 0.04 && face.getRightEyeOpenProbability() > 0.04) {
        Log.d(PREFS_NAME, "FaceGraphic FaceReady: true");
        setValue("FaceReady", true);
      }
    }
*/
    final int selectedColor = Color.WHITE;

    facePositionPaint = new Paint();
    facePositionPaint.setColor(selectedColor);

    int numColors = COLORS.length;
    idPaints = new Paint[numColors];
    boxPaints = new Paint[numColors];
    labelPaints = new Paint[numColors];
    for (int i = 0; i < numColors; i++) {
      idPaints[i] = new Paint();
      idPaints[i].setColor(COLORS[i][0] /* text color */);
      idPaints[i].setTextSize(ID_TEXT_SIZE);

      boxPaints[i] = new Paint();
      if (isEyesClosed){
        boxPaints[i].setColor(Color.GREEN);
      }else{
        boxPaints[i].setColor(COLORS[i][1]);
      }
      boxPaints[i].setStyle(Paint.Style.STROKE);
      boxPaints[i].setStrokeWidth(BOX_STROKE_WIDTH);

      labelPaints[i] = new Paint();
      labelPaints[i].setColor(COLORS[i][1]);
      labelPaints[i].setStyle(Paint.Style.FILL);
    }
  }

  /** Draws the face annotations for position on the supplied canvas. */
  @Override
  public void draw(Canvas canvas) {
    Face face = this.face;
    if (face == null) {
      return;
    }


    // Draws a circle at the position of the detected face, with the face's track id below.
    float x = translateX(face.getBoundingBox().centerX());
    float y = translateY(face.getBoundingBox().centerY());
//    canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);

    // Calculate positions.
    float left = x - scale(face.getBoundingBox().width() / 2.0f);
    float top = y - scale(face.getBoundingBox().height() / 2.0f);
    float right = x + scale(face.getBoundingBox().width() / 2.0f);
    float bottom = y + scale(face.getBoundingBox().height() / 2.0f);
    float lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH;
    float yLabelOffset = (face.getTrackingId() == null) ? 0 : -lineHeight;

    // Decide color based on face ID
    int colorID = (face.getTrackingId() == null) ? 0 : Math.abs(face.getTrackingId() % NUM_COLORS);

    // Calculate width and height of label box
    float textWidth = idPaints[colorID].measureText("ID: " + face.getTrackingId());

//    yLabelOffset = yLabelOffset - 3 * lineHeight;

      // Draw labels
      canvas.drawRect(
              left - BOX_STROKE_WIDTH,
              top + yLabelOffset,
              left + textWidth + (2 * BOX_STROKE_WIDTH),
              top,
              labelPaints[colorID]);
      yLabelOffset += ID_TEXT_SIZE;
      canvas.drawRect(left, top, right, bottom, boxPaints[colorID]);
    if (!PreferenceUtils.shouldHideDetectionInfo(graphicOverlay.getContext())) {
      if (face.getTrackingId() != null) {
        canvas.drawText("ID: " + face.getTrackingId(), left, top + yLabelOffset, idPaints[colorID]);
        yLabelOffset += lineHeight;
      }

      // Draws all face contours.
      for (FaceContour contour : face.getAllContours()) {
        for (PointF point : contour.getPoints()) {
          canvas.drawCircle(
                  translateX(point.x), translateY(point.y), FACE_POSITION_RADIUS, facePositionPaint);
        }
      }


      // Draw facial landmarks
      drawFaceLandmark(canvas, FaceLandmark.LEFT_EYE);
      drawFaceLandmark(canvas, FaceLandmark.RIGHT_EYE);
      drawFaceLandmark(canvas, FaceLandmark.LEFT_CHEEK);
      drawFaceLandmark(canvas, FaceLandmark.RIGHT_CHEEK);
    }
  }

  private void drawFaceLandmark(Canvas canvas, @LandmarkType int landmarkType) {
    FaceLandmark faceLandmark = face.getLandmark(landmarkType);
    if (faceLandmark != null) {
      canvas.drawCircle(
          translateX(faceLandmark.getPosition().x),
          translateY(faceLandmark.getPosition().y),
          FACE_POSITION_RADIUS,
          facePositionPaint);
    }
  }

  // Método para mostrar a mensagem na tela.
  private void showMessage(Canvas canvas, String message) {
    // Defina a cor de fundo com transparência.
    Paint backgroundPaint = new Paint();
    backgroundPaint.setColor(Color.parseColor("#80000000")); // Cor com transparência.

    // Defina o retângulo para o fundo da mensagem (centralizado na parte inferior da tela).
    float left = 0;
    float top = canvas.getHeight() * 0.7f; // Ajuste a posição vertical conforme necessário.
    float right = canvas.getWidth();
    float bottom = canvas.getHeight();

    // Desenhe o retângulo de fundo.
    canvas.drawRect(left, top, right, bottom, backgroundPaint);

    // Defina as configurações do texto.
    Paint textPaint = new Paint();
    textPaint.setColor(Color.WHITE); // Cor do texto.
    textPaint.setTextSize(ID_TEXT_SIZE_MESSAGE); // Tamanho da fonte.
    textPaint.setTextAlign(Paint.Align.CENTER); // Alinhamento central.

    // Calcule a posição central horizontal.
    float centerX = canvas.getWidth() / 2;

    // Desenhe o texto centralizado na tela.
    canvas.drawText(message, centerX, (top + bottom) / 2, textPaint);
  }

  public void setValue(String key, String value) {
    Log.d(PREFS_NAME, "setValue - String key:"+key+" - String value:"+value);
    editor.putString(key, value);
    editor.apply();
  }

  public void setValue(String key, int value) {
    Log.d(PREFS_NAME, "setValue - String key:"+key+" - int value:"+value);
    editor.putInt(key, value);
    editor.apply();
  }

  public void setValue(String key, boolean value) {
    Log.d(PREFS_NAME, "setValue - String key:"+key+" - boolean value:"+value);
    editor.putBoolean(key, value);
    editor.apply();
  }

  public String getValue(String key, String defaultValue) {
    Log.d(PREFS_NAME, "getValue - String key:"+key+" - String value:"+defaultValue);
    return sharedPreferencesMain.getString(key, defaultValue);
  }

  public int getValue(String key, int defaultValue) {
    Log.d(PREFS_NAME, "getValue - String key:"+key+" - int value:"+defaultValue);
    return sharedPreferencesMain.getInt(key, defaultValue);
  }

  public boolean getValue(String key, boolean defaultValue) {
    Log.d(PREFS_NAME, "getValue - String key:"+key+" - boolean value:"+defaultValue);
    return sharedPreferencesMain.getBoolean(key, defaultValue);
  }

  public void removeValue(String key) {
    Log.d(PREFS_NAME, "removeValue - String key:"+key);
    editor.remove(key);
    editor.apply();
  }
}
