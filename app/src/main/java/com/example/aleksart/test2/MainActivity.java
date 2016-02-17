package com.example.aleksart.test2;


import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.provider.MediaStore;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener {
    public final int CAMERA_RESULT = 0;
    private Button photo;
    private ImageView ivCamera;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        photo = (Button) findViewById(R.id.button1);
        ivCamera = (ImageView) findViewById(R.id.imageView1);

        photo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_RESULT);
            }

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_RESULT) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            Mat imageCV = new Mat();
            Utils.bitmapToMat(thumbnail, imageCV);
            Imgproc.cvtColor(imageCV, imageCV, Imgproc.COLOR_BGR2GRAY);
            Imgproc.Canny(imageCV, imageCV, 50, 200);
            Utils.matToBitmap(imageCV, thumbnail);
            ivCamera.setImageBitmap(thumbnail);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        return null;
    }
}