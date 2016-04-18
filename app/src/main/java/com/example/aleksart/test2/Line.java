package com.example.aleksart.test2;

import org.opencv.core.Point;

import java.util.List;
import java.util.logging.Level;

/**
 * Created by sergej on 4/2/16.
 */
public  class Line {
    private Point p1,p2;
    Line(Line l){
        p1 = new Point(l.getP1().x, l.getP1().y);
        p2 = new Point(l.getP2().x, l.getP2().y);
    }

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

    public Point getP1() {
        return p1;
    }

    public Point getP2() {
        return p2;
    }
}