package com.example.grabi;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class DisplayImageActivity extends AppCompatActivity {
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_image);
        OpenCVLoader.initDebug();
        ImageView imageView = findViewById(R.id.imageView);
        ImageView imageView1= findViewById(R.id.imageView1);
        Button RedButton=findViewById(R.id.RedButton);
        RedButton.setEnabled(false);
        TextView textView1=findViewById(R.id.textView1);
        TextView textView2=findViewById(R.id.textView2);
        TextView textView3=findViewById(R.id.textView3);
        TextView textView4=findViewById(R.id.textView4);
        TextView textView5=findViewById(R.id.textView5);
        // Retrieve image URI from intent
        String uriString = getIntent().getStringExtra("imageUri");
        //Toast.makeText(this,""+uriString,Toast.LENGTH_LONG).show();
        if (uriString != null) {
            int fb=0;
            int fl=0;
            Uri imageUri = Uri.parse(uriString);
            // Now you can use this imageUri to load the image into ImageView or perform any other operation
            imageView.setImageURI(imageUri);
            imageView.setRotation(270);
            //Toast.makeText(this,""+imageView.getRotation(),Toast.LENGTH_LONG).show();
            Drawable drawable = imageView.getDrawable();
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Bitmap rotatedBitmap = rotateBitmap(bitmap, 90);
            //imageView1.setImageBitmap(rotatedBitmap);
            imageView1.setImageResource(R.drawable.refimg);
            // Convert Bitmap to Mat
            Mat eyeMat = new Mat();
            Utils.bitmapToMat(rotatedBitmap, eyeMat);
            // Convert the image to grayscale
            Mat grayEye = new Mat();
            Imgproc.cvtColor(eyeMat, grayEye, Imgproc.COLOR_BGR2GRAY);
            // Load the pre-trained cascade classifier from the raw resources directory
            CascadeClassifier eyeCascade = new CascadeClassifier();
            InputStream inputStream = getResources().openRawResource(R.raw.haarcascade_eye);
            File cascadeFile = new File(getFilesDir(), "haarcascade_eye.xml");

            try {
                // Copy the file from raw resources to app's internal storage
                OutputStream outputStream = new FileOutputStream(cascadeFile);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();

                // Load the pre-trained cascade classifier from the internal storage
                eyeCascade.load(cascadeFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Detect eyes
            MatOfRect eyeDetections = new MatOfRect();
            eyeCascade.detectMultiScale(grayEye, eyeDetections);
            Rect[] eyesArray = eyeDetections.toArray();
            // Validate and filter eye detections
            ArrayList<Rect> filteredEyes = new ArrayList<>();
            for (Rect eye : eyesArray) {
                // Validate eye detection based on aspect ratio, size, and position
                double aspectRatio = (double) eye.width / eye.height;
                if (aspectRatio > 0.9 && aspectRatio < 1.6 && eye.area() > 350) {
                    // Add validated eye detection to filtered list
                    filteredEyes.add(eye);
                }
            }
            //Toast.makeText(this, "total eyes are" + filteredEyes.size(), Toast.LENGTH_LONG).show();
            //storing whether there is eye or not
            int totaleyes = filteredEyes.size();

            //Toast.makeText(this,""+totaleyes,Toast.LENGTH_LONG).show();
            if (totaleyes == 1)
            {

                textView3.setBackgroundResource(R.color.green);



            }
            if(isnotBlurry(rotatedBitmap))
            {
                fb=1;
                textView5.setBackgroundResource(R.color.green);

            }
            if(isLit(rotatedBitmap))
            {
                fl=1;
                textView4.setBackgroundResource(R.color.green);

            }
            if(totaleyes==1&&fb==1&&fl==1) {
                int x=filteredEyes.get(0).x;
                int  y=filteredEyes.get(0).y;
                int height=filteredEyes.get(0).height;
                int width=filteredEyes.get(0).width;
                RedButton.setEnabled(true);
                RedButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        calredness(rotatedBitmap,x,y,width,height);
                    }
                });

            }

        }
        // Set image to ImageView

    }
    public void calredness(Bitmap bitmap,int x,int y,int width,int height) {

        long sum=0;
        int np=0;
        int r=0;
        // Loop through every pixel and extract RGB values
        for (int j = x; j < bitmap.getHeight(); j++) {
            for (int i =y; i < bitmap.getWidth(); i++) {
                int pixel = bitmap.getPixel(i, j);

                int red = Color.red(pixel);
                int  green = Color.green(pixel);
                int  blue = Color.blue(pixel);
                r=red;
                if(r!=0)
                  np++;
                sum=sum+red;

            }


        }
        long redness=sum;
        long p=sum/np;
        long norm=(p-100)*100/350;
       // Toast.makeText(this,"un normalized"+p,Toast.LENGTH_LONG).show();
        if(np!=0&&norm>=0) {
            Toast.makeText(this, "Redness value : " + (norm), Toast.LENGTH_LONG).show();
            redness = norm;
            writeToFile("Redness Value : "+String.valueOf(redness),"dry_data.txt");
            writeToFile("","dry_data.txt");
        }
        else if(norm<0) {
            Toast.makeText(this, "Redness value : " + 0, Toast.LENGTH_LONG).show();
            redness = 0;
            writeToFile("Redness Value : "+String.valueOf(redness),"dry_data.txt");
            writeToFile("","dry_data.txt");
        }
        else
            Toast.makeText(this,"Take pic Again",Toast.LENGTH_LONG).show();
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


    private Bitmap rotateBitmap(Bitmap source, float angleDegrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angleDegrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    public static boolean isnotBlurry(Bitmap bitmap)
    {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        // Blur detection
        double laplacianVariance = calculateLaplacianVariance(mat);
        boolean isnotBlurr = laplacianVariance > BLUR_THRESHOLD;
        return isnotBlurr;
    }
    // Constants for thresholds
    private static final double BLUR_THRESHOLD = 7.0;
    private static final double LIGHT_THRESHOLD = 115.0;
    public static boolean isLit(Bitmap bitmap)
    {  // Convert bitmap to grayscale
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        double averageIntensity = calculateAverageIntensity(mat);
        boolean isLi = averageIntensity >LIGHT_THRESHOLD;
        return isLi;
    }
    private static double calculateLaplacianVariance(Mat mat) {
        Mat laplacian = new Mat();
        Imgproc.Laplacian(mat, laplacian, CvType.CV_64F);
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stddev);
        double variance = stddev.get(0, 0)[0] * stddev.get(0, 0)[0];
        return variance;
    }

    private static double calculateAverageIntensity(Mat mat) {
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(mat, mean, stddev);
        double averageIntensity = mean.get(0, 0)[0];
        return averageIntensity;
    }



}
