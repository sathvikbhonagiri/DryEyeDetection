package com.example.grabi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class RedcamActivity extends AppCompatActivity {

    private TextureView textureView;
    private CameraDevice cameraDevice;
    private Button captureButton;
    private Button zoomInButton;
    private Button zoomOutButton;
    private boolean permissionRequestInProgress = false;
    private ImageReader imageReader;
    private CameraManager cameraManager;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewBuilder;

    private float maxZoom;
    private Rect sensorArraySize;
    private float currentZoomLevel = 1.0f; // Default zoom level

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redcam);
        textureView = findViewById(R.id.textureView);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        captureButton = findViewById(R.id.capture);
        zoomInButton = findViewById(R.id.zoomIn);
        zoomOutButton = findViewById(R.id.zoomOut);

        captureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                captureImage();
            }
        });

        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentZoomLevel < maxZoom) {
                    currentZoomLevel += 0.1f;
                    updateZoom(previewBuilder, currentZoomLevel);
                }
            }
        });

        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentZoomLevel > 1.0f) {
                    currentZoomLevel -= 0.1f;
                    updateZoom(previewBuilder, currentZoomLevel);
                }
            }
        });

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // Handle surface size change if needed
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                closeCamera();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // Do nothing here
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            permissionRequestInProgress = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with your logic (e.g., open camera)
                openCamera();
            } else {
                // Permission denied, handle it accordingly (e.g., show a message)
                Toast.makeText(this, "Camera permission denied. Cannot open camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        // Check camera permission
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Camera", "Camera permission not granted. Requesting...");
            // Request camera permission if not granted
            if (!permissionRequestInProgress) {
                permissionRequestInProgress = true;
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1);
            }
            Log.d("Camera", "Camera permission granted. Proceeding to open camera...");
            return;
        }

        // Permission is granted, proceed to open camera
        try {
            String cameraId = getCameraId();
            if (cameraId == null) {
                Log.e("Camera", "No suitable camera found.");
                return;
            }

            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    startPreview(); // Start camera preview when camera is opened
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                    Log.d("Camera", "Camera disconnected");
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    Log.e("Camera", "Error opening camera: " + error);
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private String getCameraId() throws CameraAccessException {
        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId;
            }
        }
        return null;
    }

    private void startPreview() {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        assert texture != null;

        texture.setDefaultBufferSize(640, 480);
        Surface surface = new Surface(texture);

        // Initialize imageReader
        imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1);

        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        captureSession.setRepeatingRequest(previewBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e("Camera", "Failed to configure camera preview.");
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureImage() {
        if (cameraDevice == null) {
            return;
        }

        try {
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            // Apply the current zoom level to the capture request
            updateZoom(captureBuilder, currentZoomLevel);
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                super.onCaptureCompleted(session, request, result);
                                // Image captured successfully, handle the captured image
                                processCapturedImage();
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e("Camera", "Failed to configure camera capture.");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void processCapturedImage() {
        Image image = imageReader.acquireLatestImage();
        if (image != null) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            // Save bitmap to device's gallery
            String imageUri = saveBitmapToDownloads(bytes);
            if (imageUri != null) {
                // Pass image URI via intent to DisplayImageActivity
                Intent intent = new Intent(this, DisplayImageActivity.class);
                intent.putExtra("imageUri", imageUri);
                startActivity(intent);
            }

            image.close();
        }
    }

    private String saveBitmapToDownloads(byte[] bytes) {
        try {
            // Get the Downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                if (!downloadsDir.mkdirs()) {
                    return null; // Unable to create directory
                }
            }

            // Create a file in the Downloads directory
            File imageFile = new File(downloadsDir, "captured_image115.jpg");
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            outputStream.write(bytes);
            outputStream.close();

            // Return the absolute path of the saved image file
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private Rect calculateCropRegion(float zoomLevel) {
        if (zoomLevel < 1.0f || zoomLevel > maxZoom) {
            throw new IllegalArgumentException("Invalid zoom level");
        }

        int centerX = sensorArraySize.width() / 2;
        int centerY = sensorArraySize.height() / 2;
        int deltaX = (int) ((0.5f * sensorArraySize.width()) / zoomLevel);
        int deltaY = (int) ((0.5f * sensorArraySize.height()) / zoomLevel);

        return new Rect(centerX - deltaX, centerY - deltaY, centerX + deltaX, centerY + deltaY);
    }

    private void updateZoom(CaptureRequest.Builder captureRequestBuilder, float zoomLevel) {
        Rect cropRegion = calculateCropRegion(zoomLevel);
        captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropRegion);
        try {
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
    }
}