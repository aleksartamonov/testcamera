package com.example.aleksart.test2;

import org.opencv.core.Point;

import java.util.List;

/**
 * Created by sergej on 4/2/16.
 */
public  class Line {
    private Point p1,p2;
    Line(Point p1,Point p2) {
        this.p1 = p1;
        this.p2 = p2;
    }
    Line (double ... x) {
        p1 = new Point((int)x[0],(int)x[1]);
        p2 = new Point((int)x[2],(int)x[3]);
    }
    Line(double x1, double y1, double x2, double y2) {
        p1 = new Point((int)x1,(int)y1);
        p2 = new Point((int)x2,(int)y2);
    }
    public Line add(Line l) {
        p1.x += l.p1.x;
        p1.y += l.p1.y;
        p2.x += l.p2.x;
        p2.y += l.p2.y;
        return this;

    }

    public Point getP1() {
        return p1;
    }

    public Point getP2() {
        return p2;
    }
}