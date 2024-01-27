package com.mti.facedetectuc.mlkitfacedetection;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.Nullable;

/** Graphic instance for rendering inference info (latency, FPS, resolution) in an overlay view. */
public class InferenceInfoGraphic extends GraphicOverlay.Graphic {

  private static final int TEXT_COLOR = Color.BLACK;
  private static final float TEXT_SIZE = 30.0f;

  private final Paint textPaint;
  private final GraphicOverlay overlay;

  public InferenceInfoGraphic(
      GraphicOverlay overlay,
      long frameLatency,
      long detectorLatency,
      @Nullable Integer framesPerSecond) {
    super(overlay);
    this.overlay = overlay;
    textPaint = new Paint();
    textPaint.setColor(TEXT_COLOR);
    textPaint.setTextSize(TEXT_SIZE);
    textPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);
    postInvalidate();
  }

  /** Creates an {@link InferenceInfoGraphic} to only display image size. */
  public InferenceInfoGraphic(GraphicOverlay overlay) {
    this(overlay, 0, 0, null);
  }

  @Override
  public synchronized void draw(Canvas canvas) {
    float x = TEXT_SIZE * 0.5f;
    float y = TEXT_SIZE * 1.5f;

    canvas.drawText(
        overlay.getImageHeight() + "x" + overlay.getImageWidth(),
        x,
        y,
        textPaint);

  }
}
