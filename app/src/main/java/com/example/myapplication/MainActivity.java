package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "fann";

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;

    private TextureView mTextureView;
    private Handler mBackgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureAvailable: " + width + ", " + height);
                openCamera(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
        setContentView(mTextureView);

        mCameraManager = getSystemService(CameraManager.class);

        HandlerThread mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void openCamera(int width, int height) {
        try {
            mCameraManager.openCamera("0", new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "onOpened");
                    mCameraDevice = camera;
                    initSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d(TAG, "onDisconnected");
                    mCameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.d(TAG, "onError");
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }, null);
        } catch (Exception e) {
            Log.d(TAG, "openCamera Exception: " + e);
        }
    }

    private void initSession() {
        try {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            Surface surface = new Surface(surfaceTexture);

            List<OutputConfiguration> outConfigurations = new ArrayList<>();
            outConfigurations.add(new OutputConfiguration(surface));

            SessionConfiguration sessionConfig = new SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR, outConfigurations,
                    Executors.newSingleThreadExecutor(),
                    new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "onConfigured");
                    try {
                        CaptureRequest.Builder request = mCameraDevice.createCaptureRequest(
                                CameraDevice.TEMPLATE_PREVIEW);
                        request.addTarget(surface);
                        session.setRepeatingRequest(request.build(),
                                null, mBackgroundHandler);
                    } catch (Exception e) {
                        Log.e(TAG, "onConfigured Exception: " + e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            });

            mCameraDevice.createCaptureSession(sessionConfig);
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException: " + e);
        }
    }
}