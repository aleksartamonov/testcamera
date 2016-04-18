package com.example.aleksart.test2;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by sergej on 4/2/16.
 */
public class Fitting {

    public static double count(Mat img, List<Point> points, Scalar colour, int width) {
        Mat temp = img.clone();

        for (int i = 0; i < points.size(); i++) {
            Imgproc.line(temp, points.get(i), points.get((i + 1) % points.size()), colour, width);
        }
        Scalar response = Core.sumElems(temp);
        temp.release();
        return response.val[0];
    }


    public static Mat improveSign(List<Point> points, Mat img) {

        for (int t = 2; t >= 0; t--) {
            int k = 2;
            int r = (int) Math.pow(2, t);
            Mat thresh = new Mat();
            Imgproc.threshold(img, thresh, 120, 255, 0);
            Imgproc.Laplacian(img, img, 0);
            Imgproc.blur(img, img, new Size(r, r));
            Imgproc.threshold(img, thresh, 1, 255, 0);

            List<Point> directions = new ArrayList<Point>();
            for (int i1 = -k; i1 < k + 1; i1 += 1) {
                for (int i2 = -k; i2 < k + 1; i2 += 1) {
                    directions.add(new Point(i1 * r, i2 * r));
                }
            }

            Scalar colour = new Scalar(70);
            double m = 1e20, res = 1e20;
            while (m > 0) {
                m = 0;
                for (int i = 0; i < points.size(); i++) {
                    System.out.println("Fitting");
                    Point bestPoint = points.get(i);
                    for (Point dir : directions) {
                        Point p = points.get(i);
                        points.set(i, new Point(p.x + dir.x, p.y + dir.y));
                        double temp = count(thresh, points, colour, 1);
                        if (temp < res) {
                            bestPoint = new Point(points.get(i).x, points.get(i).y);
                            m = Math.max(m, res - temp);
                            System.out.println("Fitting " + m) ;
                            res = temp;
                        }
                        points.set(i, new Point(p.x, p.y));
                    }
                    points.set(i, bestPoint);
                }
            }
        }


        return img;

    }


//    public static void improvePole(List<Line> lines, Mat img) {
//
//        Imgproc.threshold(img, img, 120, 255, 0);
//        Imgproc.Laplacian(img, img, 0);
//
//
//        Mat thresh = new Mat();
//        Imgproc.threshold(img, thresh, 192, 255, 0);
////        Imgproc.blur(thresh, thresh, new Size(5, 5));
////        Imgproc.threshold(img, thresh, 32, 255, 0);
//
//        int k = 3;
//        List<Line> directions = new ArrayList<Line>();
//        for (int i1 = -k;i1 < k+1;i1 += 1) {
//            for (int i2 = -k;i2 < k+1;i2 += 1) {
//                directions.add(new Line(i1, 0, i2, 0));
//            }
//        }
//        for (int i1 = -k;i1 < k+1;i1 += 1) {
//            for (int i2 = -k;i2 < k+1;i2 += 1) {
//                directions.add(new Line(0, i1, 0, i2));
//            }
//        }
//        Scalar colour = new Scalar(70);
//        Random random = new Random();
//        for (int i = 0;i < lines.size(); i++) {
//
//            double min = 1e20;
//            Line bestLine = new Line(0,0,0,0);
//            for (int j = 0; j < directions.size(); j++) {
//                if (count(thresh, lines.get(i).add1(directions.get(j)), colour, 1) < min) {
//                    min = count(thresh, lines.get(i).add1(directions.get(j)), colour, 1);
//                    bestLine = lines.get(i).add1(directions.get(j));
//                }
//            }
//            lines.set(i, bestLine);
//        }
//
//
//
//
//
//
//
//
//
////                    double best = count(thresh, lines.get(i),colour,1);
////            double delta = 1;
////            while (delta > 0) {
////                delta = 0;
////                for (int j = 0; j < 20; j++) {
////                    Line direction = directions.get(directions.size());
////                    double resp = count(thresh, lines.get(i).add1(direction),colour,1);
////                    if (resp<best) {
////                        System.out.println("Scalar " + resp);
////                        delta = best - resp;
////                        best = resp;
////                        lines.set(i, lines.get(i).add1(direction));
////                    }
////                }
////                System.out.println(delta);
////            }
////
////        }
//
//    }
}
