package com.compscicenter.aleksart.test2;

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
    public static  double EPS_DIST = 30;

    public static double count(Mat img, List<Point> points, Scalar colour, int width) {
        Mat temp = img.clone();

        for (int i = 0; i < points.size(); i++) {
            Imgproc.line(temp, points.get(i), points.get((i + 1) % points.size()), colour, width);
        }
        Scalar response = Core.sumElems(temp);
        temp.release();
        return response.val[0];
    }

    public static double countHorizLine(Mat img, Line line, Scalar colour, int width) {
        Mat temp = img.clone();
        Imgproc.line(temp, line.getP1(), line.getP2(), colour, width);
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
                            System.out.println("Fitting " + m);
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


    public static Mat improvePole(List<Line> lines, Mat img) {
        List<List<Point>> result = new ArrayList<List<Point>>();
        for (int i = 0; i < lines.size(); i++) {
            result.add(new ArrayList<Point>());
        }
        for (int i = 0; i < img.height(); i++) {
            for (int j = 0; j < img.width(); j++) {
                if (img.get(i,j)[0] > 200) {
                    Point cur = new Point(i,j);
                    for (int p = 0; p < lines.size(); p++) {
                        if (dist(lines.get(p),cur) < EPS_DIST) {
                            result.get(p).add(cur);
                        }
                    }
                }
            }
        }
        System.out.println("height = "+ img.height());
        Scalar red = new Scalar(255,0,0);
        List<Line> lines2 = new ArrayList<Line>();
        List<Point> kAndB = new ArrayList<Point>();
        for (int i = 0; i < result.size(); i++) {

            double b = getB(result.get(i), img.height());
            double k = getK(result.get(i), img.height(), b);
            System.out.println("k = "+k);
            System.out.println("b = " + b);
//            Imgproc.line(img, new Point(k * img.height() + b, 0), new Point(b, img.height()), red, 1);
            lines2.add(new Line(k * img.height() + b, 0, b, img.height()));
            kAndB.add(new Point(k,b));
        }

        Mat cur = new Mat(img.rows(),img.cols(),CvType.CV_32F);
        Imgproc.cvtColor(img, cur, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(cur, cur, 120, 255, 0);
        Imgproc.Laplacian(cur, cur, 0);

        Mat kernel = new Mat(5,1, CvType.CV_32F);
        for (int i = 0; i < kernel.height();i++) {
            for (int j = 0; j < kernel.width(); j++) {
                kernel.put(i, j, 1.0 / 5);
            }

        }
        Imgproc.filter2D(cur, cur, CvType.CV_32F, kernel);
        cur.convertTo(cur, CvType.CV_8U);
        Imgproc.threshold(cur, cur, 20, 255, 0);
//        return cur;


        double min = 1e18;
        int best = 0;
        Scalar colour = new Scalar(70);
        for (int i = 10; i < img.height()/2; i++) {
            double count = countHorizLine(cur,new Line(kAndB.get(0).x * (img.height() - i) + kAndB.get(0).y,i,
                    kAndB.get(1).x * (img.height() - i) + kAndB.get(1).y,i),colour, 3);
            if (count < min) {
                min = count;
                best = i;
            }
        }
        Imgproc.line(img,new Point(kAndB.get(0).x * (img.height() - best) + kAndB.get(0).y,best),
        new Point(kAndB.get(1).x * (img.height() - best) + kAndB.get(1).y,best),red,3);

        Imgproc.line(img, new Point(kAndB.get(0).x * (img.height()-best) + kAndB.get(0).y, best), new Point(kAndB.get(0).y, img.height()), red, 3);
        Imgproc.line(img, new Point(kAndB.get(1).x * (img.height()-best) + kAndB.get(1).y, best), new Point(kAndB.get(1).y, img.height()), red, 3);

        return img;

    }

    public static double getB(List<Point> points, int height) {
        double xx = 0, xxy = 0, xy = 0, xxx = 0, x = 0;
        for (Point p: points) {
            xx += (height-p.y)*(height-p.y);
            x += height - p.y;
            xxy += (height-p.y)*(height-p.y) * p.x;
            xxx += (height-p.y)*(height-p.y) * (height-p.y) ;
            xy += (height-p.y) * p.x;
        }
        System.out.println("xxx = "+xxx);
        System.out.println("xx = "+xx);
        System.out.println("xxy = "+xxy);
        return (xxy - (xy / xx) * xxx)*1.0/(xx -(x / xx) *xxx);
    }

    public static double getK(List<Point> points, int height, double b) {
        double xx = 0, xy = 0, x = 0;
        for (Point p: points) {
            xx += (height-p.y)*(height-p.y);
            x += height - p.y;
            xy += (height-p.y) * p.x;
        }
        System.out.println("xy =" + xy);
        return (xy - b * x) * 1.0/xx;

    }

    public static double dist(Line l, Point p) {
        return Math.abs( ((l.getP1().y - l.getP2().y) * p.x + (l.getP2().x - l.getP1().x) * p.y +
                l.getP1().x * l.getP2().y - l.getP2().x * l.getP1().y)
                /Math.sqrt( Math.pow(l.getP1().x - l.getP2().x,2 ) + Math.pow(l.getP1().y - l.getP2().y,2 ) ));
    }
}

