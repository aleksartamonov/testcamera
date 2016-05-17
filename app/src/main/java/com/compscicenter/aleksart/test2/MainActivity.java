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
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import uk.co.senab.photoview.PhotoViewAttacher;
import weka.classifiers.Classifier;




public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener {
    public final int CAMERA_RESULT = 0;
    private static final int FILE_SELECT_CODE = 1;

    private Button photo;
    private Button getPhoto;
    private ImageView ivCamera;
    private TextView height;
    private TextView width;
    private MatOfKeyPoint points = null;
    private AlertDialog choose = null;
    private final int batchSize = 100;
    private final int RESIZE = 4;
    private int SHIFT_OF_SMALL_CROPPED;
    private double HEIGHT_OF_SIGN = 0.7;
    private double imageHeight;
    private boolean isHeight = true;
    private double SIGN_WIDTH;


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
//                    classify(descriptors);
                    answer = classificator.classify(descriptors);
                } catch (RuntimeException e) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            e.getMessage(), Toast.LENGTH_LONG);
                    toast.show();
//                    writeImageFile(bitmap);
                    Saver.savePhoto(bitmap);
                    return;
                }
//                MatOfPoint mainContour = getAndWriteRect(resized, answer, getPointFromKeypoint(points.toArray()));
                MatOfPoint mainContour = Algorithm.findCountourSign(resized, answer, getPointFromKeypoint(points.toArray()));
                List<Line> signLines = getLinesFromContour(mainContour);
                multiplyLines(signLines);

                if (isHeight) {
                    Mat lastResult = getRow(getMatFromLines(signLines), bitmap);
                    addNumLines(signLines, -SHIFT_OF_SMALL_CROPPED);

                    Mat gray = useFilter(lastResult);

                    List<Line> vLines;
                    try {
                        vLines = getLines(gray, lastResult);
                    } catch (RuntimeException e) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Could not find lines", Toast.LENGTH_LONG);
                        toast.show();
//                        writeImageFile(bitmap);
                        Saver.savePhoto(bitmap);
                        return;
                    }
                    Mat current = new Mat();
                    List<Point> vPoints = lineToPoints(vLines, 0);
                    List<Point> signPoints = lineToPoints(signLines, 1);
                    vLines = fitLines(vLines, signPoints, lastResult);
                    signLines = pointToLines(signPoints);
                    addNumLines(vLines, SHIFT_OF_SMALL_CROPPED);
                    addNumLines(signLines, SHIFT_OF_SMALL_CROPPED);
                    bitmap = drawLines(vLines, bitmap);
                    bitmap = drawLines(signLines, bitmap);
                    signLines = sortLines(signLines);
                    double heightRow = countHeight(signLines, vLines);
                    System.out.println("WE HAVE FOUND HEIGHT = " + height);
                    height.setText(Double.toString(heightRow));
                    ivCamera.setImageBitmap(bitmap);
                    mAttacher = new PhotoViewAttacher(ivCamera);


                } else {
                    Mat lastResult = getRowForWidth(getMatFromLines(signLines), bitmap);
                    Mat gray = useFilter(lastResult);
                    List<Line> vLines;
                    try {
                        vLines = getLines(gray, lastResult);
                    } catch (RuntimeException e) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Could not find lines", Toast.LENGTH_LONG);
                        toast.show();
//                        writeImageFile(bitmap);
                        Saver.savePhoto(bitmap);
                        return;
                    }
                    Imgproc.line(lastResult, vLines.get(0).getP1(), vLines.get(0).getP2(), new Scalar(0, 0, 255), 5);
                    Imgproc.line(lastResult, vLines.get(1).getP1(), vLines.get(1).getP2(), new Scalar(0, 0, 255), 5);
                    double resWidth = HEIGHT_OF_SIGN * Math.abs(vLines.get(0).getP1().x - vLines.get(1).getP1().x) / SIGN_WIDTH;
                    width.setText(Double.toString(resWidth));
                    bitmap = Bitmap.createBitmap(lastResult.width(), lastResult.height(), Bitmap.Config.RGB_565);
                    Utils.matToBitmap(lastResult, bitmap);
                    ivCamera.setImageBitmap(bitmap);
                    mAttacher = new PhotoViewAttacher(ivCamera);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Could not find lines", Toast.LENGTH_LONG);
                toast.show();
//                writeImageFile(bitmap);
                Saver.savePhoto(bitmap);
                return;
            }

        }
    }

    private List<Point> getPointFromKeypoint(KeyPoint[] keyPoints) {
        List<Point> result = new ArrayList<Point>();
        for (KeyPoint p : keyPoints) {
            result.add(p.pt);
        }
        return result;
    }

    private Bitmap rotateBitmap(Bitmap bitmap) {
        Mat temp = new Mat();
        Utils.bitmapToMat(bitmap, temp);
        Core.flip(temp.t(), temp, 1);
        Bitmap res = Bitmap.createBitmap(temp.width(), temp.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(temp, res);
        return res;
    }

    private Mat getRowForWidth(MatOfPoint mainContour, Bitmap bitmap) {
        Mat imageCV = new Mat();
        Utils.bitmapToMat(bitmap, imageCV);
        System.out.println(mainContour.get(3, 0)[0] + " " + mainContour.get(3, 0)[1]);

        List<Point> upPoints = getUpPointSign(mainContour);
        Point p1 = upPoints.get(0);
        Point p2 = upPoints.get(1);
        SIGN_WIDTH = p2.x - p1.x;
        System.out.println(p1);

        Rect rect = new Rect((int) ((p1.x) * 1.1), 0, (int) ((p2.x - p1.x) * 0.8), (int) (p1.y * 0.9));
        SHIFT_OF_SMALL_CROPPED = (int) p1.x - (int) ((p2.x - p1.x) * 1);
        System.out.println("rect === " + rect);
        System.out.println("heigh" + imageCV.height());
        System.out.println("width" + imageCV.width());
        imageCV = new Mat(imageCV, rect);
        System.out.println();

        return imageCV;
    }

    private List<Point> getUpPointSign(MatOfPoint contour) {
        int med = 0;
        for (int i = 0; i < 4; i++) {
            med += contour.get(i, 0)[1];
        }
        med /= 4;
        List<Point> current = new ArrayList<Point>();

        for (int i = 0; i < 4; i++) {
            if (contour.get(i, 0)[1] <= med) {//y
                current.add(new Point((int) contour.get(i, 0)[0], (int) contour.get(i, 0)[1]));
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

    private double countHeight(List<Line> signLines, List<Line> vLines) {
        System.out.println("getP1.x " + signLines.get(0).getP1().x + " getP1.y " + signLines.get(0).getP1().y);
        System.out.println("getP2.x " + signLines.get(0).getP2().x + " getP2.y " + signLines.get(0).getP2().y);
        System.out.println("getP1.x " + signLines.get(1).getP1().x + " getP1.y " + signLines.get(1).getP1().y);
        System.out.println("getP2.x " + signLines.get(1).getP2().x + " getP2.y " + signLines.get(1).getP2().y);
        System.out.println("getP1.x " + signLines.get(2).getP1().x + " getP1.y " + signLines.get(2).getP1().y);
        System.out.println("getP2.x " + signLines.get(2).getP2().x + " getP2.y " + signLines.get(2).getP2().y);
        System.out.println("SPASE");
        System.out.println("getP1.x " + vLines.get(0).getP1().x + " getP1.y " + vLines.get(0).getP1().y);
        System.out.println("getP2.x " + vLines.get(0).getP2().x + " getP2.y " + vLines.get(0).getP2().y);
        System.out.println("getP1.x " + vLines.get(1).getP1().x + " getP1.y " + vLines.get(1).getP1().y);
        System.out.println("getP2.x " + vLines.get(1).getP2().x + " getP2.y " + vLines.get(1).getP2().y);
        System.out.println("getP1.x " + vLines.get(2).getP1().x + " getP1.y " + vLines.get(2).getP1().y);
        System.out.println("getP2.x " + vLines.get(2).getP2().x + " getP2.y " + vLines.get(2).getP2().y);
        double x1 = vLines.get(1).getP1().y;
        double x2 = (signLines.get(1).getP1().y + signLines.get(1).getP2().y) / 2;
        double x3 = (signLines.get(3).getP1().y + signLines.get(3).getP2().y) / 2;
        double beta = (1.0 / 3.0 * (x3 - x1) / imageHeight) * Math.PI;
        double k = (x3 - x1) / (x3 - x2) + 1;
        System.out.println("TANGENS BETA " + Math.tan(beta));
        System.out.println("BETA " + beta);
        System.out.println("K " + k);
        System.out.println("HEIGHT " + HEIGHT_OF_SIGN);
        return Math.tan(beta) / beta * k * HEIGHT_OF_SIGN;

    }

    private List<Line> pointToLines(List<Point> vPoints) {
        List<Line> lines = new ArrayList<Line>();
        for (int i = 0; i < vPoints.size(); i++) {
            Point p1 = vPoints.get(i), p2 = vPoints.get((i + 1) % vPoints.size());
            lines.add(new Line(p1.x, p1.y, p2.x, p2.y));
        }
        return lines;
    }

    private List<Point> lineToPoints(List<Line> vLines, int key) {
        Point p1, p2, p3, p4;
        if (vLines.size() == 2) {
            p1 = new Point(vLines.get(0).getP1().x, vLines.get(0).getP1().y);
            p2 = new Point(vLines.get(0).getP2().x, vLines.get(0).getP2().y);
            p3 = new Point(vLines.get(1).getP1().x, vLines.get(1).getP1().y);
            p4 = new Point(vLines.get(1).getP2().x, vLines.get(1).getP2().y);
        } else {
            p1 = new Point(vLines.get(0).getP1().x, vLines.get(0).getP1().y);
            p2 = new Point(vLines.get(1).getP1().x, vLines.get(1).getP1().y);
            p3 = new Point(vLines.get(2).getP1().x, vLines.get(2).getP1().y);
            p4 = new Point(vLines.get(3).getP1().x, vLines.get(3).getP1().y);

        }
        if (key == 0) {
            if (p1.y > p2.y) {
                Point t = p1;
                p1 = p2;
                p2 = t;
            }

            if (p3.y < p4.y) {
                Point t = p3;
                p3 = p4;
                p4 = t;
            }
        }
        List<Point> points = new ArrayList<Point>();
        points.add(p1);
        points.add(p2);
        points.add(p3);
        points.add(p4);
        return points;
    }


    private List<Line> fitLines(List<Line> vLines, List<Point> signPoints, Mat lastResult) {
        Mat gray = new Mat();
        Imgproc.cvtColor(lastResult, gray, Imgproc.COLOR_BGR2GRAY);

//        Mat res = Fitting.improveSign(signPoints, gray);
//        List<Line> lineRow = getLines(lastResult,null);
        return Fitting.improvePole(vLines, lastResult);

    }

    private void multiplyLines(List<Line> signLines) {
        for (int i = 0; i < signLines.size(); i++) {
            signLines.set(i, signLines.get(i).multiply1(RESIZE));
        }
    }

    private void addNumLines(List<Line> signLines, int num) {
        for (int i = 0; i < signLines.size(); i++) {
            signLines.set(i, signLines.get(i).addNum(num));
        }
    }

    private List<Line> getLinesFromContour(MatOfPoint mainContour) {
        List<Line> res = new ArrayList<Line>();
        int len = mainContour.height();
        for (int i = 0; i < len; i++) {
            res.add(new Line(mainContour.get(i, 0)[0], mainContour.get(i, 0)[1],
                    mainContour.get((i + 1) % len, 0)[0], mainContour.get((i + 1) % len, 0)[1]));
        }
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

    private double length(double[] pts) {
        return (pts[0] - pts[2]) * (pts[0] - pts[2]) + (pts[1] - pts[3]) * (pts[1] - pts[3]);
    }

    private boolean notEqual(double[] l1, double[] l2) {
        return Math.abs(l1[0] - l2[0]) > 10 && Math.abs(l1[2] - l2[2]) > 10;

    }

    private List<Line> getLines(Mat result, Mat colour) {
        Mat lines = new Mat();
        int threshold = 100;
        Imgproc.threshold(result, result, 64, 255, 0);
        Imgproc.HoughLinesP(result, lines, 1, Math.PI / 180, threshold, 100, 500);
        Bitmap bitmap = Bitmap.createBitmap(result.width(), result.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(result, bitmap);

//        return bitmap;
        double max1 = 0, max2 = 0;
        int best1 = 0, best2 = 0;
        for (int i = 0; i < lines.height(); i++) {
            double[] pts = lines.get(i, 0);
            System.out.println(length(pts));
            if (max1 < length(pts)) {
                max1 = length(pts);
                best1 = i;
            }
        }
        for (int i = 0; i < lines.height(); i++) {
            double[] pts = lines.get(i, 0);
            if (i != best1 && max2 < length(pts) && notEqual(lines.get(best1, 0), lines.get(i, 0))) {
                max2 = length(pts);
                best2 = i;
            }
        }

        double[] line1 = new double[4];
        double[] line2 = new double[4];
        System.out.println("max1.....");
        System.out.println(max1);
        System.out.println(max2);
        for (int i = 0; i < 4; i++) {
            line1[i] = lines.get(best1, 0)[i];
            line2[i] = lines.get(best2, 0)[i];
//            ls.put(0,i,lines.get(best1,i));
//            ls.put(1, i, lines.get(best2, i));
        }
        List<Line> res = new ArrayList<Line>();
        res.add(new Line(line1));
        res.add(new Line(line2));
        System.out.println("line1");
        for (double e : line1) {
            System.out.print(e + " ");
        }
        System.out.println();
        System.out.println("line2");
        for (double e : line2) {
            System.out.print(e + " ");
        }
        return res;


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

    private Mat getRow(MatOfPoint mainContour, Bitmap bitmap) {
        Mat imageCV = new Mat();
        Utils.bitmapToMat(bitmap, imageCV);
        System.out.println(mainContour.get(3, 0)[0] + " " + mainContour.get(3, 0)[1]);

        List<Point> bottomPoints = getPoint(mainContour);
        Point max_y = bottomPoints.get(0);
        Point max_x = bottomPoints.get(1);
        System.out.println(max_y);

        Rect rect = new Rect((int) max_y.x - (int) ((max_x.x - max_y.x) * 1), 0, (int) ((max_x.x - max_y.x) * 3), (int) (max_y.y + 10));
        SHIFT_OF_SMALL_CROPPED = (int) max_y.x - (int) ((max_x.x - max_y.x) * 1);
        System.out.println("rect === " + rect);
        System.out.println("heigh" + imageCV.height());
        System.out.println("width" + imageCV.width());
        imageCV = new Mat(imageCV, rect);
        System.out.println();

        return imageCV;
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

    private List<Line> sortLines(List<Line> lines) {
        List<Line> result = new ArrayList<Line>();
        double medX = 0, medY = 0;
        for (Line p : lines) {
            medX += p.getP1().x;
            medY += p.getP1().y;
            medX += p.getP2().x;
            medY += p.getP2().y;
        }
        medX /= (lines.size() * 2);
        medY /= (lines.size() * 2);

        for (Line p : lines) {
            if ((p.getP1().x - medX) < 0 && (p.getP2().x - medX) < 0) {
                result.add(p);
            }
        }

        for (Line p : lines) {
            if ((p.getP1().y - medY) < 0 && (p.getP2().y - medY) < 0) {
                result.add(p);
            }
        }


        for (Line p : lines) {
            if ((p.getP1().x - medX) > 0 && (p.getP2().x - medX) > 0) {
                result.add(p);
            }
        }

        for (Line p : lines) {
            if ((p.getP1().y - medY) > 0 && (p.getP2().y - medY) > 0) {
                result.add(p);
            }
        }


        return result;
    }

    private List<Point> getPoint(MatOfPoint contour) {
        int med = 0;
        for (int i = 0; i < 4; i++) {
            med += contour.get(i, 0)[1];
        }
        med /= 4;
        List<Point> current = new ArrayList<Point>();

        for (int i = 0; i < 4; i++) {
            if (contour.get(i, 0)[1] <= med) {//y
                current.add(new Point((int) contour.get(i, 0)[0], (int) contour.get(i, 0)[1]));
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