package com.compscicenter.aleksart.test2.utils;

import android.graphics.Bitmap;

import com.compscicenter.aleksart.test2.saver.Saver;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sergej on 5/17/16.
 */
public class Algorithm {
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
}
