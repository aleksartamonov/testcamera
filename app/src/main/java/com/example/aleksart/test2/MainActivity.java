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
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.RandomForest;
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
    private MatOfKeyPoint points = null;
    private final String fileName = "test.txt";
    private final int batchSize = 100;
    private final int countPoint = 100;
    private int radius;
    private final int EPS = 40;


    private FeatureDetector fd = null;
    private DescriptorMatcher dMatcher = null;
    private DescriptorExtractor dExtractor = null;
    private Classifier smo = null;
    List<Point> egorPoints = new ArrayList<Point>();
    List<Integer> ans = new ArrayList<Integer>();

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
            smo = (RandomForest) SerializationHelper.read(inputStream);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (fd == null) {
            fd = FeatureDetector.create(FeatureDetector.BRISK);
            dExtractor = DescriptorExtractor.create(DescriptorExtractor.BRISK);

        }
        if (requestCode == CAMERA_RESULT) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");


        } else if (requestCode == FILE_SELECT_CODE) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                radius = bitmap.getHeight() / 20;
                points = new MatOfKeyPoint();
                List<Descriptor> descriptors = Descriptor.getDescriptors(bitmap, fd, points);

                writeTestData(descriptors, fileName, bitmap);
                MatOfPoint mainContour = getAndWriteRect(bitmap);
                Mat result = getRow(mainContour, bitmap);
                bitmap  = getHigh(result);

                ivCamera.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(uri.getAuthority());
        }
    }

    private Mat getRow(MatOfPoint mainContour, Bitmap bitmap) {
        Mat imageCV = new Mat();
        Utils.bitmapToMat(bitmap, imageCV);
        System.out.println(mainContour.get(3, 0)[0] + " " + mainContour.get(3, 0)[1]);

        List<Point> bottomPoints = getPoint(mainContour);
        Point max_y = bottomPoints.get(0);
        Point max_x = bottomPoints.get(1);
        System.out.println(max_y);

        Rect rect = new Rect((int) max_y.x, 0, (int) ((max_x.x - max_y.x) * 1.5), (int) (max_y.y + 10));
        System.out.println("rect === " + rect);
        System.out.println("heigh" + imageCV.height());
        System.out.println("width" + imageCV.width());
        imageCV = new Mat(imageCV, rect);

        return imageCV;
    }

    private Bitmap getHigh(Mat row) {
        Mat lines = new Mat();
        Mat cur = new Mat();
        Imgproc.cvtColor(row, cur, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(cur, cur, 50, 200);
        Imgproc.GaussianBlur(cur, cur, new Size(5, 5), 1);
        Imgproc.HoughLinesP(cur, lines, 1, Math.PI / 2, 40, 150, 100);
        Bitmap bitmap = Bitmap.createBitmap(row.width(), row.height(), Bitmap.Config.RGB_565);
        Scalar r = new Scalar(255,0,0);
        Point pt1;
        Point pt2;
        int centerSign = row.width()/3;
        for (int  i =0; i < lines.height();i++ ) {
            pt1 = new Point((int)lines.get(i,0)[0],(int)lines.get(i,0)[1]);
            pt2 = new Point((int)lines.get(i,0)[2],(int)lines.get(i,0)[3]);
            int midX = (int)(pt1.x + pt2.x)/2;
            if (Math.abs(midX - centerSign) < EPS) {
                System.out.println("p1 = " + pt1 + "    " + "p2  " + pt2);
                Imgproc.line(cur, pt1, pt2, r, 10);
            }
        }
        Utils.matToBitmap(cur,bitmap);

        return bitmap;
    }

    private List<Point> getPoint(MatOfPoint contour) {
        int med = 0;
        for (int i = 0; i < 4; i++) {
            med += contour.get(i, 0)[1];
        }
        med /= 4;
        int min_x = 10000000;
        int max_y = -1;
        List<Point> current = new ArrayList<Point>();

        for (int i = 0; i < 4; i++) {
            if (contour.get(i, 0)[1] < med) {//y
                current.add(new Point((int) contour.get(i, 0)[0], (int) contour.get(i, 0)[1]));
                min_x = (int) contour.get(i, 0)[0];
                max_y = (int) contour.get(i, 0)[1];
            }
        }
        List<Point> result = new ArrayList<Point>();
        if (current.get(0).x > current.get(1).x) {
            result.add(current.get(1));
            result.add(current.get(0));
            return result;
        }
        return current;
    }


    private int findKeyPoints(InputStream input, Mat imageCV, int start) {

        int count = 0;
        try {
            Loader loader = new CSVLoader();
            loader.setSource(input);

            Instances data = loader.getDataSet();
            data.setClassIndex(0);

            NumericToNominal filter = new NumericToNominal();
            String[] options = new String[2];
            options[0] = "-R";
            options[1] = "1";
            filter.setOptions(options);
            filter.setInputFormat(data);
            data = Filter.useFilter(data, filter);

            KeyPoint[] keyPoints = points.toArray();
            Scalar red = new Scalar(255, 0, 0);
            Scalar yellow = new Scalar(255, 255, 0);

            for (int i = 0; i < data.numInstances(); i++) {
                double result = smo.classifyInstance(data.get(i));
                egorPoints.add(keyPoints[start + i].pt);
                if (result == 1) {
//                    Imgproc.circle(imageCV, keyPoints[start + i].pt, radius, red);
                    count++;
                    ans.add(1);
                } else {
//                    Imgproc.circle(imageCV, keyPoints[start + i].pt, radius, yellow);
                    ans.add(0);
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

    private MatOfPoint getAndWriteRect(Bitmap thumbnail) {
        Mat imageCV = new Mat();
        Mat imageRES = new Mat();
        Utils.bitmapToMat(thumbnail, imageCV);
        Utils.bitmapToMat(thumbnail, imageRES);
        Imgproc.cvtColor(imageCV, imageCV, Imgproc.COLOR_RGB2HSV_FULL);
        Imgproc.cvtColor(imageCV, imageCV, Imgproc.COLOR_RGB2GRAY);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        List<MatOfPoint> list = new ArrayList<MatOfPoint>();
        Imgproc.threshold(imageCV, imageCV, 128, 255, 0);
        Imgproc.findContours(imageCV, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        List<MatOfPoint2f> egor = new ArrayList<MatOfPoint2f>();
        for (int i = 0; i < contours.size(); i++) {
            double eps = 0.1 * Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true);
            MatOfPoint2f m = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), m, eps, true);
            if (m.height() == 4) {
                list.add(new MatOfPoint(m.toArray()));
                egor.add(m);
            }
        }
        int r = voting(egorPoints, egor, ans);
        List<MatOfPoint> dra = new ArrayList<MatOfPoint>();
        dra.add(list.get(r));
        Imgproc.drawContours(imageRES, dra, -1, new Scalar(0, 255, 0), 5);
        Utils.matToBitmap(imageRES, thumbnail);
        return dra.get(0);
    }

    private int voting(List<Point> points, List<MatOfPoint2f> contours, List<Integer> ans) {
        int countourRating[] = new int[contours.size()];
        Arrays.fill(countourRating, 0);
        for (int i = 0; i < points.size(); i++) {
            for (int j = 0; j < contours.size(); j++) {
                double u = Imgproc.pointPolygonTest(contours.get(j), points.get(i), true);
                if (u > 0) countourRating[j] += 100;
                else countourRating[j] -= 1;
            }
        }
        int best = -1, max = -(int) 1e8;
        for (int i = 0; i < contours.size(); i++) {
            if (countourRating[i] > max) {
                best = i;
                max = countourRating[i];
            }
        }
//        for (int i = 0; i < contours.size(); i++)
//            System.out.println("the best point " + countourRating[i]);
//        System.out.println("Len " + best + " " + max);
        return best;
    }

    private void writeTestData(List<Descriptor> descriptors, String fileName, Bitmap b) {
        StringBuilder sb = new StringBuilder();
        Mat imageCV = new Mat();
        Utils.bitmapToMat(b, imageCV);
        int number = 0;
        int count = 0;
        try {
            while (true) {
                sb.append((descriptors.get(0).getHeader()));
                for (int i = number * batchSize; i < descriptors.size() && i < number * batchSize + batchSize; i++) {
                    sb.append(descriptors.get(i));
                    sb.append("\n");
                }
                InputStream inputStream = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
                System.out.println(sb.toString());
                sb.setLength(0);
                count += findKeyPoints(inputStream, imageCV, number * batchSize);
                System.out.println(count + "..................");
                if (number * batchSize >= descriptors.size()) {
                    System.out.println("count = " + count + "  summary..." + descriptors.size());
                    break;
                }

                number++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

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

