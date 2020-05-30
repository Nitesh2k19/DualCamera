package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    CameraManager  cameraManager;
    int cameraFacingBack,g=0;
    int cameraFacingFront,cameraFacing;
    String cameraId;
   HandlerThread backgroundThread;
    Handler backgroundHandler;
    CameraDevice.StateCallback stateCallback;
    CameraCaptureSession.CaptureCallback sessionCallBack;
    CameraDevice cameraDevice;
    TextureView textureView,textureView2,textureView3;
    Size previewSize;
     CameraCaptureSession cameraCaptureSession;
     CaptureRequest captureRequest;
    TextureView.SurfaceTextureListener surfaceTextureListener;
    File galleryFolder;
    FloatingActionButton button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button=findViewById(R.id.capture);
         final int CAMERA_REQUEST_CODE= 1888;
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
               Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_REQUEST_CODE);
       createImageGallery();
          cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
         cameraFacingBack = CameraCharacteristics.LENS_FACING_BACK;
          cameraFacingFront=CameraCharacteristics.LENS_FACING_FRONT;
          cameraFacing=cameraFacingBack;
          textureView3=findViewById(R.id.texture_view_back);
          textureView2=findViewById(R.id.texture_view_front);
          textureView=textureView3;
        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                setUpCamera();
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };
       textureView.setSurfaceTextureListener(surfaceTextureListener);
      // textureView2.setSurfaceTextureListener(surfaceTextureListener);
         stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice cameraDevice) {
                MainActivity.this.cameraDevice = cameraDevice;
                createPreviewSession();

            }

            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                cameraDevice.close();
                MainActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(CameraDevice cameraDevice, int error) {
                cameraDevice.close();
                MainActivity.this.cameraDevice = null;
            }
        };
         sessionCallBack=new CameraCaptureSession.CaptureCallback(){
             @Override
             public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                 super.onCaptureCompleted(session, request, result);
                 if(cameraFacing==cameraFacingFront && g==0) {
                     button.performClick();
                     g++;

                 }

             }
         };

    }

    private void setUpCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {

                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        cameraFacing ) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                    this.cameraId = cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);

            }
        } catch (CameraAccessException e) {
            Log.v("VVVVVVVCVVV",cameraId);
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
       backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        if(cameraFacing==cameraFacingFront)
            onTakePhotoButtonClicked(button);
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
          final  CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();
                                MainActivity.this.cameraCaptureSession = cameraCaptureSession;
                                MainActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        sessionCallBack, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if(cameraFacing==cameraFacingBack) {
            if (textureView.isAvailable()) {
                setUpCamera();
                openCamera();
            } else {
                textureView.setSurfaceTextureListener(surfaceTextureListener);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void createImageGallery() {
     //  File storageDirectory = Environment.
        File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        //ContextWrapper cw = new ContextWrapper(getApplicationContext());
        //File storageDirectory = cw.getDir("imageDir", Context.MODE_PRIVATE);
         galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name));
        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory");
            }
        }
    }

    private File createImageFile(File galleryFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp + "_";
        return File.createTempFile(imageFileName, ".jpg", galleryFolder);
    }

    public void onTakePhotoButtonClicked(View view) {
        button.setBackground(getResources().getDrawable(R.drawable.clicked));

        FileOutputStream outputPhoto = null;
        try {
            lock();
            outputPhoto = new FileOutputStream(createImageFile(galleryFolder));
           Bitmap photo= textureView.getBitmap();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
           // unlock();
            try {
                if (outputPhoto != null) {
                    outputPhoto.close();
                    closeCamera();
                    closeBackgroundThread();
                   if(cameraFacing==cameraFacingBack) {
                       textureView = textureView2;
                       cameraFacing = cameraFacingFront;
                       if (textureView.isAvailable()) {
                           setUpCamera();
                           openCamera();
                       } else {
                           textureView.setSurfaceTextureListener(surfaceTextureListener);
                       }

                   }


                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void lock() {

        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            final  CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraCaptureSession.capture(captureRequestBuilder.build(),
                    null, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlock() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            final  CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
