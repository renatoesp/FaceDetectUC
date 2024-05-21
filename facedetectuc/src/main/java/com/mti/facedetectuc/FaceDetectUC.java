package com.mti.facedetectuc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.genexus.android.core.base.controls.IGxControlRuntime;
import com.genexus.android.core.base.metadata.ActionDefinition;
import com.genexus.android.core.base.metadata.ActionParameter;
import com.genexus.android.core.base.metadata.expressions.Expression;
import com.genexus.android.core.base.metadata.layout.LayoutItemDefinition;
import com.genexus.android.core.controls.IGxEdit;
import com.genexus.android.core.ui.Coordinator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import com.mti.R;
import com.mti.facedetectuc.mlkitfacedetection.GraphicOverlay;
import com.mti.facedetectuc.mlkitfacedetection.CameraSourcePreview;
import com.mti.facedetectuc.mlkitfacedetection.CameraSource;
import com.mti.facedetectuc.mlkitfacedetection.FaceDetectorProcessor;

@SuppressLint("ViewConstructor")
@androidx.camera.core.ExperimentalGetImage
public class FaceDetectUC extends FrameLayout implements IGxEdit, IGxControlRuntime {
    final static String NAME = "FaceDetectUC";
    private final static String EVENT_RETURN_IMAGE = "ReturnImage";
    private static final String METHOD_START_CAMERA = "startcamera";
    private static final String METHOD_STOP_CAMERA = "stopcamera";
    private static final String CONTROL_ID = "@" + NAME;
    private String mName;

    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private final Activity mactivity;
    private final Context mContext;
    private final Coordinator mCoordinator;
    private final LayoutItemDefinition mLayoutDefinition;
    private static final String FACE_DETECTION = "FaceDetectionPrefs";
    private static final String TAG = "LivePreviewActivity";
    private FaceDetectorProcessor faceDetectorProcessor;
    private CameraSource cameraSource = null;
    private final CameraSourcePreview preview;
    private final GraphicOverlay graphicOverlay;
    private final SharedPreferences sharedPreferences;
    private final Button startCaptureButton;
    private final SharedPreferences.Editor editor;
    private final Handler handler = new Handler();
    private long lastSavedTime = 0;
    private static final long SAVE_INTERVAL = 500;
    private Bitmap lastBitmap;
    public FaceDetectUC(Context context, Coordinator coordinator, LayoutItemDefinition definition) {
        super(context);
        mCoordinator = coordinator;
        mContext = context;
        mLayoutDefinition = definition;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.activity_vision_live_preview, this);

        mactivity = mCoordinator.getUIContext().getActivity();

        sharedPreferences = mContext.getSharedPreferences(FACE_DETECTION, mContext.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        ImageView frameOverlay = findViewById(R.id.frameOverlay);
        frameOverlay.setVisibility(View.VISIBLE);

        startCaptureButton = findViewById(R.id.startCaptureButton);
        startCaptureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                iniciarCaptura();
            }
        });

        ImageButton toggleCameraButton = findViewById(R.id.toggleCamera);
        toggleCameraButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCamera();
            }
        });

        preview = findViewById(R.id.preview_view);
        if (preview == null) {
            Log.d(NAME, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(NAME, "graphicOverlay is null");
        }
        int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);

        if (rc == PackageManager.PERMISSION_GRANTED) {
            Log.d(NAME, "Camera com permissão");
        } else {
            Log.d(NAME, "Camera sem permissão");
            requestCameraPermission();
        }

    }

    private void iniciarCaptura(){
        startCaptureButton.setText("Capturando imagens");
        startCaptureButton.setBackgroundColor(Color.parseColor("#192F781a"));
        startCaptureButton.setTextColor(Color.parseColor("#1e1e1e"));
        setValue("FaceReady", true);
    }
    private void toggleCamera() {
        if (cameraSource != null) {
            int facing = cameraSource.getCameraFacing();
            cameraSource.setFacing(facing == CameraSource.CAMERA_FACING_FRONT ? CameraSource.CAMERA_FACING_BACK : CameraSource.CAMERA_FACING_FRONT);
            preview.stop();
            startCameraSource();
        }
    }
    private void requestCameraPermission() {
        Log.w(NAME, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(mactivity,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(mactivity, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = mCoordinator.getUIContext().getActivity();

        OnClickListener listener = view -> ActivityCompat.requestPermissions(thisActivity, permissions,
                RC_HANDLE_CAMERA_PERM);

    }

    public void changeFrameOverlayImage() {
        ImageView frameOverlay = findViewById(R.id.frameOverlay);
        frameOverlay.setImageResource(R.drawable.frameface2);
    }

    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        Log.d(NAME, "createCameraSource");

        // Se não houver uma fonte de câmera existente, crie uma.
        if (cameraSource == null) {
            cameraSource = new CameraSource(mactivity, graphicOverlay);
        }
        Log.i(NAME, "Using Face Detector Processor");

        faceDetectorProcessor = new FaceDetectorProcessor(mContext, sharedPreferences);

        faceDetectorProcessor.setFaceDetectionListener(new FaceDetectorProcessor.FaceDetectionListener() {
            @Override
            public void onFacesDetected(int faceCount) {

                if (faceCount > 0) {
                    changeFrameOverlayImage();
                }
            }
        });

        faceDetectorProcessor.setImageProcessingListener(new FaceDetectorProcessor.ImageProcessingListener() {
            @Override
            public void onImageProcessed(Bitmap bitmap) {
                Log.d(NAME, "FaceDetectUC onImageProcessed");

                if (bitmap != null) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastSavedTime >= SAVE_INTERVAL) {
                        lastSavedTime = currentTime;
                        saveImage(bitmap); // Método para salvar a imagem
                    } else {
                        lastBitmap = bitmap; // Armazena a última imagem recebida
                    }

                    // Agendar a próxima verificação
                    handler.postDelayed(() -> {
                        if (lastBitmap != null && System.currentTimeMillis() - lastSavedTime >= SAVE_INTERVAL) {
                            lastSavedTime = System.currentTimeMillis();
                            saveImage(lastBitmap);
                            lastBitmap = null;
                        }
                    }, SAVE_INTERVAL - (currentTime - lastSavedTime));
                }

            }
        });

        cameraSource.setMachineLearningFrameProcessor(faceDetectorProcessor);
    }

    private void saveImage(Bitmap bitmap) {
        File appDirectory = mContext.getApplicationContext().getFilesDir();
        File savedImageFile = saveImageFromBitmap(bitmap, appDirectory);
        if (savedImageFile != null) {
            String imagePath = savedImageFile.getAbsolutePath();
            Log.d(NAME, "FaceDetectUC imagePath:" + imagePath);
            ReturnImage(imagePath);
        }
    }

    private File saveImageFromBitmap(@NonNull Bitmap bitmap, @NonNull File directory) {
        FileOutputStream fos = null;
        try {
            String  uniqueID = UUID.randomUUID().toString();
            String namefinalImage = "img_" + uniqueID + ".png";
            File savedImageFile = new File(directory,namefinalImage );

            fos = new FileOutputStream(savedImageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            return savedImageFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    public void ReturnImage(String imagePath) {
        Log.d(NAME,"runOnTapEvent - imagePath:"+imagePath);
        ActionDefinition actionDef = mCoordinator.getControlEventHandler(this, EVENT_RETURN_IMAGE);
        for (ActionParameter param : actionDef.getEventParameters()) {
            String paramName = param.getValueDefinition().getName();
            Log.d(NAME,"paramName: "+paramName);
            mCoordinator.setValue(paramName, imagePath);
        }

        mCoordinator.runControlEvent(this, EVENT_RETURN_IMAGE);
    }

    public void stopCamera() {
        preview.stop();
        if (cameraSource != null) {
            cameraSource.release();
        }
        faceDetectorProcessor.stop();
        editor.clear();
        editor.apply();
        startCaptureButton.setText("Iniciar Captura");
        startCaptureButton.setBackgroundColor(Color.parseColor("#192F78"));
        startCaptureButton.setTextColor(Color.parseColor("#FFFFFF"));
        ImageView frameOverlay = findViewById(R.id.frameOverlay);
        frameOverlay.setImageResource(R.drawable.frameface);
    }

    private String getDefinitionProperty(String propertyName) {
        return mLayoutDefinition.getControlInfo().optStringProperty(CONTROL_ID + propertyName);
    }

    @Override
    public String getGxValue()  {
        return mName;
    }

    @Override
    public void setGxValue(String s) {
        mName = s;
    }

    @Override
    public String getGxTag()  {
        return getTag().toString();
    }
    @Override
    public void setGxTag(String s) {
        setTag(s);
    }

    @Override
    public void setValueFromIntent(Intent intent) {

    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public IGxEdit getViewControl() {
        return null;
    }

    @Override
    public IGxEdit getEditControl() {
        return null;
    }

    public void setValue(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }
    @Override
    public Expression.Value callMethod(String name, List<Expression.Value> parameters) {
        if (METHOD_START_CAMERA.equals(name)) {
            Log.d(NAME, "METHOD_START_CAMERA");
            createCameraSource();
            startCameraSource();
        }
        if (METHOD_STOP_CAMERA.equals(name)) {
            Log.d(NAME, "METHOD_STOP_CAMERA");
            stopCamera();
        }

        return null;
    }

}
