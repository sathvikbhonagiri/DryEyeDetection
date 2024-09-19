package com.example.grabi;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class Blinktest extends AppCompatActivity {
    private TimerTask blinkDetectionTask;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraManager cameraManager;
    private volatile boolean isBlinkDetectionRunning = true;
    private FaceDetector faceDetector;
    private AtomicInteger blinkCount = new AtomicInteger(0);
    private TextView blinkCountTextView;
    private TextView timerTextView;
    private static final String TAG = "Blinktest";
    private static final long TIMER_DURATION = 40 * 1000;
    private Timer timer;
    private long remainingTime = TIMER_DURATION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blinktest_activity);

        // Initialize FaceDetector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();
        faceDetector = FaceDetection.getClient(options);

        blinkCountTextView = findViewById(R.id.blink_count_text_view);
        timerTextView = findViewById(R.id.timer_text_view);
        textureView = findViewById(R.id.textureView);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        if (!checkCameraHardware(this)) {
            showToast("No camera on this device");
        }

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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
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
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1);
        } else {
            // Permission is already granted, proceed to open camera
            try {
                String cameraId = getCameraId();
                if (cameraId == null) {
                    Log.e("Camera", "No suitable camera found.");
                    return;
                }

                cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        cameraDevice = camera;
                        startPreview();
                        Thread thread2 = new Thread() {
                            public void run() {
                                startBlinkTimer();
                            }
                        };
                        processInputImage();
                        thread2.start();
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

        try {
            final CaptureRequest.Builder previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(previewBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e("Camera", "Failed to configure camera preview.");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private boolean wasEyesOpen = true; // Initialize to true assuming eyes are open initially
    int c=0;
    private void processInputImage() {
        // Capture frame from TextureView and convert it to InputImage
        if (isBlinkDetectionRunning) {
            Bitmap bitmap = textureView.getBitmap();
            int rotation = getWindowManager().getDefaultDisplay().getRotation();

            InputImage inputImage = InputImage.fromBitmap(bitmap, rotation);
            // Process the input image for face detection
            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        boolean areEyesOpen = false; // Flag to track if eyes are open in this frame
                        // For each detected face, check if the eyes are closed
                        for (Face face : faces) {
                            if (face.getLeftEyeOpenProbability() != null && face.getRightEyeOpenProbability() != null &&
                                    face.getLeftEyeOpenProbability() < 0.5& face.getRightEyeOpenProbability() < 0.5){
                                areEyesOpen = true; // Set flag to true if any detected face has closed eyes
                            }
                        }
                        // Check if eyes were open in the previous frame but closed in this frame
                        if (wasEyesOpen && !areEyesOpen&&c!=0) {
                            // Increment blink count when eyes are closed in this frame but were open in the previous frame
                            blinkCount.incrementAndGet(); // Atomically increment blink count
                            runOnUiThread(() -> blinkCountTextView.setText("Blink Count: " + String.valueOf(blinkCount.get())));
                        }
                        // Update the flag for the next frame
                        wasEyesOpen = areEyesOpen;
                        // Continue processing for the next frame
                        processInputImage();
                        c++;
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Face detection failed: " + e.getMessage()));
        }
    }



    private void startBlinkTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                remainingTime -= 1000;
                if (remainingTime >= 0) {
                    updateTimerText();
                } else {
                    writeToFile("Blink Count : "+String.valueOf(blinkCount),"dry_data.txt");
                    writeToFile("","dry_data.txt");
                    timer.cancel();
                }
            }
        }, 0, 1000);
    }

    private void updateTimerText() {
        long minutes = remainingTime / 60000;
        long seconds = (remainingTime % 60000) / 1000;
        String timeLeft = String.format("%02d", seconds);
        runOnUiThread(() -> timerTextView.setText("Time Remaining :"+timeLeft+"S"));
    }

    private synchronized void writeToFile(String data, String filename) {
        File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDirectory, filename);

        try {
            FileOutputStream fos;
            if (file.exists()) {
                // If file already exists, append data to it
                fos = new FileOutputStream(file, true);
                // Append a new line before adding new data to keep it separated from previous content
                fos.write("\n".getBytes());
            } else {
                // If file doesn't exist, create a new file
                fos = new FileOutputStream(file);
            }

            fos.write(data.getBytes());
            fos.close();
            Toast.makeText(this, "Data written to file", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}
