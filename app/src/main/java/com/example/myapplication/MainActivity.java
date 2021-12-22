package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
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

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureRequest;

    private TextureView mTextureView;
    private Handler mBackgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraManager = getSystemService(CameraManager.class);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        setContentView(mTextureView);

        HandlerThread mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        Log.d("fann", "onSurfaceTextureAvailable: " + width + ", " + height);
        openCamera(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) { }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) { }

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request,
                                        CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
        }
    };

    CameraCaptureSession.StateCallback mStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d("fann", "onConfigured!!");
            try {
                session.setRepeatingRequest(mCaptureRequest.build(),
                        mCaptureCallback, mBackgroundHandler);
            } catch (Exception e) {
                Log.e("fann", "onConfigured Exception: " + e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d("fann", "onConfigureFailed");
        }
    };

    private void openCamera(int width, int height) {
        try {
            mCameraManager.openCamera("0", new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d("fann", "onOpened");
                    mCameraDevice = camera;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d("fann", "onDisconnected");
                    mCameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.d("fann", "onError");
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }, null);
        } catch (Exception e) {
            Log.d("fann", "openCamera Exception: " + e);
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            Surface surface = new Surface(surfaceTexture);
            mCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequest.addTarget(surface);

            List<OutputConfiguration> outConfigurations = new ArrayList<>();
            outConfigurations.add(new OutputConfiguration(surface));
            SessionConfiguration sessionConfig = new SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR, outConfigurations, Executors.newSingleThreadExecutor(), mStateCallback);

            mCameraDevice.createCaptureSession(sessionConfig);
        } catch (CameraAccessException e) {
            Log.e("fann", "CameraAccessException: " + e);
        }
    }
}