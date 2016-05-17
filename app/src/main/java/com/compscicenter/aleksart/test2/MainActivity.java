package com.compscicenter.aleksart.test2;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.compscicenter.aleksart.test2.classificator.Classificator;
import com.compscicenter.aleksart.test2.saver.Saver;
import com.compscicenter.aleksart.test2.utils.Algorithm;
import com.compscicenter.aleksart.test2.utils.Line;
import com.compscicenter.aleksart.test2.utils.TypePoint;
import com.compscicenter.aleksart.test2.utils.Util;
import com.example.aleksart.test2.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photoview.PhotoViewAttacher;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener {
    public final int CAMERA_RESULT = 0;
    private static final int FILE_SELECT_CODE = 1;

    private Button photo;
    private Button getPhoto;
    private ImageView ivCamera;
    private TextView height;
    private TextView width;
    private AlertDialog choose = null;
    private final int RESIZE = 4;

    private double imageHeight;
    private boolean isHeight = true;


    private FeatureDetector fd = null;
    private DescriptorExtractor dExtractor = null;
    ;

    private String folderToSave = Environment.getExternalStorageDirectory()
            .toString();

    PhotoViewAttacher mAttacher;
    private Classificator classificator;

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
        Saver.setFolderToSave(folderToSave);
        setContentView(R.layout.activity_main);
        photo = (Button) findViewById(R.id.button1);
        getPhoto = (Button) findViewById(R.id.button2);
        ivCamera = (ImageView) findViewById(R.id.imageView1);
        height = (TextView) findViewById(R.id.height);
        width = (TextView) findViewById(R.id.width);
        height.setText("height = need do");
        width.setText("width = need do");

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
// Add the buttons
        builder.setPositiveButton("width", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                isHeight = false;
            }
        });
        builder.setNegativeButton("height", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                isHeight = true;
            }
        });


// Create the AlertDialog
        choose = builder.create();

//        trainPhoto = getTrainPhoto();
        photo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                choose.show();
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
        classificator = new Classificator(getApplicationContext().getResources().openRawResource(R.raw.svm));

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
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                if (bitmap.getHeight() < bitmap.getWidth()) {
                    bitmap = rotateBitmap(bitmap);
                }

                imageHeight = bitmap.getHeight();
                Bitmap resized = getResizeImage(bitmap);
                MatOfKeyPoint points = new MatOfKeyPoint();

                List<Descriptor> descriptors = Descriptor.getDescriptors(resized, fd, points);
                List<TypePoint> answer = null;
                try {
                    answer = classificator.classify(descriptors);
                } catch (RuntimeException e) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            e.getMessage(), Toast.LENGTH_LONG);
                    toast.show();
                    Saver.savePhoto(bitmap);
                    return;
                }
                MatOfPoint mainContour = Algorithm.findCountourSign(resized, answer, Util.getPointFromKeypoint(points.toArray()));
                List<Line> signLines = Util.getLinesFromContour(mainContour);
                Util.multiplyLines(signLines, RESIZE);

                if (isHeight) {
                    Mat lastResult = Algorithm.getRowForHeight(getMatFromLines(signLines), bitmap);
                    Util.addNumLines(signLines, -Algorithm.getShiftOfSmallCropped());

                    Mat gray = useFilter(lastResult);

                    List<Line> vLines;
                    try {
                        vLines = Algorithm.getLinesRow(gray, lastResult);
                    } catch (RuntimeException e) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Could not find lines", Toast.LENGTH_LONG);
                        toast.show();
                        Saver.savePhoto(bitmap);
                        return;
                    }
                    List<Point> signPoints = Util.lineToPoints(signLines, 1);
                    vLines = Fitting.fitLines(vLines, signPoints, lastResult);
                    signLines = Util.pointToLines(signPoints);
                    Util.addNumLines(vLines, Algorithm.getShiftOfSmallCropped());
                    Util.addNumLines(signLines, Algorithm.getShiftOfSmallCropped());
                    bitmap = drawLines(vLines, bitmap);
                    bitmap = drawLines(signLines, bitmap);
                    signLines = Line.sortLines(signLines);
                    double heightRow = Algorithm.countHeight(signLines, vLines, imageHeight);
                    System.out.println("WE HAVE FOUND HEIGHT = " + height);
                    height.setText(Double.toString(heightRow));
                    ivCamera.setImageBitmap(bitmap);
                    mAttacher = new PhotoViewAttacher(ivCamera);


                } else {
                    Mat lastResult = Algorithm.getRowForWidth(getMatFromLines(signLines), bitmap);
                    Mat gray = useFilter(lastResult);
                    List<Line> vLines;
                    try {
                        vLines = Algorithm.getLinesRow(gray, lastResult);
                    } catch (RuntimeException e) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Could not find lines", Toast.LENGTH_LONG);
                        toast.show();
                        Saver.savePhoto(bitmap);
                        return;
                    }
                    Imgproc.line(lastResult, vLines.get(0).getP1(), vLines.get(0).getP2(), new Scalar(0, 0, 255), 5);
                    Imgproc.line(lastResult, vLines.get(1).getP1(), vLines.get(1).getP2(), new Scalar(0, 0, 255), 5);
                    double resWidth = Algorithm.getSignWidth() * Math.abs(vLines.get(0).getP1().x - vLines.get(1).getP1().x)
                            / Algorithm.getSignWidth();
                    width.setText(Double.toString(resWidth));
                    bitmap = Bitmap.createBitmap(lastResult.width(), lastResult.height(), Bitmap.Config.RGB_565);
                    Utils.matToBitmap(lastResult, bitmap);
                    ivCamera.setImageBitmap(bitmap);
                    mAttacher = new PhotoViewAttacher(ivCamera);
                }
            } catch (Exception e) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Could not find lines", Toast.LENGTH_LONG);
                toast.show();
                Saver.savePhoto(bitmap);
                return;
            }

        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap) {
        Mat temp = new Mat();
        Utils.bitmapToMat(bitmap, temp);
        Core.flip(temp.t(), temp, 1);
        Bitmap res = Bitmap.createBitmap(temp.width(), temp.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(temp, res);
        return res;
    }



    private MatOfPoint getMatFromLines(List<Line> lines) {
        MatOfPoint result = new MatOfPoint();
        List<Point> cur = new ArrayList<Point>();
        cur.add(lines.get(0).getP1());
        cur.add(lines.get(1).getP1());
        cur.add(lines.get(2).getP1());
        cur.add(lines.get(3).getP1());
        result.fromList(cur);
        return result;
    }

    private Bitmap getResizeImage(Bitmap bitmap) {
        Mat image = new Mat();
        Utils.bitmapToMat(bitmap, image);
        Imgproc.resize(image, image, new Size(image.width() / RESIZE, image.height() / RESIZE));
        bitmap = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, bitmap);
        return bitmap;

    }

    private Bitmap drawLines(List<Line> lines, Bitmap res) {
        Mat result = new Mat();
        Utils.bitmapToMat(res, result);
        Scalar r = new Scalar(255, 0, 0);

        for (int i = 0; i < lines.size(); i++) {
            System.out.println();
            Point pt1 = lines.get(i).getP1();
            Point pt2 = lines.get(i).getP2();

            Imgproc.line(result, pt1, pt2, r, 3);

        }
        Bitmap bitmap = Bitmap.createBitmap(result.width(), result.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(result, bitmap);
        return bitmap;
    }


    private Mat getKernel() {
        Mat res = new Mat(5, 6, CvType.CV_32F);
        double[] ker = {-5.0, -3.0, -1.0, 1.0, 3.0, 5.0};
        for (int i = 0; i < res.height(); i++) {
            for (int j = 0; j < res.width(); j++) {
                res.put(i, j, ker[j]);
            }

        }
        return res;
    }

    private Mat useFilter(Mat row) {

        Mat lines = new Mat();
        Mat cur = new Mat(row.rows(), row.cols(), CvType.CV_32F);
        Imgproc.cvtColor(row, cur, Imgproc.COLOR_BGR2GRAY);


        Imgproc.filter2D(cur, cur, CvType.CV_32F, getKernel());

        double min = 1e8, max = -1e8;
        for (int i = 0; i < cur.height(); i++) {
            for (int j = 0; j < cur.width(); j++) {
                double t = cur.get(i, j)[0];
                t = Math.abs(t);
                cur.put(i, j, t);
                min = Math.min(min, cur.get(i, j)[0]);
                max = Math.max(max, cur.get(i, j)[0]);
            }
        }

        for (int i = 0; i < cur.height(); i++) {
            for (int j = 0; j < cur.width(); j++) {
                double t = cur.get(i, j)[0];
                t = ((t - min) / (max - min)) * 256;
                cur.put(i, j, t);
            }
        }

        cur.convertTo(cur, CvType.CV_8U);

//        Utils.matToBitmap(cur,bitmap);

        return cur;
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