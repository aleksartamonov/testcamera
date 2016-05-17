package com.compscicenter.aleksart.test2.utils;

import org.opencv.core.KeyPoint;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sergej on 5/17/16.
 */
public class Util {
    public static List<Line> pointToLines(List<Point> vPoints) {
        List<Line> lines = new ArrayList<Line>();
        for (int i = 0; i < vPoints.size(); i++) {
            Point p1 = vPoints.get(i), p2 = vPoints.get((i + 1) % vPoints.size());
            lines.add(new Line(p1.x, p1.y, p2.x, p2.y));
        }
        return lines;
    }

    public static List<Point> lineToPoints(List<Line> vLines, int key) {
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

    public static List<Point> getPointFromKeypoint(KeyPoint[] keyPoints) {
        List<Point> result = new ArrayList<Point>();
        for (KeyPoint p : keyPoints) {
            result.add(p.pt);
        }
        return result;
    }

    public static void multiplyLines(List<Line> signLines, int resize) {
        for (int i = 0; i < signLines.size(); i++) {
            signLines.set(i, signLines.get(i).multiply1(resize));
        }
    }

    public static void addNumLines(List<Line> signLines, int num) {
        for (int i = 0; i < signLines.size(); i++) {
            signLines.set(i, signLines.get(i).addNum(num));
        }
    }


}
