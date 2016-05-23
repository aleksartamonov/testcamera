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
//        Returns the points of the sign in order downleft -> leftup -> upright -> rightdown
        System.out.println("Finding contour sign1");
        Mat imageCV = new Mat();
        Mat imageRES = new Mat();
        Utils.bitmapToMat(thumbnail, imageCV);
        Utils.bitmapToMat(thumbnail, imageRES);
        Imgproc.cvtColor(imageCV, imageCV, Imgproc.COLOR_RGB2HSV_FULL);
        Imgproc.cvtColor(imageCV, imageCV, Imgproc.COLOR_RGB2GRAY);

        System.out.println("Finding contour sign2");
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.threshold(imageCV, imageCV, 128, 255, 0);
        Imgproc.findContours(imageCV, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        List<MatOfPoint2f> egor = new ArrayList<MatOfPoint2f>();
        List<MatOfPoint> egorCNT = new ArrayList<MatOfPoint>();

        System.out.println("Finding contour sign3");
        for (int i = 0; i < contours.size(); i++) {
            double eps = 0.1 * Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true);
            MatOfPoint2f m = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), m, eps, true);

            if (m.height() == 4) {
                egor.add(m);
                egorCNT.add(new MatOfPoint(contours.get(i).toArray()));
            }
        }
        System.out.println("Finding contour sign4");
        int r = voting(points, egor, ans);
        Mat curCNT = egorCNT.get(r);
        List<List<Line>> directions = getApprox(curCNT);
        List<Line> down = directions.get(0), up = directions.get(1), left = directions.get(2), right = directions.get(3);
        Line dLine = getLineFromList(down), uLine = getLineFromList(up),
                lLine = getLineFromList(left), rLine = getLineFromList(right);
        Scalar red = new Scalar(255, 0, 0);
        Scalar green = new Scalar(0, 255, 0);
        Scalar blue = new Scalar(0, 0, 255);
        Scalar gb = new Scalar(0, 255, 255);
//        for (Line l: down) {
//            Imgproc.line(imageRES, l.getP1(), l.getP2(), red, 1);
//        }
//        for (Line l: up) {
//            Imgproc.line(imageRES, l.getP1(), l.getP2(), green, 1);
//        }
//        for (Line l: left) {
//            Imgproc.line(imageRES, l.getP1(), l.getP2(), blue, 1);
//        }
//        for (Line l: right) {
//            Imgproc.line(imageRES, l.getP1(), l.getP2(), gb, 1);
//        }

        Point downleft = intersectLines(dLine, lLine);
        Point leftup = intersectLines(lLine, uLine);
        Point upright = intersectLines(uLine, rLine);
        Point rightdown = intersectLines(rLine, dLine);

        Imgproc.line(imageRES, downleft, leftup, red, 1);
        Imgproc.line(imageRES, leftup, upright, green, 1);
        Imgproc.line(imageRES, upright, rightdown, blue, 1);
        Imgproc.line(imageRES, rightdown, downleft, gb, 1);


        System.out.println("Finding contour sign5");

        MatOfPoint result = new MatOfPoint(downleft, leftup, upright, rightdown);

        Utils.matToBitmap(imageRES, thumbnail);
        System.out.println("save  contourSight");
        Saver.savePhoto(thumbnail);
        System.out.println("Finding contour sign6");

        return result;
    }

    private static List<List<Line>> getApprox(Mat curCNT) {
        Mat contour = curCNT.clone();
        double min_x = 1000000, max_x = -1, min_y = 1000000, max_y = -1;
        for (int i = 0; i < contour.height();i++) {
            min_x = Math.min(contour.get(i,0)[0], min_x);
            max_x = Math.max(contour.get(i,0)[0], max_x);
            min_y = Math.min(contour.get(i,0)[1], min_y);
            max_y = Math.min(contour.get(i,0)[1], max_y);
        }
        int height = (int)(max_x-min_x), width = (int)(max_y-min_y);
        for (int i = 0; i < contour.height();i++) {double a[] = new double[2];
            a[0] = contour.get(i,0)[0] - min_x;
            a[1] = contour.get(i,0)[1] - min_y;
            contour.put(i, 0, a);
        }
        int cx = height/2, cy = width/2;
        for (int i = 0;i < contour.height();i++) {
            double u = contour.get(i,0)[0], v = contour.get(i,0)[1];
            double a[] = new double[2];
            a[0] = u-v;
            a[1] = u+v;
            contour.put(i,0, a);
        }
        List<Line> down = new ArrayList<Line>();
        List<Line> up = new ArrayList<Line>();
        List<Line> left = new ArrayList<Line>();
        List<Line> right = new ArrayList<Line>();
        List<List<Line>> res = new ArrayList<List<Line>>();
        for (int i = 0;i < contour.height();i++){
            int j = (i+1) % contour.height();
            System.out.println(contour.get(i,0)[0]);
            System.out.println(curCNT.get(i,0)[0]);
            double x1 = contour.get(i,0)[0], y1 = contour.get(i,0)[1], x2 = contour.get(j,0)[0], y2 = contour.get(j,0)[1];
            if ((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2) > 25) {
                if (x1 < x2 && y1 < y2) {
                    down.add(new Line(curCNT.get(i, 0)[0], curCNT.get(i, 0)[1], curCNT.get(j, 0)[0], curCNT.get(j, 0)[1]));
                }
                if (x1 > x2 && y1 > y2) {
                    up.add(new Line(curCNT.get(i, 0)[0], curCNT.get(i, 0)[1], curCNT.get(j, 0)[0], curCNT.get(j, 0)[1]));
                }
                if (x1 > x2 && y1 < y2) {
                    left.add(new Line(curCNT.get(i, 0)[0], curCNT.get(i, 0)[1], curCNT.get(j, 0)[0], curCNT.get(j, 0)[1]));
                }
                if (x1 < x2 && y1 > y2) {
                    right.add(new Line(curCNT.get(i, 0)[0], curCNT.get(i, 0)[1], curCNT.get(j, 0)[0], curCNT.get(j, 0)[1]));
                }
            }
        }
        res.add(down);
        res.add(up);
        res.add(left);
        res.add(right);


        return res;
    }

    private static Point intersectLines(Line a, Line b) {
        double p0_x = a.getP1().x, p0_y = a.getP1().y, p1_x = a.getP2().x, p1_y = a.getP2().y,
            p2_x = b.getP1().x, p2_y = b.getP1().y, p3_x = b.getP2().x, p3_y = b.getP2().y;
        double s1_x, s1_y, s2_x, s2_y;
        s1_x = p1_x - p0_x;     s1_y = p1_y - p0_y;
        s2_x = p3_x - p2_x;     s2_y = p3_y - p2_y;

        double s, t;
        s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
        t = ( s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);
        double ix = p0_x + (t * s1_x), iy = p0_y + (t * s1_y);
        System.out.println("ix = " + ix + " iy = " + iy);
        return new Point(ix, iy);
    }

    private static Line getLineFromList(List<Line> lines) {
        int n = lines.size();
        List<Double> x = new ArrayList<Double>(), y = new ArrayList<Double>();
        for (int i = 0;i < n;i++) {
            x.add(lines.get(i).getP1().x);
            x.add(lines.get(i).getP2().x);
            y.add(lines.get(i).getP1().y);
            y.add(lines.get(i).getP2().y);
            System.out.println(i + " (" + lines.get(i).getP1().x + " " + lines.get(i).getP1().y + ") (" + lines.get(i).getP2().x + " " + lines.get(i).getP2().y + ")");
        }
        double sxy = 0, sx = 0, sy = 0, sxx = 0;
        double minx = 1000000, maxx = 0, miny = 1000000, maxy = 0;
        for (int i = 0;i < 2*n;i++) {
            minx = Math.min(minx, x.get(i));
            miny = Math.min(miny, y.get(i));
            maxx = Math.max(maxx, x.get(i));
            maxy = Math.max(maxy, y.get(i));
            sxy += x.get(i)*y.get(i);
            sx += x.get(i);
            sy += y.get(i);
            sxx += x.get(i)*x.get(i);
        }
        System.out.println(minx + " " + maxx + " " + n + " //////////////iiiiiiiiiiiiiiiiii");
        if (2*n*sxx-sx*sx != 0) {
            double k = 1.0 * (2.0 * n * sxy - sx * sy) / (2.0 * n * sxx - sx * sx);
            double b = 1.0 * (sy - k * sx) / n / 2.0;
            return new Line((int) (minx), (int) (minx * k + b), (int) (maxx), (int) (maxx * k + b));
        }
        else {
            System.out.println("BAD");
            return new Line((int)(sx/n/2.0), (int)(miny),(int)(sx/n/2.0), (int)(maxy));
        }
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
        System.out.println("save  getRowForHeight");
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
        System.out.println("save  getRowForWidth");
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
