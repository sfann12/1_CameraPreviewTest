package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "fann";

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;

    private TextureView mTextureView;
    private Handler mBackgroundHandler;

    private CameraCaptureSession mSession;
    private ImageReader mImageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextureView = new TextureView(this);
        mTextureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
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

        mImageReader = ImageReader.newInstance(100, 100,
                ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d(TAG, "onImageAvailable");
                Image image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                OutputStream output = null;
                try {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, "title");
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis());
                    values.put(MediaStore.Images.Media.DESCRIPTION, "description");
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    // Add the date meta data to ensure the image is added at the front of the gallery
                    values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
                    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

                    Uri fileUri = getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    output = getContentResolver().openOutputStream(fileUri);
                    output.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    image.close();
                    if (output != null) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, mBackgroundHandler);
    }

    private void openCamera(int width, int height) {
        try {
            mCameraManager.openCamera("0", new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "onOpened");
                    mCameraDevice = camera;
                    initPreviewSession();
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

    private void initPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            Surface surface = new Surface(surfaceTexture);

            List<OutputConfiguration> outConfigurations = new ArrayList<>();
            outConfigurations.add(new OutputConfiguration(surface));
            outConfigurations.add(new OutputConfiguration(mImageReader.getSurface()));

            SessionConfiguration sessionConfig = new SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR, outConfigurations,
                    Executors.newSingleThreadExecutor(),
                    new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "onConfigured");
                    try {
                        CaptureRequest.Builder previewBuilder = mCameraDevice.createCaptureRequest(
                                CameraDevice.TEMPLATE_PREVIEW);
                        previewBuilder.addTarget(surface);

                        mSession = session;
                        mSession.setRepeatingRequest(previewBuilder.build(),
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

    private void takePicture() {
        try {
            CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            CameraCaptureSession.CaptureCallback captureCallback
                    = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.d(TAG, "onCaptureCompleted");
                    Toast.makeText(getApplicationContext(), "onCaptureCompleted", Toast.LENGTH_SHORT).show();
                    initPreviewSession();
                }
            };

            mSession.stopRepeating();
            mSession.abortCaptures();
            mSession.capture(captureBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}