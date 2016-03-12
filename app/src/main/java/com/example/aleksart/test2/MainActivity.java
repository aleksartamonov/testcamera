package com.example.aleksart.test2;


import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.provider.MediaStore;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.CSVLoader;
import weka.core.converters.Loader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;




public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener {
    public final int CAMERA_RESULT = 0;
    private static final int FILE_SELECT_CODE = 1;
    private Button photo;
    private Button getPhoto;
    private ImageView ivCamera;
    private List<TrainImage> trainPhoto = new ArrayList<TrainImage>();
    private MatOfKeyPoint points = null;
    private final String fileName = "test.txt";
    private final int batchSize = 100;

    //    private String folderToSave = Environment.getExternalStorageDirectory()
//            .toString();
    private String folderToSave = "/storage/external_SD/DCIM/CAMERA";

//    private FeatureDetector fd = FeatureDetector.create(FeatureDetector.BRISK);
//    private DescriptorMatcher dMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);
//    private DescriptorExtractor dExtractor = DescriptorExtractor.create(DescriptorExtractor.BRISK);

    private FeatureDetector fd = null;
    private DescriptorMatcher dMatcher = null;
    private DescriptorExtractor dExtractor = null;
    private SMO smo = null;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private class TrainImage {
        Mat mat;
        MatOfKeyPoint matOfKeyPoint;
        Mat descriptors;


        public TrainImage(Mat mat, MatOfKeyPoint matOfKeyPoint, Mat descriptors) {
            this.mat = mat;
            this.matOfKeyPoint = matOfKeyPoint;
            this.descriptors = descriptors;

        }

        public Mat getMat() {
            return mat;
        }

        public Mat getDescriptors() {
            return descriptors;
        }

        public MatOfKeyPoint getMatOfKeyPoint() {
            return matOfKeyPoint;
        }
    }

    private class Descriptor {
        private List<Double> data;
        public Descriptor(List<Double> e) {
            data = e;
        }
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.size() - 1; i++) {
                sb.append(data.get(i));
                sb.append(",");
            }
            sb.append(data.get(data.size() - 1));
            return sb.toString();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        photo = (Button) findViewById(R.id.button1);
        getPhoto = (Button) findViewById(R.id.button2);
        ivCamera = (ImageView) findViewById(R.id.imageView1);
//        trainPhoto = getTrainPhoto();
        photo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_RESULT);
            }
        });
        getPhoto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                startActivityForResult(
                        Intent.createChooser(intent, "Select a File to Upload"),
                        FILE_SELECT_CODE);
            }
        });
        loadSVM();
    }

    private void loadSVM() {
        InputStream inputStream = getApplicationContext().getResources().openRawResource(R.raw.svm);
        try {
            smo = (SMO) SerializationHelper.read(inputStream);
            System.out.println(smo.getTechnicalInformation());
            //test();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (fd == null) {
            trainPhoto = getTrainPhoto();

        }
        if (requestCode == CAMERA_RESULT) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");

//            ivCamera.setImageBitmap(getDescriptors(thumbnail));

        } else if (requestCode == FILE_SELECT_CODE) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                List<Descriptor> descriptors = getDescriptors(bitmap);
                writeTestData(descriptors, fileName, bitmap);
//                findKeyPoints(bitmap);
                ivCamera.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(uri.getAuthority());
        }
    }

    private int findKeyPoints( InputStream input, Mat imageCV) {

        int count = 0;
        try {
            Loader loader = new CSVLoader();
            loader.setSource(input);

            Instances data = loader.getDataSet();
            NumericToNominal filter = new NumericToNominal();
            String[] options = new String[2];
            options[0] = "-R";
            options[1] = "1";
            filter.setOptions(options);
            filter.setInputFormat(data);
            data = Filter.useFilter(data, filter);
            data.setClassIndex(0);
            KeyPoint[] keyPoints = points.toArray();
            Scalar red = new Scalar(255,0,0);
            for (int i = 0; i <data.numInstances(); i++) {
                double result = smo.classifyInstance(data.get(i));
//                System.out.println(result);
                if (result == 1) {
                    Imgproc.circle(imageCV,keyPoints[i].pt,40,red);
                    count++;
                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Utils.matToBitmap(imageCV, bitmap);
        return count;
    }

    private void writeTestData(List<Descriptor> descriptors,String fileName, Bitmap b) {
        StringBuilder sb = new StringBuilder();
        Mat imageCV = new Mat();
        Utils.bitmapToMat(b, imageCV);
//        Imgproc.cvtColor(imageCV, imageCV, Imgproc.COLOR_BGR2GRAY);
        int number = 0;
        int count = 0;
        try {
            while(true) {
                sb.append(getHeader(descriptors.get(0)));
                for (int i = number * batchSize ; i < descriptors.size() && i < number * batchSize + batchSize; i++){
                    sb.append(descriptors.get(i));
                    sb.append("\n");
                }
//                System.out.println(sb.toString());
                InputStream inputStream = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
                sb.setLength(0);
                count += findKeyPoints(inputStream, imageCV);
                System.out.println(count+"..................");
                if (count > 200) {
                    Utils.matToBitmap(imageCV, b);
                    break;
                }
                number++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String getHeader(Descriptor descriptor) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < descriptor.data.size() - 1; i++) {
            sb.append("a");
            sb.append(i);
            sb.append(",");
        }
        sb.append("a");
        sb.append(descriptor.data.size() - 1);
        sb.append("\n");
        return sb.toString();
    }

    private List<Descriptor> getDescriptors(Bitmap thumbnail) {
        Mat imageCV = new Mat();
        Utils.bitmapToMat(thumbnail, imageCV);
        Imgproc.cvtColor(imageCV, imageCV, Imgproc.COLOR_BGR2GRAY);


        points = new MatOfKeyPoint();
        fd.detect(imageCV, points);

        Mat descriptorTest = new Mat();
        dExtractor.compute(imageCV, points, descriptorTest);
        List<Descriptor> result = new ArrayList<Descriptor>();
        KeyPoint[] keyPoints = points.toArray();
        for (int i = 0; i < descriptorTest.height(); i++) {
            List<Double> d = new ArrayList<Double>();
            d.add((double) 0);
            d.add((double) keyPoints[i].angle);
            d.add((double) keyPoints[i].octave);
            d.add((double) keyPoints[i].response);
            d.add((double) keyPoints[i].size);
            d.add((double) keyPoints[i].pt.x);
            d.add((double) keyPoints[i].pt.y);
            result.add(getDescriptor(d,descriptorTest, i));
        }
        System.out.println("extract feature");
        return result;
    }

    private Descriptor getDescriptor(List<Double> cur,Mat descriptors, int i) {
        List<Double> result = cur;
        for (int j = 0; j < descriptors.width(); j++) {
            result.add(descriptors.get(i,j)[0]);
        }
        return new Descriptor(result);
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

    public List<TrainImage> getTrainPhoto() {
        List<TrainImage> result = new ArrayList<TrainImage>();
//        Field[] drawables = android.R.drawable.class.getFields();
        fd = FeatureDetector.create(FeatureDetector.BRISK);
        dMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);
        dExtractor = DescriptorExtractor.create(DescriptorExtractor.BRISK);
        Drawable drawable = getResources().getDrawable(R.drawable.black);
        Bitmap cur = ((BitmapDrawable) drawable).getBitmap();
        Mat mat = new Mat();
        Utils.bitmapToMat(cur, mat);
        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        fd.detect(mat, keypoints);
        Mat descriptorTest = new Mat();
        dExtractor.compute(mat, keypoints, descriptorTest);
        result.add(new TrainImage(mat, keypoints, descriptorTest));


        return result;
    }
}