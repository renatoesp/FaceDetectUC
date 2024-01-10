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
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
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
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import com.google.mlkit.vision.common.InputImage;
import com.mti.R;
import com.mti.facedetectuc.mlkitfacedetection.GraphicOverlay;
import com.mti.facedetectuc.mlkitfacedetection.CameraSourcePreview;
import com.mti.facedetectuc.mlkitfacedetection.CameraSource;
import com.mti.facedetectuc.mlkitfacedetection.FaceDetectorProcessor;

@SuppressLint("ViewConstructor")
@androidx.camera.core.ExperimentalGetImage
public class FaceDetectUC extends FrameLayout implements IGxEdit, IGxControlRuntime {
    final static String NAME = "FaceDetectUC";
    private final static String EVENT_ON_TAP = "OnTap";
    private static final String METHOD_START_CAMERA = "startcamera";
    private static final String METHOD_STOP_CAMERA = "stopcamera";
    private static final String METHOD_TAKE_PHOTO = "takephoto";
    private static final String CONTROL_ID = "@" + NAME;
    private String mName;

    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private final Activity mactivity;
    private final Context mContext;
    private final Coordinator mCoordinator;
    private final LayoutItemDefinition mLayoutDefinition;
    private ImageCapture imageCapture;
    private static final String FACE_DETECTION = "FaceDetectionPrefs";
    private static final String TAG = "LivePreviewActivity";
    private FaceDetectorProcessor faceDetectorProcessor;
    private CameraSource cameraSource = null;
    private final CameraSourcePreview preview;
    private final GraphicOverlay graphicOverlay;
    private final SharedPreferences sharedPreferences;
    private final LottieAnimationView animationView;
    private final LottieAnimationController animationController;

    public FaceDetectUC(Context context, Coordinator coordinator, LayoutItemDefinition definition) {
        super(context);
        mCoordinator = coordinator;
        mContext = context;
        mLayoutDefinition = definition;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.activity_vision_live_preview, this);

        mactivity = mCoordinator.getUIContext().getActivity();

        sharedPreferences = mContext.getSharedPreferences(FACE_DETECTION, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        animationView = findViewById(R.id.lottieAnimationView);
        animationController = new LottieAnimationController(animationView);


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
    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        Log.d(NAME, "createCameraSource");

        // Se não houver uma fonte de câmera existente, crie uma.
        if (cameraSource == null) {
            cameraSource = new CameraSource(mactivity, graphicOverlay);
        }
        Log.i(NAME, "Using Face Detector Processor");

        faceDetectorProcessor = new FaceDetectorProcessor(mContext, sharedPreferences, animationController);
        faceDetectorProcessor.setImageProcessingListener(new FaceDetectorProcessor.ImageProcessingListener() {
            @Override
            public void onImageProcessed(Bitmap bitmap) {
                Log.d(NAME, "FaceDetectUC onImageProcessed");


                if (bitmap != null) {

//                    Log.d(NAME, "FaceDetectUC bitmap != null");
//                    ImageView imageView = findViewById(R.id.imageViewProcessed);
 //                   imageView.setImageBitmap(bitmap);

  //              } else {
                    File appDirectory = mContext.getApplicationContext().getFilesDir();
                    File savedImageFile = saveImageFromBitmap(bitmap, appDirectory);
                    if (savedImageFile != null) {
                        String imagePath = savedImageFile.getAbsolutePath();
                        Log.d(NAME, "FaceDetectUC imagePath:" + imagePath);
                        runOnTapEvent(imagePath);
                    }
                }
            }
        });

        cameraSource.setMachineLearningFrameProcessor(faceDetectorProcessor);
    }

    private File saveImageFromBitmap(@NonNull Bitmap bitmap, @NonNull File directory) {
        FileOutputStream fos = null;
        try {
            String  uniqueID = UUID.randomUUID().toString();
            ContextWrapper cw = new ContextWrapper(mactivity.getApplicationContext());
//            File directoryFile = cw.getDir("imageDir", Context.MODE_PRIVATE);
            File savedImageFile = new File(directory, uniqueID + ".jpg");

            fos = new FileOutputStream(savedImageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
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


    private File saveImageFromInputImage(InputImage image, File directory) {
        try {
            ByteBuffer buffer = image.getByteBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            String  uniqueID = UUID.randomUUID().toString();
            ContextWrapper cw = new ContextWrapper(mactivity.getApplicationContext());
            File directoryFile = cw.getDir("imageDir", Context.MODE_PRIVATE);
            File imageFile = new File(directoryFile, uniqueID + ".jpg");

            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(data);
            fos.close();

            return imageFile;
        } catch (IOException e) {
            Log.e(TAG, "Erro ao salvar a imagem: " + e.getMessage());
            return null;
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
    public void takePhoto() {
        Log.d(NAME, "TakePhoto()");
        String  uniqueID = UUID.randomUUID().toString();
        ContextWrapper cw = new ContextWrapper(mactivity.getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File file = new File(directory, uniqueID + ".jpg");

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(mContext),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = outputFileResults.getSavedUri();
                        Log.d(NAME, "Photo capture succeeded: " + savedUri);
                        runOnTapEvent(savedUri.toString());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(NAME, "Erro ao capturar a imagem: " + exception.getMessage());
                    }
                });
    }

    public void runOnTapEvent(String imagePath) {
        Log.d(NAME,"runOnTapEvent");
        ActionDefinition actionDef = mCoordinator.getControlEventHandler(this, EVENT_ON_TAP);
        for (ActionParameter param : actionDef.getEventParameters()) {
            String paramName = param.getValueDefinition().getName();
            Log.d(NAME,"paramName: "+paramName);
            mCoordinator.setValue(paramName, imagePath);
        }

        mCoordinator.runControlEvent(this, EVENT_ON_TAP);
    }

    public void stopCamera() {
        preview.stop();
        if (cameraSource != null) {
            cameraSource.release();
        }
        faceDetectorProcessor.stop();
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

    @Override
    public Expression.Value callMethod(String name, List<Expression.Value> parameters) {
        if (METHOD_START_CAMERA.equals(name)) {
            Log.d(NAME, "METHOD_START_CAMERA");
            // read parameters from the intent used to launch the activity.
            int qtdImages = Integer.parseInt(getDefinitionProperty("qtdImages"));
            Log.d(NAME, "qtdImages:"+qtdImages);

            createCameraSource();
            startCameraSource();
        }
        if (METHOD_TAKE_PHOTO.equals(name)) {
            Log.d(NAME, "METHOD_TAKE_PHOTO");
//			takePhoto();
        }
        if (METHOD_STOP_CAMERA.equals(name)) {
            Log.d(NAME, "METHOD_STOP_CAMERA");
            stopCamera();
        }

        return null;
    }

}
