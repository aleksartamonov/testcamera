package com.compscicenter.aleksart.test2.utils;

import android.graphics.Bitmap;

import com.compscicenter.aleksart.test2.saver.Saver;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sergej on 5/17/16.
 */
public class Algorithm {
    private static int SHIFT_OF_SMALL_CROPPED;
    private static double SIGN_WIDTH;
    private static double HEIGHT_OF_SIGN = 0.7;
    public static MatOfPoint findCountourSign(Bitmap thumbnail, List<TypePoint> ans, List<Point> points) {
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
        int r = voting(points, egor, ans);
        Line[] lines = new Line[4];
        Mat cur = list.get(r);

        for (int i = 0; i < cur.height(); i++) {
            lines[i] = new Line(cur.get(i, 0)[0], cur.get(i, 0)[1], cur.get((i + 1) % cur.height(), 0)[0], cur.get((i + 1) % cur.height(), 0)[1]);
        }
        Scalar red = new Scalar(255, 0, 0);

        for (Line l : lines) {
            Imgproc.line(imageRES, l.getP1(), l.getP2(), red, 10);
        }

        MatOfPoint result = new MatOfPoint(lines[0].getP1(), lines[1].getP1(), lines[2].getP1(), lines[3].getP1());

        Utils.matToBitmap(imageRES, thumbnail);
        Saver.savePhoto(thumbnail);

        return result;
    }

    public static Mat getRowForHeight(MatOfPoint mainContour, Bitmap bitmap) {
        Mat imageCV = new Mat();
        Utils.bitmapToMat(bitmap, imageCV);
        System.out.println(mainContour.get(3, 0)[0] + " " + mainContour.get(3, 0)[1]);

        List<Point> bottomPoints = getUpPointsSign(mainContour);
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
        Saver.saveMat(imageCV);
        return imageCV;
    }

    public static Mat getRowForWidth(MatOfPoint mainContour, Bitmap bitmap) {
        Mat imageCV = new Mat();
        Utils.bitmapToMat(bitmap, imageCV);
        System.out.println(mainContour.get(3, 0)[0] + " " + mainContour.get(3, 0)[1]);

        List<Point> upPoints = getUpPointsSign(mainContour);
        Point p1 = upPoints.get(0);
        Point p2 = upPoints.get(1);
        SIGN_WIDTH = p2.x - p1.x;
        System.out.println(p1);

        Rect rect = new Rect((int) ((p1.x) * 1.1), 0, (int) ((p2.x - p1.x) * 0.8), (int) (p1.y * 0.9));
        SHIFT_OF_SMALL_CROPPED = (int) p1.x - (int) ((p2.x - p1.x));
        System.out.println("rect === " + rect);
        System.out.println("heigh" + imageCV.height());
        System.out.println("width" + imageCV.width());
        imageCV = new Mat(imageCV, rect);
        System.out.println();

        Saver.saveMat(imageCV);

        return imageCV;
    }

    private static List<Point> getUpPointsSign(MatOfPoint contour) {
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

    private static int voting(List<Point> points, List<MatOfPoint2f> contours, List<TypePoint> ans) {
        int countourRating[] = new int[contours.size()];
        Arrays.fill(countourRating, 0);
        for (int i = 0; i < points.size(); i++) {
            if (ans.get(i) == TypePoint.GOOD_POINT) {
                for (int j = 0; j < contours.size(); j++) {
                    double u = Imgproc.pointPolygonTest(contours.get(j), points.get(i), true);
                    if (u > 0) countourRating[j] += 100;
                    else countourRating[j] -= 1;
                }
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

    public static int getShiftOfSmallCropped() {
        return SHIFT_OF_SMALL_CROPPED;
    }

    public static double getSignWidth() {
        return SIGN_WIDTH;
    }

    public static List<Line> getLinesRow(Mat result, Mat colour) {
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
    private static double length(double[] pts) {
        return (pts[0] - pts[2]) * (pts[0] - pts[2]) + (pts[1] - pts[3]) * (pts[1] - pts[3]);
    }

    private static boolean notEqual(double[] l1, double[] l2) {
        return Math.abs(l1[0] - l2[0]) > 10 && Math.abs(l1[2] - l2[2]) > 10;

    }

    public static double countHeight(List<Line> signLines, List<Line> vLines, double imageHeight ) {
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

}
