package com.mti.facedetectuc.mlkitfacedetection;
import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

public class CameraSourceHelper {

    // Valores padrão
    private static final int DEFAULT_WIDTH = 480;
    private static final int DEFAULT_HEIGHT = 640;

    public static String getDefaultRequestedCameraPreviewWidth(Context context) {
        // Obter as dimensões da tela
        Point displaySize = getDisplaySize(context);

        // Use o valor padrão ou a largura da tela, o que for menor
        int width = Math.min(DEFAULT_WIDTH, displaySize.x);
        return String.valueOf(width);
    }

    public static String getDefaultRequestedCameraPreviewHeight(Context context) {
        // Obter as dimensões da tela
        Point displaySize = getDisplaySize(context);

        // Use o valor padrão ou a altura da tela, o que for menor
        int height = Math.min(DEFAULT_HEIGHT, displaySize.y);
        return String.valueOf(height);
    }

    private static Point getDisplaySize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }
}
