package com.compscicenter.aleksart.test2.utils;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sergej on 4/2/16.
 */
public  class Line {
    private Point p1,p2;
    public Line(Line l){
        p1 = new Point(l.getP1().x, l.getP1().y);
        p2 = new Point(l.getP2().x, l.getP2().y);
    }

    public Line(Point p1, Point p2) {
        this.p1 = p1;
        this.p2 = p2;
    }
    public Line (double ... x) {
        p1 = new Point((int)x[0],(int)x[1]);
        p2 = new Point((int)x[2],(int)x[3]);
    }
    public Line(double x1, double y1, double x2, double y2) {
        p1 = new Point((int)x1,(int)y1);
        p2 = new Point((int)x2,(int)y2);
    }
    public void add(Line l) {
        p1.x += l.p1.x;
        p1.y += l.p1.y;
        p2.x += l.p2.x;
        p2.y += l.p2.y;
    }

    public Line add1(Line l) {
        Line t = new Line((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
        t.p1.x += l.p1.x;
        t.p1.y += l.p1.y;
        t.p2.x += l.p2.x;
        t.p2.y += l.p2.y;
        return t;
    }


    public void multiply(int k) {
        p1.x *= k;
        p1.y *= k;
        p2.x *= k;
        p2.y *= k;
    }

    public Line multiply1(int k) {
        Line t = new Line((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
        t.p1.x *= k;
        t.p1.y *= k;
        t.p2.x *= k;
        t.p2.y *= k;
        return t;
    }

    public Line addNum(int k) {
        p1.x += k;
        p2.x += k;
        return this;

    }

    public static List<Line> sortLines(List<Line> lines) {
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

    public Point getP1() {
        return p1;
    }

    public Point getP2() {
        return p2;
    }
}